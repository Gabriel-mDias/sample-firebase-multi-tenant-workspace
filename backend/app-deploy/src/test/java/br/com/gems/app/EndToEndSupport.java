package br.com.gems.app;

import br.com.gems.app.FirebaseRealTestProject.Namespace;
import br.com.gems.app.FirebaseRealTestProject.PublicWebConfig;
import br.com.gems.app.FirebaseRealTestProject.RunCleanup;
import br.com.gems.app.FirebaseRealTestProject.UserDirectory;
import br.com.gems.app.tenant.SampleTenantMigration;
import br.com.gems.firebase.auth.DelegationPolicy;
import br.com.gems.firebase.auth.AuthorizationResolver;
import br.com.gems.firebase.auth.IdTokenVerifier;
import br.com.gems.firebase.auth.FirebaseAuthException;
import br.com.gems.firebase.auth.FirebaseAdminGateway;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.SecurityEventSink;
import br.com.gems.firebase.auth.TenantAliasValidator;
import br.com.gems.firebase.auth.jdbc.JdbcGrantService;
import br.com.gems.firebase.auth.jdbc.JdbcIdentityCommandWorker;
import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import br.com.gems.firebase.auth.jdbc.JdbcTenantService;
import br.com.gems.firebase.auth.jdbc.JdbcUserService;
import br.com.gems.firebase.auth.jdbc.JdbcSecurityCatalog;
import br.com.gems.security.authorization.AuthorizationCatalog;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import br.com.gems.tenant.JpaTenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.sql.DataSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Child process: runs the real backend against the shared real Firebase test project (ADC) outside the
 * deterministic backend test JVM. Every account is synthetic, namespaced per run and removed in {@code finally}.
 */
public final class EndToEndSupport {
    private static final String SAME_ID = "00000000-0000-0000-0000-000000000123";
    private static final GsonFactory JSON = GsonFactory.getDefaultInstance();
    private final ConfigurableApplicationContext application;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout( Duration.ofSeconds( 5 ) ).build();
    private final JdbcClient jdbc;
    private final JdbcUserService users;
    private final JdbcTenantService tenants;
    private final JdbcGrantService grants;
    private final JdbcTenantProvisioner provisioner;
    private final FirebaseAdminGateway admin;
    private final AuthorizationCatalog catalog;
    private final String api;
    private final Namespace namespace;
    private final RunCleanup cleanup;
    private final UserDirectory directory;
    private final FirebaseAuth realAdmin;
    private final PublicWebConfig webConfig;
    private final String password = FirebaseRealTestProject.newPassword( new SecureRandom() );
    private String aliceToken;
    private String bobToken;
    private UUID alice;
    private UUID bob;
    private UUID alpha;
    private UUID beta;
    private UUID aliceMember;
    private UUID bobAlpha;
    private UUID bobBeta;
    private String checkpoint = "STARTUP";

    private EndToEndSupport( ConfigurableApplicationContext application, Namespace namespace, RunCleanup cleanup,
            FirebaseRealTestProject.Session session, PublicWebConfig webConfig ) {
        this.application = application;
        this.namespace = namespace;
        this.cleanup = cleanup;
        this.webConfig = webConfig;
        realAdmin = session.auth();
        directory = session.directory();
        jdbc = application.getBean( "firebaseSecurityJdbcClient", JdbcClient.class );
        var tx = application.getBean( "firebaseSecurityTransactions", TransactionTemplate.class );
        var events = application.getBean( SecurityEventSink.class );
        var clock = application.getBean( Clock.class );
        catalog = application.getBean( AuthorizationCatalog.class );
        var actor = SecurityActorProvider.trustedBootstrap( UUID.randomUUID() );
        users = new JdbcUserService( jdbc, tx, events, clock, FirebaseRealTestProject.PROJECT_ID, catalog, actor );
        tenants = new JdbcTenantService( jdbc, tx, events, clock, application.getBean( TenantAliasValidator.class ),
                catalog, actor );
        grants = new JdbcGrantService( jdbc, tx, events, catalog, application.getBean( DelegationPolicy.class ), clock,
                actor );
        provisioner = application.getBean( JdbcTenantProvisioner.class );
        admin = application.getBean( FirebaseAdminGateway.class );
        api = "http://localhost:" + ((WebServerApplicationContext) application).getWebServer().getPort();
    }

    /** Real-provider mode: no emulator profile or host, so the SDK validates the project and uses ADC. */
    private static String[] arguments() {
        return new String[]{"--spring.config.location=" + Path.of( "src/main/resources/application.yml" )
                .toAbsolutePath().toUri(), "--server.port=0", "--logging.level.root=OFF",
                "--spring.main.banner-mode=off", "--app.security.cors.allowed-origins=http://localhost:4301",
                "--spring.threads.virtual.enabled=false", "--server.tomcat.threads.max=1",
                "--server.tomcat.threads.min-spare=1"};
    }

    public static void main( String[] args ) {
        int exit = 1;
        EndToEndSupport suite = null;
        FirebaseRealTestProject.Session session = null;
        RunCleanup cleanup = null;
        try {
            session = FirebaseRealTestProject.Session.open( "t029-admin" );
            var namespace = new Namespace( System.getenv( "T029_RUN_ID" ) );
            cleanup = new RunCleanup( namespace, session.directory() );
            var webConfig = FirebaseRealTestProject.inheritedWebConfig( System::getenv );
            try ( var context = new SpringApplicationBuilder( AppApplication.class, Observation.class )
                    .run( arguments() ) ) {
                suite = new EndToEndSupport( context, namespace, cleanup, session, webConfig );
                suite.seed();
                suite.http();
                suite.constraints();
                suite.threadCleanup();
                suite.historicalIdentity();
                suite.validityChain();
                suite.negativeGates();
                suite.providerOutage();
                suite.lifecycle();
                suite.remoteRefusals();
                suite.remoteAckRace();
                suite.browser();
                suite.membership();
                System.out.println( "T029 SUITE PASS" );
                exit = 0;
            }
        } catch ( Throwable failure ) {
            exit = 1;
            if ( failure instanceof java.util.concurrent.ExecutionException
                    && failure.getCause() instanceof AssertionError ) {
                failure = failure.getCause();
            }
            // Never print remote exception causes, bearer values, passwords or reset links.
            System.err.println( "T029 FAIL " + (suite == null ? "STARTUP" : suite.checkpoint) + " "
                    + failure.getClass().getSimpleName() );
            if ( failure.getMessage() != null && (failure instanceof AssertionError
                    || failure instanceof IllegalStateException) ) {
                System.err.println( "T029 ASSERT " + failure.getMessage()
                        .replaceAll( "(?i)(Bearer\\s+|eyJ)[^\\s]+", "[REDACTED]" )
                        .replaceAll( "[A-Za-z0-9._-]+@[A-Za-z0-9.-]*gserviceaccount\\.com", "[REDACTED]" )
                        .replace( '\n', ' ' ).replace( '\r', ' ' ) );
            }
        } finally {
            if ( cleanup != null && !FirebaseRealTestProject.finishRun( cleanup ) ) {
                exit = 1;
            }
            if ( session != null ) {
                session.close();
            }
        }
        System.exit( exit );
    }

