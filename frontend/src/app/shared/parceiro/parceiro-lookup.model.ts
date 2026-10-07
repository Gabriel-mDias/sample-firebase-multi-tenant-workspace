/** Recorte compartilhado de parceiro para selects fora da feature de manutenção. */
export interface ParceiroLookupModel {
  id: string;
  nome: string;
}

/** Filtro nominal aceito pela busca paginada de parceiros. */
export interface ParceiroLookupFilterModel {
  nome?: string;
  cpf?: string;
}
