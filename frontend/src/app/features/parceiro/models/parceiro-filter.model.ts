/** Corpo de `POST /api/parceiros/search`. Campo ausente não filtra. */
export interface ParceiroFilterModel {
  id?: string;
  nome?: string;
  cpf?: string;
  estadoId?: string;
}
