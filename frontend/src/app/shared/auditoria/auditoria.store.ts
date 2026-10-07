import { Injectable } from '@angular/core';
import { GemsBaseStore, GemsPageable } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/page-response';
import { EntidadeAuditada, RegistroAuditoriaModel } from './auditoria.model';

/**
 * Acesso HTTP à trilha de auditoria.
 *
 * ## Por que vive em `shared/`, e não numa feature
 * Porque ela não tem feature. O histórico é consumido pelas **seis** telas de visualização já
 * entregues, e uma store por tela
 * significaria seis cópias do contrato de `/api/auditoria`, que divergiriam no primeiro endpoint
 * acrescentado.
 *
 * ## Só há leitura aqui, e a ausência é o requisito
 * Nenhum método de escrita, porque não há endpoint de escrita: quem grava a trilha é o mecanismo de
 * persistência do backend, e a API não expõe forma de forjá-la.
 */
@Injectable({ providedIn: 'root' })
export class AuditoriaStore extends GemsBaseStore {
  constructor() {
    super('api/auditoria');
  }

  /**
   * O histórico de um registro, do mais recente ao mais antigo.
   *
   * Página vazia é resposta legítima — um registro nunca alterado tem apenas o evento de cadastro.
   * O que **não** é legítimo é nome de entidade desconhecido: aí o backend devolve 400, de
   * propósito, porque página vazia esconderia o erro de digitação.
   */
  historico(
    entidade: EntidadeAuditada,
    id: string,
    pageable: GemsPageable = { page: 0, size: 20 },
  ): Observable<PageResponse<RegistroAuditoriaModel>> {
    return this.get<PageResponse<RegistroAuditoriaModel>>(`/${entidade}/${id}`, { pageable });
  }
}
