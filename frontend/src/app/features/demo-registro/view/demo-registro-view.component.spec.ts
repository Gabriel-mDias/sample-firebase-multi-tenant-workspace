import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { GemsAlertService, GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { Subject } from 'rxjs';
import { DemoRegistroViewComponent } from './demo-registro-view.component';
import { DemoRegistroStore } from '../services/demo-registro.store';
import { DemoRegistro } from '../models/demo-registro.model';
import { AuthService } from '../../../core/auth/auth.service';
import { Acao, Acoes } from '../../../core/auth/acoes';
import { AuthorizationScope, SessionSnapshot } from '../../../core/auth/auth.model';

describe('DemoRegistroViewComponent', () => {
  const permissions = signal<Acao[]>([]);
  const generation = signal(0);
  let invalidated: Subject<void>;
  let response: Subject<DemoRegistro>;
  let fixture: ComponentFixture<DemoRegistroViewComponent>;
  const store = { getById: vi.fn(() => response), create: vi.fn(), update: vi.fn() };
  const alert = { error: vi.fn(), errorFromApi: vi.fn(), success: vi.fn() };
  const navigation = { navigate: vi.fn() };
  const httpError = new HttpErrorResponse({
    status: 403,
    error: { errorType: 'FORBIDDEN', message: 'Acesso negado.' },
  });

  beforeEach(() => {
    vi.clearAllMocks();
    generation.set(0);
    permissions.set([Acoes.CONSULTAR_DEMO, Acoes.INSERIR_DEMO, Acoes.ALTERAR_DEMO]);
    invalidated = new Subject<void>();
    response = new Subject<DemoRegistro>();
    store.getById.mockImplementation(() => response);
    TestBed.configureTestingModule({
      imports: [DemoRegistroViewComponent],
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
              ready: true,
            }),
            isCurrent: (snapshot: SessionSnapshot) => snapshot.generation === generation(),
            isAuthorized: (actions: readonly Acao[], scope: AuthorizationScope) =>
              scope === 'tenant' && actions.some((action) => permissions().includes(action)),
          },
        },
        { provide: DemoRegistroStore, useValue: store },
        { provide: GemsAlertService, useValue: alert },
        { provide: GemsNavigationService, useValue: navigation },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'same-pk' }, url: [{ path: 'view' }] },
          },
        },
      ],
    });
  });

  function render(): void {
    fixture = TestBed.createComponent(DemoRegistroViewComponent);
    fixture.detectChanges();
  }

  function nameInput(): HTMLInputElement {
    return fixture.nativeElement.querySelector('gems-input-text input');
  }

  it.each([true, false])('deveExibirLeituraSemSave_quandoCampoRestritoPermitidoE%s', (allowed) => {
    if (allowed) permissions.update((items) => [...items, Acoes.VISUALIZAR_VALOR_RESTRITO]);
    render();
    expect(store.getById).toHaveBeenCalledExactlyOnceWith('same-pk');
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain(
      'Carregando',
    );
    response.next({
      id: 'same-pk',
      nome: 'Registro',
      descricao: 'Descrição',
      valorRestrito: '12345678901234567.89',
    });
    response.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Visualizar registro');
    expect(nameInput().value).toBe('Registro');
    expect(nameInput().disabled).toBe(true);
    const description: HTMLTextAreaElement = fixture.nativeElement.querySelector('textarea');
    expect(description.value).toBe('Descrição');
    expect(description.disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('button[type="submit"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
    expect(fixture.nativeElement.textContent.includes('Valor restrito')).toBe(allowed);
    const inputs: NodeListOf<HTMLInputElement> =
      fixture.nativeElement.querySelectorAll('gems-input-text input');
    expect(inputs.length).toBe(allowed ? 2 : 1);
    if (allowed) {
      expect(inputs[1].value).toBe('12345678901234567.89');
      expect(inputs[1].disabled).toBe(true);
    }
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(store.create).not.toHaveBeenCalled();
    expect(store.update).not.toHaveBeenCalled();
    expect(alert.success).not.toHaveBeenCalled();
    expect(navigation.navigate).not.toHaveBeenCalled();
    fixture.nativeElement.querySelector('.btn-secondary').click();
    expect(navigation.navigate).toHaveBeenCalledExactlyOnceWith(['/demo-registro/list']);
  });

  it('deveEntregarEnvelopeETerminarLoading_quandoLoadAtualFalha', () => {
    render();
    response.error(httpError);
    fixture.detectChanges();
    expect(alert.errorFromApi).toHaveBeenCalledExactlyOnceWith(httpError);
    expect(alert.error).not.toHaveBeenCalled();
    expect(alert.success).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('form').getAttribute('aria-busy')).toBe('false');
    expect(nameInput().disabled).toBe(true);
    expect(navigation.navigate).not.toHaveBeenCalled();
  });

  it.each(['success', 'error'])(
    'deveLimparLeituraEIgnorarRetornoAntigo_quandoLoadTerminaCom%sAposSwitch',
    (ending) => {
      render();
      response.next({ id: 'same-pk', nome: 'Alpha', descricao: 'Antigo' });
      fixture.detectChanges();
      expect(nameInput().value).toBe('Alpha');
      generation.update((value) => value + 1);
      permissions.set([Acoes.CONSULTAR_DEMO]);
      invalidated.next();
      fixture.detectChanges();
      expect(nameInput().value).toBe('');
      if (ending === 'success') {
        response.next({ id: 'same-pk', nome: 'Alpha' });
        response.complete();
      } else response.error(httpError);
      fixture.detectChanges();
      expect(nameInput().value).toBe('');
      expect(nameInput().disabled).toBe(true);
      expect(fixture.nativeElement.querySelector('textarea').value).toBe('');
      expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
      expect(fixture.nativeElement.querySelector('button[type="submit"]')).toBeNull();
      expect(alert.errorFromApi).not.toHaveBeenCalled();
      expect(alert.error).not.toHaveBeenCalled();
      expect(alert.success).not.toHaveBeenCalled();
      expect(navigation.navigate).not.toHaveBeenCalled();
      expect(store.create).not.toHaveBeenCalled();
      expect(store.update).not.toHaveBeenCalled();
    },
  );
});
