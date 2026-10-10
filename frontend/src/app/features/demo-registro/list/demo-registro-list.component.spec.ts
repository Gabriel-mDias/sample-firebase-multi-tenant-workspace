import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import {
  GemsAlertService,
  GemsNavigationService,
  GemsTableComponent,
} from '@gabriel-mdias/angular-gems-sdk';
import { By } from '@angular/platform-browser';
import { DemoRegistroListComponent } from './demo-registro-list.component';
import { DemoRegistroStore } from '../services/demo-registro.store';
import { AuthService } from '../../../core/auth/auth.service';
import { DemoRegistro } from '../models/demo-registro.model';
import { PageResponse } from '../../../shared/models/page-response';

describe('DemoRegistroListComponent', () => {
  it('naoDeveReporAlphaOuMostrarCampo_quandoRespostaAntigaChegaSemAcaoRestrita', () => {
    let generation = 0;
    const invalidated = new Subject<void>();
    const response = new Subject<PageResponse<DemoRegistro>>();
    const auth = {
      invalidated,
      capture: () => ({ generation }),
      isCurrent: (snapshot: { generation: number }) => snapshot.generation === generation,
      currentTenant: () => ({ name: 'Beta' }),
      isAuthorized: (roles: string[]) => roles.every((role) => role === 'CONSULTAR_DEMO'),
    };
    TestBed.configureTestingModule({
      imports: [DemoRegistroListComponent],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: DemoRegistroStore, useValue: { search: () => response } },
        { provide: GemsAlertService, useValue: { error: vi.fn() } },
        { provide: GemsNavigationService, useValue: { navigate: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(DemoRegistroListComponent);
    fixture.detectChanges();
    generation++;
    invalidated.next();
    response.next({
      content: [{ id: 'same-pk', nome: 'Alpha', valorRestrito: '999.00' }],
      number: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
    });
    fixture.detectChanges();
    const table = fixture.debugElement.query(By.directive(GemsTableComponent))
      .componentInstance as GemsTableComponent;
    expect(table.data()).toEqual([]);
    expect(table.columns().map((column) => column.field)).not.toContain('valorRestrito');
    expect(table.actions().map((action) => action.actionName)).toEqual(['view']);
    expect(fixture.nativeElement.textContent).not.toContain('Novo registro');
  });
});
