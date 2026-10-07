import { Injectable } from '@angular/core';
import { GemsBaseStore } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { CepEnderecoModel } from './models/cep-endereco.model';

/**
 * Consulta de endereço por CEP.
 *
 * Vive em `core/` e não numa feature: é consumida por todos os cadastros que coletam endereço —
 * hoje o parceiro. Store separada da `LocalidadeStore` porque o recurso é outro
 * (`api/enderecos`, não `api/localidades`).
 *
 * O backend faz o de-para da UF para o estado do cadastro de referência; a tela só aplica o
 * `patchValue`.
 */
@Injectable({ providedIn: 'root' })
export class CepStore extends GemsBaseStore {
  constructor() {
    super('api/enderecos');
  }

  buscarPorCep(cep: string): Observable<CepEnderecoModel> {
    return this.get<CepEnderecoModel>(`/cep/${cep}`);
  }
}
