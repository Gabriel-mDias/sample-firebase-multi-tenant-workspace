import { FormBuilder, FormGroup, Validators } from '@angular/forms';

/** O CEP trafega só com dígitos: o operador vê 22250-040, o backend recebe 22250040. */
export const TAMANHO_CEP = 8;

/** País cujo cadastro exige UF e CEP. Fora dele os dois viram opcionais. */
export const PAIS_COM_UF_E_CEP = 'Brasil';

export const ERRO_PAIS = { required: 'O país é obrigatório.' };
export const ERRO_ESTADO = { required: 'O estado é obrigatório.' };
export const ERRO_CEP = {
  required: 'O CEP é obrigatório.',
  minlength: 'O CEP deve ter 8 dígitos.',
  maxlength: 'O CEP deve ter 8 dígitos.',
};
export const ERRO_CIDADE = { required: 'A cidade é obrigatória.' };
export const ERRO_BAIRRO = { required: 'O bairro é obrigatório.' };
export const ERRO_LOGRADOURO = { required: 'O logradouro é obrigatório.' };

/**
 * Estrutura canônica do endereço, compartilhada por Parceiro, Usuário e Parceiro.
 *
 * <p>`comPais` distingue o único caso que diverge: a sede de um parceiro estrangeiro fica sempre
 * num país fixo, então lá o país não é escolha do operador e o control não existe.
 *
 * <p>Os selects nascem em `null`, nunca `''` — o `gems-select` só reconhece `null`/`undefined`
 * como "sem seleção", e com string vazia o browser exibe a primeira opção sem disparar `change`.
 * `numero` e `complemento` são os únicos campos opcionais em qualquer país.
 */
export function createEnderecoForm(fb: FormBuilder, opcoes: { comPais: boolean }): FormGroup {
  return fb.group({
    ...(opcoes.comPais ? { paisId: [null as string | null, [Validators.required]] } : {}),
    estadoId: [null as string | null, [Validators.required]],
    cep: [
      '',
      [Validators.required, Validators.minLength(TAMANHO_CEP), Validators.maxLength(TAMANHO_CEP)],
    ],
    cidade: ['', [Validators.required, Validators.maxLength(255)]],
    bairro: ['', [Validators.required, Validators.maxLength(255)]],
    logradouro: ['', [Validators.required, Validators.maxLength(255)]],
    numero: ['', [Validators.maxLength(255)]],
    complemento: ['', [Validators.maxLength(255)]],
  });
}

/**
 * Liga e desliga a obrigatoriedade de UF e CEP conforme o país escolhido.
 *
 * <p>Devolve `true` quando o país exige os dois — é o valor que a tela usa para decidir se o label
 * leva asterisco e se a consulta de CEP faz sentido. País desconhecido conta como Brasil: enquanto
 * a lista não carregou, o formulário permanece no comportamento mais restritivo.
 */
export function aplicarObrigatoriedadeDeUfECep(form: FormGroup, nomePais?: string): boolean {
  const exige = !nomePais || nomePais.toLowerCase() === PAIS_COM_UF_E_CEP.toLowerCase();

  const cep = form.get('cep');
  const estado = form.get('estadoId');

  if (exige) {
    cep?.setValidators([
      Validators.required,
      Validators.minLength(TAMANHO_CEP),
      Validators.maxLength(TAMANHO_CEP),
    ]);
    estado?.setValidators([Validators.required]);
  } else {
    cep?.setValidators([Validators.minLength(TAMANHO_CEP), Validators.maxLength(TAMANHO_CEP)]);
    estado?.clearValidators();
  }

  cep?.updateValueAndValidity({ emitEvent: false });
  estado?.updateValueAndValidity({ emitEvent: false });

  return exige;
}
