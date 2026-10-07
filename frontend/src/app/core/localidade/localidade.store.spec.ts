import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { LocalidadeStore } from './localidade.store';

const API = 'http://api.test';

describe('LocalidadeStore', () => {
  let store: LocalidadeStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideGemsHttp(API)],
    });

    store = TestBed.inject(LocalidadeStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deveFazerGetEmPaises_quandoListarPaises', () => {
    store.listarPaises().subscribe();

    const req = http.expectOne(`${API}/api/localidades/paises`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('deveIncluirPaisNaRota_quandoListarEstados', () => {
    store.listarEstados('pais-1').subscribe();

    const req = http.expectOne(`${API}/api/localidades/paises/pais-1/estados`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
