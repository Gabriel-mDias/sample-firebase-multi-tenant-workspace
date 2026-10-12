package br.com.gems.app.administracao;

import br.com.gems.firebase.auth.FirebaseAdminGateway;
import br.com.gems.firebase.auth.IdTokenVerifier;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.VerifiedIdentity;
import br.com.gems.utils.ObjectUtil;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Explicit CLI-only capability, after Admin verification of the requested UID. */
public record BootstrapOperator(SecurityActorProvider actor, VerifiedIdentity identity) {
    public static BootstrapOperator verify( String project, String uid, String token, IdTokenVerifier verifier,
            FirebaseAdminGateway admin ) {
        if ( ObjectUtil.isNullOrEmpty( project ) || ObjectUtil.isNullOrEmpty( uid )
                || ObjectUtil.isNullOrEmpty( token ) ) {
            throw new IllegalArgumentException( "Bootstrap exige projeto, UID e prova efêmera." );
        }
        var identity = verifier.verify( token );
        if ( !project.equals( identity.projectId() ) || !uid.equals( identity.uid() ) || !identity.emailVerified()
                || !"google.com".equals( identity.signInProvider() ) ) {
            throw new IllegalArgumentException( "Bootstrap inicial exige UID Google verificado no projeto informado." );
        }
        var remote = admin.getUser( uid );
        if ( !uid.equals( remote.uid() ) || remote.disabled() || !remote.emailVerified()
                || ObjectUtil.isNullOrEmpty( remote.email() )
                || !remote.email().equalsIgnoreCase( identity.email() ) ) {
            throw new IllegalArgumentException( "Operador não confirmado pelo Admin." );
        }
        var actor = UUID.nameUUIDFromBytes( (project + ":" + uid).getBytes( StandardCharsets.UTF_8 ) );
        return new BootstrapOperator( SecurityActorProvider.trustedBootstrap( actor ), identity );
    }
}
