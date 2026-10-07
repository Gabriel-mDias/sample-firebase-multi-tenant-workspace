import { FormBuilder } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { createContatoForm } from './contato-form';

describe('createContatoForm', () => {
  it('deveManterCamposObrigatoriosNaOrdemCanonica_quandoCriarFormulario', () => {
    const form = createContatoForm(new FormBuilder());

    expect(Object.keys(form.controls)).toEqual(['email', 'telefone']);
    expect(form.invalid).toBe(true);

    form.patchValue({ email: 'parceiro@exemplo.com', telefone: '+39 06 1234567' });
    expect(form.valid).toBe(true);
  });
});
