package br.com.gems.app;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.FastCheckService;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Instalação, upgrade do histórico publicado, rollback e boot com ddl-auto validate. */
@Testcontainers
class MigrationContractIntegrationTest {

    private static final String PLATFORM = "db/changelog/changelog-firebase-platform.xml";
    private static final String TENANT = "db/changelog/changelog-firebase-tenant.xml";
    private static final String PLATFORM_RENAME = "firebase-name-global-audit-constraints|app|" + "db/changelog/changelog-firebase-global-constraint-names.xml";
    private static final String TENANT_RENAME = "firebase-name-tenant-constraints|app|" + "db/changelog/changelog-firebase-tenant-constraint-names.xml";
    private static final String SCHEMA = "tenant_contrato";
    private static final Map<String, String> AUDIT = Map.of( "audit_operation:p", "pk_audit_operation", "audit_change:p", "pk_audit_change", "audit_change:f", "fk_audit_change_operation" );
    private static final Map<String, String> BUSINESS = businessConstraints( false );
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" );

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deveNomearConstraintsSemDrift_quandoBancoNovoOuAtualizado( boolean upgrade ) throws Exception {
        String url = newDatabase();
        try (var connection = connection( url ); var statement = connection.createStatement()) {
            statement.execute( "CREATE SCHEMA " + SCHEMA );
        }
        Map<String, String> publishedPlatform = Map.of();
        Map<String, String> publishedTenant = Map.of();
        Map<String, Long> oldGlobalIds = Map.of();
        Map<String, Long> oldTenantIds = Map.of();
        Map<String, String> oldGlobalDefinitions = Map.of();
        Map<String, String> oldTenantDefinitions = Map.of();
        if ( upgrade ) {
            // Três changesets originais de cada master, sem reconstruir DDL à mão.
            migrate( url, "public", PLATFORM, 3 );
            migrate( url, SCHEMA, TENANT, 3 );
            assertThat( constraintNames( url, "auditoria" ) ).isEqualTo( legacyAudit() );
            assertThat( constraintNames( url, SCHEMA ) ).isEqualTo( businessConstraints( true ) );
            publishedPlatform = history( url, "public" );
            publishedTenant = history( url, SCHEMA );
            oldGlobalIds = constraintIds( url, "auditoria" );
            oldTenantIds = constraintIds( url, SCHEMA );
            oldGlobalDefinitions = constraintDefinitions( url, "auditoria" );
            oldTenantDefinitions = constraintDefinitions( url, SCHEMA );
        }

        migrate( url, "public", PLATFORM, 0 );
        migrate( url, SCHEMA, TENANT, 0 );

        assertThat( constraintNames( url, "auditoria" ) ).isEqualTo( AUDIT );
        assertThat( constraintNames( url, SCHEMA ) ).isEqualTo( BUSINESS );
        assertThat( history( url, "public" ) ).containsAllEntriesOf( publishedPlatform );
        assertThat( history( url, SCHEMA ) ).containsAllEntriesOf( publishedTenant );
        if ( upgrade ) {
            assertThat( constraintIds( url, "auditoria" ) ).isEqualTo( oldGlobalIds );
            assertThat( constraintIds( url, SCHEMA ) ).isEqualTo( oldTenantIds );
            assertThat( constraintDefinitions( url, "auditoria" ) ).isEqualTo( oldGlobalDefinitions );
            assertThat( constraintDefinitions( url, SCHEMA ) ).isEqualTo( oldTenantDefinitions );
        }
        var migrated = snapshot( url, false );
        migrate( url, "public", PLATFORM, 0 );
        migrate( url, SCHEMA, TENANT, 0 );
        assertThat( snapshot( url, false ) ).isEqualTo( migrated );

        rollbackRename( url, SCHEMA, TENANT, TENANT_RENAME );
        rollbackRename( url, "public", PLATFORM, PLATFORM_RENAME );
        assertThat( constraintNames( url, "auditoria" ) ).isEqualTo( legacyAudit() );
        assertThat( constraintNames( url, SCHEMA ) ).isEqualTo( businessConstraints( true ) );
        migrate( url, "public", PLATFORM, 0 );
        migrate( url, SCHEMA, TENANT, 0 );
        assertThat( history( url, "public" ) ).containsKey( PLATFORM_RENAME );
        assertThat( history( url, SCHEMA ) ).containsKey( TENANT_RENAME );
        assertThat( constraintNames( url, "auditoria" ) ).isEqualTo( AUDIT );
        assertThat( constraintNames( url, SCHEMA ) ).isEqualTo( BUSINESS );

        assertThat( snapshot( url, false ) ).isEqualTo( migrated );

        boot( url );
        var firstStartup = snapshot( url, true );
        boot( url );
        assertThat( snapshot( url, true ) ).isEqualTo( firstStartup );
        assertThat( constraintNames( url, "modelo" ) ).isEqualTo( BUSINESS );
    }

