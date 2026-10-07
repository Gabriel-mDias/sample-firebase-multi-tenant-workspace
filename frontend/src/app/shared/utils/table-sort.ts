/** Estado de ordenação compartilhado que a `gems-table` espera receber de volta nos inputs. */
export interface TableSortState {
  field?: string;
  direction: 'asc' | 'desc';
}

/**
 * Lê o `sort` emitido pelo `pageChange` da `gems-table`.
 *
 * A tabela é **controlada**: `sortBy()` consulta os inputs `sortField`/`sortDirection` para
 * decidir a próxima direção e apenas emite o resultado. Se o componente pai não devolver esses
 * inputs, a seta nunca muda de estado **e o alternar asc↔desc nunca acontece** — a direção
 * calculada fica presa em `asc`. Por isso o pai precisa extrair o que foi emitido e reenviar.
 *
 * O formato é o do Spring Data: um array de uma única string `"campo,direcao"`. O campo é o
 * `sortParam` da coluna quando ele existe, e é exatamente esse valor que a tabela compara para
 * escolher qual cabeçalho recebe a seta.
 *
 * @param sort array emitido pela tabela; ausente ou malformado devolve o padrão da tela.
 * @param padrao estado a assumir quando não há ordenação aplicável.
 */
export function parseTableSort(
  sort: string[] | undefined,
  padrao: TableSortState = { field: undefined, direction: 'asc' },
): TableSortState {
  const primeiro = sort?.[0];

  if (!primeiro) {
    return padrao;
  }

  const [field, direction] = primeiro.split(',');

  if (!field) {
    return padrao;
  }

  return { field, direction: direction === 'desc' ? 'desc' : 'asc' };
}
