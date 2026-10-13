package br.com.gems.app.security;

import br.com.gems.firebase.auth.FirebaseAuthenticationFilter;
import br.com.gems.firebase.auth.FirebaseAuthenticationToken;
import br.com.gems.firebase.auth.jdbc.JdbcIdentityCommandWorker;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.security.authorization.TenantAuthorizationInterceptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FirebaseAdoptionTest {

    @Test
    void deveCarregarArtefatoComVersaoMavenEOrigemJar_quandoAdotaGarantiasFirebase() throws Exception {
        assertArtifact( FirebaseAuthenticationFilter.class, "gems-firebase-auth" );
        assertArtifact( FirebaseAuthenticationToken.class, "gems-firebase-auth" );
        assertArtifact( JdbcIdentityCommandWorker.class, "gems-firebase-auth" );
        assertArtifact( AuthorizationCatalog.class, "gems-security-authorization" );
        assertArtifact( TenantAuthorizationInterceptor.class, "gems-security-authorization" );
    }

    private void assertArtifact( Class<?> type, String artifact ) throws Exception {
        var location = type.getProtectionDomain().getCodeSource().getLocation();
        var path = Path.of( location.toURI() );

        assertThat( location.getProtocol() ).isEqualTo( "file" );
        assertThat( Files.isRegularFile( path ) ).as( "JAR carregado por %s", type.getName() ).isTrue();
        assertThat( path.getFileName().toString() ).isEqualTo( artifact + "-3.5.0.jar" );
        try (var jar = new JarFile( path.toFile() )) {
            var metadata = jar.getJarEntry( "META-INF/maven/br.com.gems/" + artifact + "/pom.properties" );
            assertThat( metadata ).as( "Metadados Maven no JAR efetivamente carregado" ).isNotNull();
            var properties = new Properties();
            try (var stream = jar.getInputStream( metadata )) {
                properties.load( stream );
            }
            assertThat( properties.getProperty( "groupId" ) ).isEqualTo( "br.com.gems" );
            assertThat( properties.getProperty( "artifactId" ) ).isEqualTo( artifact );
            assertThat( properties.getProperty( "version" ) ).isEqualTo( "3.5.0" );
            String classEntry = type.getName().replace( '.', '/' ) + ".class";
            assertThat( jar.getJarEntry( classEntry ) ).isNotNull();
            assertThat( type.getResource( "/" + classEntry ).toExternalForm() )
                    .isEqualTo( "jar:" + location.toExternalForm() + "!/" + classEntry );
        }
    }

}
