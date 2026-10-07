import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DadosPessoaisFieldsComponent } from './dados-pessoais-fields.component';
import { createDadosPessoaisForm } from './dados-pessoais-form';

describe('DadosPessoaisFieldsComponent', () => {
  let fixture: ComponentFixture<DadosPessoaisFieldsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DadosPessoaisFieldsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DadosPessoaisFieldsComponent);
    fixture.componentRef.setInput('form', createDadosPessoaisForm(new FormBuilder()));
    fixture.detectChanges();
  });

  it('deveRenderizarCamposEIconesNaOrdemCanonica_quandoCriarComponente', () => {
    const campos = Array.from(
      fixture.nativeElement.querySelectorAll(
        'gems-input-text, gems-input-document, gems-input-date',
      ),
    ) as HTMLElement[];

    expect(campos.map((campo) => campo.getAttribute('formcontrolname'))).toEqual([
      'primeiroNome',
      'sobrenome',
      'cpf',
      'rg',
      'dataNascimento',
    ]);
    expect(campos[0].getAttribute('icon')).toBe('fa-solid fa-user');
    expect(campos[1].getAttribute('icon')).toBe('fa-solid fa-user');
    expect(campos[3].getAttribute('icon')).toBe('fa-solid fa-address-card');
  });

  it('deveEmitirBusca_quandoClicarNoBotaoVisivel', () => {
    const buscarCpf = vi.fn();
    fixture.componentInstance.buscarCpf.subscribe(buscarCpf);

    const botao = fixture.nativeElement.querySelector(
      '[data-testid="buscar-cpf"]',
    ) as HTMLButtonElement;
    botao.click();

    expect(buscarCpf).toHaveBeenCalledOnce();
  });

  it('deveOcultarBusca_quandoModoForSomenteLeitura', () => {
    fixture.componentRef.setInput('isReadOnly', true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="buscar-cpf"]')).toBeNull();
  });

  it('deveDesabilitarBuscaEExibirLoading_quandoConsultaEstiverEmAndamento', () => {
    fixture.componentRef.setInput('isSearching', true);
    fixture.detectChanges();

    const botao = fixture.nativeElement.querySelector(
      '[data-testid="buscar-cpf"]',
    ) as HTMLButtonElement;

    expect(botao.disabled).toBe(true);
    expect(botao.querySelector('.fa-circle-notch.fa-spin')).not.toBeNull();
  });
});
