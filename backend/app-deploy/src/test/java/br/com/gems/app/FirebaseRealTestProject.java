package br.com.gems.app;

import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Shared real Firebase test project used by the T029 end-to-end suite (ADC, never the Auth Emulator).
 * Everything here is fail-closed: the preflight runs before any mutation, only synthetic accounts of the
 * automation namespace are ever created or deleted, and nothing prints credential paths, keys, tokens
 * or service account identities. Google OAuth and reset e-mail delivery are verified by a human; see
 * specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md.
 */
public final class FirebaseRealTestProject {
    public static final String PROJECT_ID = "sample-firebase-multi-tenant";
    /** Reserved prefix: only accounts whose UID or e-mail start with it are ever touched by automation. */
    public static final String AUTOMATION_PREFIX = "gems-t029-";
    public static final String EMAIL_DOMAIN = "example.com";
    public static final Duration ORPHAN_MAX_AGE = Duration.ofHours( 2 );
    public static final String SKIP_NOTICE = "T029 AVISO: E2E Firebase real IGNORADO (T029_FIREBASE_REAL=skip, PR de fork"
            + " sem secret); nenhuma credencial, conta ou projeto foi consultado e este NAO e um PASS do gate.";

    private static final String SKIP_VARIABLE = "T029_FIREBASE_REAL";
    private static final String CREDENTIAL_VARIABLE = "GOOGLE_APPLICATION_CREDENTIALS";
    private static final GsonFactory JSON = GsonFactory.getDefaultInstance();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout( Duration.ofSeconds( 15 ) ).build();
    private static final Pattern AUTOMATION_EMAIL = Pattern
            .compile( "^" + Pattern.quote( AUTOMATION_PREFIX ) + "[a-z0-9-]+@" + Pattern.quote( EMAIL_DOMAIN ) + "$" );
    private static final Pattern CASE_NAME = Pattern.compile( "[a-z0-9-]{1,16}" );
    private static final Pattern APP_ID = Pattern.compile( "[A-Za-z0-9:_-]{5,128}" );
    private static final List<String> REQUIRED_PERMISSIONS = List.of( "firebaseauth.users.get",
            "firebaseauth.users.create", "firebaseauth.users.update", "firebaseauth.users.delete" );
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!#%+-_";

    private FirebaseRealTestProject() {
    }

    /** Whether the real suite runs or is explicitly skipped (fork pull requests only). */
    public enum Mode {
        RUN, SKIP
    }

