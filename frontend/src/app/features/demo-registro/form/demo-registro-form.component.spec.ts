import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { signal } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { GemsAlertService, GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { DemoRegistroFormComponent } from './demo-registro-form.component';
import { DemoRegistroStore } from '../services/demo-registro.store';
import { AuthService } from '../../../core/auth/auth.service';
import { Acao, Acoes } from '../../../core/auth/acoes';
import { AuthorizationScope, SessionSnapshot } from '../../../core/auth/auth.model';
import { DemoRegistro } from '../models/demo-registro.model';
describe('DemoRegistroFormComponent', () => {
  const invalidated = new Subject<void>();
  let generation = 0;
  let restricted = false;
  let routeId: string | null = null;
  const auth = {
    invalidated,
    generation: signal(0),
    capture: () => ({ generation }),
    isCurrent: (s: { generation: number }) => s.generation === generation,
    isAuthorized: (roles: string[]) => restricted || !roles.includes('VISUALIZAR_VALOR_RESTRITO'),
  };
  const store = { create: vi.fn(() => of({ id: 'one' })), update: vi.fn(), getById: vi.fn() };
  const navigation = { navigate: vi.fn() };
  beforeEach(() => {
    generation = 0;
    restricted = false;
    routeId = null;
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      imports: [DemoRegistroFormComponent],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: DemoRegistroStore, useValue: store },
        { provide: GemsNavigationService, useValue: navigation },
        {
          provide: GemsAlertService,
          useValue: { error: vi.fn(), errorFromApi: vi.fn(), success: vi.fn() },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => routeId }, url: [{ path: 'form' }] } },
        },
      ],
    });
  });
  it('deveReenviarDecimalLidoSemArredondar_quandoEditaRegistroNoLimite', () => {
    restricted = true;
    routeId = 'exact-pk';
    store.getById.mockReturnValueOnce(
      of({ id: routeId, nome: 'Decimal', valorRestrito: '12345678901234567.89' }),
    );
    store.update.mockReturnValueOnce(of({ id: routeId }));
    const fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance.form.controls.valorRestrito.value).toBe(
      '12345678901234567.89',
    );
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(store.update).toHaveBeenCalledWith(routeId, {
      id: routeId,
      nome: 'Decimal',
      descricao: '',
      valorRestrito: '12345678901234567.89',
    });
  });
  it.each([150, 151])('deveRespeitarLimiteDoNome_quandoNomePossui%scaracteres', (length) => {
    const fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.nome.setValue('n'.repeat(length));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(fixture.componentInstance.form.valid).toBe(length === 150);
    expect(store.create).toHaveBeenCalledTimes(length === 150 ? 1 : 0);
    expect(
      fixture.nativeElement.querySelector('gems-input-text input').getAttribute('maxlength'),
    ).toBe('150');
  });
  it.each([
    '12345678901234567.89',
    '99999999999999999.99',
    '-99999999999999999.99',
    '0.01',
    '0.00',
  ])('devePreservarDecimalExato_quandoValorValidoE%s', (decimal) => {
    restricted = true;
    const fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ nome: 'Alpha', valorRestrito: decimal });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(store.create).toHaveBeenCalledWith({
      nome: 'Alpha',
      descricao: '',
      valorRestrito: decimal,
    });
  });
  it.each(['100000000000000000.00', '1.001', 'NaN', 'Infinity', '1e3', '1,50'])(
    'naoDeveEnviarDecimalInvalido_quandoValorE%s',
    (decimal) => {
      restricted = true;
      const fixture = TestBed.createComponent(DemoRegistroFormComponent);
      fixture.detectChanges();
      fixture.componentInstance.form.patchValue({ nome: 'Alpha', valorRestrito: decimal });
      fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
      expect(store.create).not.toHaveBeenCalled();
      expect(fixture.componentInstance.form.controls.valorRestrito.hasError('decimal')).toBe(true);
    },
  );
  it('deveCanonizarDecimalSemNumber_quandoValorTemZerosOuUmaCasa', () => {
    restricted = true;
    const fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({
      nome: 'Alpha',
      valorRestrito: ' +00012345678901234567.8 ',
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(store.create).toHaveBeenCalledWith({
      nome: 'Alpha',
      descricao: '',
      valorRestrito: '12345678901234567.80',
    });
  });
  it('naoDeveEnviarValorRestrito_quandoAcaoAusente', () => {
    const fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      nome: 'Alpha',
      descricao: 'Descrição',
      valorRestrito: '123.50',
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(store.create).toHaveBeenCalledWith({ nome: 'Alpha', descricao: 'Descrição' });
    expect(fixture.nativeElement.textContent).not.toContain('Valor restrito');
  });
  it('deveLimparFormulario_quandoContextoInvalidado', () => {
    const fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ nome: 'Alpha' });
    generation++;
    invalidated.next();
    expect(fixture.componentInstance.form.controls.nome.value).toBe('');
  });
  it('naoDeveNavegarOuReporDados_quandoSaveAlphaResolveDepoisDeSwitch', () => {
    const response = new Subject<{ id: string }>();
    store.create.mockReturnValueOnce(response);
    const fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ nome: 'Alpha' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    generation++;
    invalidated.next();
    response.next({ id: 'same-pk' });
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.controls.nome.value).toBe('');
  });
});

