/**
 * A trilha de auditoria, do lado de quem lê.
 *
 * Espelha `RegistroAuditoriaResponseDTO` e `CampoAlteradoDTO` do backend. Nada mais: a trilha é
 * somente leitura na API, então não há modelo de escrita aqui — e não deve haver.
 */

/** As três operações que a trilha registra. */
export type OperacaoAuditoria = 'INSERCAO' | 'ALTERACAO' | 'EXCLUSAO';

/**
 * Um campo que mudou.
 *
 * `valorAnterior` e `valorNovo` vêm **nulos** quando `sigiloso` é verdadeiro, e isso é intencional,
 * não falta de dado: o valor sigiloso nunca chega a ser gravado na trilha. A tela precisa
 * dizer "valor não registrado" — deixar em branco faria parecer que o campo ficou vazio.
 */
export interface CampoAlteradoModel {
  campo: string;
  valorAnterior: string | null;
  valorNovo: string | null;
  sigiloso: boolean;
}

export interface RegistroAuditoriaModel {
  id: string;
  entidade: string;
  registroAfetado: string;
  operacao: OperacaoAuditoria;
  operacaoDescricao: string;
  dataOperacao: string;
  autorNome: string;
  camposAlterados: CampoAlteradoModel[];
}

/**
 * O nome da entidade como o backend a conhece — é o nome simples da classe Java, e é o que vai no
 * caminho de `GET /api/auditoria/{entidade}/{id}`.
 *
 * O tipo é fechado de propósito: um nome errado devolve **400**, não página vazia, e é melhor que o
 * compilador pegue antes.
 */
export type EntidadeAuditada =
  'Parceiro' | 'Parceiro' | 'Contrato' | 'EtapaSolicitacao' | 'Processo' | 'Solicitacao';

/** Rótulos das operações, para quando o backend não mandar a descrição. */
export const OPERACAO_LABEL: Record<OperacaoAuditoria, string> = {
  INSERCAO: 'Cadastro',
  ALTERACAO: 'Alteração',
  EXCLUSAO: 'Exclusão',
};

/**
 * De qual parte do cadastro veio a alteração.
 *
 * O histórico de um cadastro soma a linha raiz e os agregados dela — o telefone de um parceiro mora
 * em `Contato`, o nome em `Pessoa`. Sem este rótulo, `telefone` apareceria sem dizer de onde, e dois
 * agregados do mesmo cadastro podem ter campo de mesmo nome.
 *
 * Nome não mapeado cai no próprio nome da entidade: é melhor mostrar `Usuário` do que esconder
 * a origem porque ninguém acrescentou a linha aqui.
 */
export const ORIGEM_LABEL: Record<string, string> = {
  Pessoa: 'Dados pessoais',
  Contato: 'Contato',
  Endereco: 'Endereço',
};

/** Ícone por operação — Font Awesome 6 Solid, a única biblioteca do projeto. */
export const OPERACAO_ICONE: Record<OperacaoAuditoria, string> = {
  INSERCAO: 'fa-solid fa-plus',
  ALTERACAO: 'fa-solid fa-pen',
  EXCLUSAO: 'fa-solid fa-trash',
};
