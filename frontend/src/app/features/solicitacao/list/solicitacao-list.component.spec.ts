import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { GemsAlertService, GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { SolicitacaoStore } from '../services/solicitacao.store';
import { SolicitacaoListComponent } from './solicitacao-list.component';

describe('SolicitacaoListComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('deveConsultarAcaoDeInserir_quandoCriarComponente', () => {
    const isAuthorized = vi.fn(() => false);
    TestBed.configureTestingModule({
      imports: [SolicitacaoListComponent],
      providers: [
        FormBuilder,
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthorized } },
        { provide: SolicitacaoStore, useValue: { search: vi.fn() } },
        { provide: GemsAlertService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });

    TestBed.createComponent(SolicitacaoListComponent);

    expect(isAuthorized).toHaveBeenCalledWith({
      scope: 'global',
      actions: ['INSERIR_SOLICITACAO'],
    });
  });

  it('deveNavegarParaForm_quandoSolicitarNovaSolicitacao', () => {
    const navigate = vi.fn();
    TestBed.configureTestingModule({
      imports: [SolicitacaoListComponent],
      providers: [
        FormBuilder,
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthorized: vi.fn(() => false) } },
        { provide: SolicitacaoStore, useValue: { search: vi.fn() } },
        { provide: GemsAlertService, useValue: {} },
        { provide: GemsNavigationService, useValue: { navigate } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });
    const component = TestBed.createComponent(SolicitacaoListComponent)
      .componentInstance as unknown as { novo(): void };

    component.novo();

    expect(navigate).toHaveBeenCalledWith(['/solicitacoes/form']);
  });
});
