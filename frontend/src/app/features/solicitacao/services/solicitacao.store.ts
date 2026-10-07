import { Injectable } from '@angular/core';
import { GemsBaseStore, GemsPageable } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../shared/models/page-response';
import {
  SolicitacaoFilterModel,
  SolicitacaoModel,
  SolicitacaoResponseModel,
} from '../models/solicitacao.model';

/**
 * Acesso HTTP ao recurso de solicitações.
 *
 * <p>Repare que **não existe** um método que grave a situação. Cada transição é uma chamada
 * própria, contra o endpoint de ação correspondente — é o espelho exato do contrato do backend, e
 * é o que impede alguém de "só mandar a situação no PUT" a partir daqui.
 */
@Injectable({ providedIn: 'root' })
export class SolicitacaoStore extends GemsBaseStore {
  constructor() {
    super('api/solicitacoes');
  }

  getById(id: string): Observable<SolicitacaoModel> {
    return this.get<SolicitacaoModel>(`/${id}`);
  }

  search(
    filter: SolicitacaoFilterModel,
    pageable: GemsPageable,
  ): Observable<PageResponse<SolicitacaoResponseModel>> {
    return this.post<PageResponse<SolicitacaoResponseModel>, SolicitacaoFilterModel>(
      '/search',
      filter,
      { pageable },
    );
  }

  create(dto: SolicitacaoModel): Observable<SolicitacaoModel> {
    return this.post<SolicitacaoModel, SolicitacaoModel>('', dto);
  }

  /** Altera apenas título e descrição. A situação do corpo é ignorada pelo backend. */
  update(id: string, dto: SolicitacaoModel): Observable<SolicitacaoModel> {
    return this.put<SolicitacaoModel, SolicitacaoModel>(`/${id}`, dto);
  }

  deleteSolicitacao(id: string): Observable<void> {
    return this.delete<void>(`/${id}`);
  }

  // ---- Transições ---------------------------------------------------------------------------

  abrir(id: string): Observable<SolicitacaoModel> {
    return this.post<SolicitacaoModel, void>(`/${id}/abrir`, undefined as never);
  }

  iniciar(id: string): Observable<SolicitacaoModel> {
    return this.post<SolicitacaoModel, void>(`/${id}/iniciar`, undefined as never);
  }

  concluir(id: string): Observable<SolicitacaoModel> {
    return this.post<SolicitacaoModel, void>(`/${id}/concluir`, undefined as never);
  }

  /** O motivo é obrigatório; o backend recusa o cancelamento sem ele. */
  cancelar(id: string, motivo: string): Observable<SolicitacaoModel> {
    return this.post<SolicitacaoModel, { motivo: string }>(`/${id}/cancelar`, { motivo });
  }
}