describe('DemoRegistroFormComponent UI e geração', () => {
  const permissions = signal<Acao[]>([]);
  const generation = signal(0);
  const ready = signal(true);
  let invalidated: Subject<void>;
  let load: Subject<DemoRegistro>;
  let save: Subject<DemoRegistro>;
  let routeId: string | null;
  let fixture: ComponentFixture<DemoRegistroFormComponent>;
  const store = {
    getById: vi.fn((): Observable<DemoRegistro> => load),
    create: vi.fn((): Observable<DemoRegistro> => save),
    update: vi.fn((): Observable<DemoRegistro> => save),
  };
  const alert = { error: vi.fn(), errorFromApi: vi.fn(), success: vi.fn() };
  const navigation = { navigate: vi.fn() };
  const httpError = new HttpErrorResponse({
    status: 400,
    error: { errorType: 'BUSINESS', message: 'Confira os dados.' },
  });

  beforeEach(() => {
    vi.clearAllMocks();
    routeId = null;
    generation.set(0);
    ready.set(true);
    permissions.set([Acoes.CONSULTAR_DEMO, Acoes.INSERIR_DEMO, Acoes.ALTERAR_DEMO]);
    invalidated = new Subject<void>();
    load = new Subject<DemoRegistro>();
    save = new Subject<DemoRegistro>();
    store.getById.mockImplementation(() => load);
    store.create.mockImplementation(() => save);
    store.update.mockImplementation(() => save);
    TestBed.configureTestingModule({
      imports: [DemoRegistroFormComponent],
      providers: [
        {
          provide: AuthService,
          useValue: {
            invalidated,
            generation,
            capture: (): SessionSnapshot => ({
              generation: generation(),
              session: 1,
              uid: 'user',
              alias: generation() ? 'beta' : 'alpha',
              ready: ready(),
            }),
            isCurrent: (snapshot: SessionSnapshot) => snapshot.generation === generation(),
            isAuthorized: (actions: readonly Acao[], scope: AuthorizationScope) =>
              scope === 'tenant' &&
              ready() &&
              actions.some((action) => permissions().includes(action)),
          },
        },
        { provide: DemoRegistroStore, useValue: store },
        { provide: GemsAlertService, useValue: alert },
        { provide: GemsNavigationService, useValue: navigation },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => routeId }, url: [{ path: 'form' }] } },
        },
      ],
    });
  });

  function render(edit = false): void {
    routeId = edit ? 'same-pk' : null;
    fixture = TestBed.createComponent(DemoRegistroFormComponent);
    fixture.detectChanges();
  }

  function nameInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('gems-input-text input');
  }

  function fill(nome = ' Registro '): void {
    const input = nameInput();
    input.value = nome;
    input.dispatchEvent(new Event('input'));
    const description: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    description.value = 'Descrição';
    description.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function submit(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function invalidate(): void {
    generation.update((value) => value + 1);
    ready.set(false);
    invalidated.next();
    fixture.detectChanges();
  }

  it.each([false, true])('deveSalvarPayloadEEmitirUmSucesso_quandoEdicaoE%s', (edit) => {
    render(edit);
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain(
      edit ? 'Editar registro' : 'Novo registro',
    );
    if (edit) {
      expect(store.getById).toHaveBeenCalledExactlyOnceWith('same-pk');
      expect(fixture.nativeElement.querySelector('form').getAttribute('aria-busy')).toBe('true');
      load.next({ id: 'same-pk', nome: 'Anterior', descricao: 'Texto' });
      load.complete();
      fixture.detectChanges();
      expect(nameInput().value).toBe('Anterior');
    } else expect(store.getById).not.toHaveBeenCalled();
    fill();
    submit();
    submit();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    if (edit) {
      expect(store.update).toHaveBeenCalledExactlyOnceWith('same-pk', {
        id: 'same-pk',
        nome: 'Registro',
        descricao: 'Descrição',
      });
      expect(store.create).not.toHaveBeenCalled();
    } else {
      expect(store.create).toHaveBeenCalledExactlyOnceWith({
        nome: 'Registro',
        descricao: 'Descrição',
      });
      expect(store.update).not.toHaveBeenCalled();
    }
    expect(navigation.navigate).not.toHaveBeenCalled();
    save.next({ id: 'same-pk', nome: 'Registro' });
    save.complete();
    fixture.detectChanges();
    expect(alert.success).toHaveBeenCalledExactlyOnceWith(
      'Sucesso',
      edit ? 'Registro atualizado com sucesso.' : 'Registro criado com sucesso.',
    );
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/demo-registro/list']);
    expect(fixture.nativeElement.querySelector('form').getAttribute('aria-busy')).toBe('false');
    expect(alert.errorFromApi).not.toHaveBeenCalled();
    expect(alert.error).not.toHaveBeenCalled();
  });

  it.each([false, true])('deveNegarGravacao_quandoEdicaoE%sSemAcao', (edit) => {
    permissions.set([Acoes.CONSULTAR_DEMO]);
    render(edit);
    if (edit) {
      load.next({ id: 'same-pk', nome: 'Registro' });
      load.complete();
    }
    fill();
    expect(fixture.nativeElement.querySelector('button[type="submit"]')).toBeNull();
    submit();
    expect(store.create).not.toHaveBeenCalled();
    expect(store.update).not.toHaveBeenCalled();
    expect(navigation.navigate).not.toHaveBeenCalled();
  });

  it.each([true, false])('deveRenderizarCampoRestritoEPayload_quandoPermissaoE%s', (allowed) => {
    if (allowed) permissions.update((items) => [...items, Acoes.VISUALIZAR_VALOR_RESTRITO]);
    render(true);
    load.next({ id: 'same-pk', nome: 'Registro', valorRestrito: '12345678901234567.89' });
    load.complete();
    fixture.detectChanges();
    const inputs: NodeListOf<HTMLInputElement> =
      fixture.nativeElement.querySelectorAll('gems-input-text input');
    expect(inputs.length).toBe(allowed ? 2 : 1);
    expect(fixture.nativeElement.textContent.includes('Valor restrito')).toBe(allowed);
    if (allowed) expect(inputs[1].value).toBe('12345678901234567.89');
    submit();
    expect(store.update).toHaveBeenCalledWith('same-pk', {
      id: 'same-pk',
      nome: 'Registro',
      descricao: '',
      ...(allowed ? { valorRestrito: '12345678901234567.89' } : {}),
    });
  });

  it.each(['load', 'create', 'update'])(
    'deveEntregarEnvelopeELiberarLoading_quando%sFalhaNaGeracaoAtual',
    (operation) => {
      render(operation !== 'create');
      if (operation === 'load') load.error(httpError);
      else {
        if (operation === 'update') {
          load.next({ id: 'same-pk', nome: 'Registro' });
          load.complete();
        }
        fill();
        submit();
        save.error(httpError);
      }
      fixture.detectChanges();
      expect(alert.errorFromApi).toHaveBeenCalledExactlyOnceWith(httpError);
      expect(alert.error).not.toHaveBeenCalled();
      expect(alert.success).not.toHaveBeenCalled();
      expect(navigation.navigate).not.toHaveBeenCalled();
      expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
      expect(fixture.nativeElement.querySelector('form').getAttribute('aria-busy')).toBe('false');
      expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
    },
  );

  it.each([
    ['load', 'success'],
    ['load', 'error'],
    ['create', 'success'],
    ['create', 'error'],
    ['update', 'success'],
    ['update', 'error'],
  ])('deveIgnorarRetornoAntigo_quando%sResolveCom%sAposSwitch', (operation, ending) => {
    render(operation !== 'create');
    if (operation !== 'load') {
      if (operation === 'update') {
        load.next({ id: 'same-pk', nome: 'Alpha' });
        load.complete();
      }
      fill('Alpha');
      submit();
    }
    invalidate();
    expect(nameInput().value).toBe('');
    expect(fixture.nativeElement.querySelector('button[type="submit"]')).toBeNull();
    ready.set(true);
    fill('Beta');
    const pending = operation === 'load' ? load : save;
    if (ending === 'success') {
      pending.next({ id: 'same-pk', nome: 'Alpha', descricao: 'Antigo' });
      pending.complete();
    } else pending.error(httpError);
    fixture.detectChanges();
    expect(nameInput().value).toBe('Beta');
    expect(fixture.nativeElement.querySelector('textarea').value).toBe('Descrição');
    expect(alert.errorFromApi).not.toHaveBeenCalled();
    expect(alert.error).not.toHaveBeenCalled();
    expect(alert.success).not.toHaveBeenCalled();
    expect(navigation.navigate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('form').getAttribute('aria-busy')).toBe('false');
  });

  it('deveManterSaveAtualPendente_quandoFinalizacaoAntigaChega', () => {
    render();
    fill('Alpha');
    submit();
    invalidate();
    ready.set(true);
    fill('Beta');
    const beta = new Subject<DemoRegistro>();
    store.create.mockReturnValueOnce(beta);
    submit();
    save.next({ id: 'same-pk', nome: 'Alpha' });
    save.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    expect(alert.success).not.toHaveBeenCalled();
    expect(navigation.navigate).not.toHaveBeenCalled();
    beta.next({ id: 'same-pk', nome: 'Beta' });
    beta.complete();
    expect(store.create).toHaveBeenLastCalledWith({ nome: 'Beta', descricao: 'Descrição' });
    expect(alert.success).toHaveBeenCalledTimes(1);
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/demo-registro/list']);
  });

  it.each(['.btn-back', '.btn-secondary'])(
    'deveVoltarParaLista_quandoUsuarioAciona%s',
    (selector) => {
      render();
      fixture.nativeElement.querySelector(selector).click();
      expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/demo-registro/list']);
      expect(store.create).not.toHaveBeenCalled();
      expect(store.update).not.toHaveBeenCalled();
    },
  );
});
