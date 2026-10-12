package br.com.gems.app;

import br.com.gems.app.tenant.SampleTenantMigration;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.SecurityEventSink;
import br.com.gems.firebase.auth.TenantAliasValidator;
import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import br.com.gems.firebase.auth.jdbc.JdbcTenantService;
import br.com.gems.security.authorization.AuthorizationCatalog;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Falha de unlock na sessão real; o fixture nunca invalida a conexão em nome do SDK.
 * SDK 3.4.1 preserva a causa para CLI/job, sem expor detalhes internos na mensagem de topo.
 */
@SpringBootTest
@Testcontainers
@Import(FirebaseBackendIntegrationTest.Ports.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantProvisioningFailureIntegrationTest {

    private static final String RAW_FAILURE = "detalhe-interno-do-unlock";
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" );

    @DynamicPropertySource
    static void database( DynamicPropertyRegistry registry ) {
        registry.add( "spring.datasource.url", DATABASE::getJdbcUrl );
        registry.add( "spring.datasource.username", DATABASE::getUsername );
        registry.add( "spring.datasource.password", DATABASE::getPassword );
    }

    @Autowired
    SecurityEventSink events;
    @Autowired
    TenantAliasValidator aliases;
    @Autowired
    AuthorizationCatalog catalog;
    @Autowired
    Clock clock;
    @Autowired
    @Qualifier("firebaseSecurityJdbcClient")
    JdbcClient jdbc;
    @Autowired
    @Qualifier("firebaseSecurityTransactions")
    TransactionTemplate tx;

    @ParameterizedTest
    @CsvSource({ "false, false, TENANT_PROVISIONING_SESSION_FAILED", "false, true, TENANT_PROVISIONING_SESSION_FAILED", "true, false, TENANT_PROVISIONING_FAILED", "true, true, TENANT_PROVISIONING_FAILED" })
    void deveDescartarSessaoEPermitirRetry_quandoUnlockFalha( boolean migrationFails, boolean abortFails, String expectedMessage ) throws Exception {
        var actor = SecurityActorProvider.trustedBootstrap( UUID.randomUUID() );
        var tenants = new JdbcTenantService( jdbc, tx, events, clock, aliases, catalog, actor );
        String alias = "unlock" + UUID.randomUUID().toString().replace( "-", "" );
        UUID id = tenants.create( alias, "Falha de unlock" );
        var config = new HikariConfig();
        config.setJdbcUrl( DATABASE.getJdbcUrl() );
        config.setUsername( DATABASE.getUsername() );
        config.setPassword( DATABASE.getPassword() );
        config.setMaximumPoolSize( 1 );
        config.setMinimumIdle( 0 );
        config.setConnectionTimeout( 2000 );
        try (var pool = new HikariDataSource( config )) {
            var faulty = new UnlockFailureDataSource( pool, abortFails );
            var provisioner = new JdbcTenantProvisioner( faulty, jdbc, tx, events, aliases, clock );
            var migration = new SampleTenantMigration();
            var pinnedPid = new AtomicInteger();
            String originalSchema;
            try (var session = pool.getConnection()) {
                originalSchema = session.getSchema();
            }
            var failingMigration = new JdbcTenantProvisioner.Migration() {

                @Override
                public void apply( Connection session, String schema ) throws Exception {
                    Probe before = probe( session );
                    assertThat( before.locked() ).isTrue();
                    pinnedPid.set( before.pid() );
                    migration.apply( session, schema );
                    assertThat( probe( session ).pid() ).isEqualTo( pinnedPid.get() );
                    session.setSchema( schema );
                    session.setAutoCommit( false );
                    if ( migrationFails ) {
                        throw new SQLException( RAW_FAILURE );
                    }
                }

                @Override
                public long appliedRevision( Connection session, String schema ) throws Exception {
                    assertThat( probe( session ).pid() ).isEqualTo( pinnedPid.get() );
                    return migration.appliedRevision( session, schema );
                }

            };

            Throwable failure = catchThrowable( () -> provisioner.provision( id, 1, failingMigration ) );

            assertThat( failure ).isInstanceOf( IllegalStateException.class )
                    .hasMessage( expectedMessage ).hasRootCauseInstanceOf( SQLException.class )
                    .hasRootCauseMessage( RAW_FAILURE );
            assertThat( failure.getMessage() ).doesNotContain( RAW_FAILURE );
            assertThat( faulty.unlockFailures.get() ).isOne();
            assertThat( faulty.unlockPid.get() ).isEqualTo( pinnedPid.get() );
            assertThat( faulty.returnedSessions.get() ).isOne();
            var assertions = new SoftAssertions();
            assertions.assertThat( jdbc.sql( "SELECT cd_provisionamento FROM security.tenant WHERE id_tenant=:id" )
                    .param( "id", id ).query( String.class ).single() ).isEqualTo( "FAILED" );
            assertions.assertThat( jdbc.sql( "SELECT cd_erro_provisionamento FROM security.tenant WHERE id_tenant=:id" )
                    .param( "id", id ).query( String.class ).optional() )
                    .hasValueSatisfying( error -> assertThat( error ).matches( "[A-Z_]+" ) );
            assertions.assertThat( jdbc.sql( "SELECT nr_revisao_aplicada FROM security.tenant WHERE id_tenant=:id" )
                    .param( "id", id ).query( Long.class ).single() ).isZero();
            Probe next;
            try (var session = pool.getConnection()) {
                next = probe( session );
            }
            assertions.assertThat( next.pid() ).as( "Sessão descartada não pode voltar ao pool" )
                    .isNotEqualTo( pinnedPid.get() );
            assertions.assertThat( next.locked() ).as( "Nova sessão não herda advisory lock" ).isFalse();
            assertions.assertThat( next.schema() ).isEqualTo( originalSchema );
            assertions.assertThat( next.autoCommit() ).isTrue();
            assertions.assertThat( locks( pinnedPid.get() ) ).as( "Lock da sessão invalidada foi liberado" )
                    .isZero();

            provisioner.provision( id, 1, migration );

            assertThat( jdbc.sql( "SELECT cd_provisionamento FROM security.tenant WHERE id_tenant=:id" )
                    .param( "id", id ).query( String.class ).single() ).isEqualTo( "READY" );
            assertThat( jdbc.sql( "SELECT cd_erro_provisionamento IS NULL FROM security.tenant WHERE id_tenant=:id" )
                    .param( "id", id ).query( Boolean.class ).single() ).isTrue();
            assertThat( jdbc.sql( "SELECT nr_revisao_aplicada FROM security.tenant WHERE id_tenant=:id" )
                    .param( "id", id ).query( Long.class ).single() ).isOne();
            assertThat( jdbc.sql( "SELECT count(*) FROM tenant_" + alias + ".revisao_modelo" )
                    .query( Long.class ).single() ).isOne();
            try (var session = pool.getConnection()) {
                Probe afterRetry = probe( session );
                assertions.assertThat( afterRetry.locked() ).isFalse();
                assertions.assertThat( afterRetry.schema() ).isEqualTo( originalSchema );
                assertions.assertThat( afterRetry.autoCommit() ).isTrue();
            }
            assertions.assertAll();
        }
    }

    private long locks( int pid ) {
        return jdbc.sql( "SELECT count(*) FROM pg_locks WHERE pid=:pid AND locktype='advisory' AND granted" )
                .param( "pid", pid ).query( Long.class ).single();
    }

    private static Probe probe( Connection session ) throws SQLException {
        try (var statement = session.createStatement(); var rows = statement.executeQuery( "SELECT pg_backend_pid(), " + "EXISTS(SELECT 1 FROM pg_locks WHERE pid=pg_backend_pid() " + "AND locktype='advisory' AND granted)" )) {
            assertThat( rows.next() ).isTrue();
            return new Probe( rows.getInt( 1 ), rows.getBoolean( 2 ), session.getSchema(), session.getAutoCommit() );
        }
    }

    private record Probe(int pid, boolean locked, String schema, boolean autoCommit) {

    }

    private static final class UnlockFailureDataSource extends AbstractDataSource {

        private final HikariDataSource pool;
        private final boolean abortFails;
        private final AtomicBoolean failUnlock = new AtomicBoolean( true );
        private final AtomicInteger unlockFailures = new AtomicInteger();
        private final AtomicInteger unlockPid = new AtomicInteger();
        private final AtomicInteger returnedSessions = new AtomicInteger();

        private UnlockFailureDataSource( HikariDataSource pool, boolean abortFails ) {
            this.pool = pool;
            this.abortFails = abortFails;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection borrowed = pool.getConnection();
            return (Connection) Proxy.newProxyInstance( Connection.class.getClassLoader(), new Class<?>[] { Connection.class }, ( proxy, method, args ) -> {
                if ( method.getName().equals( "abort" ) ) {
                    if ( abortFails ) {
                        // JDBC permite abort indisponível; close sozinho devolve uma sessão com lock.
                        throw new SQLException( RAW_FAILURE, "0A000" );
                    }
                }
                if ( method.getName().equals( "close" ) ) {
                    returnedSessions.incrementAndGet();
                }
                Object result = invoke( borrowed, method, args );
                if ( method.getName().equals( "prepareStatement" )
                        && ((String) args[0]).contains( "pg_advisory_unlock" ) ) {
                    return unlockStatement( borrowed, (PreparedStatement) result );
                }
                return result;
            } );
        }

        @Override
        public Connection getConnection( String username, String password ) throws SQLException {
            return getConnection();
        }

        private PreparedStatement unlockStatement( Connection session, PreparedStatement statement ) {
            return (PreparedStatement) Proxy.newProxyInstance( PreparedStatement.class.getClassLoader(), new Class<?>[] { PreparedStatement.class }, ( proxy, method, args ) -> {
                if ( method.getName().equals( "executeQuery" ) && failUnlock.compareAndSet( true, false ) ) {
                    unlockFailures.incrementAndGet();
                    Probe atUnlock = probe( session );
                    assertThat( atUnlock.locked() ).isTrue();
                    unlockPid.set( atUnlock.pid() );
                    throw new SQLException( RAW_FAILURE, "XX000" );
                }
                return invoke( statement, method, args );
            } );
        }

        private static Object invoke( Object target, Method method, Object[] args ) throws Throwable {
            try {
                return method.invoke( target, args );
            } catch ( InvocationTargetException failure ) {
                throw failure.getCause();
            }
        }

    }

}
