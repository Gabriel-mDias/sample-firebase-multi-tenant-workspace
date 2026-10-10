package br.com.gems.app.security;

import br.com.gems.security.authorization.AuthorizationAction;
import br.com.gems.security.authorization.AuthorizationScope;

/** Fonte única do catálogo; SQL continua sendo a autoridade temporal. */
public enum AcaoSistema implements AuthorizationAction {
    CONSULTAR_PROPRIA_CONTA(AuthorizationScope.GLOBAL), CONSULTAR_USUARIO(AuthorizationScope.GLOBAL), INSERIR_USUARIO(
            AuthorizationScope.GLOBAL), ALTERAR_USUARIO(AuthorizationScope.GLOBAL), SUSPENDER_USUARIO(
                    AuthorizationScope.GLOBAL), REATIVAR_USUARIO(AuthorizationScope.GLOBAL), CONSULTAR_TENANT(
                            AuthorizationScope.GLOBAL), INSERIR_TENANT(AuthorizationScope.GLOBAL), ALTERAR_TENANT(
                                    AuthorizationScope.GLOBAL), SUSPENDER_TENANT(
                                            AuthorizationScope.GLOBAL), CRIAR_ADMINISTRADOR_TENANT(
                                                    AuthorizationScope.GLOBAL), CONSULTAR_ACAO_GLOBAL(
                                                            AuthorizationScope.GLOBAL), CONCEDER_ACAO_GLOBAL(
                                                                    AuthorizationScope.GLOBAL), REVOGAR_ACAO_GLOBAL(
                                                                            AuthorizationScope.GLOBAL), CONSULTAR_PROPRIO_CONTEXTO_TENANT(
                                                                                    AuthorizationScope.TENANT), CONSULTAR_MEMBERSHIP(
                                                                                            AuthorizationScope.TENANT), INSERIR_MEMBERSHIP(
                                                                                                    AuthorizationScope.TENANT), ENCERRAR_MEMBERSHIP(
                                                                                                            AuthorizationScope.TENANT), CONSULTAR_GRANT(
                                                                                                                    AuthorizationScope.TENANT), CONCEDER_GRANT(
                                                                                                                            AuthorizationScope.TENANT), REVOGAR_GRANT(
                                                                                                                                    AuthorizationScope.TENANT), CONSULTAR_DEMO(
                                                                                                                                            AuthorizationScope.TENANT), INSERIR_DEMO(
                                                                                                                                                    AuthorizationScope.TENANT), ALTERAR_DEMO(
                                                                                                                                                            AuthorizationScope.TENANT), EXCLUIR_DEMO(
                                                                                                                                                                    AuthorizationScope.TENANT), VISUALIZAR_VALOR_RESTRITO(
                                                                                                                                                                            AuthorizationScope.TENANT);
    private final AuthorizationScope scope;
    AcaoSistema( AuthorizationScope scope ) {
        this.scope = scope;
    }
    @Override
    public AuthorizationScope scope() {
        return scope;
    }
}