    private void seed() throws Exception {
        checkpoint = "SEED";
        alice = create( "Alice", "alice" );
        bind( alice );
        aliceToken = login( "alice" );
        bob = create( "Bob", "bob" );
        bind( bob );
        bobToken = login( "bob" );
        expect( "GET", "/api/auth/me", bobToken, null, null, 200 );
        alpha = tenants.create( "alpha", "Alpha" );
        beta = tenants.create( "beta", "Beta" );
        provisioner.provision( alpha, 1, new SampleTenantMigration() );
        provisioner.provision( beta, 1, new SampleTenantMigration() );
        aliceMember = tenants.addMember( alpha, alice );
        bobAlpha = tenants.addMember( alpha, bob );
        bobBeta = tenants.addMember( beta, bob );
        for ( String action : catalog.tenantActions() ) {
            grants.grant( GrantAdministration.Target.MEMBER, alpha, bobAlpha, action );
            grants.grant( GrantAdministration.Target.MEMBER, beta, bobBeta, action );
            if ( !action.equals( "VISUALIZAR_VALOR_RESTRITO" ) ) {
                grants.grant( GrantAdministration.Target.MEMBER, alpha, aliceMember, action );
            }
        }
        for ( String action : catalog.globalActions() ) {
            grants.grantGlobal( bob, action );
        }
        for ( String alias : List.of( "alpha", "beta" ) ) {
            jdbc.sql( "INSERT INTO tenant_" + alias
                    + ".demo_registro VALUES(:id,:name,:description,:value,now(),NULL)" )
                    .param( "id", UUID.fromString( SAME_ID ) )
                    .param( "name", alias.equals( "alpha" ) ? "Alpha" : "Beta" )
                    .param( "description", alias + " somente" )
                    .param( "value", alias.equals( "alpha" ) ? 123.45 : 999.99 )
                    .update();
        }
        passed();
    }

    private void http() throws Exception {
        checkpoint = "HTTP_AUTH_ISOLATION_READY_ESCALATION";
        expect( "GET", "/api/auth/me", null, null, null, 401 );
        expect( "GET", "/api/auth/me", "invalid", null, null, 401 );
        assertThat( expect( "GET", "/api/auth/me", aliceToken, null, null, 200 ) ).contains( "Alice" )
                .doesNotContain( "cd_uid_planejado" );
        expect( "GET", "/api/auth/context", aliceToken, "alpha", null, 200 );
        expect( "GET", "/api/auth/context", aliceToken, "beta", null, 403 );
        expect( "GET", "/api/auth/context", bobToken, null, null, 403 );
        assertThat( expect( "GET", "/api/demo-registros/" + SAME_ID, bobToken, "alpha", null, 200 ) )
                .contains( "Alpha" ).doesNotContain( "Beta" );
        assertThat( expect( "GET", "/api/demo-registros/" + SAME_ID, bobToken, "beta", null, 200 ) )
                .contains( "Beta" ).doesNotContain( "Alpha" );
        assertThat( expect( "GET", "/api/demo-registros/" + SAME_ID, aliceToken, "alpha", null, 200 ) )
                .doesNotContain( "123.45" );
        expect( "GET", "/api/demo-registros/" + SAME_ID, aliceToken, "beta", null, 403 );
        expect( "POST", "/api/admin/central/tenants", aliceToken, null,
                "{\"alias\":\"evil\",\"name\":\"Evil\"}", 403 );
        expect( "POST", "/api/admin/tenants/" + alpha + "/grants", aliceToken, "alpha",
                "{\"ownerId\":\"" + aliceMember + "\",\"action\":\"VISUALIZAR_VALOR_RESTRITO\"}", 403 );
        expect( "POST", "/api/admin/tenants/" + beta + "/memberships", aliceToken, "alpha",
                "{\"userId\":\"" + alice + "\"}", 403 );
        var pending = tenants.create( "pending", "Pending" );
        tenants.addMember( pending, bob );
        expect( "GET", "/api/auth/context", bobToken, "pending", null, 403 );
        jdbc.sql( "UPDATE security.tenant SET nr_revisao_aplicada=0 WHERE id_tenant=:id" ).param( "id", alpha )
                .update();
        try {
            expect( "GET", "/api/auth/context", bobToken, "alpha", null, 403 );
        } finally {
            jdbc.sql( "UPDATE security.tenant SET nr_revisao_aplicada=1 WHERE id_tenant=:id" ).param( "id", alpha )
                    .update();
        }
        passed();
    }

