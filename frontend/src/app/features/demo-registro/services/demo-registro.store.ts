import { Injectable } from '@angular/core';
import { GemsBaseStore, GemsPageable } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { DemoRegistro } from '../models/demo-registro.model';
import { DemoRegistroFilter } from '../models/demo-registro-filter.model';
import { PageResponse } from '../../../shared/models/page-response';
@Injectable({ providedIn: 'root' })
export class DemoRegistroStore extends GemsBaseStore {
  constructor() {
    super('api/demo-registros');
  }

  getById(id: string): Observable<DemoRegistro> {
    return this.get<DemoRegistro>(id);
  }

  search(
    filter: DemoRegistroFilter,
    pageable: GemsPageable,
  ): Observable<PageResponse<DemoRegistro>> {
    return this.post<PageResponse<DemoRegistro>, DemoRegistroFilter>('search', filter, {
      pageable,
    });
  }

  create(body: DemoRegistro): Observable<DemoRegistro> {
    return this.post<DemoRegistro, DemoRegistro>('', body);
  }

  update(id: string, body: DemoRegistro): Observable<DemoRegistro> {
    return this.put<DemoRegistro, DemoRegistro>(id, body);
  }

  deleteRegistro(id: string): Observable<void> {
    return this.delete<void>(id);
  }
}
