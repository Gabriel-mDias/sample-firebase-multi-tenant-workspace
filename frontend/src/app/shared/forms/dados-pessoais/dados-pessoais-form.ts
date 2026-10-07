import { FormBuilder, FormGroup, Validators } from '@angular/forms';

/**
 * Sexo da pessoa. Definição canônica do projeto: `Parceiro`, `Usuário` e todo cadastro que um
 * projeto derivado acrescentar compartilham a mesma `Pessoa`, então o tipo precisa ser um só.
 * Espelha o enum `Sexo` do backend.
 */
export type Sexo = 'MASCULINO' | 'FEMININO' | 'NAO_INFORMADO';

/**
 * Opções do select de sexo.
 *
 * <p>`NAO_INFORMADO` é uma escolha **explícita**, não a ausência de escolha: o campo é obrigatório
 * para que o operador decida, em vez de deixar o registro em branco por esquecimento. Num domínio
 * que dependa dessa informação, o branco por esquecimento decide a regra errada.
 */
export const SEXO_OPTIONS: ReadonlyArray<{ label: string; value: Sexo }> = [
  { label: 'Masculino', value: 'MASCULINO' },
  { label: 'Feminino', value: 'FEMININO' },
  { label: 'Não informado', value: 'NAO_INFORMADO' },
];

/** Rótulo de exibição do sexo. Devolve travessão quando o campo ainda não foi preenchido. */
export function sexoLabel(sexo?: Sexo | null): string {
  return SEXO_OPTIONS.find((opcao) => opcao.value === sexo)?.label ?? '—';
}

export interface DadosPessoaisFormPayload {
  primeiroNome: string;
  sobrenome: string;
  cpf: string;
  rg: string;
  sexo: Sexo | null;
  dataNascimento: string;
}

export const TAMANHO_CPF = 11;

export const ERRO_PRIMEIRO_NOME = { required: 'O primeiro nome é obrigatório.' };
export const ERRO_SOBRENOME = { required: 'O sobrenome é obrigatório.' };
export const ERRO_CPF = {
  required: 'O CPF é obrigatório.',
  minlength: 'O CPF deve ter 11 dígitos.',
  maxlength: 'O CPF deve ter 11 dígitos.',
};
export const ERRO_SEXO = { required: 'O sexo é obrigatório.' };
export const ERRO_DATA_NASCIMENTO = { required: 'A data de nascimento é obrigatória.' };

/**
 * Opções da estrutura canônica. O padrão — tudo obrigatório — é o do Parceiro e o do Usuário:
 * identidade civil de pessoa viva e presente, que tem documento e o informa.
 *
 * <p>As duas opções existem para o cadastro que diverge disso, e um projeto derivado quase sempre
 * tem um:
 *
 * <ul>
 *   <li><b>CPF opcional</b> — pessoa histórica, estrangeira, ou terceiro que nunca se apresentou
 *       não tem CPF, e exigi-lo impediria cadastrar exatamente quem o domínio existe para tratar.
 *       Quando informado, continua tendo de estar completo.</li>
 *   <li><b>Sem data de nascimento aqui</b> — quando o nascimento não é identidade civil e sim um
 *       dado entre outros (ao lado de falecimento, origem, vigência), ele pertence ao passo que
 *       trata desses dados, não a este.</li>
 * </ul>
 */
export interface DadosPessoaisFormOpcoes {
  cpfObrigatorio?: boolean;
  comDataNascimento?: boolean;
}

/**
 * Cria a estrutura canônica compartilhada por Parceiro, Usuário e futuros cadastros de Pessoa.
 *
 * <p>Todos gravam a **mesma** `Pessoa` no backend, então o `FormGroup` e seus validadores existem
 * aqui uma vez só. Um cadastro que reconstrua a identidade civil à mão é como um campo novo deixa
 * de aparecer em metade das telas — o defeito não dá erro, só some. Campo civil novo entra aqui.
 */
export function createDadosPessoaisForm(
  fb: FormBuilder,
  opcoes: DadosPessoaisFormOpcoes = {},
): FormGroup {
  const { cpfObrigatorio = true, comDataNascimento = true } = opcoes;

  const validadoresDeCpf = [Validators.minLength(TAMANHO_CPF), Validators.maxLength(TAMANHO_CPF)];

  return fb.group({
    primeiroNome: ['', [Validators.required, Validators.maxLength(255)]],
    sobrenome: ['', [Validators.required, Validators.maxLength(255)]],
    cpf: ['', cpfObrigatorio ? [Validators.required, ...validadoresDeCpf] : validadoresDeCpf],
    rg: ['', [Validators.maxLength(20)]],
    // `null`, nunca `''`: o `gems-select` só exibe o placeholder com `null`.
    sexo: [null as Sexo | null, [Validators.required]],
    ...(comDataNascimento ? { dataNascimento: ['', [Validators.required]] } : {}),
  });
}

/**
 * Reduz um documento a dígitos. O `gems-input-document` já grava o CPF sem máscara, mas o valor
 * pode vir do backend ou de uma hidratação de sessão com pontuação — comparar contra
 * {@link TAMANHO_CPF} sem normalizar produziria falso negativo.
 */
export function apenasDigitos(valor?: string | null): string {
  return valor?.replace(/\D/g, '') ?? '';
}