    @ParameterizedTest
    @CsvSource({ "public, auditoria, false", "public, auditoria, true", "tenant_contrato, tenant_contrato, false", "tenant_contrato, tenant_contrato, true" })
    void deveRejeitarMigracao_quandoCheckSubstituiFk( String schema, String auditSchema, boolean legacy ) throws Exception {
        String changelog = "public".equals( schema ) ? PLATFORM : TENANT;
        String rename = "public".equals( schema ) ? PLATFORM_RENAME : TENANT_RENAME;
        String url = migrationDatabase( schema, changelog, 3 );
        String checkName = (legacy ? legacyAudit() : AUDIT).get( "audit_change:f" );
        replaceAuditForeignKeyWithCheck( url, auditSchema, legacyAudit().get( "audit_change:f" ), checkName );
        var beforeHistory = history( url, schema );
        var beforeConstraints = constraints( url, auditSchema );
        assertThat( beforeHistory ).doesNotContainKey( rename );

        assertThatThrownBy( () -> migrate( url, schema, changelog, 0 ) )
                .isInstanceOf( LiquibaseException.class )
                .hasMessageContaining( "Invalid constraint definition for " + checkName )
                .hasMessageContaining( auditSchema + ".audit_change" );

        assertThat( history( url, schema ) ).isEqualTo( beforeHistory ).doesNotContainKey( rename );
        assertThat( constraints( url, auditSchema ) ).isEqualTo( beforeConstraints );
        assertCheckConstraint( url, auditSchema, checkName );
    }

    @ParameterizedTest
    @CsvSource({ "public, auditoria, false", "public, auditoria, true", "tenant_contrato, tenant_contrato, false", "tenant_contrato, tenant_contrato, true" })
    void deveRejeitarRollback_quandoCheckSubstituiFk( String schema, String auditSchema, boolean legacy ) throws Exception {
        String changelog = "public".equals( schema ) ? PLATFORM : TENANT;
        String rename = "public".equals( schema ) ? PLATFORM_RENAME : TENANT_RENAME;
        String url = migrationDatabase( schema, changelog, 0 );
        String checkName = (legacy ? legacyAudit() : AUDIT).get( "audit_change:f" );
        replaceAuditForeignKeyWithCheck( url, auditSchema, AUDIT.get( "audit_change:f" ), checkName );
        var beforeHistory = history( url, schema );
        var beforeConstraints = constraints( url, auditSchema );
        assertThat( beforeHistory ).containsKey( rename );

        assertThatThrownBy( () -> rollbackRename( url, schema, changelog, rename ) )
                .isInstanceOf( LiquibaseException.class )
                .hasMessageContaining( "Invalid constraint definition for " + checkName )
                .hasMessageContaining( auditSchema + ".audit_change" );

        // O rollback rejeitado conserva a execução anterior, sem remover ou regravar o histórico.
        assertThat( history( url, schema ) ).isEqualTo( beforeHistory );
        assertThat( constraints( url, auditSchema ) ).isEqualTo( beforeConstraints );
        assertCheckConstraint( url, auditSchema, checkName );
    }

    private String migrationDatabase( String schema, String changelog, int count ) throws Exception {
        String url = newDatabase();
        if ( !"public".equals( schema ) ) {
            try (var connection = connection( url ); var statement = connection.createStatement()) {
                statement.execute( "CREATE SCHEMA " + schema );
            }
        }
        migrate( url, schema, changelog, count );
        return url;
    }

    private void replaceAuditForeignKeyWithCheck( String url, String schema, String foreignKey, String checkName ) throws Exception {
        try (var connection = connection( url ); var statement = connection.createStatement()) {
            statement.execute( "ALTER TABLE " + schema + ".audit_change DROP CONSTRAINT " + foreignKey );
            statement.execute( "ALTER TABLE " + schema + ".audit_change ADD CONSTRAINT " + checkName + " CHECK (true)" );
        }
    }

