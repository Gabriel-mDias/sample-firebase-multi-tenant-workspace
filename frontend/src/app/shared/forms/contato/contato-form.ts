import { FormBuilder, FormGroup, Validators } from '@angular/forms';

export interface ContatoFormPayload {
  email: string;
  telefone: string;
}

export const ERRO_EMAIL = {
  required: 'O e-mail é obrigatório.',
  email: 'Informe um e-mail válido.',
};
export const ERRO_TELEFONE = { required: 'O telefone é obrigatório.' };

/** Cria a estrutura canônica de contato sem mascarar ou truncar telefones internacionais. */
export function createContatoForm(fb: FormBuilder): FormGroup {
  return fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    telefone: ['', [Validators.required, Validators.maxLength(255)]],
  });
}
