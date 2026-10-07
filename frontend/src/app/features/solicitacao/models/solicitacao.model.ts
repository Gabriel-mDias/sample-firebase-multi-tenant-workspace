/**
 * As situações da solicitação — espelho de `SituacaoSolicitacao.java`.
 *
 * A tela nunca **define** a situação: ela a exibe e oferece as transições que o usuário pode
 * executar. Quem decide se a transição é válida é o backend, e quem decide se ela é permitida é a
 * ação de `AcaoSistema` do endpoint correspondente.
 */
export type SituacaoSolicitacao =
  'RASCUNHO' | 'ABERTA' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'CANCELADA';

export const SITUACAO_LABELS: Record<SituacaoSolicitacao, string> = {
  RASCUNHO: 'Rascunho',
  ABERTA: 'Aberta',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
};

/** Classe de cor do badge. Os nomes são os do `styles.css` global — nada de hex no componente. */
export const SITUACAO_CLASSES: Record<SituacaoSolicitacao, string> = {
  RASCUNHO: 'badge-neutro',
  ABERTA: 'badge-info',
  EM_ANDAMENTO: 'badge-warning',
  CONCLUIDA: 'badge-success',
  CANCELADA: 'badge-danger',
};

export interface EtapaSolicitacaoModel {
  id?: string;
  nome: string;
  ordem: number;
  concluida?: boolean;
  dataConclusao?: string;
}

/** Espelha `SolicitacaoDTO`. */
export interface SolicitacaoModel {
  id?: string;
  titulo: string;
  descricao?: string;
  parceiroId?: string;
  parceiroNome?: string;

  /**
   * **Somente leitura.** O backend ignora este campo na gravação — a situação avança apenas pelos
   * endpoints de transição, cada um com a sua ação. Ele viaja no DTO para que a tela saiba o que
   * exibir, não para que alguém a defina.
   */
  situacao?: SituacaoSolicitacao;

  /** Somente leitura. Preenchido pelo endpoint de cancelamento. */
  motivoCancelamento?: string;

  /** Somente leitura. Preenchido pelo endpoint de conclusão. */
  dataConclusao?: string;

  etapas?: EtapaSolicitacaoModel[];
}

/** Corpo de `POST /api/solicitacoes/search`. */
export interface SolicitacaoFilterModel {
  id?: string;
  titulo?: string;
  situacao?: SituacaoSolicitacao;
  parceiroId?: string;
}

/** Linha da busca paginada — espelha `SolicitacaoResponseDTO`. */
export interface SolicitacaoResponseModel {
  id: string;
  titulo: string;
  situacao: SituacaoSolicitacao;
  parceiroId: string;
  parceiroNome: string;
  dataCriacao?: string;
  dataConclusao?: string;
}
