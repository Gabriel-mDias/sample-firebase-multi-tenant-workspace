package br.com.gems.app.benchmark;

import br.com.gems.app.tenant.SampleTenantMigration;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.SecurityEventSink;
import br.com.gems.firebase.auth.TenantAliasValidator;
import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import br.com.gems.firebase.auth.jdbc.JdbcTenantService;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.tenant.migration.TenantSchemaSource;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

/** Medição opt-in, sem SLA e sem chamadas ao Firebase. */
@EnabledIfSystemProperty(named = "benchmark.tenants", matches = "[1-9][0-9]*")
@SpringBootTest
@Testcontainers
@Import(TenantProvisioningBenchmark.FixturePorts.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantProvisioningBenchmark {
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" )
            .withCommand( "postgres", "-c", "shared_preload_libraries=pg_stat_statements",
                    "-c", "pg_stat_statements.track=all" );
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
    TenantSchemaSource schemaSource;
    @Autowired
    JdbcTenantProvisioner provisioner;
    @Autowired
    @Qualifier("firebaseSecurityJdbcClient")
    JdbcClient jdbc;
    @Autowired
    @Qualifier("firebaseSecurityTransactions")
    TransactionTemplate tx;

    @Test
    void deveMedirProvisionamentoEInventario_quandoBenchmarkHabilitado() throws Exception {
        int n = Integer.parseInt( System.getProperty( "benchmark.tenants" ) );
        if ( n > 100 ) {
            throw new IllegalArgumentException( "Escopo aprovado: de 1 a 100 tenants" );
        }
        var actor = SecurityActorProvider.trustedBootstrap( UUID.randomUUID() );
        var tenants = new JdbcTenantService( jdbc, tx, events, clock, aliases, catalog, actor );
        var migration = new SampleTenantMigration();
        String sdk = sdkVersion();
        String postgres = jdbc.sql( "SHOW server_version" ).query( String.class ).single();
        jdbc.sql( "CREATE EXTENSION IF NOT EXISTS pg_stat_statements" ).update();
        jdbc.sql( "SELECT pg_stat_statements_reset()" ).query( Object.class ).single();
        long heapBefore = usedHeap();
        long[] durations = new long[n];
        long start = System.nanoTime();
        for ( int i = 0; i < n; i++ ) {
            long tenantStart = System.nanoTime();
            UUID id = tenants.create( "benchmark" + i, "Tenant de benchmark " + i );
            provisioner.provision( id, 1, migration );
            durations[i] = System.nanoTime() - tenantStart;
        }
        long total = System.nanoTime() - start;
        long inventoryStart = System.nanoTime();
        var expected = schemaSource.expectedSchemas();
        long expectedTime = System.nanoTime() - inventoryStart;
        long readyStart = System.nanoTime();
        var ready = jdbc.sql( "SELECT cd_alias FROM security.tenant WHERE cd_provisionamento='READY'" )
                .query( String.class ).list();
        long readyTime = System.nanoTime() - readyStart;
        long inventory = System.nanoTime() - inventoryStart;
        long sql = jdbc.sql( "SELECT COALESCE(sum(calls),0)::bigint FROM pg_stat_statements "
                + "WHERE dbid=(SELECT oid FROM pg_database WHERE datname=current_database()) "
                + "AND query NOT LIKE '%pg_stat_statements%'" ).query( Long.class ).single();
        long heapAfter = usedHeap();
        var actual = jdbc.sql( "SELECT nspname FROM pg_namespace WHERE left(nspname,7)='tenant_'" )
                .query( String.class ).list();
        assertThat( ready ).as( "N tenants READY distintos" ).hasSize( n ).doesNotHaveDuplicates();
        assertThat( actual ).as( "N schemas tenant_ distintos" ).hasSize( n ).doesNotHaveDuplicates();
        assertThat( expected ).as( "Inventário corresponde aos schemas físicos" )
                .containsExactlyInAnyOrderElementsOf( actual );
        assertThat( sdk ).as( "SDK aprovado" ).isEqualTo( "3.4.1" );
        Arrays.sort( durations );
        String json = String.format( Locale.ROOT, """
                {
                  "n": %d,
                  "provisionamento": {"totalMs": %.6f, "p50Ms": %.6f, "p95Ms": %.6f, "maxMs": %.6f},
                  "inventario": {"totalMs": %.6f, "expectedSchemasMs": %.6f, "listagemReadyMs": %.6f},
                  "heap": {"antesBytes": %d, "depoisBytes": %d},
                  "sql": {
                    "statements": %d, "metodo": "pg_stat_statements",
                    "escopo": "criacao, provisionamento e inventario; sem bootstrap e asserts"
                  },
                  "sdkVersion": "%s",
                  "postgresqlVersion": "%s"
                }
                """, n, ms( total ), percentile( durations, 0.50 ), percentile( durations, 0.95 ),
                ms( durations[n - 1] ), ms( inventory ), ms( expectedTime ), ms( readyTime ),
                heapBefore, heapAfter, sql, sdk, postgres );
        Path output = Path.of( System.getProperty( "benchmark.output", "target/benchmark-tenants.json" ) )
                .toAbsolutePath();
        Files.createDirectories( output.getParent() );
        Files.writeString( output, json );
        System.out.printf( Locale.ROOT, "BENCHMARK N=%d total=%.3fms p95=%.3fms inventario=%.3fms sql=%d%n",
                n, ms( total ), percentile( durations, 0.95 ), ms( inventory ), sql );
    }
    private static long usedHeap() {
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    private static double ms( long nanos ) {
        return nanos / 1_000_000.0;
    }
    private static double percentile( long[] sorted, double percentile ) {
        return ms( sorted[(int) Math.ceil( sorted.length * percentile ) - 1] );
    }
    private static String sdkVersion() throws Exception {
        var properties = new Properties();
        var resource = JdbcTenantService.class.getResource( "JdbcTenantService.class" );
        if ( resource != null && resource.openConnection() instanceof JarURLConnection connection ) {
            var jar = connection.getJarFile();
            var entries = jar.entries();
            while ( entries.hasMoreElements() ) {
                var entry = entries.nextElement();
                if ( entry.getName().startsWith( "META-INF/maven/br.com.gems/" )
                        && entry.getName().endsWith( "/pom.properties" ) ) {
                    try ( InputStream input = jar.getInputStream( entry ) ) {
                        properties.load( input );
                    }
                    return properties.getProperty( "version" );
                }
            }
        }
        throw new IllegalStateException( "Metadados da versão do SDK indisponíveis" );
    }

    /** Importa as portas existentes apesar da visibilidade package-private do fixture. */
    public static class FixturePorts implements ImportSelector {
        @Override
        public String[] selectImports( AnnotationMetadata metadata ) {
            return new String[]{"br.com.gems.app.FirebaseBackendIntegrationTest$Ports"};
        }
    }
}
