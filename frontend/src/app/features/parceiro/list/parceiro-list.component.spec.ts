import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { GemsAlertService, GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { ParceiroStore } from '../services/parceiro.store';
import { ParceiroListComponent } from './parceiro-list.component';

describe('ParceiroListComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('deveConsultarAcaoDeInserir_quandoCriarComponente', () => {
    const isAuthorized = vi.fn(() => false);
    TestBed.configureTestingModule({
      imports: [ParceiroListComponent],
      providers: [
        FormBuilder,
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthorized } },
        { provide: ParceiroStore, useValue: { search: vi.fn() } },
        { provide: GemsAlertService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });

    TestBed.createComponent(ParceiroListComponent);

    expect(isAuthorized).toHaveBeenCalledWith({
      scope: 'global',
      actions: ['INSERIR_PARCEIRO'],
    });
  });

  it('deveNavegarParaForm_quandoSolicitarNovoParceiro', () => {
    const navigate = vi.fn();
    TestBed.configureTestingModule({
      imports: [ParceiroListComponent],
      providers: [
        FormBuilder,
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthorized: vi.fn(() => false) } },
        { provide: ParceiroStore, useValue: { search: vi.fn() } },
        { provide: GemsAlertService, useValue: {} },
        { provide: GemsNavigationService, useValue: { navigate } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });
    const component = TestBed.createComponent(ParceiroListComponent)
      .componentInstance as unknown as { novo(): void };

    component.novo();

    expect(navigate).toHaveBeenCalledWith(['/parceiros/form']);
  });
});
