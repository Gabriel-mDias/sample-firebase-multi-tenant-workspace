import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { DemoRegistroStore } from './demo-registro.store';
import { gemsApiRoot } from '../../../core/auth/firebase.interceptor';
describe('DemoRegistroStore / consumer GemsBaseStore', () => {
  let http: HttpTestingController;
  let store: DemoRegistroStore;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideGemsHttp(gemsApiRoot('https://api.example.test/api')),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(DemoRegistroStore);
  });
  afterEach(() => http.verify());
  it.each([
    '12345678901234567.89',
    '99999999999999999.99',
    '-99999999999999999.99',
    '0.01',
    '0.00',
  ])('deveManterStringDecimalNoRoundtrip_quandoCRUDUsa%s', (decimal) => {
    const body = { nome: 'Decimal', valorRestrito: decimal };
    const saved = { ...body, id: 'exact-pk' };
    store.create(body).subscribe((item) => expect(item.valorRestrito).toBe(decimal));
    let request = http.expectOne('https://api.example.test/api/demo-registros');
    expect(JSON.parse(String(request.request.serializeBody())).valorRestrito).toBe(decimal);
    request.flush(JSON.parse(JSON.stringify(saved)));
    store.getById('exact-pk').subscribe((item) => expect(item.valorRestrito).toBe(decimal));
    request = http.expectOne('https://api.example.test/api/demo-registros/exact-pk');
    request.flush(saved);
    store
      .search({ nome: 'Decimal' }, {})
      .subscribe((page) => expect(page.content[0].valorRestrito).toBe(decimal));
    request = http.expectOne((req) =>
      req.url.startsWith('https://api.example.test/api/demo-registros/search'),
    );
    request.flush({ content: [saved], number: 0, size: 10, totalElements: 1, totalPages: 1 });
    store.update('exact-pk', saved).subscribe((item) => expect(item.valorRestrito).toBe(decimal));
    request = http.expectOne('https://api.example.test/api/demo-registros/exact-pk');
    expect(JSON.parse(String(request.request.serializeBody())).valorRestrito).toBe(decimal);
    request.flush(saved);
  });
  it('devePreservarBodyPaginaEUUID_quandoSDKMontaContrato', () => {
    const body = { nome: 'Exemplo', descricao: 'Descrição', valorRestrito: '12.50' };
    store.create(body).subscribe();
    let request = http.expectOne('https://api.example.test/api/demo-registros');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ ...body, id: 'same-pk' });
    store
      .search({ nome: 'Exemplo' }, { page: 2, size: 10, sort: ['nome,asc', 'id,desc'] })
      .subscribe((page) => expect(page.number).toBe(2));
    request = http.expectOne(
      (req) => req.url === 'https://api.example.test/api/demo-registros/search',
    );
    expect(request.request.params.getAll('sort')).toEqual(['nome,asc', 'id,desc']);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.body).toEqual({ nome: 'Exemplo' });
    request.flush({ content: [], number: 2, size: 10, totalElements: 0, totalPages: 0 });
    store.update('same-pk', { ...body, id: 'same-pk' }).subscribe();
    request = http.expectOne('https://api.example.test/api/demo-registros/same-pk');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.id).toBe('same-pk');
    request.flush(body);
    store.deleteRegistro('same-pk').subscribe();
    request = http.expectOne('https://api.example.test/api/demo-registros/same-pk');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
