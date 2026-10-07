/**
 * Catálogo de ações do frontend — **espelho exato** de `AcaoSistema.java` do backend.
 *
 * Os nomes DEVEM ser idênticos às realm roles do Keycloak (case-sensitive) e ao enum do backend.
 * A sincronia entre os dois é mantida por disciplina, não por ferramenta: é dívida registrada e
 * aceita porque a divergência quebra de forma **barulhenta** — a tela some ou o acesso é negado —
 * e não em silêncio.
 *
 * `ADMIN` não aparece aqui de propósito: ele é a role **composta** que contém todas as ações, e o
 * Keycloak a expande no token. Uma consulta da ação `EXCLUIR_PARCEIRO` já é verdadeira para o
 * administrador — sem nenhum caso especial na tela.
 */
export const Acoes = {
  // ---- Parceiro ----------------------------------------------------------------------------
  CONSULTAR_PARCEIRO: 'CONSULTAR_PARCEIRO',
  INSERIR_PARCEIRO: 'INSERIR_PARCEIRO',
  ALTERAR_PARCEIRO: 'ALTERAR_PARCEIRO',
  EXCLUIR_PARCEIRO: 'EXCLUIR_PARCEIRO',

  /**
   * Autorização de CAMPO, não de tela. Quem não a tem abre o parceiro e não enxerga a comissão —
   * o backend devolve o campo nulo. Usá-la só para esconder o elemento na tela é defeito: a API
   * continuaria entregando o número a quem chamasse direto.
   */
  VISUALIZAR_COMISSAO: 'VISUALIZAR_COMISSAO',

  // ---- Solicitação -------------------------------------------------------------------------
  CONSULTAR_SOLICITACAO: 'CONSULTAR_SOLICITACAO',
  INSERIR_SOLICITACAO: 'INSERIR_SOLICITACAO',
  ALTERAR_SOLICITACAO: 'ALTERAR_SOLICITACAO',
  EXCLUIR_SOLICITACAO: 'EXCLUIR_SOLICITACAO',

  // Uma ação por transição. É o que faz cada botão de fluxo aparecer para quem realmente pode
  // executá-lo, em vez de todos aparecerem para quem pode "editar".
  ABRIR_SOLICITACAO: 'ABRIR_SOLICITACAO',
  INICIAR_SOLICITACAO: 'INICIAR_SOLICITACAO',
  CONCLUIR_SOLICITACAO: 'CONCLUIR_SOLICITACAO',
  CANCELAR_SOLICITACAO: 'CANCELAR_SOLICITACAO',

  // ---- Etapas da solicitação ---------------------------------------------------------------
  INSERIR_ETAPA_SOLICITACAO: 'INSERIR_ETAPA_SOLICITACAO',
  ALTERAR_ETAPA_SOLICITACAO: 'ALTERAR_ETAPA_SOLICITACAO',
  EXCLUIR_ETAPA_SOLICITACAO: 'EXCLUIR_ETAPA_SOLICITACAO',

  // ---- Auditoria ---------------------------------------------------------------------------
  CONSULTAR_AUDITORIA: 'CONSULTAR_AUDITORIA',

  // ---- Usuários ----------------------------------------------------------------------------
  CONSULTAR_USUARIO: 'CONSULTAR_USUARIO',
  INSERIR_USUARIO: 'INSERIR_USUARIO',
  ALTERAR_USUARIO: 'ALTERAR_USUARIO',
  SUSPENDER_USUARIO: 'SUSPENDER_USUARIO',
} as const;

export type Acao = (typeof Acoes)[keyof typeof Acoes];

/** Requisito de autorização explícito consumido pela fachada e pelos guards. */
export interface AuthorizationRequirement {
  readonly scope: 'global';
  readonly actions: readonly Acao[];
}

/** O sample é single-tenant: todas as ações pertencem ao escopo global. */
export function globalAuthorization(...actions: Acao[]): AuthorizationRequirement {
  return { scope: 'global', actions };
}
