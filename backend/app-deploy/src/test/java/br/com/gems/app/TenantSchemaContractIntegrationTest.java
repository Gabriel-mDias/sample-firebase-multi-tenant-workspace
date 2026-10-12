package br.com.gems.app;

import br.com.gems.app.tenant.SampleTenantMigration;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.SecurityEventSink;
import br.com.gems.firebase.auth.TenantAliasValidator;
import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import br.com.gems.firebase.auth.jdbc.JdbcTenantService;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.tenant.TenantSchemaNaming;
import br.com.gems.tenant.migration.TenantSchemaSource;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Inventário real READY e derivação compartilhada entre Firebase JDBC e JPA. */
@SpringBootTest
@Testcontainers
@Import(FirebaseBackendIntegrationTest.Ports.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantSchemaContractIntegrationTest {

    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" );

    @DynamicPropertySource
    static void database( DynamicPropertyRegistry registry ) {
        registry.add( "spring.datasource.url", DATABASE::getJdbcUrl );
        registry.add( "spring.datasource.username", DATABASE::getUsername );
        registry.add( "spring.datasource.password", DATABASE::getPassword );
    }

    @Autowired
    TenantSchemaNaming naming;
    @Autowired
    TenantSchemaSource schemaSource;
    @Autowired
    JdbcTenantProvisioner provisioner;
    @Autowired
    TenantAliasValidator aliases;
    @Autowired
    SecurityEventSink events;
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

    @Test
    void deveDerivarSchemasEInventariarSuspensos_quandoSomenteREADYEstaDisponivel() {
        var actor = SecurityActorProvider.trustedBootstrap( UUID.randomUUID() );
        var tenants = new JdbcTenantService( jdbc, tx, events, clock, aliases, catalog, actor );
        String suffix = UUID.randomUUID().toString().replace( "-", "" );
        String active = "ativo" + suffix;
        String suspended = "suspenso" + suffix;
        String pending = "pendente" + suffix;
        String failed = "falho" + suffix;
        String boundary = "a".repeat( 24 ) + suffix;
        UUID activeId = tenants.create( active.toUpperCase( java.util.Locale.ROOT ), "Ativo" );
        UUID suspendedId = tenants.create( suspended, "Suspenso" );
        UUID pendingId = tenants.create( pending, "Pendente" );
        UUID failedId = tenants.create( failed, "Falho" );
        UUID boundaryId = tenants.create( boundary, "Limite" );
        var migration = new SampleTenantMigration();
        provisioner.provision( activeId, 1, migration );
        provisioner.provision( suspendedId, 1, migration );
        provisioner.provision( boundaryId, 1, migration );
        tenants.suspend( suspendedId );
        assertThatThrownBy( () -> provisioner.provision( failedId, 2, migration ) )
                .isInstanceOf( IllegalStateException.class ).hasMessage( "TENANT_PROVISIONING_FAILED" );
        Set<String> before = Set.copyOf( schemaSource.expectedSchemas() );

        Set<String> actual = schemaSource.expectedSchemas();

        Set<String> persisted = jdbc.sql( "SELECT cd_alias FROM security.tenant WHERE cd_provisionamento='READY'" )
                .query( String.class ).list().stream().map( naming::schemaFor ).collect( Collectors.toSet() );
        assertThat( actual ).isEqualTo( persisted );
        assertThat( actual ).containsExactlyInAnyOrder( naming.schemaFor( active ), naming.schemaFor( suspended ), naming.schemaFor( boundary ) );
        assertThat( actual ).isEqualTo( before );
        assertThat( actual ).doesNotContain( active, suspended, boundary, naming.schemaFor( pending ), naming.schemaFor( failed ) );
        assertMetadata( activeId, active, "READY", false );
        assertMetadata( suspendedId, suspended, "READY", true );
        assertMetadata( boundaryId, boundary, "READY", false );
        assertMetadata( pendingId, pending, "PENDING", false );
        assertMetadata( failedId, failed, "FAILED", false );
        assertThat( naming.schemaFor( boundary ) ).hasSize( 63 );
        for ( String alias : Set.of( active, suspended, boundary, failed ) ) {
            String persistedSchema = jdbc.sql( "SELECT nspname FROM pg_namespace WHERE nspname=:schema" )
                    .param( "schema", naming.schemaFor( alias ) ).query( String.class ).single();
            assertThat( persistedSchema ).isEqualTo( naming.schemaFor( alias ) );
            assertThat( migrationRevision( persistedSchema ) ).isOne();
        }
        assertThat( jdbc.sql( "SELECT count(*) FROM pg_namespace WHERE nspname=:schema" )
                .param( "schema", naming.schemaFor( pending ) ).query( Long.class ).single() ).isZero();
    }

    private void assertMetadata( UUID id, String alias, String state, boolean suspended ) {
        assertThat( jdbc.sql( "SELECT cd_alias FROM security.tenant WHERE id_tenant=:id" ).param( "id", id )
                .query( String.class ).single() ).isEqualTo( alias );
        assertThat( jdbc.sql( "SELECT cd_provisionamento FROM security.tenant WHERE id_tenant=:id" )
                .param( "id", id ).query( String.class ).single() ).isEqualTo( state );
        assertThat( jdbc.sql( "SELECT dt_fim IS NOT NULL FROM security.tenant WHERE id_tenant=:id" )
                .param( "id", id ).query( Boolean.class ).single() ).isEqualTo( suspended );
        assertThat( jdbc.sql( "SELECT nr_revisao_aplicada FROM security.tenant WHERE id_tenant=:id" )
                .param( "id", id ).query( Long.class ).single() ).isEqualTo( state.equals( "READY" ) ? 1L : 0L );
    }

    private long migrationRevision( String schema ) {
        return jdbc.sql( "SELECT MAX(nr_revisao) FROM " + schema + ".revisao_modelo" )
                .query( Long.class ).single();
    }

}
