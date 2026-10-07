import { Injectable } from '@angular/core';
import { GemsBaseStore } from '@gabriel-mdias/angular-gems-sdk';
import { Observable } from 'rxjs';
import { EstadoModel } from './models/estado.model';
import { PaisModel } from './models/pais.model';

/**
 * Dados de referência que alimentam os selects do wizard e dos filtros (FR-007).
 *
 * Somente leitura: país e estado são carregados por seed no backend e não têm manutenção pela UI.
 * Vive em `core/` (e não numa feature específica) porque é consumida por mais de um módulo de
 * negócio que tenha endereço — hoje `parceiro`.
 */
@Injectable({ providedIn: 'root' })
export class LocalidadeStore extends GemsBaseStore {
  constructor() {
    super('api/localidades');
  }

  listarPaises(): Observable<PaisModel[]> {
    return this.get<PaisModel[]>('/paises');
  }

  listarEstados(paisId: string): Observable<EstadoModel[]> {
    return this.get<EstadoModel[]>(`/paises/${paisId}/estados`);
  }
}
