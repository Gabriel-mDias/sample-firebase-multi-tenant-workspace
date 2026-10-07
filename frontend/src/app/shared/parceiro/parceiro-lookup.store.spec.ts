import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { ParceiroLookupStore } from './parceiro-lookup.store';

const API = 'http://api.test';

describe('ParceiroLookupStore', () => {
  let store: ParceiroLookupStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideGemsHttp(API)],
    });

    store = TestBed.inject(ParceiroLookupStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deveBuscarParceirosPaginados_quandoConsultarLookup', () => {
    store.search({ nome: 'Acme' }, { page: 0, size: 100, sort: ['nome,asc'] }).subscribe();

    const req = http.expectOne(`${API}/api/parceiros/search?page=0&size=100&sort=nome,asc`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Acme' });
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 });
  });
});
