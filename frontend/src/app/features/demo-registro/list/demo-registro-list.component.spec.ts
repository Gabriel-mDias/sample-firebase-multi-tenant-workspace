import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, Subject } from 'rxjs';
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
import { Acao, Acoes } from '../../../core/auth/acoes';
import { AuthorizationScope, SessionSnapshot } from '../../../core/auth/auth.model';

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

describe('DemoRegistroListComponent UI e geração', () => {
  const permissions = signal<Acao[]>([]);
  const generation = signal(0);
  const tenant = signal<{ alias: string; name: string } | null>(null);
  let invalidated: Subject<void>;
  let response: Subject<PageResponse<DemoRegistro>>;
  let deletion: Subject<void>;
  let fixture: ComponentFixture<DemoRegistroListComponent>;
  const store = {
    search: vi.fn((): Observable<PageResponse<DemoRegistro>> => response),
    deleteRegistro: vi.fn(() => deletion),
  };
  const alert = {
    error: vi.fn(),
    errorFromApi: vi.fn(),
    success: vi.fn(),
    confirm: vi.fn(() => Promise.resolve({ isConfirmed: true })),
  };
  const navigation = { navigate: vi.fn() };
  const page = (nome: string, number = 0): PageResponse<DemoRegistro> => ({
    content: [{ id: 'same-pk', nome, descricao: 'Descrição', valorRestrito: '999.00' }],
    number,
    size: 10,
    totalElements: 31,
    totalPages: 4,
  });
  const httpError = new HttpErrorResponse({
    status: 400,
    error: { errorType: 'BUSINESS', message: 'Registro indisponível.' },
  });

  beforeEach(() => {
    vi.clearAllMocks();
    generation.set(0);
    tenant.set({ alias: 'alpha', name: 'Alpha' });
    permissions.set([
      Acoes.CONSULTAR_DEMO,
      Acoes.INSERIR_DEMO,
      Acoes.ALTERAR_DEMO,
      Acoes.EXCLUIR_DEMO,
      Acoes.VISUALIZAR_VALOR_RESTRITO,
    ]);
    invalidated = new Subject<void>();
    response = new Subject<PageResponse<DemoRegistro>>();
    deletion = new Subject<void>();
    store.search.mockImplementation(() => response);
    store.deleteRegistro.mockImplementation(() => deletion);
    alert.confirm.mockImplementation(() => Promise.resolve({ isConfirmed: true }));
    TestBed.configureTestingModule({
      imports: [DemoRegistroListComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            invalidated,
            generation,
            currentTenant: tenant,
            capture: (): SessionSnapshot => ({
              generation: generation(),
              session: 1,
              uid: 'user',
              alias: tenant()?.alias ?? null,
              ready: !!tenant(),
            }),
            isCurrent: (snapshot: SessionSnapshot) =>
              snapshot.generation === generation() && snapshot.alias === (tenant()?.alias ?? null),
            isAuthorized: (actions: readonly Acao[], scope: AuthorizationScope) =>
              scope === 'tenant' &&
              !!tenant() &&
              actions.some((action) => permissions().includes(action)),
          },
        },
        { provide: DemoRegistroStore, useValue: store },
        { provide: GemsAlertService, useValue: alert },
        { provide: GemsNavigationService, useValue: navigation },
      ],
    });
  });

  function render(): void {
    fixture = TestBed.createComponent(DemoRegistroListComponent);
    fixture.detectChanges();
  }

  function table(): GemsTableComponent {
    return fixture.debugElement.query(By.directive(GemsTableComponent)).componentInstance;
  }

  function action(name: string): void {
    const element = fixture.debugElement.query(By.directive(GemsTableComponent));
    element.triggerEventHandler('actionClick', {
      action: name,
      row: { id: 'same-pk' },
    });
  }

  function submit(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function invalidate(): void {
    generation.update((value) => value + 1);
    tenant.set(null);
    invalidated.next();
    fixture.detectChanges();
  }

  it.each(['complete', 'error'])(
    'deveLiberarPesquisaEReiniciarPagina_quandoContextoMudaEAntigaTerminaCom%s',
    (ending) => {
      render();
      response.next(page('Alpha', 3));
      response.complete();
      fixture.detectChanges();
      const old = new Subject<PageResponse<DemoRegistro>>();
      store.search.mockReturnValueOnce(old);
      const input: HTMLInputElement = fixture.nativeElement.querySelector('gems-input-text input');
      input.value = 'Alpha';
      input.dispatchEvent(new Event('input'));
      submit();
      expect(table().isLoading()).toBe(true);
      invalidate();
      expect(table().isLoading()).toBe(false);
      expect(table().page()).toBe(0);
      expect(table().totalRecords()).toBe(0);
      expect(table().data()).toEqual([]);
      expect(input.value).toBe('');
      submit();
      expect(store.search).toHaveBeenCalledTimes(2);
      tenant.set({ alias: 'beta', name: 'Beta' });
      const beta = new Subject<PageResponse<DemoRegistro>>();
      store.search.mockReturnValueOnce(beta);
      submit();
      expect(store.search).toHaveBeenLastCalledWith(
        { nome: '' },
        { page: 0, size: 10, sort: ['nome,asc'] },
      );
      old.next(page('Alpha', 3));
      if (ending === 'error') old.error(httpError);
      else old.complete();
      fixture.detectChanges();
      expect(table().isLoading()).toBe(true);
      expect(table().data()).toEqual([]);
      beta.next(page('Beta'));
      beta.complete();
      fixture.detectChanges();
      expect(table().isLoading()).toBe(false);
      expect(
        table()
          .data()
          .map((row) => row['nome']),
      ).toEqual(['Beta']);
      expect(fixture.nativeElement.textContent).toContain('Beta');
      expect(fixture.nativeElement.textContent).not.toContain('Alpha');
      expect(alert.errorFromApi).not.toHaveBeenCalled();
      expect(alert.error).not.toHaveBeenCalled();
    },
  );

  it('deveRenderizarResultadosEPaginacao_quandoPesquisaTemSucesso', () => {
    render();
    expect(table().isLoading()).toBe(true);
    response.next(page('Registro', 2));
    response.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Registro');
    expect(table().page()).toBe(2);
    expect(table().totalRecords()).toBe(31);
    expect(table().isLoading()).toBe(false);
    const next = new Subject<PageResponse<DemoRegistro>>();
    store.search.mockReturnValueOnce(next);
    fixture.debugElement.query(By.directive(GemsTableComponent)).triggerEventHandler('pageChange', {
      page: 3,
    });
    expect(store.search).toHaveBeenLastCalledWith(
      { nome: '' },
      { page: 3, size: 10, sort: ['nome,asc'] },
    );
  });

  it('deveExibirEstadoVazio_quandoPesquisaNaoTemRegistros', () => {
    render();
    response.next({ content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 });
    response.complete();
    fixture.detectChanges();
    expect(table().data()).toEqual([]);
    expect(table().totalRecords()).toBe(0);
    expect(table().isLoading()).toBe(false);
    expect(fixture.nativeElement.querySelectorAll('tbody tr button')).toHaveLength(0);
    expect(fixture.nativeElement.querySelector('gems-table').textContent).toContain('Nenhum');
  });

  it('deveEntregarEnvelopeELiberarPesquisa_quandoErroAtual', () => {
    render();
    response.error(httpError);
    fixture.detectChanges();
    expect(alert.errorFromApi).toHaveBeenCalledExactlyOnceWith(httpError);
    expect(alert.error).not.toHaveBeenCalled();
    expect(table().isLoading()).toBe(false);
    expect(fixture.nativeElement.querySelector('form button').disabled).toBe(false);
    store.search.mockReturnValueOnce(new Subject<PageResponse<DemoRegistro>>());
    submit();
    expect(store.search).toHaveBeenCalledTimes(2);
  });

  it.each([true, false])('deveEspelharCampoRestrito_quandoPermissaoE%s', (allowed) => {
    if (!allowed)
      permissions.set(permissions().filter((item) => item !== Acoes.VISUALIZAR_VALOR_RESTRITO));
    render();
    response.next(page('Registro'));
    response.complete();
    fixture.detectChanges();
    expect(
      table()
        .columns()
        .some((column) => column.field === 'valorRestrito'),
    ).toBe(allowed);
    expect(fixture.nativeElement.textContent.includes('999.00')).toBe(allowed);
  });

  it.each([
    ['view', Acoes.CONSULTAR_DEMO, '/demo-registro/view'],
    ['edit', Acoes.ALTERAR_DEMO, '/demo-registro/form'],
  ] as const)('deveNavegar_quandoAcao%sPermitida', (name, permission, path) => {
    permissions.set([permission]);
    render();
    action(name);
    expect(
      table()
        .actions()
        .map((item) => item.actionName),
    ).toContain(name);
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith([path, 'same-pk']);
  });

  it.each([
    ['view', Acoes.CONSULTAR_DEMO],
    ['edit', Acoes.ALTERAR_DEMO],
    ['delete', Acoes.EXCLUIR_DEMO],
  ] as const)('deveNegarAcao_quando%sSemPermissao', (name, permission) => {
    permissions.update((items) => items.filter((item) => item !== permission));
    render();
    expect(
      table()
        .actions()
        .map((item) => item.actionName),
    ).not.toContain(name);
    action(name);
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(alert.confirm).not.toHaveBeenCalled();
    expect(store.deleteRegistro).not.toHaveBeenCalled();
  });

  it.each([true, false])('deveEspelharCriacao_quandoPermissaoE%s', (allowed) => {
    if (!allowed)
      permissions.update((items) => items.filter((item) => item !== Acoes.INSERIR_DEMO));
    render();
    const button: HTMLButtonElement | null = fixture.nativeElement.querySelector('.btn-novo');
    expect(!!button).toBe(allowed);
    button?.click();
    expect(navigation.navigate).toHaveBeenCalledTimes(allowed ? 1 : 0);
    if (allowed) expect(navigation.navigate).toHaveBeenCalledWith(['/demo-registro/form']);
  });

  it('deveCancelarDelete_quandoConfirmacaoNegada', async () => {
    alert.confirm.mockResolvedValueOnce({ isConfirmed: false });
    render();
    action('delete');
    await Promise.resolve();
    expect(alert.confirm).toHaveBeenCalledTimes(1);
    expect(store.deleteRegistro).not.toHaveBeenCalled();
    expect(alert.success).not.toHaveBeenCalled();
  });

  it('deveIgnorarConfirmacaoAntiga_quandoEmpresaMudaAntesDeConfirmar', async () => {
    let confirm!: (value: { isConfirmed: boolean }) => void;
    alert.confirm.mockReturnValueOnce(new Promise((resolve) => (confirm = resolve)));
    render();
    action('delete');
    invalidate();
    tenant.set({ alias: 'beta', name: 'Beta' });
    confirm({ isConfirmed: true });
    await Promise.resolve();
    expect(store.deleteRegistro).not.toHaveBeenCalled();
    expect(alert.success).not.toHaveBeenCalled();
    expect(navigation.navigate).not.toHaveBeenCalled();
  });

  it('deveExcluirEEmitirUmSucesso_quandoConfirmacaoAtual', async () => {
    render();
    response.next(page('Registro', 2));
    response.complete();
    store.search.mockReturnValueOnce(of(page('Restante', 2)));
    action('delete');
    await Promise.resolve();
    expect(alert.confirm).toHaveBeenCalledTimes(1);
    expect(store.deleteRegistro).toHaveBeenCalledExactlyOnceWith('same-pk');
    deletion.next();
    deletion.complete();
    fixture.detectChanges();
    expect(alert.success).toHaveBeenCalledExactlyOnceWith(
      'Sucesso',
      'Registro excluído com sucesso.',
    );
    expect(store.search).toHaveBeenLastCalledWith(
      { nome: '' },
      { page: 2, size: 10, sort: ['nome,asc'] },
    );
    expect(fixture.nativeElement.textContent).toContain('Restante');
  });

  it.each(['current', 'stale-success', 'stale-error'])(
    'deveRespeitarGeracaoNoFeedback_quandoDeleteTerminaEm%s',
    async (ending) => {
      render();
      action('delete');
      await Promise.resolve();
      expect(store.deleteRegistro).toHaveBeenCalledWith('same-pk');
      if (ending !== 'current') {
        invalidate();
        tenant.set({ alias: 'beta', name: 'Beta' });
      }
      if (ending === 'stale-success') {
        deletion.next();
        deletion.complete();
      } else deletion.error(httpError);
      expect(alert.errorFromApi).toHaveBeenCalledTimes(ending === 'current' ? 1 : 0);
      if (ending === 'current') expect(alert.errorFromApi).toHaveBeenCalledWith(httpError);
      expect(alert.success).not.toHaveBeenCalled();
      expect(alert.error).not.toHaveBeenCalled();
      expect(navigation.navigate).not.toHaveBeenCalled();
      expect(store.search).toHaveBeenCalledTimes(1);
    },
  );
});
