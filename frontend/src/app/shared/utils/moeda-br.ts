/**
 * Conversão entre o texto que o operador digita e o número que o backend recebe.
 *
 * <p>Vive em `shared/utils/` porque é função pura, e porque valor monetário não é exclusividade do
 * toda tela que exibe dinheiro. O primeiro cadastro com valor monetário já existe, e o próximo
 * com preço encontraria a regra pronta. A SDK **não tem** máscara de moeda (`GemsMaskType` cobre
 * só `cep`/`phone`/`rg`/`email`), então a conversão é responsabilidade do projeto.
 */

/**
 * Lê o valor digitado, aceitando tanto `1.234,56` quanto `1234.56`.
 *
 * <p>Os dois formatos são aceitos de propósito: o operador digita como está acostumado, e obrigá-lo
 * a um só seria rigor sem ganho. Texto que não vira número resulta em `null` — o campo é opcional,
 * e um valor ilegível é ausência de valor, não zero.
 *
 * <h2>Como o separador decimal é decidido</h2>
 * <ul>
 *   <li><b>Ponto e vírgula juntos</b> — o que aparece por último é o decimal; o outro é milhar.
 *       `1.234,56` e `1,234.56` resolvem os dois para 1234.56.</li>
 *   <li><b>Só vírgula</b> — é o decimal. É o formato brasileiro.</li>
 *   <li><b>Só ponto</b> — é ambíguo, e a desambiguação é pelo número de casas: exatamente três
 *       dígitos depois do único ponto significam milhar (`2.500` é dois mil e quinhentos, não dois
 *       e meio); qualquer outra quantidade significa decimal (`1234.56`).</li>
 * </ul>
 *
 * <p><b>Cuidado com o defeito de 100×.</b> Uma conversão que remova <i>todos</i> os
 * pontos antes de trocar a vírgula, de modo que `1234.56` — a forma que o teclado numérico produz —
 * virava <b>123456</b>. O valor gravado era plausível, cem vezes maior, e não havia nada na tela
 * que denunciasse. O Javadoc já prometia aceitar os dois formatos; era o código que não cumpria.
 */
export function parseMoedaBr(texto?: string | null): number | null {
  if (!texto?.trim()) {
    return null;
  }

  const limpo = texto.trim().replace(/\s/g, '');
  const ultimoPonto = limpo.lastIndexOf('.');
  const ultimaVirgula = limpo.lastIndexOf(',');

  let normalizado: string;

  if (ultimoPonto >= 0 && ultimaVirgula >= 0) {
    // Os dois presentes: o que vem depois é o decimal, o outro é milhar.
    normalizado =
      ultimoPonto > ultimaVirgula
        ? limpo.split(',').join('')
        : limpo.split('.').join('').replace(',', '.');
  } else if (ultimoPonto >= 0 || ultimaVirgula >= 0) {
    // Um separador só, repetido ou não. Ele é milhar quando aparece mais de uma vez, ou quando é
    // único e deixa exatamente três dígitos atrás de si — `2.500` é dois mil e quinhentos.
    const separador = ultimoPonto >= 0 ? '.' : ',';
    const posicao = Math.max(ultimoPonto, ultimaVirgula);
    const repetido = limpo.indexOf(separador) !== posicao;
    const casas = limpo.length - posicao - 1;
    const ehMilhar = repetido || casas === 3;

    normalizado = ehMilhar ? limpo.split(separador).join('') : limpo.replace(separador, '.');
  } else {
    normalizado = limpo;
  }

  const numero = Number(normalizado);

  return Number.isFinite(numero) ? numero : null;
}

/** Formata para exibição em pt-BR, sempre com duas casas. */
export function formatarMoedaBr(valor?: number | null): string {
  if (valor == null) {
    return '';
  }

  return valor.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
