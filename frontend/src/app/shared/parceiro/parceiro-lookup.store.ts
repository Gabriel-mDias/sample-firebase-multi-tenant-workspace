import { Injectable } from '@angular/core';
import { GemsBaseStore, GemsPageable } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/page-response';
import { ParceiroLookupFilterModel, ParceiroLookupModel } from './parceiro-lookup.model';

/** Consulta compartilhada de parceiros para telas que precisam apenas de id e nome. */
@Injectable({ providedIn: 'root' })
export class ParceiroLookupStore extends GemsBaseStore {
  constructor() {
    super('api/parceiros');
  }

  search(
    filter: ParceiroLookupFilterModel,
    pageable: GemsPageable,
  ): Observable<PageResponse<ParceiroLookupModel>> {
    return this.post<PageResponse<ParceiroLookupModel>, ParceiroLookupFilterModel>(
      '/search',
      filter,
      { pageable },
    );
  }
}
