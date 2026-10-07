/**
 * Formatação de data para leitura, em pt-BR.
 *
 * ## Por que uma função, e não um `DatePipe`
 * A `gems-table` **não projeta template por célula**: qualquer formatação precisa acontecer antes,
 * ao montar a linha no componente — e ali não há pipe. Como as telas de acervo também exibem data
 * fora de tabela, uma função pura serve aos dois casos.
 *
 * ## Por que em `shared/utils/`
 * Esta função já existiu em três cópias privadas, uma por tela que formatava data.
 * `shared/utils/` é onde vive constante
 * e função pura — e três cópias é o número a partir do qual elas começam a divergir.
 *
 * ## Sobre o corte da string
 * A entrada é sempre ISO vinda do backend (`2026-08-27` ou `2026-08-27T10:00:00`), e o corte em 10
 * caracteres é o que faz as duas formas funcionarem. **Não** se usa `new Date(valor)` de propósito:
 * `new Date('2026-08-27')` é interpretado como UTC e, em fuso negativo, exibe o dia anterior.
 */
export function dataBr(valor?: string | null): string | null {
  if (!valor) {
    return null;
  }

  const [ano, mes, dia] = valor.slice(0, 10).split('-');

  if (!ano || !mes || !dia) {
    return null;
  }

  return `${dia}/${mes}/${ano}`;
}

/** Mesma formatação, com um texto no lugar da data ausente. Padrão do projeto para tabela: `—`. */
export function dataBrOu(valor: string | null | undefined, ausente = '—'): string {
  return dataBr(valor) ?? ausente;
}
