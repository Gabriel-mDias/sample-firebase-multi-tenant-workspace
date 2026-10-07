import { ContatoModel } from '../../../shared/forms/contato/contato.model';
import { EnderecoModel } from '../../../shared/forms/endereco/endereco.model';

/** Identidade civil, espelho de `PessoaDTO`. */
export interface PessoaModel {
  id?: string;
  primeiroNome: string;
  sobrenome: string;
  cpf?: string;
  rg?: string;
  dataNascimento?: string;
  sexo?: string;
}

/**
 * Espelha `ParceiroDTO` — o payload único do wizard.
 *
 * Parceiro, pessoa, contato e endereço vão e voltam juntos, numa só requisição: o backend grava os
 * quatro na mesma transação.
 */
export interface ParceiroModel {
  id?: string;
  nome: string;
  descricao?: string;
  pessoa: PessoaModel;
  contato: ContatoModel;
  endereco: EnderecoModel;

  /**
   * Só vem preenchida para quem tem `VISUALIZAR_COMISSAO`; para os demais o backend devolve
   * `null`.
   *
   * **Não trate o `null` como "sem comissão".** Ele significa "você não pode ver", e é por isso
   * que o formulário só envia este campo quando o usuário tem a ação: enviar `null` sobrescreveria
   * o valor gravado. O backend também se protege disso, e as duas guardas são deliberadas.
   */
  comissao?: number | null;

  /** Somente leitura: mantido pelo listener do evento de conclusão de solicitação. */
  solicitacoesConcluidas?: number;
}

/** Linha da busca paginada — espelha `ParceiroResponseDTO`, a projeção por construtor da HQL. */
export interface ParceiroResponseModel {
  id: string;
  nome: string;
  cpf?: string;
  email?: string;
  estadoSigla?: string;
  /** `null` para quem não tem `VISUALIZAR_COMISSAO`. O backend apaga a coluna linha a linha. */
  comissao?: number | null;
  solicitacoesConcluidas?: number;
}
