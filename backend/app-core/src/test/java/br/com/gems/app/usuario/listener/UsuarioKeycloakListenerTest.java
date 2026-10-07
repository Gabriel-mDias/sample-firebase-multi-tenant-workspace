package br.com.gems.app.usuario.listener;

import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.usuario.event.UsuarioAtualizadoEvent;
import br.com.gems.app.usuario.event.UsuarioCriadoEvent;
import br.com.gems.app.usuario.event.UsuarioDesativadoEvent;
import br.com.gems.app.usuario.event.UsuarioReativadoEvent;
import br.com.gems.exception.exception.SecurityException;
import br.com.gems.keycloak.admin.KeycloakAdminGateway;
import br.com.gems.keycloak.admin.KeycloakRealmGroupGateway;
import br.com.gems.keycloak.admin.KeycloakUserLifecycleGateway;
import br.com.gems.keycloak.admin.KeycloakUserSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioKeycloakListenerTest {

    private static final String KEYCLOAK_ID = "kc-0001";
    private static final String GRUPO_ID_OPERADOR = "grp-operador";
    private static final String GRUPO_ID_CONSULTA = "grp-consulta";

    @Mock
    private KeycloakAdminGateway keycloakAdminGateway;

    @Mock
    private KeycloakUserLifecycleGateway keycloakUserLifecycleGateway;

    @Mock
    private KeycloakRealmGroupGateway keycloakRealmGroupGateway;

    @InjectMocks
    private UsuarioKeycloakListener listener;

    @BeforeEach
    void abrirSincronizacao() {
        TransactionSynchronizationManager.initSynchronization();
        when( keycloakAdminGateway.createUser( anyString(), anyString(), anyString(), anyString() ) )
                .thenReturn( new KeycloakAdminGateway.User(
                        KEYCLOAK_ID, "Ana Souza", "ana.souza", "ana@exemplo.com.br", true ) );
        when( keycloakUserLifecycleGateway.snapshotUser( KEYCLOAK_ID ) ).thenReturn( snapshot() );
        when( keycloakUserLifecycleGateway.listUserGroupIds( KEYCLOAK_ID ) ).thenReturn( Set.of() );
        when( keycloakRealmGroupGateway.findRealmGroupByName( "OPERADOR" ) )
                .thenReturn( Optional.of( grupo( GRUPO_ID_OPERADOR, "OPERADOR" ) ) );
        when( keycloakRealmGroupGateway.findRealmGroupByName( "CONSULTA" ) )
                .thenReturn( Optional.of( grupo( GRUPO_ID_CONSULTA, "CONSULTA" ) ) );
    }

    @AfterEach
    void fecharSincronizacao() {
        if ( TransactionSynchronizationManager.isSynchronizationActive() ) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("criação grava o id publicado e associa o grupo do perfil")
    void deveGravarIdEAssociarGrupo_quandoCriaConta() {
        var usuario = usuarioTransiente();

        listener.aoCriar( eventoCriacao( usuario, PerfilAtuacao.OPERADOR ) );

        assertThat( usuario.getKeycloakId() ).isEqualTo( KEYCLOAK_ID );
        verify( keycloakUserLifecycleGateway ).joinRealmGroup( KEYCLOAK_ID, GRUPO_ID_OPERADOR );
    }

    @Test
    @DisplayName("rollback da criação exclui a conta externa")
    void deveExcluirConta_quandoCriacaoSofreRollback() {
        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );

        verify( keycloakUserLifecycleGateway ).deleteUser( KEYCLOAK_ID );
    }

    @Test
    @DisplayName("commit da criação preserva a conta externa")
    void devePreservarConta_quandoCriacaoConfirma() {
        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        dispararDesfecho( TransactionSynchronization.STATUS_COMMITTED );

        verify( keycloakUserLifecycleGateway, never() ).deleteUser( anyString() );
    }

    @Test
    @DisplayName("grupo inexistente é falha explícita")
    void deveFalharExplicitamente_quandoGrupoNaoExiste() {
        when( keycloakRealmGroupGateway.findRealmGroupByName( "OPERADOR" ) ).thenReturn( Optional.empty() );

        assertThatThrownBy( () -> listener.aoCriar(
                eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) ) )
                .isInstanceOf( SecurityException.class )
                .hasMessageContaining( "não existe no realm" );
    }

    @Test
    @DisplayName("troca de perfil remove grupo anterior e associa o novo")
    void deveTrocarGrupo_quandoAtualizaPerfil() {
        when( keycloakUserLifecycleGateway.listUserGroupIds( KEYCLOAK_ID ) )
                .thenReturn( Set.of( GRUPO_ID_OPERADOR ) );

        listener.aoAtualizar( eventoAtualizacao( PerfilAtuacao.CONSULTA ) );

        verify( keycloakUserLifecycleGateway ).leaveRealmGroup( KEYCLOAK_ID, GRUPO_ID_OPERADOR );
        verify( keycloakUserLifecycleGateway ).joinRealmGroup( KEYCLOAK_ID, GRUPO_ID_CONSULTA );
    }

    @Test
    @DisplayName("atualização preserva identidade e propaga os dados cadastrais")
    void devePreservarIdentidadeEPropagarDados_quandoAtualizaConta() {
        listener.aoAtualizar( eventoAtualizacao( PerfilAtuacao.CONSULTA ) );

        var captor = ArgumentCaptor.forClass( KeycloakUserSnapshot.class );
        verify( keycloakUserLifecycleGateway ).updateUser( captor.capture() );
        assertThat( captor.getValue().id() ).isEqualTo( KEYCLOAK_ID );
        assertThat( captor.getValue().username() ).isEqualTo( "ana.souza" );
        assertThat( captor.getValue().email() ).isEqualTo( "novo@exemplo.com.br" );
        assertThat( captor.getValue().firstName() ).isEqualTo( "Ana Paula" );
        assertThat( captor.getValue().lastName() ).isEqualTo( "Souza Lima" );
    }

    @Test
    @DisplayName("rollback da atualização restaura o snapshot inteiro")
    void deveRestaurarSnapshot_quandoAtualizacaoSofreRollback() {
        var anterior = snapshot();
        when( keycloakUserLifecycleGateway.snapshotUser( KEYCLOAK_ID ) ).thenReturn( anterior );
        listener.aoAtualizar( eventoAtualizacao( PerfilAtuacao.CONSULTA ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );

        verify( keycloakUserLifecycleGateway ).restoreUser( anterior );
    }

    @Test
    @DisplayName("desativação apenas desabilita a conta")
    void deveDesabilitarSemExcluir_quandoDesativa() {
        listener.aoDesativar( new UsuarioDesativadoEvent( usuarioPersistido() ) );

        verify( keycloakUserLifecycleGateway ).setUserEnabled( KEYCLOAK_ID, false );
        verify( keycloakUserLifecycleGateway, never() ).deleteUser( anyString() );
    }

    @Test
    @DisplayName("rollback da desativação restaura o snapshot")
    void deveRestaurarSnapshot_quandoDesativacaoSofreRollback() {
        var anterior = snapshot();
        listener.aoDesativar( new UsuarioDesativadoEvent( usuarioPersistido() ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );

        verify( keycloakUserLifecycleGateway ).restoreUser( anterior );
    }

    @Test
    @DisplayName("reativação habilita a conta e associa o perfil sem criar senha")
    void deveHabilitarEAssociarGrupo_quandoReativa() {
        listener.aoReativar( new UsuarioReativadoEvent( usuarioPersistido(), PerfilAtuacao.CONSULTA ) );

        verify( keycloakUserLifecycleGateway ).setUserEnabled( KEYCLOAK_ID, true );
        verify( keycloakUserLifecycleGateway ).joinRealmGroup( KEYCLOAK_ID, GRUPO_ID_CONSULTA );
        verify( keycloakAdminGateway, never() ).createUser( anyString(), anyString(), anyString(), anyString() );
    }

    @Test
    @DisplayName("falha dentro da compensação não mascara a causa original")
    void naoDevePropagarFalha_quandoCompensacaoFalha() {
        doThrow( new IllegalStateException( "Keycloak fora do ar" ) )
                .when( keycloakUserLifecycleGateway ).deleteUser( KEYCLOAK_ID );
        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );
    }

    private void dispararDesfecho( int status ) {
        List.copyOf( TransactionSynchronizationManager.getSynchronizations() )
                .forEach( sincronizacao -> sincronizacao.afterCompletion( status ) );
    }

    private UsuarioCriadoEvent eventoCriacao( Usuario usuario, PerfilAtuacao perfil ) {
        return new UsuarioCriadoEvent( usuario, "Senha@Forte123", perfil, "ana@exemplo.com.br", "Ana", "Souza" );
    }

    private UsuarioAtualizadoEvent eventoAtualizacao( PerfilAtuacao perfil ) {
        return new UsuarioAtualizadoEvent(
                usuarioPersistido(), perfil, "novo@exemplo.com.br", "Ana Paula", "Souza Lima" );
    }

    private Usuario usuarioTransiente() {
        return Usuario.builder().login( "ana.souza" ).ativo( true ).build();
    }

    private Usuario usuarioPersistido() {
        return Usuario.builder()
                .id( UUID.fromString( "77777777-7777-7777-7777-777777777777" ) )
                .login( "ana.souza" )
                .keycloakId( KEYCLOAK_ID )
                .ativo( true )
                .build();
    }

    private KeycloakUserSnapshot snapshot() {
        return new KeycloakUserSnapshot(
                KEYCLOAK_ID, "Ana", "Souza", "ana.souza", "ana@exemplo.com.br", true,
                Map.of(), Set.of( GRUPO_ID_OPERADOR ) );
    }

    private KeycloakRealmGroupGateway.RealmGroup grupo( String id, String nome ) {
        return new KeycloakRealmGroupGateway.RealmGroup( id, nome );
    }
}
