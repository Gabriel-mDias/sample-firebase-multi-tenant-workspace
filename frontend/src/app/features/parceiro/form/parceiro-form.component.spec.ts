import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { GemsAlertService } from '@gabriel-mdias/angular-gems-sdk';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { ParceiroStore } from '../services/parceiro.store';
import { ParceiroFormComponent } from './parceiro-form.component';

describe('ParceiroFormComponent', () => {
  it('deveConsultarAcaoDeComissao_quandoCriarComponente', () => {
    const isAuthorized = vi.fn(() => false);
    TestBed.configureTestingModule({
      imports: [ParceiroFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: {}, paramMap: { get: () => null } } },
        },
        { provide: AuthService, useValue: { isAuthorized } },
        { provide: ParceiroStore, useValue: {} },
        { provide: GemsAlertService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });

    const component = TestBed.createComponent(ParceiroFormComponent).componentInstance;
    component.ngOnInit();

    expect(isAuthorized).toHaveBeenCalledWith({
      scope: 'global',
      actions: ['VISUALIZAR_COMISSAO'],
    });
  });
});
