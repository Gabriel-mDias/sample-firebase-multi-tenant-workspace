package br.com.gems.app.security;

import br.com.gems.security.authorization.AuthorizationAction;
import br.com.gems.security.authorization.AuthorizationScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo de ações do sistema — a menor unidade de permissão que existe aqui.
 *
 * <p>
 * Cada constante vira uma <b>realm role</b> no Keycloak, criada pelo {@code AutorizacaoBootstrap}
 * a cada início da aplicação. <b>Acrescentar uma ação aqui basta</b> para que ela passe a existir no
 * provedor de identidade: não há uma segunda lista no {@code app-realm.yaml} — dois escritores para
 * o mesmo recurso seriam duas fontes de verdade, que divergiriam no dia em que alguém editasse só uma.
 *
 * <h2>Por que o nome é MAIÚSCULO COM UNDERSCORE</h2>
 * Não é preferência de estilo. O conversor de JWT do {@code SecurityConfig} faz
 * {@code "ROLE_" + role.toUpperCase()}, e o {@code hasRole('x')} do Spring prefixa {@code ROLE_} sem
 * alterar a caixa. Uma role {@code upload-arquivo} viraria a authority {@code ROLE_UPLOAD-ARQUIVO},
 * enquanto {@code hasRole('upload-arquivo')} procuraria {@code ROLE_upload-arquivo} — não casa.
 * Manter minúsculas obrigaria {@code hasAuthority('ROLE_UPLOAD-ARQUIVO')} em toda anotação.
 *
 * <h2>Convenção de nome</h2>
 * {@code VERBO_RECURSO}, recurso no <b>singular</b>. Os verbos de cadastro são sempre os mesmos
 * quatro — {@code CONSULTAR}, {@code INSERIR}, {@code ALTERAR}, {@code EXCLUIR} —, nada de
 * {@code CRIAR}/{@code CADASTRAR}/{@code SALVAR} misturados. Ação que não é de cadastro usa o verbo
 * do negócio: {@code VISUALIZAR}, {@code ABRIR}, {@code CONCLUIR}, {@code CANCELAR}.
 *
 * <h2>Por que vive no app-security, e não no app-model</h2>
 * É apenas uma lista de nomes, sem dependência de entidade nenhuma. E {@code app-security} não
 * depende de {@code app-model} — o grafo do reator garante isso em tempo de compilação. Quem casa
 * as duas metades (perfil e ação) é o {@code MapaPerfilAcoes}, no {@code app-core}.
 *
 * @see SecurityRoles
 */
@Getter
@RequiredArgsConstructor
public enum AcaoSistema implements AuthorizationAction {

    // ---- Parceiro ----------------------------------------------------------------------------
    CONSULTAR_PARCEIRO("Consultar parceiros"), INSERIR_PARCEIRO("Cadastrar parceiro"), ALTERAR_PARCEIRO(
            "Alterar parceiro"), EXCLUIR_PARCEIRO("Excluir parceiro"),

    /**
     * Única ação que não guarda um endpoint. Ela decide se o valor da comissão é preenchido no
     * DTO devolvido pelo {@code ParceiroService} — em {@code toDTO}, na busca <b>e</b> na gravação.
     *
     * <p>
     * É a implementação de referência da <b>autorização de campo</b> do Princípio V: quem não a
     * tem enxerga o parceiro inteiro, menos esse valor. Aplicá-la só na UI é defeito, não estilo —
     * a API continuaria devolvendo o número a quem chamasse direto.
     */
    VISUALIZAR_COMISSAO("Visualizar o valor da comissão do parceiro"),

    // ---- Solicitação -------------------------------------------------------------------------
    // CONSULTAR e INSERIR são amplos: quem atende precisa abrir e ver. O que é estreito são as
    // transições de situação, logo abaixo — cada uma tem a sua própria ação.
    CONSULTAR_SOLICITACAO("Consultar solicitações"), INSERIR_SOLICITACAO("Abrir solicitação"), ALTERAR_SOLICITACAO(
            "Alterar solicitação"), EXCLUIR_SOLICITACAO("Excluir solicitação"),

    /**
     * Publicar a solicitação: de {@code RASCUNHO} para {@code ABERTA}.
     *
     * <p>
     * <b>Por que uma ação por transição, e não um {@code ALTERAR_SOLICITACAO} genérico.</b> A
     * situação nunca entra pelo corpo da requisição (Princípio V). Se entrasse, quem pudesse editar
     * o cadastro poderia concluir qualquer solicitação — a máquina de estados viraria sugestão e a
     * autorização por transição, ficção.
     */
    ABRIR_SOLICITACAO("Publicar solicitação"),

    /** Assumir a execução: de {@code ABERTA} para {@code EM_ANDAMENTO}. */
    INICIAR_SOLICITACAO("Iniciar a execução da solicitação"),

    /**
     * Concluir a solicitação. É o marco que publica o {@code SolicitacaoConcluidaEvent}, consumido
     * pelo domínio {@code parceiro} — a demonstração da fronteira do Princípio I: acionar regra de
     * outro domínio é por evento, nunca chamada de service a service.
     */
    CONCLUIR_SOLICITACAO("Concluir solicitação"),

    /** Cancelar a solicitação, com motivo obrigatório. Terminal, como a conclusão. */
    CANCELAR_SOLICITACAO("Cancelar solicitação"),

    // ---- Etapas da solicitação ---------------------------------------------------------------
    INSERIR_ETAPA_SOLICITACAO("Acrescentar etapa à solicitação"), ALTERAR_ETAPA_SOLICITACAO(
            "Alterar etapa da solicitação"), EXCLUIR_ETAPA_SOLICITACAO("Excluir etapa da solicitação"),

    // ---- Auditoria ---------------------------------------------------------------------------
    /**
     * Consultar a trilha de auditoria.
     *
     * <p>
     * Estreita de propósito: a trilha expõe o padrão de trabalho de cada usuário — quantas
     * correções alguém fez, em que horário, em quais cadastros. É informação de gestão, da mesma
     * natureza da comissão, e segue a mesma disciplina.
     */
    CONSULTAR_AUDITORIA("Consultar a trilha de auditoria"),

    // ---- Usuários ----------------------------------------------------------------------------
    CONSULTAR_USUARIO("Consultar usuários"), INSERIR_USUARIO(
            "Cadastrar usuário e provisionar o acesso"), ALTERAR_USUARIO(
                    "Alterar usuário"), SUSPENDER_USUARIO("Suspender o acesso de um usuário");

    /** Vira a descrição da realm role no Keycloak, visível no console administrativo. */
    private final String descricao;

    @Override
    public AuthorizationScope scope() {
        return AuthorizationScope.GLOBAL;
    }

}
