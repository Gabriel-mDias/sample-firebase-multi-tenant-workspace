/**
 * Recorte do `Page<T>` do Spring que as listagens consomem.
 *
 * <p>Vive em `shared/models/` porque atravessa **todas** as features e o próprio `shared/`: até a rodada
 * 014 ele morava em `features/parceiro/models/parceiro-response.model.ts` e era importado por
 * seis features e pela `ArquivoStore` — uma dependência de `shared/` para `features/`, na direção
 * contrária à que os AGENTS.md estabelecem. Nada aqui é de feature nenhuma; é o contrato de página
 * do Spring, e nenhuma feature é dona dele.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
