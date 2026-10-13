package br.com.gems.app.tenant;

import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import java.sql.Connection;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

/** Migration adapter uses the pinned SDK provisioning session. */
public class SampleTenantMigration implements JdbcTenantProvisioner.Migration {

    @Override
    public void apply( Connection session, String schema ) throws Exception {
        validateSchema( schema );
        boolean originalAutoCommit = session.getAutoCommit();
        var borrowed = new JdbcConnection( session ) {

            @Override
            public void close() throws DatabaseException {
                /* sessão pertence ao provisioner SDK */ }

        };
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation( borrowed );
        database.setDefaultSchemaName( schema );
        database.setLiquibaseSchemaName( schema );
        try (var resources = new ClassLoaderResourceAccessor(); var liquibase = new Liquibase( "db/changelog/changelog-firebase-tenant.xml", resources, database )) {
            liquibase.update( new Contexts(), new LabelExpression() );
        } finally {
            session.setAutoCommit( originalAutoCommit );
        }
    }

    @Override
    public long appliedRevision( Connection session, String schema ) throws Exception {
        validateSchema( schema );
        try (var statement = session.createStatement(); var rows = statement
                .executeQuery( "SELECT COALESCE(MAX(NR_REVISAO),0) FROM " + schema + ".REVISAO_MODELO" )) {
            if ( !rows.next() ) {
                throw new IllegalStateException( "MIGRATION_REVISION_MISSING" );
            }
            return rows.getLong( 1 );
        }
    }

    private void validateSchema( String schema ) {
        if ( schema == null || !schema.startsWith( "tenant_" ) ) {
            throw new IllegalArgumentException( "Schema empresarial obrigatório" );
        }
        String alias = new br.com.gems.firebase.auth.StrictTenantAliasValidator().validate( schema.substring( 7 ) );
        if ( !schema.equals( "tenant_" + alias ) ) {
            throw new IllegalArgumentException( "Schema deve coincidir com alias canônico" );
        }
    }

}
