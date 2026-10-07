import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { beforeEach, describe, expect, it } from 'vitest';
import { ContatoFieldsComponent } from './contato-fields.component';
import { createContatoForm } from './contato-form';

describe('ContatoFieldsComponent', () => {
  let fixture: ComponentFixture<ContatoFieldsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ContatoFieldsComponent] }).compileComponents();

    fixture = TestBed.createComponent(ContatoFieldsComponent);
    fixture.componentRef.setInput('form', createContatoForm(new FormBuilder()));
    fixture.detectChanges();
  });

  it('deveRenderizarEmailETelefone_quandoCriarComponente', () => {
    const campos = Array.from(
      fixture.nativeElement.querySelectorAll('gems-input-text'),
    ) as HTMLElement[];

    expect(campos.map((campo) => campo.getAttribute('formcontrolname'))).toEqual([
      'email',
      'telefone',
    ]);
    expect(campos.map((campo) => campo.getAttribute('icon'))).toEqual([
      'fa-solid fa-envelope',
      'fa-solid fa-phone',
    ]);
    expect(campos[1].getAttribute('type')).toBe('tel');
  });
});
