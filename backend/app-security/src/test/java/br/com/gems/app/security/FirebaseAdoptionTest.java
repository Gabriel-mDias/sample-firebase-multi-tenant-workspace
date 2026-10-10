package br.com.gems.app.security;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FirebaseAdoptionTest {
    @Test
    void deveConsumirSDKRemotaEEliminarAutoridadePorClaims_quandoAdotaFirebase() throws Exception {
        String parent = Files.readString( Path.of( "../pom.xml" ) );
        String module = Files.readString( Path.of( "pom.xml" ) );
        String security = Files
                .readString(
                        Path.of( "../app-deploy/src/main/java/br/com/gems/app/security/config/SecurityConfig.java" ) );
        assertThat( parent ).contains( "<gems.version>3.4.0</gems.version>" );
        assertThat( module ).contains( "gems-firebase-auth" ).doesNotContain( "gems-keycloak-admin" );
        assertThat( security ).contains( "FirebaseAuthenticationFilter" ).doesNotContain( "realm_access",
                "resource_access", "jwtAuthenticationConverter", "\"/api/auth/**\"" );
    }
}
