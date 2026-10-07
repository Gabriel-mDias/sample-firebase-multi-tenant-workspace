package br.com.gems.app.security.service;

import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.security.SecurityRoles;
import br.com.gems.utils.ObjectUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta sobre quem está fazendo a requisição em curso.
 *
 * <p>
 * Existe para que uma regra de negócio possa depender do perfil do chamador sem ler o
 * {@code SecurityContextHolder} diretamente — no caso de referência, o sigilo da comissão do parceiro, que
 * precisa valer tanto na leitura quanto na escrita e por isso não pode viver no controller.
 *
 * <p>
 * É um <b>bean</b>, e não um utilitário estático, deliberadamente: é isso que permite mocá-lo num
 * teste de service com Mockito puro, sem subir contexto Spring.
 *
 * <p>
 * Funciona igualmente bem com roles atribuídas diretamente ao usuário ou herdadas de grupo: o
 * Keycloak resolve a herança ao emitir o token, então {@code realm_access.roles} chega da mesma
 * forma nos dois casos.
 */
@Service
public class UsuarioAutenticadoService {

    /**
     * Pergunta se o chamador possui uma ação.
     *
     * <p>
     * Funciona igualmente bem para quem recebeu a ação diretamente e para quem a herdou da role
     * composta do seu perfil: o Keycloak <b>expande as compostas</b> ao emitir o token — medido no
     * spike de 2026-08-26, em que 34 roles chegaram em {@code realm_access.roles} a partir de um
     * único mapeamento de grupo. Aqui só se lê o que já chegou.
     *
     * <p>
     * Use isto <b>apenas</b> quando a regra for de serviço, não de fronteira. Bloquear um
     * endpoint inteiro é trabalho de {@code @PreAuthorize} no controller. Este método existe para o
     * caso em que um <b>campo</b> é omitido no meio de uma resposta que o chamador pode ver — hoje,
     * o valor da comissão, que um perfil de consulta não enxerga numa tela que ele pode abrir.
     *
     * @param acao
     *            a ação exigida.
     * @return {@code true} se o chamador a possui.
     */
    public boolean possuiAcao( AcaoSistema acao ) {
        return possuiAuthority( SecurityRoles.PREFIXO_AUTHORITY + acao.name() );
    }

    /**
     * O identificador de quem está fazendo a requisição: o {@code sub} do token, que é o id do
     * usuário no Keycloak.
     *
     * <p>
     * <b>Por que o {@code sub} e não o id da tabela local.</b> A trilha de auditoria precisa registrar
     * quem executou um ato, e essa marca tem de sobreviver a tudo — inclusive à remoção do
     * usuário do sistema. O {@code sub} é imutável, não depende de nenhuma tabela nossa e
     * corresponde ao {@code CD_KEYCLOAK_ID} de {@code security.USUARIO}, então continua sendo
     * possível descobrir o nome de quem assinou enquanto o cadastro existir.
     *
     * <p>
     * Nunca aceite este valor vindo do corpo de uma requisição: um cliente que declarasse o
     * próprio autor poderia assinar em nome de outro usuário.
     *
     * @return o id, ou vazio quando não há autenticação por JWT no contexto
     */
    public Optional<UUID> idDoUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( ObjectUtil.isNullOrEmpty( authentication )
                || !(authentication.getPrincipal() instanceof Jwt jwt) ) {
            return Optional.empty();
        }

        var sub = jwt.getSubject();

        // O sub pode faltar: ele entra no access token pelo mapper do escopo `basic`, e um cliente
        // que declare `defaultClientScopes` sem ele emite token completo de roles e sem identidade.
        // Aconteceu de verdade na homologação da 012 — `UUID.fromString(null)` lança NPE, que o
        // catch de IllegalArgumentException abaixo NÃO pega, e a requisição morria em 500.
        if ( ObjectUtil.isNullOrEmpty( sub ) ) {
            return Optional.empty();
        }

        try {
            return Optional.of( UUID.fromString( sub ) );
        } catch ( IllegalArgumentException e ) {
            // O sub do Keycloak é sempre um UUID; um valor fora desse formato significa outro
            // emissor, e nesse caso é melhor não registrar autor nenhum do que registrar lixo.
            return Optional.empty();
        }
    }

    /**
     * O login de quem está fazendo a requisição: o {@code preferred_username} do token.
     *
     * <p>
     * <b>Por que o login, se {@link #idDoUsuarioAutenticado()} já identifica.</b> A trilha de
     * auditoria grava os dois, e a coluna {@code NM_AUTOR} é <b>desnormalizada de propósito</b>:
     * ela precisa continuar legível anos depois, mesmo que o usuário tenha saído e a linha de
     * {@code security.USUARIO} não exista mais para resolver o identificador. Um histórico que
     * responde "quem alterou isso?" com um UUID órfão não responde nada.
     *
     * <p>
     * Este método nasceu porque o {@code /speckit-analyze} encontrou o vão: a trilha exigia o
     * login e nada neste serviço o expunha, enquanto {@code NM_AUTOR} era {@code NOT NULL}. O
     * requisito era literalmente inexequível.
     *
     * <p>
     * Vazio quando não há JWT no contexto — o caso do bootstrap da aplicação, em que a trilha
     * registra o autor como o próprio sistema.
     *
     * @return o login, ou vazio quando não há autenticação por JWT no contexto
     */
    public Optional<String> loginDoUsuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( ObjectUtil.isNullOrEmpty( authentication )
                || !(authentication.getPrincipal() instanceof Jwt jwt) ) {
            return Optional.empty();
        }

        var login = jwt.getClaimAsString( "preferred_username" );

        return ObjectUtil.isNullOrEmpty( login ) ? Optional.empty() : Optional.of( login );
    }

    private boolean possuiAuthority( String authorityEsperada ) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( ObjectUtil.isNullOrEmpty( authentication )
                || ObjectUtil.isNullOrEmpty( authentication.getAuthorities() ) ) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch( authority -> authorityEsperada.equals( authority.getAuthority() ) );
    }

}
