package br.com.gems.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.gems.app.FirebaseRealTestProject.Account;
import br.com.gems.app.FirebaseRealTestProject.Mode;
import br.com.gems.app.FirebaseRealTestProject.Namespace;
import br.com.gems.app.FirebaseRealTestProject.PreflightException;
import br.com.gems.app.FirebaseRealTestProject.RunCleanup;
import br.com.gems.app.FirebaseRealTestProject.UserDirectory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Network-free coverage of the real Firebase preflight, namespace, orphan filter and cleanup guarantees. */
class FirebaseRealTestPreflightTest {
    private static final String SECRET_MARKER = "fake-service-account@diverge.iam.gserviceaccount.com";
    private static final Namespace RUN = new Namespace( "0123456789abcdef" );

    @TempDir
    Path directory;

    private Map<String, String> environmentWith( Path credential ) {
        var environment = new HashMap<String, String>();
        environment.put( "GOOGLE_APPLICATION_CREDENTIALS", credential.toString() );
        return environment;
    }

    private Path credential( String projectJson ) throws IOException {
        return Files.writeString( directory.resolve( "adc.json" ),
                "{\"type\":\"service_account\"," + projectJson + "\"client_email\":\"" + SECRET_MARKER + "\"}" );
    }

    @Test
    void deveFalharSemRede_quandoACredencialAdcEstaAusente() {
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( key -> null, line -> {
        } ) ).isInstanceOf( PreflightException.class ).hasMessageContaining( "GOOGLE_APPLICATION_CREDENTIALS" );
    }

    @Test
    void deveFalharSemVazarOCaminho_quandoOArquivoDaCredencialNaoExiste() {
        var missing = directory.resolve( "ausente-segredo.json" );
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( environmentWith( missing )::get, line -> {
        } ) ).isInstanceOf( PreflightException.class )
                .satisfies( failure -> assertThat( failure.getMessage() ).doesNotContain( "ausente-segredo" )
                        .doesNotContain( directory.toString() ) );
    }

    @Test
    void deveFalhar_quandoACredencialNaoEJsonValido() throws IOException {
        var broken = Files.writeString( directory.resolve( "adc.json" ), "nao e json" );
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( environmentWith( broken )::get, line -> {
        } ) ).isInstanceOf( PreflightException.class ).hasMessageContaining( "JSON" );
    }

    @Test
    void deveFalharSemVazarDados_quandoOProjectIdDivergeDoProjetoDeTestes() throws IOException {
        var other = credential( "\"project_id\":\"outro-projeto-real\"," );
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( environmentWith( other )::get, line -> {
        } ) ).isInstanceOf( PreflightException.class ).hasMessageContaining( "diverge" )
                .satisfies( failure -> assertThat( failure.getMessage() ).doesNotContain( SECRET_MARKER )
                        .doesNotContain( "outro-projeto-real" ).doesNotContain( directory.toString() ) );
    }

    @Test
    void deveFalhar_quandoACredencialNaoTemProjectId() throws IOException {
        var anonymous = credential( "" );
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( environmentWith( anonymous )::get, line -> {
        } ) ).isInstanceOf( PreflightException.class ).hasMessageContaining( "project_id" );
    }

    @Test
    void deveAceitar_quandoOProjectIdConfereComOProjetoDeTestes() throws IOException {
        var valid = credential( "\"project_id\":\"" + FirebaseRealTestProject.PROJECT_ID + "\"," );
        assertThat( FirebaseRealTestProject.checkEnvironment( environmentWith( valid )::get, line -> {
        } ) ).isEqualTo( Mode.RUN );
    }

    @Test
    void deveFalhar_quandoOHostDoEmuladorEstaDefinido() throws IOException {
        var valid = credential( "\"project_id\":\"" + FirebaseRealTestProject.PROJECT_ID + "\"," );
        var environment = environmentWith( valid );
        environment.put( "FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099" );
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( environment::get, line -> {
        } ) ).isInstanceOf( PreflightException.class ).hasMessageContaining( "FIREBASE_AUTH_EMULATOR_HOST" );
    }

    @Test
    void deveFalhar_quandoOProjetoDoAmbienteDivergeDoProjetoDeTestes() throws IOException {
        var valid = credential( "\"project_id\":\"" + FirebaseRealTestProject.PROJECT_ID + "\"," );
        var environment = environmentWith( valid );
        environment.put( "FIREBASE_PROJECT_ID", "demo-gems" );
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( environment::get, line -> {
        } ) ).isInstanceOf( PreflightException.class ).hasMessageContaining( "FIREBASE_PROJECT_ID" );
    }

    @Test
    void deveIgnorarComAvisoSemLerCredencial_quandoASuiteRealEstaEmSkip() {
        var warnings = new ArrayList<String>();
        var environment = new HashMap<String, String>();
        environment.put( "T029_FIREBASE_REAL", " SKIP " );
        assertThat( FirebaseRealTestProject.checkEnvironment( environment::get, warnings::add ) )
                .isEqualTo( Mode.SKIP );
        assertThat( warnings ).singleElement().asString().contains( "IGNORADO" ).contains( "T029_FIREBASE_REAL=skip" );
    }

    @Test
    void deveContinuarFalhando_quandoOValorDaVariavelDeSkipNaoESkip() {
        var environment = new HashMap<String, String>();
        environment.put( "T029_FIREBASE_REAL", "false" );
        assertThatThrownBy( () -> FirebaseRealTestProject.checkEnvironment( environment::get, line -> {
        } ) ).isInstanceOf( PreflightException.class );
    }

    @Test
    void deveGerarRunIdsDistintos_quandoDuasExecucoesComecamNoMesmoAmbiente() {
        var first = Namespace.create( key -> null, UUID::randomUUID );
        var second = Namespace.create( key -> null, UUID::randomUUID );
        assertThat( first.runId() ).isNotEqualTo( second.runId() );
        assertThat( first.email( "alice" ) ).isNotEqualTo( second.email( "alice" ) );
        assertThat( first.email( "alice" ) ).startsWith( FirebaseRealTestProject.AUTOMATION_PREFIX )
                .endsWith( "@example.com" ).hasSizeLessThanOrEqualTo( 64 + "@example.com".length() );
    }

    @Test
    void deveIncluirOsIdentificadoresDoGithub_quandoElesExistem() {
        var environment = Map.of( "GITHUB_RUN_ID", "9876543210", "GITHUB_RUN_ATTEMPT", "2" );
        var namespace = Namespace.create( environment::get, UUID::randomUUID );
        assertThat( namespace.runId() ).startsWith( "gh9876543210a2-" );
        assertThat( namespace.uid( "m-group-profile" ) ).startsWith( namespace.prefix() )
                .hasSizeLessThanOrEqualTo( 128 );
        assertThat( namespace.email( "m-group-profile" ).indexOf( '@' ) ).isLessThanOrEqualTo( 64 );
    }

    @Test
    void deveIgnorarIdentificadoresInvalidosDoGithub_quandoNaoSaoNumericos() {
        var environment = Map.of( "GITHUB_RUN_ID", "../etc", "GITHUB_RUN_ATTEMPT", "x" );
        assertThat( Namespace.create( environment::get, UUID::randomUUID ).runId() ).doesNotStartWith( "gh" );
    }

    @Test
    void deveRecusarNomeDeCaso_quandoOFormatoNaoEPermitido() {
        assertThatThrownBy( () -> RUN.email( "Alice@evil" ) ).isInstanceOf( IllegalArgumentException.class );
        assertThatThrownBy( () -> RUN.uid( "" ) ).isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    void deveReconhecerSomenteOsProprios_quandoOLimiteDeNamespaceEConsultado() {
        assertThat( RUN.owns( RUN.uid( "bob" ), null ) ).isTrue();
        assertThat( RUN.owns( UUID.randomUUID().toString(), RUN.email( "bob" ) ) ).isTrue();
        assertThat( RUN.owns( "gems-t029-fedcba9876543210-bob", null ) ).isFalse();
        assertThat( RUN.owns( "outra-conta", "gente@example.com" ) ).isFalse();
        assertThat( RUN.owns( "x", RUN.prefix() + "bob@evil.example" ) ).isFalse();
    }

    @Test
    void deveEleger_quandoContaDeAutomacaoAntigaDeOutraExecucaoTemUidEEmailDoPrefixo() {
        long now = Instant.parse( "2026-10-10T12:00:00Z" ).toEpochMilli();
        long old = now - Duration.ofHours( 3 ).toMillis();
        var other = "gems-t029-fedcba9876543210-bob";
        assertThat( FirebaseRealTestProject.isOrphan( other, other + "@example.com", old, now,
                FirebaseRealTestProject.ORPHAN_MAX_AGE, RUN ) ).isTrue();
    }

    @Test
    void deveNaoEleger_quandoSoOUidOuSoOEmailPertenceAoPrefixo() {
        long now = Instant.parse( "2026-10-10T12:00:00Z" ).toEpochMilli();
        long old = now - Duration.ofHours( 3 ).toMillis();
        var other = "gems-t029-fedcba9876543210-bob";
        var maxAge = FirebaseRealTestProject.ORPHAN_MAX_AGE;
        assertThat( FirebaseRealTestProject.isOrphan( other, null, old, now, maxAge, RUN ) ).as( "so UID" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( other, "gente@example.com", old, now, maxAge, RUN ) )
                .as( "UID do prefixo com e-mail alheio" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( UUID.randomUUID().toString(), other + "@example.com", old, now,
                maxAge, RUN ) ).as( "so e-mail" ).isFalse();
    }

    @Test
    void deveNaoEleger_quandoAContaForaDoPrefixoOuRecenteOuDoRunAtual() {
        long now = Instant.parse( "2026-10-10T12:00:00Z" ).toEpochMilli();
        long old = now - Duration.ofHours( 3 ).toMillis();
        long young = now - Duration.ofMinutes( 30 ).toMillis();
        var maxAge = FirebaseRealTestProject.ORPHAN_MAX_AGE;
        var other = "gems-t029-fedcba9876543210-bob";
        assertThat(
                FirebaseRealTestProject.isOrphan( "operador-central", "gabriel@example.com", old, now, maxAge, RUN ) )
                .as( "fora do prefixo" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( "xgems-t029-aaaa", "x-gems-t029-a@example.com", old, now, maxAge,
                RUN ) ).as( "prefixo no meio" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( other, "gems-t029-a@outro-dominio.com", old, now, maxAge, RUN ) )
                .as( "dominio diferente" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( other, other + "@example.com", young, now, maxAge, RUN ) )
                .as( "mais recente que o limite" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( other, other + "@example.com", 0, now, maxAge, RUN ) )
                .as( "idade desconhecida" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( RUN.uid( "bob" ), RUN.email( "bob" ), old, now, maxAge, RUN ) )
                .as( "run atual" ).isFalse();
        assertThat( FirebaseRealTestProject.isOrphan( null, null, old, now, maxAge, RUN ) ).isFalse();
    }
    @Test
    void deveApagarSomenteOrfaos_quandoAVarreduraPercorreOProjeto() {
        long now = Instant.parse( "2026-10-10T12:00:00Z" ).toEpochMilli();
        long old = now - Duration.ofHours( 3 ).toMillis();
        var store = new FakeDirectory();
        store.add( "gems-t029-fedcba9876543210-bob", "gems-t029-fedcba9876543210-bob@example.com", old );
        store.add( "operador-central", "operador@example.com", old );
        store.add( "gems-t029-fedcba9876543211-new", "gems-t029-fedcba9876543211-new@example.com", now );
        store.add( RUN.uid( "alice" ), RUN.email( "alice" ), old );
        assertThat( FirebaseRealTestProject.sweepOrphans( store, RUN, Instant.ofEpochMilli( now ),
                FirebaseRealTestProject.ORPHAN_MAX_AGE ) ).isOne();
        assertThat( store.accounts.keySet() ).containsExactlyInAnyOrder( "operador-central",
                "gems-t029-fedcba9876543211-new", RUN.uid( "alice" ) );
    }

    @Test
    void deveDeixarONamespaceVazio_quandoOsIdsRegistradosSaoApagadosNoFinally() {
        var store = new FakeDirectory();
        var cleanup = new RunCleanup( RUN, store );
        for ( String caze : List.of( "alice", "bob" ) ) {
            store.add( RUN.uid( caze ), RUN.email( caze ), 1 );
            cleanup.register( RUN.uid( caze ) );
        }
        store.add( "operador-central", "operador@example.com", 1 );
        var result = cleanup.closeAndAssertEmpty();
        assertThat( result.registered() ).isEqualTo( 2 );
        assertThat( result.deleted() ).isEqualTo( 2 );
        assertThat( store.accounts.keySet() ).containsExactly( "operador-central" );
    }

    @Test
    void deveFalharAAfirmacaoDeNamespaceVazio_quandoORegistroNaoApagaAsContas() {
        var store = new FakeDirectory();
        store.deleteEnabled = false;
        var cleanup = new RunCleanup( RUN, store );
        store.add( RUN.uid( "alice" ), RUN.email( "alice" ), 1 );
        cleanup.register( RUN.uid( "alice" ) );
        assertThatThrownBy( cleanup::closeAndAssertEmpty ).isInstanceOf( AssertionError.class )
                .hasMessageContaining( "Namespace do run" );
    }

    @Test
    void deveFalharEApagarOResiduo_quandoUmaContaDoNamespaceNaoFoiRegistrada() {
        var store = new FakeDirectory();
        store.add( RUN.uid( "esquecida" ), RUN.email( "esquecida" ), 1 );
        var cleanup = new RunCleanup( RUN, store );
        assertThatThrownBy( cleanup::closeAndAssertEmpty ).isInstanceOf( AssertionError.class );
        assertThat( store.accounts ).isEmpty();
    }

    @Test
    void deveGerarSenhaAleatoria_quandoUmaContaSinteticaPrecisaDeCredencial() {
        var random = new SecureRandom();
        var first = FirebaseRealTestProject.newPassword( random );
        assertThat( first ).hasSize( 32 ).isNotEqualTo( FirebaseRealTestProject.newPassword( random ) );
    }

    @Test
    void deveEsconderAConfiguracaoPublica_quandoOObjetoEImpresso() {
        var config = new FirebaseRealTestProject.PublicWebConfig( "chave-publica", "dominio.example",
                FirebaseRealTestProject.PROJECT_ID, "1:1:web:1" );
        assertThat( config.toString() ).doesNotContain( "chave-publica" ).doesNotContain( "dominio.example" );
    }

    private static final String ALL_PERMISSIONS = "{\"permissions\":[\"firebaseauth.users.get\","
            + "\"firebaseauth.users.create\",\"firebaseauth.users.update\",\"firebaseauth.users.delete\"]}";

    @Test
    void deveAceitar_quandoOIamConfirmaTodasAsPermissoesDeEscrita() {
        FirebaseRealTestProject
                .requireWritePermissions( () -> new FirebaseRealTestProject.IamResponse( 200, ALL_PERMISSIONS ) );
    }

    @Test
    void deveRecusarAntesDeMutar_quandoOIamDevolveConcessaoParcial() {
        var partial = "{\"permissions\":[\"firebaseauth.users.get\"]}";
        assertThatThrownBy( () -> FirebaseRealTestProject
                .requireWritePermissions( () -> new FirebaseRealTestProject.IamResponse( 200, partial ) ) )
                .isInstanceOf( PreflightException.class ).hasMessageContaining( "firebaseauth.users.create" )
                .hasMessageContaining( "firebaseauth.users.delete" );
        assertThatThrownBy( () -> FirebaseRealTestProject
                .requireWritePermissions( () -> new FirebaseRealTestProject.IamResponse( 200, "{}" ) ) )
                .isInstanceOf( PreflightException.class );
    }

    @Test
    void deveRecusarAntesDeMutar_quandoOIamNaoResponde200() {
        assertThatThrownBy( () -> FirebaseRealTestProject
                .requireWritePermissions( () -> new FirebaseRealTestProject.IamResponse( 403, "{\"error\":{}}" ) ) )
                .isInstanceOf( PreflightException.class ).hasMessageContaining( "HTTP 403" )
                .hasMessageContaining( "nao confirmadas" );
    }

    @Test
    void deveRecusarAntesDeMutar_quandoOIamFalhaPorTransporte() {
        assertThatThrownBy( () -> FirebaseRealTestProject.requireWritePermissions( () -> {
            throw new IOException( "caminho-secreto" );
        } ) ).isInstanceOf( PreflightException.class ).hasMessageContaining( "nao confirmadas" )
                .satisfies( failure -> assertThat( failure.getMessage() ).doesNotContain( "caminho-secreto" ) );
    }

    @Test
    void deveRecusarAntesDeMutar_quandoOCorpoDoIamEIlegivel() {
        assertThatThrownBy( () -> FirebaseRealTestProject
                .requireWritePermissions( () -> new FirebaseRealTestProject.IamResponse( 200, "nao e json" ) ) )
                .isInstanceOf( PreflightException.class );
    }

    @Test
    void deveFalharPorQuota_quandoOIamDevolve429() {
        assertThatThrownBy( () -> FirebaseRealTestProject.requireWritePermissions(
                () -> new FirebaseRealTestProject.IamResponse( 429,
                        "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\"}}" ) ) )
                .hasMessageContaining( "Quota" );
    }

    @Test
    void deveApagarNoFinally_quandoOCorpoDaSuiteFalha() {
        var store = new FakeDirectory();
        var cleanup = new RunCleanup( RUN, store );
        assertThatThrownBy( () -> FirebaseRealTestProject.runAndClean( cleanup, () -> {
            store.add( RUN.uid( "alice" ), RUN.email( "alice" ), 1 );
            cleanup.register( RUN.uid( "alice" ) );
            throw new IllegalStateException( "falha da suite" );
        } ) ).isInstanceOf( IllegalStateException.class ).hasMessage( "falha da suite" );
        assertThat( store.accounts ).isEmpty();
    }

    @Test
    void deveApagarNoFinally_quandoOCorpoDaSuiteTermina() throws Exception {
        var store = new FakeDirectory();
        var cleanup = new RunCleanup( RUN, store );
        FirebaseRealTestProject.runAndClean( cleanup, () -> {
            store.add( RUN.uid( "bob" ), RUN.email( "bob" ), 1 );
            cleanup.register( RUN.uid( "bob" ) );
        } );
        assertThat( store.accounts ).isEmpty();
    }

    @Test
    void deveFalharNoFinally_quandoOCorpoTerminaEAContaNaoPodeSerApagada() {
        var store = new FakeDirectory();
        store.deleteEnabled = false;
        var cleanup = new RunCleanup( RUN, store );
        assertThatThrownBy( () -> FirebaseRealTestProject.runAndClean( cleanup, () -> {
            store.add( RUN.uid( "bob" ), RUN.email( "bob" ), 1 );
            cleanup.register( RUN.uid( "bob" ) );
        } ) ).isInstanceOf( AssertionError.class ).hasMessageContaining( "Namespace do run" );
    }

    @Test
    void deveManterAFalhaDoCorpoComoPrimaria_quandoALimpezaTambemFalha() {
        var store = new FakeDirectory();
        store.deleteEnabled = false;
        var cleanup = new RunCleanup( RUN, store );
        assertThatThrownBy( () -> FirebaseRealTestProject.runAndClean( cleanup, () -> {
            store.add( RUN.uid( "bob" ), RUN.email( "bob" ), 1 );
            cleanup.register( RUN.uid( "bob" ) );
            throw new IllegalStateException( "falha da suite" );
        } ) ).isInstanceOf( IllegalStateException.class ).hasMessage( "falha da suite" )
                .satisfies( failure -> assertThat( failure.getSuppressed() ).singleElement()
                        .isInstanceOf( AssertionError.class ) );
    }

    @Test
    void deveFinalizarOFilhoComSucessoSoSeONamespaceFicaVazio_quandoRegistraEApaga() {
        var store = new FakeDirectory();
        var cleanup = new RunCleanup( RUN, store );
        store.add( RUN.uid( "alice" ), RUN.email( "alice" ), 1 );
        cleanup.register( RUN.uid( "alice" ) );
        assertThat( FirebaseRealTestProject.finishRun( cleanup ) ).isTrue();
        assertThat( store.accounts ).isEmpty();
        store.deleteEnabled = false;
        store.add( RUN.uid( "bob" ), RUN.email( "bob" ), 1 );
        cleanup.register( RUN.uid( "bob" ) );
        assertThat( FirebaseRealTestProject.finishRun( cleanup ) ).isFalse();
    }
    /** In-memory stand-in for the shared project's user store. */
    private static final class FakeDirectory implements UserDirectory {
        private final Map<String, Account> accounts = new LinkedHashMap<>();
        private boolean deleteEnabled = true;

        void add( String uid, String email, long created ) {
            accounts.put( uid, new Account( uid, email, created ) );
        }

        @Override
        public List<Account> list() {
            return List.copyOf( accounts.values() );
        }

        @Override
        public boolean delete( String uid ) {
            return deleteEnabled && accounts.remove( uid ) != null;
        }
    }
}