    private void constraints() {
        checkpoint = "SQL_COMPOSITE_SCOPE_INTERVAL_IDENTITY_CONSTRAINTS";
        var pa = grants.createProfile( alpha, "P", "Profile" );
        var pb = grants.createProfile( beta, "P", "Profile" );
        var ga = grants.createGroup( alpha, "G", "Group" );
        var gb = grants.createGroup( beta, "G", "Group" );
        var role = role( "CONSULTAR_DEMO" );
        var global = role( "CONSULTAR_PROPRIA_CONTA" );
        for ( Object[] invalid : List.of( new Object[]{"perfil_role", "id_perfil", pb, "id_role", role},
                new Object[]{"usuario_tenant_role", "id_usuario_tenant", bobBeta, "id_role", role},
                new Object[]{"grupo_role", "id_grupo", gb, "id_role", role},
                new Object[]{"usuario_tenant_perfil", "id_usuario_tenant", bobAlpha, "id_perfil", pb},
                new Object[]{"usuario_tenant_perfil", "id_usuario_tenant", bobBeta, "id_perfil", pa},
                new Object[]{"grupo_usuario", "id_grupo", ga, "id_usuario_tenant", bobBeta},
                new Object[]{"grupo_usuario", "id_grupo", gb, "id_usuario_tenant", bobAlpha},
                new Object[]{"grupo_perfil", "id_grupo", ga, "id_perfil", pb},
                new Object[]{"grupo_perfil", "id_grupo", gb, "id_perfil", pa} ) ) {
            invalidInsert( invalid, alpha );
        }
        for ( Object[] invalid : List.of( new Object[]{"perfil_role", "id_perfil", pa, "id_role", global},
                new Object[]{"grupo_role", "id_grupo", ga, "id_role", global},
                new Object[]{"usuario_tenant_role", "id_usuario_tenant", bobAlpha, "id_role", global} ) ) {
            invalidInsert( invalid, alpha );
        }
        assertThatThrownBy( () -> jdbc.sql( "INSERT INTO security.usuario_role_global "
                + "(id_usuario_role_global,id_usuario,id_role,dt_inicio) VALUES(:id,:user,:role,now())" )
                .param( "id", UUID.randomUUID() ).param( "user", alice ).param( "role", role ).update() )
                .isInstanceOf( DataIntegrityViolationException.class );
        grants.link( GrantAdministration.Link.MEMBER_PROFILE, alpha, bobAlpha, pa );
        grants.link( GrantAdministration.Link.GROUP_MEMBER, alpha, ga, bobAlpha );
        grants.link( GrantAdministration.Link.GROUP_PROFILE, alpha, ga, pa );
        grants.grant( GrantAdministration.Target.PROFILE, alpha, pa, "CONSULTAR_DEMO" );
        grants.grant( GrantAdministration.Target.GROUP, alpha, ga, "CONSULTAR_DEMO" );
        for ( String table : List.of( "usuario", "usuario_identidade", "provedor_identidade", "tenant",
                "usuario_tenant",
                "role_seguranca", "perfil", "grupo", "perfil_role", "grupo_role", "usuario_tenant_role",
                "usuario_tenant_perfil", "grupo_usuario", "grupo_perfil", "usuario_role_global" ) ) {
            assertThatThrownBy( () -> jdbc.sql( "UPDATE security." + table
                    + " SET dt_fim=dt_inicio-interval '1 second'" ).update() ).as( table )
                    .isInstanceOf( DataIntegrityViolationException.class );
        }
        assertThatThrownBy(
                () -> jdbc.sql( "UPDATE security.usuario_identidade SET id_usuario=:other WHERE id_usuario=:id" )
                        .param( "id", bob ).param( "other", alice ).update() )
                .isInstanceOf( DataIntegrityViolationException.class );
        assertThatThrownBy( () -> jdbc.sql( "UPDATE security.tenant SET cd_alias='changed' WHERE id_tenant=:id" )
                .param( "id", alpha ).update() ).isInstanceOf( DataIntegrityViolationException.class );
        passed();
    }

    private void invalidInsert( Object[] values, UUID tenant ) {
        assertThatThrownBy( () -> jdbc.sql( "INSERT INTO security." + values[0] + "(id_" + values[0] + ",id_tenant,"
                + values[1] + "," + values[3] + ",dt_inicio) VALUES(:id,:tenant,:left,:right,now())" )
                .param( "id", UUID.randomUUID() ).param( "tenant", tenant ).param( "left", values[2] )
                .param( "right", values[4] ).update() ).as( values[0].toString() )
                .isInstanceOf( DataIntegrityViolationException.class );
    }

    private void historicalIdentity() throws Exception {
        checkpoint = "HISTORICAL_UID_UNIQUE_AFTER_IDENTITY_END";
        // The concurrent Google first-link race needs a real Google OAuth sign-in, which is outside the
        // automation (human verification: specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md).
        var id = create( "Historical", "historical" );
        bind( id );
        String token = login( "historical" );
        expect( "GET", "/api/auth/me", token, null, null, 200 );
        jdbc.sql( "UPDATE security.usuario_identidade SET dt_fim=dt_inicio WHERE id_usuario=:id" ).param( "id", id )
                .update();
        expect( "GET", "/api/auth/me", token, null, null, 403 );
        assertThatThrownBy( () -> jdbc.sql( "INSERT INTO security.usuario_identidade SELECT :id,:user,"
                + "id_provedor_identidade,cd_emissor,cd_sujeito_externo,cd_email_identidade,dt_inicio,NULL "
                + "FROM security.usuario_identidade WHERE id_usuario=:owner" ).param( "id", UUID.randomUUID() )
                .param( "user", alice ).param( "owner", id ).update() )
                .isInstanceOf( DataIntegrityViolationException.class ).satisfies( failure -> assertThat(
                        ((java.sql.SQLException) ((DataIntegrityViolationException) failure).getMostSpecificCause())
                                .getSQLState() )
                        .isEqualTo( "23505" ) );
        System.out.println( "T029 HISTORICAL_UID duplicateHistoricalUidRejected=23505" );
        passed();
    }

