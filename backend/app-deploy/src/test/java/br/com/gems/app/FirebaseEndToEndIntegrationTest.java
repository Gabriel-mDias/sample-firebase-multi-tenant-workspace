package br.com.gems.app;

import br.com.gems.app.FirebaseRealTestProject.Mode;
import br.com.gems.app.FirebaseRealTestProject.Namespace;
import br.com.gems.app.FirebaseRealTestProject.RunCleanup;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real HTTP, disposable PostgreSQL and the shared real Firebase test project (ADC). Only synthetic accounts of
 * this run's namespace are created, and all of them are removed even on failure. Google OAuth popups and reset
 * e-mail delivery are not automated; see specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md.
 * With {@code T029_FIREBASE_REAL=skip} (fork pull requests) the test is ignored with an explicit warning.
 */
class FirebaseEndToEndIntegrationTest {
    @Test
    void deveIntegrarHttpBrowserEFirebaseReal_quandoNamespaceDaExecucaoEIsolado() throws Exception {
        // Fail-closed preflight before Docker, before any network call and before any mutation.
        Mode mode = FirebaseRealTestProject.checkEnvironment( System::getenv );
        Assumptions.assumeTrue( mode == Mode.RUN, FirebaseRealTestProject.SKIP_NOTICE );
        Namespace namespace = Namespace.create( System::getenv, UUID::randomUUID );
        try ( var session = FirebaseRealTestProject.Session.open( "t029-parent" ) ) {
            session.verifyAccess();
            var webConfig = FirebaseRealTestProject.publicWebConfig( System::getenv, session );
            var directory = session.directory();
            int swept = FirebaseRealTestProject.sweepOrphans( directory, namespace, Instant.now(),
                    FirebaseRealTestProject.ORPHAN_MAX_AGE );
            System.out.println( "T029 ORPHAN_SWEEP prefix=" + FirebaseRealTestProject.AUTOMATION_PREFIX + " deleted="
                    + swept );
            var cleanup = new RunCleanup( namespace, directory );
            // The child cleans its own registry; this authoritative finally pass removes and rejects any residue.
            FirebaseRealTestProject.runAndClean( cleanup, () -> runSuite( namespace, webConfig ) );
        }
    }

    private void runSuite( Namespace namespace, FirebaseRealTestProject.PublicWebConfig webConfig ) throws Exception {
        var guard = Path.of( "../../frontend/src/app/core/guards/role.guard.ts" ).toAbsolutePath().normalize();
        var guardSnapshot = Files.readAllBytes( guard );
        try ( var database = new PostgreSQLContainer( "postgres:16-alpine" ).withLabel( "com.gems.sample.test",
                "T029" ) ) {
            database.start();
            var arguments = Files.createTempFile( Path.of( "target" ), "t029-classpath-", ".args" );
            Files.writeString( arguments, "-Dspring.devtools.restart.enabled=false\n-cp\n\""
                    + System.getProperty( "surefire.test.class.path", System.getProperty( "java.class.path" ) )
                            .replace( "\\", "/" )
                    + "\"\n" + EndToEndSupport.class.getName() + "\n" );
            var process = new ProcessBuilder(
                    Path.of( System.getProperty( "java.home" ), "bin", "java" ).toString(),
                    "@" + arguments.toAbsolutePath() );
            var environment = process.environment();
            // ADC (GOOGLE_APPLICATION_CREDENTIALS) is inherited on purpose; the emulator is never reachable.
            environment.remove( "FIREBASE_AUTH_EMULATOR_HOST" );
            environment.put( "DB_URL", database.getJdbcUrl() );
            environment.put( "DB_USERNAME", database.getUsername() );
            environment.put( "DB_PASSWORD", database.getPassword() );
            environment.put( "FIREBASE_PROJECT_ID", FirebaseRealTestProject.PROJECT_ID );
            environment.put( "FIREBASE_EMULATOR_ENABLED", "false" );
            environment.put( "T029_RUN_ID", namespace.runId() );
            environment.putAll( webConfig.asEnvironment() );
            process.redirectErrorStream( true );
            var child = process.start();
            var output = Thread.ofVirtual().start( () -> {
                try ( var lines = child.inputReader() ) {
                    lines.lines().forEach( System.out::println );
                } catch ( java.io.IOException failure ) {
                    System.err.println( "T029 output stream closed" );
                }
            } );
            try {
                assertThat( child.waitFor( 20, TimeUnit.MINUTES ) ).as( "T029 child completed within timeout" )
                        .isTrue();
                assertThat( child.exitValue() ).as( "T029 HTTP and browser assertions" ).isZero();
                output.join( 5000 );
            } finally {
                Files.deleteIfExists( arguments );
                if ( child.isAlive() ) {
                    child.descendants().forEach( ProcessHandle::destroy );
                    child.destroyForcibly();
                    child.waitFor( 10, TimeUnit.SECONDS );
                }
                assertThat( Files.readAllBytes( guard ) ).containsExactly( guardSnapshot );
            }
        }
    }
}
