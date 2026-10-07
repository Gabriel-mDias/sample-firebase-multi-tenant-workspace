import { PaisModel } from './pais.model';

/**
 * Espelha `EstadoDTO` do backend.
 *
 * `pais` vem preenchido na leitura de um parceiro (`GET /api/parceiros/{id}`) e **ausente** na
 * listagem de referência (`GET /api/localidades/paises/{paisId}/estados`), onde o país já é o
 * parâmetro da rota.
 */
export interface EstadoModel {
  id: string;
  nome?: string;
  siglaEstado?: string;
  pais?: PaisModel;
}
