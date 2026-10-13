package br.com.gems.app.administracao;

import br.com.gems.firebase.auth.FirebaseAdminGateway;
import br.com.gems.firebase.auth.IdTokenVerifier;
import br.com.gems.firebase.auth.VerifiedIdentity;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BootstrapOperatorTest {

    private final FirebaseAdminGateway admin = mock( FirebaseAdminGateway.class );
    private final VerifiedIdentity identity = new VerifiedIdentity( "project-test", "https://securetoken.google.com/project-test", "operator", "operator@example.test", true, "google.com", "Operador", Instant.now() );
    private final IdTokenVerifier verifier = value -> identity;

    @Test
    void deveVerificarUIDProjetoEAdmin_quandoBootstrapExplicito() {
        when( admin.getUser( "operator" ) ).thenReturn( new FirebaseAdminGateway.User( "operator", "operator@example.test", "Operador", false, true ) );

        var first = BootstrapOperator.verify( "project-test", "operator", "ephemeral-proof", verifier, admin );
        var second = BootstrapOperator.verify( "project-test", "operator", "ephemeral-proof", verifier, admin );

        assertThat( first.actor().currentActor().bootstrap() ).isTrue();
        assertThat( first.actor().currentActor().userId() ).isEqualTo( second.actor().currentActor().userId() );
        assertThat( first.identity() ).isEqualTo( identity );
    }

    @Test
    void deveRecusarOutroUID_quandoTokenNaoComprovaOperador() {
        // Arrange: o token comprova o UID "operator"; o operador pedido é outro.

        // Act + Assert
        assertThatThrownBy( () -> BootstrapOperator.verify( "project-test", "another", "ephemeral-proof", verifier, admin ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    void deveRecusarContaDesabilitada_quandoAdminNegaUID() {
        when( admin.getUser( "operator" ) ).thenReturn( new FirebaseAdminGateway.User( "operator", "operator@example.test", "Operador", true, true ) );

        assertThatThrownBy( () -> BootstrapOperator.verify( "project-test", "operator", "ephemeral-proof", verifier, admin ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

}
