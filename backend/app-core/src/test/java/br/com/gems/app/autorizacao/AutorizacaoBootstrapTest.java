package br.com.gems.app.autorizacao;

import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.security.SecurityRoles;
import br.com.gems.keycloak.admin.KeycloakRealmGroupGateway;
import br.com.gems.keycloak.admin.KeycloakRealmGroupGateway.RealmGroup;
import br.com.gems.keycloak.admin.KeycloakRealmRoleGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prova as três propriedades do bootstrap que não são visíveis em nenhum outro lugar.
 *
 * <p>
 * Nenhuma delas é sobre "chamou o método certo": são sobre <b>ordem</b>, <b>totalidade</b> e
 * <b>resiliência</b>, e cada uma falha em silêncio se quebrar.
 */
class AutorizacaoBootstrapTest {

    private KeycloakRealmRoleGateway roleGateway;
    private KeycloakRealmGroupGateway groupGateway;
    private AutorizacaoBootstrap bootstrap;

    @BeforeEach
    void preparar() {
        roleGateway = mock( KeycloakRealmRoleGateway.class );
        groupGateway = mock( KeycloakRealmGroupGateway.class );
        bootstrap = new AutorizacaoBootstrap( roleGateway, groupGateway );

        when( groupGateway.findRealmGroupByName( anyString() ) ).thenReturn( Optional.empty() );
        when( groupGateway.createRealmGroup( anyString() ) ).thenReturn( "id-do-grupo" );
    }

    @Test
    @DisplayName("a ordem é ações, depois compostas, depois grupos — inverter quebra num realm limpo")
    void deveComprovarOrdemDosTresPassos_quandoExecutaOCenario() {
        bootstrap.sincronizarAutorizacao();

        // addComposites e mapearRoleNoGrupo resolvem a role pelo NOME e falham se ela não existir.
        // A ordem não é estilo: é a diferença entre subir e não subir contra um realm recém-criado.
        InOrder ordem = inOrder( roleGateway, groupGateway );
        ordem.verify( roleGateway, atLeastOnce() ).ensureRealmRole( anyString(), anyString() );
        ordem.verify( roleGateway, atLeastOnce() ).ensureCompositeRealmRole( anyString(), anyString(), any() );
        ordem.verify( groupGateway, atLeastOnce() ).ensureRealmRoleOnRealmGroup( anyString(), anyString() );
    }

    @Test
    @DisplayName("toda ação do enum vira realm role — sem lista paralela")
    void deveComprovarTodaAcaoViraRole_quandoExecutaOCenario() {
        bootstrap.sincronizarAutorizacao();

        var nomes = ArgumentCaptor.forClass( String.class );
        verify( roleGateway, times( AcaoSistema.values().length ) )
                .ensureRealmRole( nomes.capture(), anyString() );

        assertThat( nomes.getAllValues() )
                .as( "uma ação nova no enum precisa chegar ao realm sem que ninguém edite mais nada" )
                .containsExactlyInAnyOrderElementsOf(
                        java.util.Arrays.stream( AcaoSistema.values() ).map( Enum::name ).toList() );
    }

    @Test
    @DisplayName("a composta ADMIN contém todas as ações, e não uma lista mantida à mão")
    @SuppressWarnings("unchecked")
    void deveComprovarAdminContemTudo_quandoExecutaOCenario() {
        bootstrap.sincronizarAutorizacao();

        var filhos = ArgumentCaptor.forClass( Set.class );
        verify( roleGateway ).ensureCompositeRealmRole( eq( SecurityRoles.ADMIN ), anyString(), filhos.capture() );

        assertThat( (Set<String>) filhos.getValue() )
                .hasSize( AcaoSistema.values().length );
    }

    @Test
    @DisplayName("o administrador não ganha uma composta PERFIL_*: o grupo dele mapeia ADMIN")
    void deveComprovarAdministradorMapeiaAdminDiretamente_quandoExecutaOCenario() {
        bootstrap.sincronizarAutorizacao();

        verify( roleGateway, never() ).ensureCompositeRealmRole(
                eq( "PERFIL_" + PerfilAtuacao.ADMINISTRADOR.name() ), anyString(), any() );
        verify( groupGateway ).ensureRealmRoleOnRealmGroup( anyString(), eq( SecurityRoles.ADMIN ) );
    }

    @Test
    @DisplayName("todo perfil ganha grupo e composta mapeada")
    void deveComprovarTodoPerfilGanhaGrupo_quandoExecutaOCenario() {
        bootstrap.sincronizarAutorizacao();

        for ( var perfil : PerfilAtuacao.values() ) {
            verify( groupGateway ).findRealmGroupByName( perfil.getNomeGrupo() );
        }
        verify( groupGateway, times( PerfilAtuacao.values().length ) )
                .ensureRealmRoleOnRealmGroup( anyString(), anyString() );
    }

    @Test
    @DisplayName("falha do Keycloak não derruba a aplicação — o resto continua sendo garantido")
    void deveComprovarFalhaNaoPropaga_quandoExecutaOCenario() {
        // Uma exceção que escape de um listener de ApplicationReadyEvent aborta o
        // SpringApplication.run. Ficar sem um perfil é ruim; não subir é pior.
        when( roleGateway.ensureRealmRole( anyString(), anyString() ) )
                .thenThrow( new RuntimeException( "Keycloak fora do ar" ) );

        bootstrap.sincronizarAutorizacao();

        verify( groupGateway, times( PerfilAtuacao.values().length ) )
                .ensureRealmRoleOnRealmGroup( anyString(), anyString() );
    }

    @Test
    @DisplayName("grupo preexistente tem a composta reconferida, não pulada")
    void deveComprovarGrupoPreexistenteEhReconferido_quandoExecutaOCenario() {
        var representacao = new RealmGroup( "id-existente", "OPERADOR" );
        when( groupGateway.findRealmGroupByName( anyString() ) ).thenReturn( Optional.of( representacao ) );

        bootstrap.sincronizarAutorizacao();

        // Um grupo sem a composta mapeada é o pior defeito possível: o usuário loga e não recebe
        // permissão nenhuma. Reconferir sempre custa uma chamada e fecha essa porta.
        verify( groupGateway, never() ).createRealmGroup( anyString() );
        verify( groupGateway, times( PerfilAtuacao.values().length ) )
                .ensureRealmRoleOnRealmGroup( eq( "id-existente" ), anyString() );
    }
}
