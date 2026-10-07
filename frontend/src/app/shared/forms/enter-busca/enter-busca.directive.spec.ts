import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { EnterBuscaDirective } from './enter-busca.directive';

/**
 * O que este teste protege: o Enter nas telas de filtro é a única forma de disparar a busca sem
 * tirar a mão do teclado. Ele não tem sintoma visível quando quebra — a tecla simplesmente não
 * faz nada, e o operador volta ao mouse sem reportar defeito nenhum.
 *
 * O caso do `<textarea>` é o que impede a diretiva de estragar um campo multilinha no dia em que
 * um filtro tiver um.
 */
@Component({
  imports: [EnterBuscaDirective],
  template: `
    <form appEnterBusca (buscar)="buscas.set(buscas() + 1)">
      <input type="text" data-testid="texto" />
      <textarea data-testid="area"></textarea>
    </form>
  `,
})
class HospedeiroComponent {
  readonly buscas = signal(0);
}

function criar() {
  const fixture = TestBed.createComponent(HospedeiroComponent);
  fixture.detectChanges();
  return fixture;
}

function pressionarEnter(fixture: ReturnType<typeof criar>, testid: string): KeyboardEvent {
  const alvo: HTMLElement = fixture.nativeElement.querySelector(`[data-testid="${testid}"]`);
  const evento = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
  alvo.dispatchEvent(evento);
  fixture.detectChanges();
  return evento;
}

describe('EnterBuscaDirective', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HospedeiroComponent] }).compileComponents();
  });

  it('deveEmitirBusca_quandoPressionarEnterEmCampoDeTexto', () => {
    const fixture = criar();

    pressionarEnter(fixture, 'texto');

    expect(fixture.componentInstance.buscas()).toBe(1);
  });

  it('deveCancelarEvento_quandoPressionarEnter', () => {
    const fixture = criar();

    const evento = pressionarEnter(fixture, 'texto');

    expect(evento.defaultPrevented).toBe(true);
  });

  it('naoDeveBuscar_quandoPressionarEnterEmTextarea', () => {
    const fixture = criar();

    const evento = pressionarEnter(fixture, 'area');

    expect(fixture.componentInstance.buscas()).toBe(0);
    expect(evento.defaultPrevented).toBe(false);
  });

  it('deveIgnorarEnter_quandoConfirmarCandidatoDeIme', () => {
    const fixture = criar();
    const alvo: HTMLElement = fixture.nativeElement.querySelector('[data-testid="texto"]');

    alvo.dispatchEvent(
      new KeyboardEvent('keydown', {
        key: 'Enter',
        isComposing: true,
        bubbles: true,
        cancelable: true,
      }),
    );
    fixture.detectChanges();

    expect(fixture.componentInstance.buscas()).toBe(0);
  });

  it('deveIgnorarEvento_quandoTeclaNaoForEnter', () => {
    const fixture = criar();
    const alvo: HTMLElement = fixture.nativeElement.querySelector('[data-testid="texto"]');

    alvo.dispatchEvent(new KeyboardEvent('keydown', { key: 'a', bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(fixture.componentInstance.buscas()).toBe(0);
  });
});