    /** Preflight refusal. The message never carries a credential path, key, token or account identity. */
    public static final class PreflightException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public PreflightException( String message ) {
            super( message );
        }
    }

    // ------------------------------------------------------------------ preflight (no network)

    public static Mode checkEnvironment( Function<String, String> env ) {
        return checkEnvironment( env, System.out::println );
    }

    /**
     * Fail-closed validation of the process environment. The single exception is T029_FIREBASE_REAL=skip,
     * which is announced through {@code warn} and returns {@link Mode#SKIP} without reading any credential.
     */
    public static Mode checkEnvironment( Function<String, String> env, java.util.function.Consumer<String> warn ) {
        if ( "skip".equalsIgnoreCase( blankToEmpty( env.apply( SKIP_VARIABLE ) ).strip() ) ) {
            warn.accept( SKIP_NOTICE );
            return Mode.SKIP;
        }
        if ( !blankToEmpty( env.apply( "FIREBASE_AUTH_EMULATOR_HOST" ) ).isBlank() ) {
            throw new PreflightException(
                    "FIREBASE_AUTH_EMULATOR_HOST esta definido: o E2E usa somente o projeto Firebase"
                            + " real de testes " + PROJECT_ID + ". Remova a variavel do ambiente." );
        }
        String configured = blankToEmpty( env.apply( "FIREBASE_PROJECT_ID" ) ).strip();
        if ( !configured.isEmpty() && !PROJECT_ID.equals( configured ) ) {
            throw new PreflightException( "FIREBASE_PROJECT_ID diverge do projeto de testes " + PROJECT_ID + "." );
        }
        String credential = blankToEmpty( env.apply( CREDENTIAL_VARIABLE ) ).strip();
        if ( credential.isEmpty() ) {
            throw new PreflightException( "Credencial ADC ausente: defina " + CREDENTIAL_VARIABLE
                    + " com o JSON da conta de servico do projeto " + PROJECT_ID + " (ou use T029_FIREBASE_REAL=skip"
                    + " somente em PR de fork)." );
        }
        Path file;
        try {
            file = Path.of( credential );
        } catch ( InvalidPathException invalid ) {
            throw new PreflightException( "Caminho de " + CREDENTIAL_VARIABLE + " invalido." );
        }
        if ( !Files.isRegularFile( file ) || !Files.isReadable( file ) ) {
            throw new PreflightException( "Arquivo de " + CREDENTIAL_VARIABLE + " ausente ou ilegivel." );
        }
        Map<?, ?> document;
        try {
            document = JSON.fromString( Files.readString( file, StandardCharsets.UTF_8 ), Map.class );
        } catch ( IOException | RuntimeException unreadable ) {
            throw new PreflightException( "Arquivo de " + CREDENTIAL_VARIABLE + " ilegivel ou nao e JSON valido." );
        }
        if ( document == null || !PROJECT_ID.equals( document.get( "project_id" ) ) ) {
            throw new PreflightException( "O project_id da credencial ADC e ausente ou diverge do projeto de testes "
                    + PROJECT_ID + "; recusado antes de qualquer operacao." );
        }
        return Mode.RUN;
    }

    // ------------------------------------------------------------------ namespace

    /** Unique-per-execution synthetic identity namespace: reserved prefix + run id + case. */
    public record Namespace(String runId) {
        private static final Pattern RUN_ID = Pattern.compile( "[a-z0-9-]{16,40}" );

        public Namespace {
            if ( runId == null || !RUN_ID.matcher( runId ).matches() ) {
                throw new IllegalArgumentException( "Run ID invalido" );
            }
        }

        /** A fresh UUID per execution; GITHUB_RUN_ID/GITHUB_RUN_ATTEMPT only add traceability. */
        public static Namespace create( Function<String, String> env, Supplier<UUID> uuids ) {
            String token = uuids.get().toString().replace( "-", "" ).substring( 0, 16 );
            String run = digits( env.apply( "GITHUB_RUN_ID" ), 12 );
            String attempt = digits( env.apply( "GITHUB_RUN_ATTEMPT" ), 3 );
            return new Namespace(
                    run == null ? token : "gh" + run + "a" + (attempt == null ? "1" : attempt) + "-" + token );
        }

        public String prefix() {
            return AUTOMATION_PREFIX + runId + "-";
        }

        public String email( String caseName ) {
            return uid( caseName ) + "@" + EMAIL_DOMAIN;
        }

        public String uid( String caseName ) {
            if ( caseName == null || !CASE_NAME.matcher( caseName ).matches() ) {
                throw new IllegalArgumentException( "Nome de caso invalido" );
            }
            return prefix() + caseName;
        }

        /** True for any UID or e-mail that belongs to this run. */
        public boolean owns( String uid, String email ) {
            return uid != null && uid.startsWith( prefix() ) || email != null
                    && email.toLowerCase( Locale.ROOT ).startsWith( prefix() )
                    && email.toLowerCase( Locale.ROOT ).endsWith( "@" + EMAIL_DOMAIN );
        }
    }

    private static String digits( String value, int maximum ) {
        return value != null && value.matches( "[0-9]{1," + maximum + "}" ) ? value : null;
    }

    private static String blankToEmpty( String value ) {
        return value == null ? "" : value;
    }

    /** Random in-memory credential for synthetic accounts; it is never persisted or printed. */
    public static String newPassword( SecureRandom random ) {
        var password = new StringBuilder( "Aa1!" );
        for ( int index = 0; index < 28; index++ ) {
            password.append( PASSWORD_ALPHABET.charAt( random.nextInt( PASSWORD_ALPHABET.length() ) ) );
        }
        return password.toString();
    }

    // ------------------------------------------------------------------ orphan sweep and run cleanup

    /** An account as seen by the cleanup logic. */
    public record Account(String uid, String email, long createdMillis) {
    }

    /** Abstraction of the shared project's user store; faked in unit tests, real in the E2E. */
    public interface UserDirectory {
        List<Account> list();

        /** @return true when the account existed and was deleted; false when it was already absent */
        boolean delete( String uid );
    }

    /**
     * Orphan filter: only accounts whose UID carries the automation prefix AND whose e-mail matches the reserved
     * pattern, from another run and older than {@code maxAge}, qualify. Accounts of unknown age or outside
     * the prefix never do.
     */
    public static boolean isOrphan( String uid, String email, long createdMillis, long nowMillis, Duration maxAge,
            Namespace current ) {
        boolean automation = uid != null && uid.startsWith( AUTOMATION_PREFIX )
                && email != null && AUTOMATION_EMAIL.matcher( email.toLowerCase( Locale.ROOT ) ).matches();
        if ( !automation || current.owns( uid, email ) ) {
            return false;
        }
        return createdMillis > 0 && nowMillis - createdMillis >= maxAge.toMillis();
    }

    /** Deletes stale automation accounts from earlier runs; returns how many were removed. */
    public static int sweepOrphans( UserDirectory directory, Namespace current, Instant now, Duration maxAge ) {
        int deleted = 0;
        for ( Account account : directory.list() ) {
            if ( isOrphan( account.uid(), account.email(), account.createdMillis(), now.toEpochMilli(), maxAge,
                    current )
                    && directory.delete( account.uid() ) ) {
                deleted++;
            }
        }
        return deleted;
    }

    /** Outcome of a run cleanup: accounts registered and accounts that were still present and removed. */
    public record CleanupResult(int registered, int deleted) {
    }

    /** Registers every created account and guarantees they are removed, asserting an empty namespace. */
    public static final class RunCleanup {
        private final Namespace namespace;
        private final UserDirectory directory;
        private final Set<String> registered = ConcurrentHashMap.newKeySet();

        public RunCleanup( Namespace namespace, UserDirectory directory ) {
            this.namespace = namespace;
            this.directory = directory;
        }

        public void register( String uid ) {
            if ( uid == null || uid.isBlank() ) {
                throw new IllegalArgumentException( "UID invalido" );
            }
            registered.add( uid );
        }

        public Set<String> registeredUids() {
            return Set.copyOf( registered );
        }

        /**
         * Deletes every registered account, then asserts that no account of this run's namespace remains.
         * Residue not covered by the registry is still removed, but reported as a failure.
         */
        public CleanupResult closeAndAssertEmpty() {
            int deleted = 0;
            int failed = 0;
            for ( String uid : registered ) {
                try {
                    if ( directory.delete( uid ) ) {
                        deleted++;
                    }
                } catch ( RuntimeException failure ) {
                    failed++;
                }
            }
            List<Account> remaining = directory.list().stream()
                    .filter( account -> namespace.owns( account.uid(), account.email() ) ).toList();
            for ( Account account : remaining ) {
                try {
                    directory.delete( account.uid() );
                } catch ( RuntimeException failure ) {
                    failed++;
                }
            }
            if ( failed > 0 || !remaining.isEmpty() ) {
                throw new AssertionError( "Namespace do run nao ficou vazio: " + remaining.size()
                        + " conta(s) restante(s) e " + failed + " falha(s) de remocao" );
            }
            return new CleanupResult( registered.size(), deleted );
        }
    }

    /** Test body that may fail; used with {@link #runAndClean}. */
    public interface Body {
        void run() throws Exception;
    }

    /**
     * The E2E wiring: run the body and, in {@code finally}, delete the registered accounts and assert an empty
     * namespace. A body failure stays primary; a cleanup failure is attached to it (or thrown when alone).
     */
    public static void runAndClean( RunCleanup cleanup, Body body ) throws Exception {
        Throwable failure = null;
        try {
            body.run();
        } catch ( Throwable bodyFailure ) {
            failure = bodyFailure;
            throw bodyFailure;
        } finally {
            try {
                var result = cleanup.closeAndAssertEmpty();
                System.out.println( "T029 NAMESPACE remaining=0 registered=" + result.registered() + " deleted="
                        + result.deleted() );
            } catch ( Throwable cleanupFailure ) {
                if ( failure == null ) {
                    throw cleanupFailure;
                }
                failure.addSuppressed( cleanupFailure );
            }
        }
    }

    /** Child-process wiring of the same guarantee; returns false (and reports without secrets) on residue. */
    public static boolean finishRun( RunCleanup cleanup ) {
        try {
            var result = cleanup.closeAndAssertEmpty();
            System.out.println( "T029 USERS registered=" + result.registered() + " deletedByCleanup="
                    + result.deleted() + " remaining=0" );
            return true;
        } catch ( Throwable failure ) {
            System.err.println( "T029 FAIL CLEANUP " + failure.getClass().getSimpleName() );
            if ( failure instanceof AssertionError && failure.getMessage() != null ) {
                System.err.println( "T029 ASSERT " + failure.getMessage().replace( '\n', ' ' ) );
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ real project access (ADC)

    /** Raw answer of the IAM permission check. */
    public record IamResponse(int status, String body) {
    }

    /** The IAM permission check; a lambda in unit tests, the real call in the E2E. */
    public interface IamProbe {
        IamResponse call() throws IOException, InterruptedException;
    }

    /**
     * Fail-closed: only a positive 200 answer listing every required firebaseauth.users.* permission passes.
     * Transport errors, non-200 answers (for example a disabled IAM API) and partial grants all refuse the run
     * before any mutation, without sensitive details.
     */
    public static void requireWritePermissions( IamProbe probe ) {
        IamResponse response;
        try {
            response = probe.call();
        } catch ( IOException failure ) {
            throw new PreflightException(
                    "Permissoes da ADC nao confirmadas (falha de rede ou token); recusado antes de qualquer mutacao." );
        } catch ( InterruptedException failure ) {
            Thread.currentThread().interrupt();
            throw new PreflightException( "Verificacao de permissoes da ADC interrompida; recusado." );
        }
        failOnQuota( response.status(), response.body(), "testIamPermissions" );
        if ( response.status() != 200 ) {
            throw new PreflightException( "Permissoes da ADC nao confirmadas (HTTP " + response.status()
                    + "): habilite a Cloud Resource Manager API do projeto " + PROJECT_ID
                    + " e confirme as permissoes; recusado antes de qualquer mutacao." );
        }
        Object granted;
        try {
            granted = JSON.fromString( response.body(), Map.class ).get( "permissions" );
        } catch ( IOException | RuntimeException malformed ) {
            throw new PreflightException( "Resposta de permissoes da ADC ilegivel; recusado." );
        }
        var missing = new ArrayList<String>( REQUIRED_PERMISSIONS );
        if ( granted instanceof List<?> values ) {
            missing.removeAll( values );
        }
        if ( !missing.isEmpty() ) {
            throw new PreflightException( "ADC sem permissao no projeto " + PROJECT_ID + ": " + missing + "." );
        }
    }

    /** Translates Admin failures into explicit, secret-free messages; quota is never retried. */
    static IllegalStateException translate( String operation, FirebaseAuthException failure ) {
        ErrorCode code = failure.getErrorCode();
        if ( code == ErrorCode.RESOURCE_EXHAUSTED ) {
            return new IllegalStateException( "Quota do Firebase excedida (RESOURCE_EXHAUSTED) em " + operation
                    + "; sem retry automatico." );
        }
        if ( code == ErrorCode.PERMISSION_DENIED ) {
            return new IllegalStateException( "ADC sem permissao no projeto " + PROJECT_ID + " em " + operation + "." );
        }
        if ( code == ErrorCode.UNAUTHENTICATED ) {
            return new IllegalStateException( "Credencial ADC rejeitada pelo projeto " + PROJECT_ID + " em " + operation
                    + "." );
        }
        return new IllegalStateException( "Firebase real falhou em " + operation + " (" + code + "/"
                + failure.getAuthErrorCode() + ")." );
    }

    /** Sets the synthetic account password through the real Admin SDK (the backend gateway never handles passwords). */
    public static void setPassword( FirebaseAuth auth, String uid, String password ) {
        try {
            auth.updateUser( new UserRecord.UpdateRequest( uid ).setPassword( password ) );
        } catch ( FirebaseAuthException failure ) {
            throw translate( "updateUser(password)", failure );
        }
    }

    /** Fails explicitly on quota exhaustion; there is deliberately no backoff that could hide a credential error. */
    static void failOnQuota( int status, String body, String operation ) {
        String text = body == null ? "" : body;
        if ( status == 429 || text.contains( "RESOURCE_EXHAUSTED" ) || text.contains( "QUOTA_EXCEEDED" )
                || text.contains( "TOO_MANY_ATTEMPTS_TRY_LATER" ) ) {
            throw new IllegalStateException( "Quota do Firebase excedida (HTTP " + status + ") em " + operation
                    + "; sem retry automatico." );
        }
    }

    /** User store backed by the real Admin SDK of the shared test project. */
    public static final class AdminDirectory implements UserDirectory {
        private final FirebaseAuth auth;

        AdminDirectory( FirebaseAuth auth ) {
            this.auth = auth;
        }

        @Override
        public List<Account> list() {
            var accounts = new ArrayList<Account>();
            try {
                for ( ExportedUserRecord user : auth.listUsers( null ).iterateAll() ) {
                    accounts.add( new Account( user.getUid(), user.getEmail(),
                            user.getUserMetadata() == null ? 0 : user.getUserMetadata().getCreationTimestamp() ) );
                }
            } catch ( FirebaseAuthException failure ) {
                throw translate( "listUsers", failure );
            }
            return accounts;
        }

        @Override
        public boolean delete( String uid ) {
            try {
                auth.deleteUser( uid );
                return true;
            } catch ( FirebaseAuthException failure ) {
                if ( failure.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND ) {
                    return false;
                }
                throw translate( "deleteUser", failure );
            }
        }
    }

    /** An Admin SDK app on the shared project, authorized by ADC only. */
    public static final class Session implements AutoCloseable {
        private final FirebaseApp app;
        private final GoogleCredentials credentials;

        private Session( FirebaseApp app, GoogleCredentials credentials ) {
            this.app = app;
            this.credentials = credentials;
        }

        public static Session open( String name ) {
            try {
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                FirebaseApp app = FirebaseApp.initializeApp( FirebaseOptions.builder().setProjectId( PROJECT_ID )
                        .setCredentials( credentials ).setConnectTimeout( 15_000 ).setReadTimeout( 30_000 ).build(),
                        name );
                return new Session( app, credentials );
            } catch ( IOException | IllegalStateException failure ) {
                throw new PreflightException( "A credencial ADC nao pode ser carregada pelo Admin SDK." );
            }
        }

        public FirebaseAuth auth() {
            return FirebaseAuth.getInstance( app );
        }

        public AdminDirectory directory() {
            return new AdminDirectory( auth() );
        }

        /** Read probe plus, when the IAM API answers, the exact write permissions; no account is mutated. */
        public void verifyAccess() {
            try {
                auth().getUser( AUTOMATION_PREFIX + "preflight-" + UUID.randomUUID() );
                throw new PreflightException( "Consulta de preflight retornou uma conta inesperada." );
            } catch ( FirebaseAuthException failure ) {
                if ( failure.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND ) {
                    IllegalStateException translated = translate( "preflight getUser", failure );
                    throw new PreflightException( translated.getMessage() );
                }
            }
            verifyPermissions();
        }

        private void verifyPermissions() {
            requireWritePermissions( () -> {
                String body = JSON.toString( Map.of( "permissions", REQUIRED_PERMISSIONS ) );
                var response = HTTP.send( HttpRequest
                        .newBuilder( URI.create( "https://cloudresourcemanager.googleapis.com/v1/projects/"
                                + PROJECT_ID + ":testIamPermissions" ) )
                        .timeout( Duration.ofSeconds( 30 ) )
                        .header( "Content-Type", "application/json" ).header( "Authorization", "Bearer " + bearer() )
                        .POST( HttpRequest.BodyPublishers.ofString( body ) ).build(),
                        HttpResponse.BodyHandlers.ofString() );
                return new IamResponse( response.statusCode(), response.body() );
            } );
        }

        String bearer() throws IOException {
            GoogleCredentials scoped = credentials.createScoped( "https://www.googleapis.com/auth/cloud-platform" );
            scoped.refreshIfExpired();
            if ( scoped.getAccessToken() == null ) {
                scoped.refresh();
            }
            return scoped.getAccessToken().getTokenValue();
        }

        @Override
        public void close() {
            app.delete();
        }
    }

    // ------------------------------------------------------------------ public web app configuration

    /** Public Firebase Web configuration (not a secret) obtained at runtime; never versioned or logged. */
    public record PublicWebConfig(String apiKey, String authDomain, String projectId, String appId) {
        public PublicWebConfig {
            if ( blankToEmpty( apiKey ).isBlank() || blankToEmpty( authDomain ).isBlank()
                    || blankToEmpty( appId ).isBlank() || !PROJECT_ID.equals( projectId ) ) {
                throw new PreflightException( "Configuracao publica do app Web incompleta." );
            }
        }

        static Optional<PublicWebConfig> fromEnvironment( Function<String, String> env ) {
            String key = blankToEmpty( env.apply( "T029_FIREBASE_API_KEY" ) ).strip();
            String domain = blankToEmpty( env.apply( "T029_FIREBASE_AUTH_DOMAIN" ) ).strip();
            String app = blankToEmpty( env.apply( "T029_FIREBASE_APP_ID" ) ).strip();
            return key.isEmpty() || domain.isEmpty() || app.isEmpty()
                    ? Optional.empty()
                    : Optional.of( new PublicWebConfig( key, domain, PROJECT_ID, app ) );
        }

        /** Variables handed to child processes; the values must not be echoed. */
        public Map<String, String> asEnvironment() {
            return Map.of( "T029_FIREBASE_API_KEY", apiKey, "T029_FIREBASE_AUTH_DOMAIN", authDomain,
                    "T029_FIREBASE_APP_ID", appId, "T029_FIREBASE_PROJECT_ID", projectId );
        }

        @Override
        public String toString() {
            return "PublicWebConfig[redacted]";
        }
    }

    /** Child side: the parent already resolved the configuration and passed it through the environment. */
    public static PublicWebConfig inheritedWebConfig( Function<String, String> env ) {
        return PublicWebConfig.fromEnvironment( env )
                .orElseThrow( () -> new PreflightException( "Configuracao publica do app Web nao herdada." ) );
    }

    /**
     * Environment overrides (T029_FIREBASE_API_KEY, T029_FIREBASE_AUTH_DOMAIN, T029_FIREBASE_APP_ID) win;
     * otherwise the Firebase management API is read with ADC. No Web app is ever created by automation.
     */
    public static PublicWebConfig publicWebConfig( Function<String, String> env, Session session ) {
        Optional<PublicWebConfig> override = PublicWebConfig.fromEnvironment( env );
        if ( override.isPresent() ) {
            return override.get();
        }
        String base = "https://firebase.googleapis.com/v1beta1/projects/" + PROJECT_ID + "/webApps";
        try {
            String appId = blankToEmpty( env.apply( "T029_FIREBASE_APP_ID" ) ).strip();
            if ( appId.isEmpty() ) {
                Object apps = managementGet( session, base + "?pageSize=100" ).get( "apps" );
                if ( !(apps instanceof List<?> list) || list.isEmpty() ) {
                    throw new PreflightException(
                            "O projeto " + PROJECT_ID + " nao tem app Web registrado; registre-o no"
                                    + " console (a automacao nao cria apps) ou informe as variaveis T029_FIREBASE_*." );
                }
                appId = chooseApp( list );
            }
            if ( !APP_ID.matcher( appId ).matches() ) {
                throw new PreflightException( "appId do app Web invalido." );
            }
            Map<?, ?> config = managementGet( session, base + "/" + appId + "/config" );
            return new PublicWebConfig( text( config, "apiKey" ), text( config, "authDomain" ), PROJECT_ID,
                    text( config, "appId" ) );
        } catch ( IOException failure ) {
            throw new PreflightException( "Falha de rede ao ler a configuracao publica do app Web." );
        } catch ( InterruptedException failure ) {
            Thread.currentThread().interrupt();
            throw new PreflightException( "Leitura da configuracao publica interrompida." );
        }
    }

    /** Prefers the app named sample-web, otherwise the first registered Web app. */
    private static String chooseApp( List<?> apps ) {
        String first = "";
        for ( Object item : apps ) {
            if ( item instanceof Map<?, ?> app && app.get( "appId" ) instanceof String id ) {
                if ( "sample-web".equals( app.get( "displayName" ) ) ) {
                    return id;
                }
                if ( first.isEmpty() ) {
                    first = id;
                }
            }
        }
        return first;
    }
    private static String text( Map<?, ?> values, String key ) {
        return values.get( key ) instanceof String value ? value : "";
    }

    private static Map<?, ?> managementGet( Session session, String url ) throws IOException, InterruptedException {
        var response = HTTP.send( HttpRequest.newBuilder( URI.create( url ) ).timeout( Duration.ofSeconds( 30 ) )
                .header( "Authorization", "Bearer " + session.bearer() ).GET().build(),
                HttpResponse.BodyHandlers.ofString() );
        failOnQuota( response.statusCode(), response.body(), "firebase management API" );
        if ( response.statusCode() == 403 || response.statusCode() == 401 ) {
            throw new PreflightException( "ADC sem permissao para ler o app Web (firebase.clients.list/get); defina"
                    + " T029_FIREBASE_API_KEY, T029_FIREBASE_AUTH_DOMAIN e T029_FIREBASE_APP_ID." );
        }
        if ( response.statusCode() != 200 ) {
            throw new PreflightException( "A API de gerenciamento do Firebase respondeu HTTP " + response.statusCode()
                    + " ao ler o app Web." );
        }
        return JSON.fromString( response.body(), Map.class );
    }
}
