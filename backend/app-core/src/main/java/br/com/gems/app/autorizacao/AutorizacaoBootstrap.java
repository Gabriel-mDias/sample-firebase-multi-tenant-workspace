package br.com.gems.app.autorizacao;

import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.security.SecurityRoles;
import br.com.gems.keycloak.admin.KeycloakRealmGroupGateway;
import br.com.gems.keycloak.admin.KeycloakRealmRoleGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converge o modelo de autorização no Keycloak, a cada subida da aplicação.
 *
 * <h2>O modelo, em uma figura</h2>
 *
 * <pre>
 *   USUÁRIO ──entra no──▶ GRUPO OPERADOR ──mapeia──▶ PERFIL_OPERADOR (composta)
 *                                                          │
 *                                                          ├─▶ CONSULTAR_PARCEIRO
 *                                                          ├─▶ INSERIR_SOLICITACAO
 *                                                          └─▶ CONCLUIR_SOLICITACAO
 * </pre>
 *
 * O usuário <b>nunca</b> recebe role diretamente. O Keycloak expande a composta ao emitir o token, e
 * {@code hasRole('CONSULTAR_PARCEIRO')} enxerga cada ação a partir de <b>um</b> mapeamento.
 *
 * <h2>A ordem dos três passos é obrigatória</h2>
 * <ol>
 * <li>{@link #garantirAcoes()} — cria as realm roles que faltarem;</li>
 * <li>{@link #garantirCompostas()} — cria/atualiza {@code PERFIL_*} e {@code ADMIN};</li>
 * <li>{@link #garantirGrupos()} — mapeia a composta no grupo.</li>
 * </ol>
 * Não é estilo: {@code mapearRoleNoGrupo} e {@code addComposites} resolvem a role pelo nome, com
 * {@code realm.roles().get(nome).toRepresentation()}, que <b>falha se ela não existir</b>. Inverter a
 * ordem quebra no primeiro boot de um realm limpo.
 *
 * <h2>Por que em {@code app-core}</h2>
 * Precisa de {@link PerfilAtuacao} (de {@code app-model}) e de {@link AcaoSistema} e dos serviços do
 * Keycloak (de {@code app-security}). O {@code app-security} não depende do {@code app-model} — o
 * grafo do reator impede —, então {@code app-core} é o único módulo que enxerga as duas metades.
 *
 * <h2>Por que o catálogo não está no arquivo de realm</h2>
 * Declarar as roles em {@code environments/shared/keycloak/app-realm.yaml} criaria <b>dois
 * escritores para o mesmo recurso</b>, e duas fontes de verdade que divergiriam no dia em que
 * alguém editasse só uma. Acrescentar uma constante ao {@link AcaoSistema} basta.
 *
 * <h2>Falha não derruba a aplicação</h2>
 * Uma exceção que escape de um listener de {@link ApplicationReadyEvent} <b>aborta o
 * {@code SpringApplication.run}</b>. Como o Keycloak é infraestrutura auxiliar, cada item é tratado
 * isoladamente e qualquer falha vira log — inclusive o caso de um perfil sem conjunto de ações
 * declarado, que falha sozinho e não impede os demais. Ficar sem subir seria pior que ficar sem um
 * perfil.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.keycloak.bootstrap-grupos.enabled", havingValue = "true", matchIfMissing = true)
public class AutorizacaoBootstrap {

    /** Prefixo das roles compostas de perfil. O administrador é a exceção: usa {@code ADMIN}. */
    private static final String PREFIXO_COMPOSTA = "PERFIL_";

    private static final String DESCRICAO_ADMIN = "Composta de todas as ações do sistema. Mapeada no grupo do administrador.";

    private final KeycloakRealmRoleGateway keycloakRealmRoleGateway;
    private final KeycloakRealmGroupGateway keycloakRealmGroupGateway;

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAutorizacao() {
        var acoesCriadas = garantirAcoes();
        var compostasAjustadas = garantirCompostas();
        var gruposGarantidos = garantirGrupos();

        // Resumo sempre emitido, inclusive quando nada mudou. Sucesso silencioso aqui é perigoso: um
        // grupo sem composta mapeada só se manifesta como "acesso negado" no login do usuário, muito
        // depois e longe da causa. A linha no boot é o que torna o estado conferível.
        var totalPerfis = PerfilAtuacao.values().length;

        if ( gruposGarantidos == totalPerfis ) {
            log.info( "Autorização sincronizada no Keycloak: {} ações no catálogo ({} criadas agora), "
                    + "{} filhos acrescentados às compostas, {}/{} grupos mapeados.",
                    AcaoSistema.values().length, acoesCriadas, compostasAjustadas,
                    gruposGarantidos, totalPerfis );
        } else {
            log.warn( "Autorização sincronizada PARCIALMENTE: {}/{} grupos mapeados. Usuários dos "
                    + "perfis restantes podem logar sem permissão alguma.",
                    gruposGarantidos, totalPerfis );
        }
    }

    /**
     * Passo 1 — cria as realm roles de ação que faltarem.
     *
     * @return quantas foram criadas nesta execução.
     */
    private int garantirAcoes() {
        var criadas = 0;

        for ( var acao : AcaoSistema.values() ) {
            try {
                if ( keycloakRealmRoleGateway.ensureRealmRole( acao.name(), acao.getDescricao() ) ) {
                    criadas++;
                    log.info( "Ação '{}' criada como realm role.", acao.name() );
                }
            } catch ( Exception e ) {
                // Por item, de propósito: uma ação problemática não pode impedir as outras.
                log.error( "Falha ao garantir a ação '{}' no Keycloak. A aplicação segue no ar.",
                        acao.name(), e );
            }
        }

        return criadas;
    }

    /**
     * Passo 2 — cria ou atualiza {@code ADMIN} e as compostas {@code PERFIL_*}.
     *
     * <p>
     * {@code ADMIN} recebe <b>todas</b> as ações do enum, não uma lista mantida à mão: é o que faz
     * o administrador alcançar uma ação nova sem que ninguém precise lembrar de concedê-la.
     *
     * @return quantos filhos foram acrescentados no total.
     */
    private int garantirCompostas() {
        var acrescentados = 0;

        try {
            var todasAsAcoes = Arrays.stream( AcaoSistema.values() )
                    .map( AcaoSistema::name )
                    .collect( Collectors.toCollection( LinkedHashSet::new ) );

            acrescentados += keycloakRealmRoleGateway.ensureCompositeRealmRole(
                    SecurityRoles.ADMIN, DESCRICAO_ADMIN, todasAsAcoes );
        } catch ( Exception e ) {
            log.error( "Falha ao garantir a composta '{}' no Keycloak. A aplicação segue no ar.",
                    SecurityRoles.ADMIN, e );
        }

        for ( var perfil : PerfilAtuacao.values() ) {
            if ( perfil.isAdministrador() ) {
                // O grupo do administrador mapeia ADMIN diretamente; não existe PERFIL_ADMINISTRADOR.
                continue;
            }

            try {
                acrescentados += keycloakRealmRoleGateway.ensureCompositeRealmRole(
                        nomeComposta( perfil ),
                        "Ações do perfil de atuação %s.".formatted( perfil.getDescricao() ),
                        nomesDasAcoes( perfil ) );
            } catch ( Exception e ) {
                log.error( "Falha ao garantir a composta '{}' no Keycloak. A aplicação segue no ar.",
                        nomeComposta( perfil ), e );
            }
        }

        return acrescentados;
    }

    /**
     * Passo 3 — garante o grupo de cada perfil e mapeia nele a composta certa.
     *
     * @return quantos perfis foram garantidos por completo.
     */
    private int garantirGrupos() {
        var garantidos = 0;

        for ( var perfil : PerfilAtuacao.values() ) {
            try {
                garantirGrupo( perfil );
                garantidos++;
            } catch ( Exception e ) {
                // Exception genérica e por perfil, de propósito: um grupo problemático não pode
                // impedir os outros, e nada aqui pode escapar.
                log.error( "Falha ao garantir o grupo '{}' no Keycloak. A aplicação segue no ar; "
                        + "crie-o manualmente ou reinicie com o Keycloak disponível.",
                        perfil.getNomeGrupo(), e );
            }
        }

        return garantidos;
    }

    private void garantirGrupo( PerfilAtuacao perfil ) {
        var existente = keycloakRealmGroupGateway.findRealmGroupByName( perfil.getNomeGrupo() );

        var groupId = existente
                .map( KeycloakRealmGroupGateway.RealmGroup::id )
                .orElseGet( () -> {
                    var id = keycloakRealmGroupGateway.createRealmGroup( perfil.getNomeGrupo() );
                    log.info( "Grupo '{}' criado no realm.", perfil.getNomeGrupo() );
                    return id;
                } );

        // Sempre reconferido, mesmo em grupo preexistente: um grupo sem a composta mapeada é o pior
        // defeito possível aqui — o usuário loga normalmente e não recebe permissão nenhuma, sintoma
        // que aparece como "acesso negado" e não como erro de cadastro.
        keycloakRealmGroupGateway.ensureRealmRoleOnRealmGroup( groupId, roleDoGrupo( perfil ) );
    }

    /** {@code ADMIN} para o administrador; {@code PERFIL_<NOME>} para os demais. */
    private String roleDoGrupo( PerfilAtuacao perfil ) {
        return perfil.isAdministrador() ? SecurityRoles.ADMIN : nomeComposta( perfil );
    }

    private String nomeComposta( PerfilAtuacao perfil ) {
        return PREFIXO_COMPOSTA + perfil.name();
    }

    /**
     * Pode devolver conjunto vazio, para um perfil cuja composta ainda não recebeu ações. Vazio é
     * diferente de ausente: a composta vazia é onde as ações entram quando chegarem, sem mexer neste
     * bootstrap.
     */
    private Set<String> nomesDasAcoes( PerfilAtuacao perfil ) {
        return MapaPerfilAcoes.acoesDe( perfil )
                .stream()
                .map( AcaoSistema::name )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

}