    private void lifecycle() throws Exception {
        checkpoint = "OUTBOX_RESTART_REMOTE_DISABLE_REVOKE_LATEST_VERSION";
        var id = create( "Lifecycle", "lifecycle" );
        // Persisted expired claim survives constructing a new worker instance.
        jdbc.sql(
                "UPDATE security.comando_identidade SET cd_estado='CLAIMED',cd_claim=:claim,dt_lease=now()-interval '1 minute'"
                        + " WHERE id_usuario=:id" )
                .param( "claim", UUID.randomUUID() ).param( "id", id ).update();
        bind( id );
        var uid = plannedUid( id );
        var token = login( "lifecycle" );
        expect( "GET", "/api/auth/me", token, null, null, 200 );
        users.suspend( id );
        expect( "GET", "/api/auth/me", token, null, null, 403 );
        drain();
        assertThat( admin.getUser( uid ).disabled() ).isTrue();
        expectSettled( "GET", "/api/auth/me", token, 401 );
        users.requestReactivation( id );
        users.cancelReactivation( id );
        drain();
        assertThat( admin.getUser( uid ).disabled() ).isTrue();
        assertThat( jdbc.sql( "SELECT count(*) FROM security.comando_identidade WHERE id_usuario=:id"
                + " AND cd_tipo='ENABLE' AND cd_estado='OBSOLETE'" ).param( "id", id ).query( Long.class ).single() )
                .isOne();
        assertThat(
                jdbc.sql( "SELECT count(*) FROM security.usuario_role_global WHERE id_usuario=:id AND dt_fim IS NULL" )
                        .param( "id", id ).query( Long.class ).single() )
                .isZero();
        passed();
    }

    /** Revoke, disable and delete are applied by the real Admin SDK; the real verifier must refuse each. */
    private void remoteRefusals() throws Exception {
        checkpoint = "REAL_ADMIN_REVOKE_DISABLE_DELETE_REFUSED";
        var id = create( "Refusal", "refusal" );
        bind( id );
        var uid = plannedUid( id );
        String token = login( "refusal" );
        expect( "GET", "/api/auth/me", token, null, null, 200 );
        // Token validity and revocation have one-second resolution: separate the boundaries.
        Thread.sleep( 1500 );
        admin.revokeRefreshTokens( uid );
        Thread.sleep( 1500 );
        expectSettled( "after-revoke", "GET", "/api/auth/me", token, 401 );
        String renewed = login( "refusal" );
        expect( "GET", "/api/auth/me", renewed, null, null, 200 );
        admin.setDisabled( uid, true );
        expectSettled( "after-disable", "GET", "/api/auth/me", renewed, 401 );
        admin.setDisabled( uid, false );
        expectSettled( "after-enable", "GET", "/api/auth/me", renewed, 200 );
        assertThat( directory.delete( uid ) ).as( "real Admin delete" ).isTrue();
        expectSettled( "after-delete", "GET", "/api/auth/me", renewed, 401 );
        passed();
    }