    private void assertCheckConstraint( String url, String schema, String name ) throws Exception {
        try (var connection = connection( url ); var statement = connection.prepareStatement( "SELECT c.contype, pg_get_constraintdef(c.oid) " + "AS definition FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid " + "JOIN pg_namespace n ON n.oid=t.relnamespace " + "WHERE n.nspname=? AND t.relname='audit_change' AND c.conname=?" )) {
            statement.setString( 1, schema );
            statement.setString( 2, name );
            try (var rows = statement.executeQuery()) {
                assertThat( rows.next() ).isTrue();
                assertThat( rows.getString( "contype" ) ).isEqualTo( "c" );
                assertThat( rows.getString( "definition" ) ).isEqualTo( "CHECK (true)" );
                assertThat( rows.next() ).isFalse();
            }
        }
    }

    private static Map<String, String> businessConstraints( boolean legacy ) {
        var expected = new HashMap<>( legacy ? legacyAudit() : AUDIT );
        expected.put( "demo_registro:p", legacy ? "demo_registro_pkey" : "pk_demo_registro" );
        expected.put( "revisao_modelo:p", legacy ? "revisao_modelo_pkey" : "pk_revisao_modelo" );
        return Map.copyOf( expected );
    }

    private static Map<String, String> legacyAudit() {
        return Map.of( "audit_operation:p", "audit_operation_pkey", "audit_change:p", "audit_change_pkey", "audit_change:f", "audit_change_id_audit_operation_fkey" );
    }

    private String newDatabase() throws Exception {
        String name = "be3c_" + UUID.randomUUID().toString().replace( "-", "" );
        try (var connection = connection( DATABASE.getJdbcUrl() ); var statement = connection.createStatement()) {
            statement.execute( "CREATE DATABASE " + name );
        }
        return DATABASE.getJdbcUrl().replace( "/" + DATABASE.getDatabaseName(), "/" + name );
    }

    private Connection connection( String url ) throws Exception {
        return DriverManager.getConnection( url, DATABASE.getUsername(), DATABASE.getPassword() );
    }

    private void migrate( String url, String schema, String changelog, int count ) throws Exception {
        try (var connection = connection( url ); var resources = new ClassLoaderResourceAccessor()) {
            connection.setSchema( schema );
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation( new JdbcConnection( connection ) );
            database.setDefaultSchemaName( schema );
            database.setLiquibaseSchemaName( schema );
            try (var liquibase = new Liquibase( changelog, resources, database )) {
                if ( count > 0 ) {
                    liquibase.update( count, new Contexts(), new LabelExpression() );
                } else {
                    liquibase.update( new Contexts(), new LabelExpression() );
                }
            }
        }
    }

    private void rollbackRename( String url, String schema, String changelog, String rename ) throws Exception {
        var remainingHistory = new LinkedHashMap<>( history( url, schema ) );
        assertThat( remainingHistory ).containsKey( rename );
        remainingHistory.remove( rename );
        try (var connection = connection( url ); var resources = new ClassLoaderResourceAccessor()) {
            connection.setSchema( schema );
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation( new JdbcConnection( connection ) );
            database.setDefaultSchemaName( schema );
            database.setLiquibaseSchemaName( schema );
            try (var liquibase = new Liquibase( changelog, resources, database )) {
                liquibase.rollback( 1, new Contexts(), new LabelExpression() );
            }
        }
        assertThat( history( url, schema ) ).isEqualTo( remainingHistory );
        // Liquibase 5.0.3 conserva o fast check no Scope entre instâncias.
        // rollbackCount remove o histórico sem invalidar o resultado "up to date".
        // Limpar após rollback reproduz comandos executados em processos separados.
        Scope.getCurrentScope().getSingleton( FastCheckService.class ).clearCache();
    }

    private Map<String, String> constraintNames( String url, String schema ) throws Exception {
        var names = new LinkedHashMap<String, String>();
        for ( var entry : constraints( url, schema ).entrySet() ) {
            names.put( entry.getKey(), entry.getValue().name() );
        }
        return names;
    }

