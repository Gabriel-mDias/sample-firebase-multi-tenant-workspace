package br.com.gems.app.usuario.listener;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Espelha no Keycloak o ciclo de vida das credenciais do sistema. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioKeycloakListener {

    private final KeycloakAdminGateway keycloakAdminGateway;
    private final KeycloakUserLifecycleGateway keycloakUserLifecycleGateway;
    private final KeycloakRealmGroupGateway keycloakRealmGroupGateway;

    @EventListener
    public void aoCriar( UsuarioCriadoEvent evento ) {
        var nome = (evento.primeiroNome() + " " + evento.sobrenome()).trim();
        var conta = keycloakAdminGateway.createUser(
                nome, evento.usuario().getLogin(), evento.email(), evento.senha() );

        registrarCompensacao( () -> keycloakUserLifecycleGateway.deleteUser( conta.id() ) );
        entrarNoGrupoDoPerfil( conta.id(), evento.perfil() );
        evento.usuario().setKeycloakId( conta.id() );
    }

    @EventListener
    public void aoAtualizar( UsuarioAtualizadoEvent evento ) {
        var keycloakId = evento.usuario().getKeycloakId();
        var anterior = keycloakUserLifecycleGateway.snapshotUser( keycloakId );
        registrarCompensacao( () -> keycloakUserLifecycleGateway.restoreUser( anterior ) );

        var atual = new KeycloakUserSnapshot(
                anterior.id(), evento.primeiroNome(), evento.sobrenome(), anterior.username(), evento.email(),
                anterior.enabled(), anterior.attributes(), anterior.groupIds() );
        keycloakUserLifecycleGateway.updateUser( atual );
        entrarNoGrupoDoPerfil( keycloakId, evento.perfil() );
    }

    @EventListener
    public void aoReativar( UsuarioReativadoEvent evento ) {
        var keycloakId = evento.usuario().getKeycloakId();
        var anterior = keycloakUserLifecycleGateway.snapshotUser( keycloakId );
        registrarCompensacao( () -> keycloakUserLifecycleGateway.restoreUser( anterior ) );

        keycloakUserLifecycleGateway.setUserEnabled( keycloakId, true );
        entrarNoGrupoDoPerfil( keycloakId, evento.perfil() );
    }

    @EventListener
    public void aoDesativar( UsuarioDesativadoEvent evento ) {
        var keycloakId = evento.usuario().getKeycloakId();
        var anterior = keycloakUserLifecycleGateway.snapshotUser( keycloakId );
        registrarCompensacao( () -> keycloakUserLifecycleGateway.restoreUser( anterior ) );
        keycloakUserLifecycleGateway.setUserEnabled( keycloakId, false );
    }

    private void entrarNoGrupoDoPerfil( String keycloakId, PerfilAtuacao perfil ) {
        var destino = keycloakRealmGroupGateway.findRealmGroupByName( perfil.getNomeGrupo() )
                .orElseThrow( () -> new SecurityException(
                        "O grupo '" + perfil.getNomeGrupo() + "' não existe no realm. "
                                + "Verifique o bootstrap de grupos e as permissões do service account." ) );
        var gruposAtuais = keycloakUserLifecycleGateway.listUserGroupIds( keycloakId );

        for ( var outroPerfil : PerfilAtuacao.values() ) {
            if ( outroPerfil == perfil ) {
                continue;
            }
            keycloakRealmGroupGateway.findRealmGroupByName( outroPerfil.getNomeGrupo() )
                    .filter( grupo -> gruposAtuais.contains( grupo.id() ) )
                    .ifPresent( grupo -> keycloakUserLifecycleGateway.leaveRealmGroup( keycloakId, grupo.id() ) );
        }

        keycloakUserLifecycleGateway.joinRealmGroup( keycloakId, destino.id() );
    }

    private void registrarCompensacao( Runnable desfazer ) {
        if ( !TransactionSynchronizationManager.isSynchronizationActive() ) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization( new TransactionSynchronization() {
            @Override
            public void afterCompletion( int status ) {
                if ( status != TransactionSynchronization.STATUS_ROLLED_BACK ) {
                    return;
                }

                try {
                    desfazer.run();
                } catch ( Exception e ) {
                    log.error( "Compensação no Keycloak falhou após rollback. "
                            + "Verifique manualmente se restou usuário órfão ou filiação indevida no realm.", e );
                }
            }
        } );
    }
}
