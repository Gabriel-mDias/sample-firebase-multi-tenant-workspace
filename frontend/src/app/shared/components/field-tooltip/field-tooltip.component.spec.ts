import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { FieldTooltipComponent } from './field-tooltip.component';

describe('FieldTooltipComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [FieldTooltipComponent] }).compileComponents();
  });

  it('deveAbrirTooltip_quandoClicarNoBotao', () => {
    const fixture = TestBed.createComponent(FieldTooltipComponent);
    fixture.componentRef.setInput('label', 'Dante causa');
    fixture.componentRef.setInput('text', 'Percentual pago ao parceiro por solicitação concluída.');
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    const tooltip = fixture.nativeElement.querySelector('[role="tooltip"]') as HTMLElement;

    expect(button.getAttribute('aria-describedby')).toBe(tooltip.id);
    button.click();
    fixture.detectChanges();

    expect(button.getAttribute('aria-expanded')).toBe('true');
    expect(tooltip.textContent).toContain('Percentual pago ao parceiro');
  });

  it('devePosicionarBalaoNaJanela_quandoAbrirDentroDoCard', () => {
    const fixture = TestBed.createComponent(FieldTooltipComponent);
    fixture.componentRef.setInput('label', 'Dante causa');
    fixture.componentRef.setInput('text', 'Percentual pago ao parceiro por solicitação concluída.');
    fixture.detectChanges();
    const tooltip = fixture.nativeElement.querySelector('[role="tooltip"]') as HTMLElement;

    // Sem abrir, nada foi medido ainda.
    expect(tooltip.style.top).toBe('0px');

    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();

    // O componente escreve top/left explícitos; o CSS declara `position: fixed`, de modo que
    // essas coordenadas são relativas à janela e não ao card que contém o campo.
    expect(tooltip.style.top).not.toBe('');
    expect(tooltip.style.left).not.toBe('');
  });
});