    private void remoteAckRace() throws Exception {
        checkpoint = "NATIVE_ADMIN_CREATE_ACK_CAS_INFLIGHT";
        var id = create( "Remote race", "remote-race" );
        var reached = new CountDownLatch( 1 );
        var release = new CountDownLatch( 1 );
        var forwarding = new FirebaseAdminGateway() {
            public User getUser( String uid ) {
                return admin.getUser( uid );
            }
            public User createUser( String uid, String email, String name ) {
                var created = admin.createUser( uid, email, name );
                reached.countDown();
                try {
                    if ( !release.await( 20, TimeUnit.SECONDS ) )
                        throw new IllegalStateException( "RACE_TIMEOUT" );
                } catch ( InterruptedException failure ) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException( "RACE_INTERRUPTED" );
                }
                return created;
            }
            public void setDisabled( String uid, boolean disabled ) {
                admin.setDisabled( uid, disabled );
            }
            public void revokeRefreshTokens( String uid ) {
                admin.revokeRefreshTokens( uid );
            }
            public String passwordResetLink( String email ) {
                throw new UnsupportedOperationException();
            }
        };
        var worker = new JdbcIdentityCommandWorker( jdbc,
                application.getBean( "firebaseSecurityTransactions", TransactionTemplate.class ),
                application.getBean( SecurityEventSink.class ),
                application.getBean( "firebaseSecurityDataSource", DataSource.class ), forwarding,
                application.getBean( Clock.class ), Duration.ofMinutes( 2 ) );
        try ( var executor = Executors.newSingleThreadExecutor() ) {
            var pending = executor.submit( worker::runOnce );
            try {
                assertThat( reached.await( 20, TimeUnit.SECONDS ) ).as( "real Admin CREATE reached before ACK" )
                        .isTrue();
                var uid = jdbc.sql( "SELECT cd_uid_planejado FROM security.usuario WHERE id_usuario=:id" )
                        .param( "id", id ).query( String.class ).single();
                assertThat( admin.getUser( uid ).email() ).isEqualTo( namespace.email( "remote-race" ) );
                assertNamespaced( uid );
                // Deliberately adversarial SQL writer bypasses the service advisory lock.
                // This is the actual persisted-version CAS boundary, with real Admin forwarding.
                jdbc.sql( "UPDATE security.usuario SET nr_versao_identidade=nr_versao_identidade+1,"
                        + "dt_fim=now() WHERE id_usuario=:id" ).param( "id", id ).update();
            } finally {
                release.countDown();
            }
            assertThat( pending.get( 20, TimeUnit.SECONDS ) ).isTrue();
        }
        assertThat( jdbc.sql( "SELECT cd_estado FROM security.comando_identidade WHERE id_usuario=:id"
                + " AND cd_tipo='CREATE'" ).param( "id", id ).query( String.class ).single() ).isEqualTo( "OBSOLETE" );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_identidade WHERE id_usuario=:id" )
                .param( "id", id ).query( Long.class ).single() ).isZero();
        FirebaseRealTestProject.setPassword( realAdmin, plannedUid( id ), password );
        expect( "GET", "/api/auth/me", login( "remote-race" ), null, null, 403 );
        passed();
    }

    private void threadCleanup() throws Exception {
        checkpoint = "NATIVE_SERVLET_REUSED_THREAD_SUCCESS_DENIAL_EXCEPTION_CLEANUP";
        assertThat( tomcatProtocol().getMaxThreads() ).as( "segregated cleanup phase worker limit" ).isOne();
        Observation.requests.clear();
        assertThat( expect( "GET", "/api/demo-registros/" + SAME_ID, bobToken, "alpha", null, 200 ) )
                .contains( "Alpha" ).doesNotContain( "Beta" );
        expect( "GET", "/api/auth/context", aliceToken, "beta", null, 403 );
        expect( "GET", "/api/demo-registros/not-a-uuid", bobToken, "alpha", null, 400 );
        assertThat( Observation.requests ).hasSize( 3 ).allSatisfy( observation -> {
            assertThat( observation.beforeClean() ).as( "thread initial state" ).isTrue();
            assertThat( observation.afterClean() ).as( "security/tenant/actor/transaction final state" ).isTrue();
        } );
        assertThat( Observation.requests.stream().map( ObservedRequest::thread ).distinct().count() )
                .as( "actual servlet thread reused across success, denial and MVC exception" ).isOne();
        assertThat( Observation.requests.stream().map( ObservedRequest::status ).toList() )
                .containsExactly( 200, 403, 400 );
        System.out.println( "T029 WORKER_PHASE cleanup max=1 observedThreads=1 statuses=200,403,400" );
        passed();
    }

    private org.apache.coyote.AbstractProtocol<?> tomcatProtocol() {
        var server = (org.springframework.boot.tomcat.TomcatWebServer) ((WebServerApplicationContext) application)
                .getWebServer();
        return (org.apache.coyote.AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler();
    }

    private record ObservedRequest(long thread, boolean beforeClean, boolean afterClean, int status) {
    }

    /** Explicitly installed in the child only; observes the real chain and poisons its input. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Observation {
        private static final ConcurrentLinkedQueue<ObservedRequest> requests = new ConcurrentLinkedQueue<>();
        private static final Clock clock = Clock.tickMillis( java.time.ZoneOffset.UTC );
        @Bean
        @org.springframework.context.annotation.Primary
        public Clock t029Clock() {
            return clock;
        }
        @Bean
        public FilterRegistrationBean<OncePerRequestFilter> t029ThreadObserver() {
            var registration = new FilterRegistrationBean<OncePerRequestFilter>();
            registration.setOrder( Ordered.HIGHEST_PRECEDENCE );
            registration.setFilter( new OncePerRequestFilter() {
                protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response,
                        FilterChain chain ) throws ServletException, IOException {
                    boolean before = clean();
                    JpaTenantContext.setCurrentTenant( "beta" );
                    MDC.put( "actorId", "t029-poisoned-actor" );
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken( "t029-poisoned-user", null, List.of() ) );
                    try {
                        chain.doFilter( request, response );
                    } finally {
                        requests.add( new ObservedRequest( Thread.currentThread().threadId(), before, clean(),
                                response.getStatus() ) );
                    }
                }
                private boolean clean() {
                    return JpaTenantContext.current().isEmpty()
                            && SecurityContextHolder.getContext().getAuthentication() == null
                            && MDC.get( "actorId" ) == null
                            && !TransactionSynchronizationManager.isActualTransactionActive()
                            && TransactionSynchronizationManager.getResourceMap().isEmpty();
                }
            } );
            return registration;
        }
    }

    private void validityChain() throws Exception {
        checkpoint = "AUTHORIZATION_EVERY_EDGE_EXPIRED_AND_FUTURE";
        var resolver = application.getBean( AuthorizationResolver.class );
        var verifier = application.getBean( IdTokenVerifier.class );
        var tx = application.getBean( "firebaseSecurityTransactions", TransactionTemplate.class );
        var clock = application.getBean( Clock.class );
        int observed = 0;
        for ( String path : List.of( "DIRECT", "PROFILE", "GROUP", "GROUP_PROFILE", "GLOBAL" ) ) {
            // Password-provisioned accounts: the Google provider path needs real OAuth (human verification).
            String caze = "m-" + path.toLowerCase( java.util.Locale.ROOT ).replace( '_', '-' );
            var user = create( "Matrix", caze );
            bind( user );
            var token = login( caze );
            var identity = verifier.verify( token );
            resolver.resolve( identity, Optional.empty(), clock.instant() );
            var member = tenants.addMember( alpha, user );
            var profile = grants.createProfile( alpha, path + "P", "Matrix profile" );
            var group = grants.createGroup( alpha, path + "G", "Matrix group" );
            var edges = new HashMap<String, UUID>();
            edges.put( "usuario", user );
            edges.put( "usuario_identidade",
                    jdbc.sql( "SELECT id_usuario_identidade FROM security.usuario_identidade WHERE id_usuario=:id" )
                            .param( "id", user ).query( UUID.class ).single() );
            edges.put( "provedor_identidade",
                    jdbc.sql( "SELECT id_provedor_identidade FROM security.provedor_identidade" ).query( UUID.class )
                            .single() );
            if ( path.equals( "GLOBAL" ) ) {
                edges.clear();
                edges.put( "usuario_role_global", grants.grantGlobal( user, "CONSULTAR_TENANT" ) );
                edges.put( "role_seguranca", role( "CONSULTAR_TENANT" ) );
            } else {
                edges.put( "tenant", alpha );
                edges.put( "usuario_tenant", member );
                edges.put( "role_seguranca", role( "CONSULTAR_DEMO" ) );
                switch ( path ) {
                    case "DIRECT" -> edges.put( "usuario_tenant_role",
                            grants.grant( GrantAdministration.Target.MEMBER, alpha, member, "CONSULTAR_DEMO" ) );
                    case "PROFILE" -> {
                        edges.put( "usuario_tenant_perfil",
                                grants.link( GrantAdministration.Link.MEMBER_PROFILE, alpha, member, profile ) );
                        edges.put( "perfil", profile );
                        edges.put( "perfil_role",
                                grants.grant( GrantAdministration.Target.PROFILE, alpha, profile, "CONSULTAR_DEMO" ) );
                    }
                    case "GROUP", "GROUP_PROFILE" -> {
                        edges.put( "grupo_usuario",
                                grants.link( GrantAdministration.Link.GROUP_MEMBER, alpha, group, member ) );
                        edges.put( "grupo", group );
                        if ( path.equals( "GROUP" ) ) {
                            edges.put( "grupo_role",
                                    grants.grant( GrantAdministration.Target.GROUP, alpha, group, "CONSULTAR_DEMO" ) );
                        } else {
                            edges.put( "grupo_perfil",
                                    grants.link( GrantAdministration.Link.GROUP_PROFILE, alpha, group, profile ) );
                            edges.put( "perfil", profile );
                            edges.put( "perfil_role", grants.grant( GrantAdministration.Target.PROFILE, alpha, profile,
                                    "CONSULTAR_DEMO" ) );
                        }
                    }
                    default -> throw new AssertionError( path );
                }
            }
            var alias = path.equals( "GLOBAL" ) ? Optional.<String>empty() : Optional.of( "alpha" );
            String action = path.equals( "GLOBAL" ) ? "CONSULTAR_TENANT" : "CONSULTAR_DEMO";
            for ( var edge : edges.entrySet() ) {
                for ( boolean future : List.of( false, true ) ) {
                    tx.executeWithoutResult( status -> {
                        status.setRollbackOnly();
                        var baseline = resolver.resolve( identity, alias, clock.instant() ).context();
                        assertThat( path.equals( "GLOBAL" ) ? baseline.globalActions() : baseline.tenantActions() )
                                .contains( action );
                        String key = edge.getKey().equals( "role_seguranca" ) ? "id_role" : "id_" + edge.getKey();
                        jdbc.sql( "UPDATE security." + edge.getKey() + " SET "
                                + (future ? "dt_inicio=now()+interval '1 day'" : "dt_fim=dt_inicio") + " WHERE " + key
                                + "=:id" )
                                .param( "id", edge.getValue() ).update();
                        try {
                            var changed = resolver.resolve( identity, alias, clock.instant() ).context();
                            assertThat( path.equals( "GLOBAL" ) ? changed.globalActions() : changed.tenantActions() )
                                    .as( path + ":" + edge.getKey() + ":" + future ).doesNotContain( action );
                        } catch ( FirebaseAuthException denied ) {
                            assertThat( denied.reason() ).isEqualTo( FirebaseAuthException.Reason.LOCAL_DENIED );
                        }
                    } );
                    observed++;
                }
            }
        }
        assertThat( observed ).isGreaterThanOrEqualTo( 48 );
        passed();
        System.out.println( "T029 CHAIN_CASES " + observed );
    }

    private void negativeGates() throws Exception {
        checkpoint = "SCANNER_CATALOG_AND_INTEGRATION_MUTATIONS_RESTORED";
        var mapping = application.getBean( "requestMappingHandlerMapping",
                org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class );
        var route = org.springframework.web.servlet.mvc.method.RequestMappingInfo.paths( "/api/t029-unclassified" )
                .build();
        var scanner = application.getBean( "firebaseEndpointAuthorizationScan",
                org.springframework.beans.factory.SmartInitializingSingleton.class );
        mapping.registerMapping( route, new UnsafeEndpoint(), UnsafeEndpoint.class.getMethod( "unsafe" ) );
        try {
            assertThatThrownBy( scanner::afterSingletonsInstantiated )
                    .hasMessageContaining( "endpoint sem classifica" );
        } finally {
            mapping.unregisterMapping( route );
        }
        scanner.afterSingletonsInstantiated();
        var tenantActions = new java.util.HashSet<>( catalog.tenantActions() );
        var globalActions = new java.util.HashSet<>( catalog.globalActions() );
        tenantActions.remove( "CONSULTAR_DEMO" );
        globalActions.add( "CONSULTAR_DEMO" );
        var mutated = new JdbcSecurityCatalog( jdbc,
                application.getBean( "firebaseSecurityTransactions", TransactionTemplate.class ),
                application.getBean( SecurityEventSink.class ),
                new AuthorizationCatalog( globalActions, tenantActions ),
                () -> tenantActions, application.getBean( Clock.class ), FirebaseRealTestProject.PROJECT_ID );
        assertThatThrownBy( mutated::initialize ).isInstanceOf( IllegalStateException.class );
        application.getBean( JdbcSecurityCatalog.class ).initialize();
        jdbc.sql( "UPDATE tenant_beta.demo_registro SET nm_registro='Alpha' WHERE id_demo_registro=:id" )
                .param( "id", UUID.fromString( SAME_ID ) ).update();
        try {
            assertThatThrownBy(
                    () -> assertThat( expect( "GET", "/api/demo-registros/" + SAME_ID, bobToken, "beta", null, 200 ) )
                            .contains( "Beta" ) )
                    .isInstanceOf( AssertionError.class );
        } finally {
            jdbc.sql( "UPDATE tenant_beta.demo_registro SET nm_registro='Beta' WHERE id_demo_registro=:id" )
                    .param( "id", UUID.fromString( SAME_ID ) ).update();
        }
        assertThat( expect( "GET", "/api/demo-registros/" + SAME_ID, bobToken, "beta", null, 200 ) ).contains( "Beta" );
        passed();
    }

    /** Only registered temporarily to prove the actual sample scanner rejects a mutated handler set. */
    public static final class UnsafeEndpoint {
        public String unsafe() {
            return "unsafe";
        }
    }

    private void providerOutage() throws Exception {
        checkpoint = "LOCAL_FAULT_INJECTION_PROVIDER_OUTAGE_503_AND_DURABLE_LOCAL_BLOCK";
        // Deterministic and local: a second backend instance whose Admin SDK can reach its identity provider only
        // through an unreachable loopback proxy. The shared Firebase project is never stopped or degraded.
        var id = create( "Outage", "outage" );
        bind( id );
        String token = login( "outage" );
        String uid = plannedUid( id );
        expect( "GET", "/api/auth/me", token, null, null, 200 );
        int unreachable = unreachablePort();
        try ( var degraded = degradedInstance( unreachable ) ) {
            String degradedApi = "http://localhost:"
                    + ((WebServerApplicationContext) degraded.context()).getWebServer().getPort();
            String body = expectAt( degradedApi, "GET", "/api/auth/me", token, null, null, 503 );
            assertThat( body ).isEqualTo( "{\"code\":\"PROVIDER_UNAVAILABLE\"}" ).doesNotContain( "127.0.0.1" )
                    .doesNotContain( String.valueOf( unreachable ) )
                    .doesNotContain( FirebaseRealTestProject.PROJECT_ID )
                    .doesNotContain( token );
            // The same bearer stays valid on the healthy instance: the 503 belongs to the degraded provider path.
            expect( "GET", "/api/auth/me", token, null, null, 200 );
            users.suspend( id );
            drain( degraded.context().getBean( FirebaseAdminGateway.class ) );
            assertThat( jdbc.sql( "SELECT dt_fim IS NOT NULL FROM security.usuario WHERE id_usuario=:id" )
                    .param( "id", id ).query( Boolean.class ).single() ).isTrue();
            assertThat( jdbc.sql(
                    "SELECT count(*) FROM security.comando_identidade WHERE id_usuario=:id AND cd_estado='RETRY'" )
                    .param( "id", id ).query( Long.class ).single() ).isPositive();
            expect( "GET", "/api/auth/me", token, null, null, 403 );
        }
        // Recovery: the provider path works again, the durable commands complete and the remote state converges.
        jdbc.sql( "UPDATE security.comando_identidade SET dt_proxima_tentativa=now() WHERE id_usuario=:id" )
                .param( "id", id ).update();
        drain();
        assertThat( jdbc.sql(
                "SELECT count(*) FROM security.comando_identidade WHERE id_usuario=:id AND cd_estado IN ('RETRY','PENDING','CLAIMED')" )
                .param( "id", id ).query( Long.class ).single() ).isZero();
        assertThat( admin.getUser( uid ).disabled() ).isTrue();
        expectSettled( "GET", "/api/auth/me", token, 401 );
        expect( "GET", "/api/auth/me", aliceToken, null, null, 200 );
        passed();
    }

    private static int unreachablePort() throws IOException {
        // Reserve and release an ephemeral loopback port: nothing listens on it afterwards.
        try ( var reservation = new ServerSocket( 0, 1, InetAddress.getLoopbackAddress() ) ) {
            return reservation.getLocalPort();
        }
    }

    private DegradedInstance degradedInstance( int unreachable ) {
        var transport = new NetHttpTransport.Builder()
                .setProxy( new Proxy( Proxy.Type.HTTP, new InetSocketAddress( "127.0.0.1", unreachable ) ) ).build();
        var app = FirebaseApp.initializeApp(
                FirebaseOptions.builder().setProjectId( FirebaseRealTestProject.PROJECT_ID )
                        .setCredentials( GoogleCredentials
                                .create( new AccessToken( "fault-injection", new Date( Long.MAX_VALUE ) ) ) )
                        .setHttpTransport( transport ).setConnectTimeout( 2_000 ).setReadTimeout( 2_000 ).build(),
                "t029-degraded" );
        DegradedProvider.app = app;
        try {
            return new DegradedInstance( app,
                    new SpringApplicationBuilder( AppApplication.class, DegradedProvider.class ).run( arguments() ) );
        } catch ( RuntimeException failure ) {
            app.delete();
            throw failure;
        }
    }

    /** A second, local backend instance whose Admin SDK only reaches an unreachable endpoint. */
    private record DegradedInstance(FirebaseApp app, ConfigurableApplicationContext context) implements AutoCloseable {
        @Override
        public void close() {
            try {
                context.close();
            } finally {
                app.delete();
            }
        }
    }

    /** Replaces only the Admin app of the degraded instance; verifier, gateway, SQL and HTTP stay the real ones. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class DegradedProvider {
        private static volatile FirebaseApp app;

        @Bean(destroyMethod = "")
        public FirebaseApp t029DegradedFirebaseApp() {
            return app;
        }
    }

    private void browser() throws Exception {
        checkpoint = "BROWSER";
        var frontend = Path.of( "../../frontend" ).toAbsolutePath().normalize();
        // Google popup and password reset e-mail delivery are verified by a human, not by this automation.
        assertThat( browserGate( frontend, List.of() ) ).as( "browser suite exit" ).isZero();
        passed();
        // The accepted guard mutation receipt belongs to correction3. Attempt4 may
        // run the regression but has no authority to mutate any frontend source.
    }

    private int browserGate( Path frontend, List<String> extra ) throws Exception {
        // Direct node invocation avoids shell wrappers: the same command line works on Windows and Linux.
        var cli = frontend.resolve( Path.of( "node_modules", "@playwright", "test", "cli.js" ) );
        assertThat( cli ).as( "Playwright installed in frontend (npm ci)" ).isRegularFile();
        var node = System.getProperty( "os.name", "" ).toLowerCase( java.util.Locale.ROOT ).startsWith( "windows" )
                ? "node.exe"
                : "node";
        var arguments = new java.util.ArrayList<>( List.of( node, cli.toString(), "test" ) );
        arguments.addAll( extra );
        var process = new ProcessBuilder( arguments )
                .directory( frontend.toFile() );
        var environment = process.environment();
        // Admin credentials, database and backend settings never reach the browser toolchain.
        for ( String name : List.of( "GOOGLE_APPLICATION_CREDENTIALS", "DB_URL", "DB_USERNAME", "DB_PASSWORD",
                "FIREBASE_PROJECT_ID", "FIREBASE_EMULATOR_ENABLED", "FIREBASE_AUTH_EMULATOR_HOST", "T029_RUN_ID" ) ) {
            environment.remove( name );
        }
        environment.put( "T029_API_URL", api + "/api" );
        environment.put( "T029_PASSWORD", password );
        environment.put( "T029_ALICE_EMAIL", namespace.email( "alice" ) );
        environment.put( "T029_BOB_EMAIL", namespace.email( "bob" ) );
        environment.put( "T029_UNKNOWN_EMAIL", namespace.email( "unknown" ) );
        environment.putAll( webConfig.asEnvironment() );
        process.inheritIO();
        var browser = process.start();
        try {
            assertThat( browser.waitFor( 10, TimeUnit.MINUTES ) ).isTrue();
            return browser.exitValue();
        } finally {
            if ( browser.isAlive() ) {
                browser.descendants().forEach( ProcessHandle::destroy );
                browser.destroyForcibly();
            }
        }
    }

    private void membership() throws Exception {
        checkpoint = "MEMBERSHIP_REVOKED_ON_NEXT_HTTP_WITHOUT_REMOTE_DISABLE";
        tenants.endMembership( alpha, aliceMember );
        expect( "GET", "/api/auth/context", aliceToken, "alpha", null, 403 );
        expect( "GET", "/api/auth/me", aliceToken, null, null, 200 );
        passed();
    }

    private void drain() {
        drain( admin );
    }

    private void drain( FirebaseAdminGateway gateway ) {
        var worker = new JdbcIdentityCommandWorker( jdbc,
                application.getBean( "firebaseSecurityTransactions", TransactionTemplate.class ),
                application.getBean( SecurityEventSink.class ),
                application.getBean( "firebaseSecurityDataSource", DataSource.class ), gateway,
                application.getBean( Clock.class ), Duration.ofSeconds( 30 ) );
        for ( int index = 0; index < 100 && worker.runOnce(); index++ ) {
        }
    }

    private UUID role( String action ) {
        return jdbc.sql( "SELECT id_role FROM security.role_seguranca WHERE cd_acao=:action" ).param( "action", action )
                .query( UUID.class ).single();
    }

    /**
     * Plans a synthetic account in the run namespace. The SDK plans a random UID, so before any command runs the
     * planned UID of the new user (and its pending CREATE) is pinned to the reserved namespace UID; it is
     * registered for cleanup before the remote CREATE.
     */
    private UUID create( String name, String caze ) {
        UUID id = users.provisionPassword( name, namespace.email( caze ) );
        String uid = namespace.uid( caze );
        assertThat( jdbc.sql( "UPDATE security.usuario SET cd_uid_planejado=:uid WHERE id_usuario=:id" )
                .param( "uid", uid ).param( "id", id ).update() ).isOne();
        assertThat( jdbc.sql( "UPDATE security.comando_identidade SET cd_uid=:uid WHERE id_usuario=:id"
                + " AND cd_tipo='CREATE'" ).param( "uid", uid ).param( "id", id ).update() ).isOne();
        cleanup.register( uid );
        return id;
    }

    /** The account really created in the project carries the reserved prefix, run and case in UID and e-mail. */
    private void assertNamespaced( String uid ) {
        var remote = admin.getUser( uid );
        assertThat( remote.uid() ).startsWith( namespace.prefix() );
        assertThat( remote.email() ).startsWith( namespace.prefix() )
                .endsWith( "@" + FirebaseRealTestProject.EMAIL_DOMAIN );
    }

    private String plannedUid( UUID id ) {
        return jdbc.sql( "SELECT cd_uid_planejado FROM security.usuario WHERE id_usuario=:id" ).param( "id", id )
                .query( String.class ).single();
    }

    /** Runs the durable CREATE against the real project and gives the account its random in-memory password. */
    private void bind( UUID id ) {
        drain();
        assertThat( jdbc.sql( "SELECT cd_elegibilidade FROM security.usuario WHERE id_usuario=:id" ).param( "id", id )
                .query( String.class ).single() )
                .as( "real Admin CREATE acknowledged (check ADC permission, quota and project)" )
                .isEqualTo( "BOUND" );
        assertNamespaced( plannedUid( id ) );
        FirebaseRealTestProject.setPassword( realAdmin, plannedUid( id ), password );
    }

    private String login( String caze ) throws Exception {
        var response = client.send(
                HttpRequest
                        .newBuilder(
                                URI.create( "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
                                        + URLEncoder.encode( webConfig.apiKey(), StandardCharsets.UTF_8 ) ) )
                        .header( "Content-Type", "application/json" ).timeout( Duration.ofSeconds( 20 ) )
                        .POST( HttpRequest.BodyPublishers.ofString( JSON.toString( Map.of( "email",
                                namespace.email( caze ), "password", password, "returnSecureToken", true ) ) ) )
                        .build(),
                HttpResponse.BodyHandlers.ofString() );
        FirebaseRealTestProject.failOnQuota( response.statusCode(), response.body(), "signInWithPassword" );
        assertThat( response.statusCode() ).as( "real signInWithPassword" ).isEqualTo( 200 );
        return (String) JSON.fromString( response.body(), Map.class ).get( "idToken" );
    }

    private String expect( String method, String path, String token, String alias, String body, int status )
            throws Exception {
        return expectAt( api, method, path, token, alias, body, status );
    }

    private String expectAt( String base, String method, String path, String token, String alias, String body,
            int status ) throws Exception {
        var response = send( base, method, path, token, alias, body );
        assertThat( response.statusCode() ).as( method + " " + path ).isEqualTo( status );
        return response.body();
    }

    /** Bounded read-after-write wait for the real provider; an unexpected 503 is never retried. */
    private void expectSettled( String method, String path, String token, int status ) throws Exception {
        expectSettled( "", method, path, token, status );
    }

    private void expectSettled( String step, String method, String path, String token, int status )
            throws Exception {
        int actual = -1;
        for ( int attempt = 0; attempt < 30 && actual != status; attempt++ ) {
            if ( attempt > 0 ) {
                Thread.sleep( 500 );
            }
            actual = send( api, method, path, token, null, null ).statusCode();
            assertThat( actual ).as( method + " " + path + " provider availability" ).isNotEqualTo( 503 );
        }
        assertThat( actual ).as( step + " " + method + " " + path ).isEqualTo( status );
    }

    private HttpResponse<String> send( String base, String method, String path, String token, String alias,
            String body ) throws Exception {
        var builder = HttpRequest.newBuilder( URI.create( base + path ) ).timeout( Duration.ofSeconds( 20 ) )
                .header( "Content-Type", "application/json" );
        if ( token != null ) {
            builder.header( "Authorization", "Bearer " + token );
        }
        if ( alias != null ) {
            builder.header( "X-Tenant-Alias", alias );
        }
        return client.send( builder.method( method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString( body ) ).build(), HttpResponse.BodyHandlers.ofString() );
    }

    private void passed() {
        System.out.println( "T029 PASS " + checkpoint );
    }
}
