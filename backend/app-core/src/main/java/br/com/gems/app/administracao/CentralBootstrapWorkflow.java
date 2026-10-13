package br.com.gems.app.administracao;

import br.com.gems.firebase.auth.AuthorizationResolver;
import br.com.gems.firebase.auth.FirebaseAuthException;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.UserAdministration;
import br.com.gems.security.authorization.AuthorizationCatalog;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.support.TransactionTemplate;

/** CLI explícita: capacidade verificada, vínculo SDK e grants globais atômicos/idempotentes. */
public final class CentralBootstrapWorkflow {

    public UUID initialize( BootstrapOperator operator, UserAdministration users, GrantAdministration grants, AuthorizationResolver resolver, AuthorizationCatalog catalog, Clock clock, TransactionTemplate tx ) {
        Optional<UUID> existing;
        try {
            existing = Optional.of( resolver.resolve( operator.identity(), Optional.empty(), clock.instant() ).principal().userId() );
        } catch ( FirebaseAuthException denied ) {
            if ( denied.reason() != FirebaseAuthException.Reason.LOCAL_DENIED ) {
                throw denied;
            }
            existing = Optional.empty();
        }
        var known = existing;
        return tx.execute( status -> {
            UUID user = known.orElseGet( () -> {
                users.preProvisionGoogle( operator.identity().displayName(), operator.identity().email(), "CONSULTAR_PROPRIA_CONTA" );
                return resolver.resolve( operator.identity(), Optional.empty(), clock.instant() ).principal().userId();
            } );
            catalog.globalActions().forEach( action -> grants.grantGlobal( user, action ) );
            return user;
        } );
    }

}
