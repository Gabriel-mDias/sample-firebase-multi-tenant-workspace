import { Injectable } from '@angular/core';
import { GemsBaseStore, GemsPageable } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../shared/models/page-response';
import { ParceiroFilterModel } from '../models/parceiro-filter.model';
import { ParceiroModel, ParceiroResponseModel } from '../models/parceiro.model';

/**
 * Acesso HTTP ao recurso de parceiros. Componentes nunca usam `HttpClient` direto — passam por
 * aqui (Princípio VI).
 *
 * O path vai **sem barra inicial**: o `GemsBaseStore` monta a URL final concatenando
 * `baseUrl` + `controllerPath` + endpoint.
 */
@Injectable({ providedIn: 'root' })
export class ParceiroStore extends GemsBaseStore {
  constructor() {
    super('api/parceiros');
  }

  getById(id: string): Observable<ParceiroModel> {
    return this.get<ParceiroModel>(`/${id}`);
  }

  /** Busca paginada é POST /search com o filtro no corpo e a paginação em query params. */
  search(
    filter: ParceiroFilterModel,
    pageable: GemsPageable,
  ): Observable<PageResponse<ParceiroResponseModel>> {
    return this.post<PageResponse<ParceiroResponseModel>, ParceiroFilterModel>('/search', filter, {
      pageable,
    });
  }

  create(dto: ParceiroModel): Observable<ParceiroModel> {
    return this.post<ParceiroModel, ParceiroModel>('', dto);
  }

  update(id: string, dto: ParceiroModel): Observable<ParceiroModel> {
    return this.put<ParceiroModel, ParceiroModel>(`/${id}`, dto);
  }

  /**
   * Não pode se chamar `delete`: colidiria com o `protected delete<T>()` do `GemsBaseStore`, que é
   * justamente o método usado aqui dentro.
   */
  deleteParceiro(id: string): Observable<void> {
    return this.delete<void>(`/${id}`);
  }
}
