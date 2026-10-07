/**
 * Formatação de RG compartilhada por todos os módulos que editam identidade civil.
 *
 * <p>O RG não tem formato nacional único: há emissores que usam somente dígitos (SP,
 * `12.345.678-9`), emissores que prefixam a sigla da UF (MG, `MG-12.345.678`) e dígitos
 * verificadores alfabéticos (`12.345.678-X`). Por isso a máscara aqui é **não destrutiva**:
 * ela agrupa e pontua o que reconhece e devolve o resto intacto, nunca descartando caractere
 * digitado — ao contrário do `maskType="rg"` da SDK, que remove letras e trunca em 9 dígitos.
 *
 * <p>Como `Pessoa` é compartilhada entre Parceiro, Usuário e todo cadastro que um projeto
 * derivado acrescentar, um RG gravado por um módulo é reaberto por outro: qualquer perda de
 * caractere aqui corromperia o cadastro alheio.
 */

/** Máximo de caracteres crus. Garante que o valor formatado nunca ultrapasse 20 caracteres. */
const MAX_CARACTERES_CRUS = 14;

/** Sigla de UF: duas letras iniciais seguidas de dígito. */
const PREFIXO_UF = /^([A-Z]{2})(?=\d)/;

const SOMENTE_DIGITOS = /^\d+$/;

/** Reduz a entrada ao conteúdo significativo: maiúsculas, sem pontuação, dentro do limite. */
export function sanitizeRg(value?: string | null): string {
  if (!value) {
    return '';
  }
  return value
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, MAX_CARACTERES_CRUS);
}

/**
 * Aplica a pontuação de exibição. É idempotente: `formatRg(formatRg(x)) === formatRg(x)`,
 * o que permite chamá-la tanto a cada tecla quanto ao carregar um valor já formatado.
 */
export function formatRg(value?: string | null): string {
  const raw = sanitizeRg(value);
  if (!raw) {
    return '';
  }

  const prefixo = PREFIXO_UF.exec(raw)?.[1] ?? '';
  const resto = raw.slice(prefixo.length);

  const { corpo, dv } = separarDigitoVerificador(resto, !!prefixo);
  const corpoFormatado = SOMENTE_DIGITOS.test(corpo) ? agruparEmMilhares(corpo) : corpo;

  return (prefixo ? `${prefixo}-` : '') + corpoFormatado + (dv ? `-${dv}` : '');
}

/**
 * O último caractere vira dígito verificador quando é uma letra (o `X` paulista) ou quando
 * o RG é só de dígitos e longo o bastante para tê-lo. Com sigla de UF não se extrai DV: o
 * formato mineiro (`MG-12.345.678`) não tem separador final.
 */
function separarDigitoVerificador(
  resto: string,
  temPrefixo: boolean,
): { corpo: string; dv: string } {
  const semUltimo = resto.slice(0, -1);
  if (resto.length < 2 || !SOMENTE_DIGITOS.test(semUltimo)) {
    return { corpo: resto, dv: '' };
  }

  const ultimo = resto.at(-1) ?? '';
  const ultimoEhLetra = !SOMENTE_DIGITOS.test(ultimo);
  if (ultimoEhLetra || (!temPrefixo && resto.length >= 8)) {
    return { corpo: semUltimo, dv: ultimo };
  }
  return { corpo: resto, dv: '' };
}

/** Agrupa da direita para a esquerda de três em três, como o RG é lido em voz alta. */
function agruparEmMilhares(digitos: string): string {
  return digitos.replace(/\B(?=(\d{3})+$)/g, '.');
}
