package br.com.gems.app.auditoria;

import br.com.gems.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaSchemaMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveExporSchemaAtomicoDaSdkSemAsTabelasLegadas_quandoLiquibaseConclui() {
        var tabelas = jdbc.queryForList( """
                select table_name from information_schema.tables
                where table_schema = 'auditoria'
                order by table_name
                """, String.class );

        assertThat( tabelas ).contains( "audit_operation", "audit_change" );
        assertThat( tabelas ).doesNotContain( "registro_auditoria", "campo_alterado" );

        var colunas = jdbc.queryForList( """
                select column_name from information_schema.columns
                where table_schema = 'auditoria' and table_name = 'audit_operation'
                """, String.class );

        assertThat( colunas ).contains( "id_actor", "cd_correlation", "cd_actor_type" );
    }

    @Test
    void deveManterRollbackNominal_quandoChangesetExecutaMigracao() {
        var recurso = getClass().getResourceAsStream(
                "/db/changelog/changelog-auditoria.xml" );

        assertThat( recurso ).isNotNull();
        try ( recurso ) {
            var xml = new String( recurso.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8 );
            assertThat( xml ).contains( "migrate-auditoria-to-gems-sdk", "<rollback>" );
            assertThat( xml ).doesNotContain( "TRUNCATE" );
        } catch ( java.io.IOException e ) {
            throw new IllegalStateException( e );
        }
    }
}
