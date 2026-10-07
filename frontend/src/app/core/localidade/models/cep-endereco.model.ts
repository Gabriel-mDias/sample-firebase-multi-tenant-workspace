import { EstadoModel } from './estado.model';
import { PaisModel } from './pais.model';

/**
 * Resposta da consulta por CEP: um endereço **parcial**.
 *
 * `numero` e `complemento` nunca vêm — são do endereço, não do CEP. `estado` pode vir nulo quando
 * a UF não está no cadastro de referência, e nesse caso o resto do endereço continua válido.
 */
export interface CepEnderecoModel {
  pais: PaisModel;
  estado: EstadoModel | null;
  cep: string;
  cidade: string;
  bairro: string;
  logradouro: string;
}
