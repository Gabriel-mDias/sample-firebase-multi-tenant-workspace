import { NO_ERRORS_SCHEMA, Signal, WritableSignal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { GemsAlertService, provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { ParceiroLookupStore } from '../../../shared/parceiro/parceiro-lookup.store';
import { SolicitacaoModel } from '../models/solicitacao.model';
import { SolicitacaoStore } from '../services/solicitacao.store';
import { SolicitacaoFormComponent } from './solicitacao-form.component';

describe('SolicitacaoFormComponent', () => {
  it('deveConsultarAcaoDeConcluir_quandoSolicitacaoEstaEmAndamento', () => {
    const isAuthorized = vi.fn(() => true);
    TestBed.configureTestingModule({
      imports: [SolicitacaoFormComponent],
      providers: [
        FormBuilder,
        provideHttpClient(),
        provideGemsHttp('http://localhost/api'),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: {}, paramMap: { get: () => null } } },
        },
        { provide: AuthService, useValue: { isAuthorized } },
        { provide: ParceiroLookupStore, useValue: { search: vi.fn(() => of({ content: [] })) } },
        { provide: SolicitacaoStore, useValue: {} },
        { provide: GemsAlertService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });
    const component = TestBed.createComponent(SolicitacaoFormComponent)
      .componentInstance as unknown as {
      solicitacao: WritableSignal<SolicitacaoModel | null>;
      podeConcluir: Signal<boolean>;
    };
    component.solicitacao.set({ titulo: 'Solicitacao', situacao: 'EM_ANDAMENTO' });

    expect(component.podeConcluir()).toBe(true);
    expect(isAuthorized).toHaveBeenCalledWith({
      scope: 'global',
      actions: ['CONCLUIR_SOLICITACAO'],
    });
  });

  it('deveCarregarOpcoesDeParceiro_quandoInicializar', () => {
    const search = vi.fn(() =>
      of({
        content: [
          { id: 'parceiro-1', nome: 'Acme' },
          { id: 'parceiro-2', nome: 'Beta' },
        ],
      }),
    );
    TestBed.configureTestingModule({
      imports: [SolicitacaoFormComponent],
      providers: [
        FormBuilder,
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: {}, paramMap: { get: () => null } } },
        },
        { provide: AuthService, useValue: { isAuthorized: () => false } },
        { provide: ParceiroLookupStore, useValue: { search } },
        { provide: SolicitacaoStore, useValue: {} },
        { provide: GemsAlertService, useValue: { errorFromApi: vi.fn() } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });
    const fixture = TestBed.createComponent(SolicitacaoFormComponent);

    fixture.detectChanges();

    expect(search).toHaveBeenCalledWith({}, { page: 0, size: 100, sort: ['nome,asc'] });
    expect(
      (
        fixture.componentInstance as unknown as {
          parceiros: Signal<Array<{ label: string; value: string }>>;
        }
      ).parceiros(),
    ).toEqual([
      { label: 'Acme', value: 'parceiro-1' },
      { label: 'Beta', value: 'parceiro-2' },
    ]);
  });
});
