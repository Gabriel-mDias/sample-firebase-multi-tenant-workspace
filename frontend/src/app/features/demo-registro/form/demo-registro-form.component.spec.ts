import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { signal } from '@angular/core';
import { Subject, of } from 'rxjs';
import { GemsAlertService, GemsNavigationService } from '@gabriel-mdias/angular-gems-sdk';
import { DemoRegistroFormComponent } from './demo-registro-form.component';
import { DemoRegistroStore } from '../services/demo-registro.store';
import { AuthService } from '../../../core/auth/auth.service';
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
        { provide: GemsAlertService, useValue: { error: vi.fn() } },
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