    private Map<String, Long> constraintIds( String url, String schema ) throws Exception {
        var ids = new LinkedHashMap<String, Long>();
        for ( var entry : constraints( url, schema ).entrySet() ) {
            ids.put( entry.getKey(), entry.getValue().id() );
        }
        return ids;
    }

    private Map<String, String> constraintDefinitions( String url, String schema ) throws Exception {
        var definitions = new LinkedHashMap<String, String>();
        for ( var entry : constraints( url, schema ).entrySet() ) {
            definitions.put( entry.getKey(), entry.getValue().definition() );
        }
        return definitions;
    }

    private Map<String, Constraint> constraints( String url, String schema ) throws Exception {
        var result = new LinkedHashMap<String, Constraint>();
        try (var connection = connection( url ); var statement = connection.prepareStatement( "SELECT t.relname, c.contype, c.conname, c.oid, " + "pg_get_constraintdef(c.oid) AS definition, c.convalidated " + "FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid " + "JOIN pg_namespace n ON n.oid=t.relnamespace " + "WHERE n.nspname=? AND c.contype IN ('p','f') " + "AND t.relname IN ('demo_registro','revisao_modelo','audit_operation','audit_change') " + "ORDER BY t.relname,c.contype" )) {
            statement.setString( 1, schema );
            try (var rows = statement.executeQuery()) {
                while ( rows.next() ) {
                    assertThat( rows.getBoolean( "convalidated" ) ).isTrue();
                    String key = rows.getString( "relname" ) + ":" + rows.getString( "contype" );
                    assertThat( result ).doesNotContainKey( key );
                    result.put( key, new Constraint( rows.getLong( "oid" ), rows.getString( "conname" ), rows.getString( "definition" ) ) );
                }
            }
        }
        return result;
    }

    private Map<String, String> history( String url, String schema ) throws Exception {
        var result = new LinkedHashMap<String, String>();
        try (var connection = connection( url ); var statement = connection.createStatement(); var rows = statement.executeQuery( "SELECT id,author,filename,md5sum,exectype FROM " + schema + ".databasechangelog ORDER BY orderexecuted" )) {
            while ( rows.next() ) {
                String key = rows.getString( "id" ) + "|" + rows.getString( "author" ) + "|" + rows.getString( "filename" );
                if ( PLATFORM_RENAME.equals( key ) || TENANT_RENAME.equals( key ) ) {
                    assertThat( rows.getString( "exectype" ) ).isEqualTo( "EXECUTED" );
                }
                result.put( key, rows.getString( "md5sum" ) );
            }
        }
        return result;
    }

    private Map<String, Object> snapshot( String url, boolean model ) throws Exception {
        var result = new LinkedHashMap<String, Object>();
        result.put( "auditoria", constraints( url, "auditoria" ) );
        result.put( SCHEMA, constraints( url, SCHEMA ) );
        result.put( "platform-history", history( url, "public" ) );
        result.put( "tenant-history", history( url, SCHEMA ) );
        if ( model ) {
            result.put( "modelo", constraints( url, "modelo" ) );
            result.put( "model-history", history( url, "modelo" ) );
        }
        return result;
    }

    private void boot( String url ) {
        var properties = new HashMap<String, Object>();
        properties.put( "DB_URL", url );
        properties.put( "DB_USERNAME", DATABASE.getUsername() );
        properties.put( "DB_PASSWORD", DATABASE.getPassword() );
        properties.put( "FIREBASE_PROJECT_ID", "sample-isolated-test" );
        properties.put( "gems.firebase.auth.emulator.enabled", false );
        properties.put( "logging.level.root", "WARN" );
        String yaml = Path.of( "src/main/resources/application.yml" ).toAbsolutePath().toUri().toString();
        try (var context = new SpringApplicationBuilder( AppApplication.class, FirebaseBackendIntegrationTest.Ports.class )
                .initializers( app -> app.getEnvironment().getPropertySources()
                .addFirst( new MapPropertySource( "migration-contract", properties ) ) )
                .run( "--spring.config.location=" + yaml, "--server.port=0", "--spring.main.banner-mode=off" )) {
            assertThat( context.isActive() ).isTrue();
            assertThat( context.getEnvironment().getProperty( "spring.jpa.hibernate.ddl-auto" ) )
                    .isEqualTo( "validate" );
        }
    }

    private record Constraint(long id, String name, String definition) {

    }

}
