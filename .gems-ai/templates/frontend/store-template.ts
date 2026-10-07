// origem: local; precedencia: local-vence; versao: 2.0.0
import { Injectable } from '@angular/core';
import { GemsBaseStore, Pageable } from 'gems-sdk';
import { Observable } from 'rxjs';
import { {Entidade}Model } from '../models/{entidade}.model';
import { {Entidade}FilterModel } from '../models/{entidade}-filter.model';

@Injectable({
  providedIn: 'root'
})
export class {Entidade}Store extends GemsBaseStore {

  constructor() {
    super('api/{recurso}');   // Path base sem barra inicial
  }

  getAll(): Observable<{Entidade}Model[]> {
    return this.get<{Entidade}Model[]>('');
  }

  getById(id: string): Observable<{Entidade}Model> {
    return this.get<{Entidade}Model>(`/${id}`);
  }

  search(filter: {Entidade}FilterModel, pageable: Pageable): Observable<{ content: {Entidade}Model[]; totalElements: number }> {
    return this.post<{ content: {Entidade}Model[]; totalElements: number }, {Entidade}FilterModel>(
      '/search',
      filter,
      { pageable }
    );
  }

  create(dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.post<{Entidade}Model, {Entidade}Model>('', dto);
  }

  update(id: string, dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.put<{Entidade}Model, {Entidade}Model>(`/${id}`, dto);
  }

  delete{Entidade}(id: string): Observable<void> {
    return this.delete<void>(`/${id}`);
  }
}
