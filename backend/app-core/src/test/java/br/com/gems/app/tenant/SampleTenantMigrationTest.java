package br.com.gems.app.tenant;

import br.com.gems.firebase.auth.FirebaseAuthException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** Contrato do adapter: validar antes de tocar SQL ou alterar a sessão emprestada. */
class SampleTenantMigrationTest {

    private final SampleTenantMigration migration = new SampleTenantMigration();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "alpha", "public", "modelo", "TENANT_alpha", "tenant_Alpha", "tenant_", "tenant_public", "tenant_security", "tenant_administracao", "tenant_auditoria", "tenant_modelo", "tenant_information_schema", "tenant_global", "tenant_pg_catalog", "tenant_tenant_alpha", "tenant_área", "tenant_ alpha", "tenant_alpha ", "tenant_alpha;DROP SCHEMA security", "tenant_alpha.beta", "tenant_1alpha" })
    void deveRecusarSemTocarSessao_quandoSchemaNaoCanonico( String schema ) {
        var session = mock( Connection.class );

        assertThatThrownBy( () -> migration.apply( session, schema ) )
                .isInstanceOfAny( IllegalArgumentException.class, FirebaseAuthException.class );
        assertThatThrownBy( () -> migration.appliedRevision( session, schema ) )
                .isInstanceOfAny( IllegalArgumentException.class, FirebaseAuthException.class );

        verifyNoInteractions( session );
    }

    @ParameterizedTest
    @MethodSource("schemasLongos")
    void deveRecusarAntesDoSQL_quandoAliasExcedeLimitePostgreSQL( String schema ) {
        var session = mock( Connection.class );

        assertThatThrownBy( () -> migration.apply( session, schema ) ).isInstanceOf( FirebaseAuthException.class );
        assertThatThrownBy( () -> migration.appliedRevision( session, schema ) )
                .isInstanceOf( FirebaseAuthException.class );

        verifyNoInteractions( session );
    }

    @ParameterizedTest
    @MethodSource("schemasCanonicos")
    void deveConsultarRevisaoSemFecharSessao_quandoSchemaCanonico( String schema ) throws Exception {
        var session = mock( Connection.class );
        var statement = mock( Statement.class );
        var rows = mock( ResultSet.class );
        when( session.createStatement() ).thenReturn( statement );
        String sql = "SELECT COALESCE(MAX(NR_REVISAO),0) FROM " + schema + ".REVISAO_MODELO";
        when( statement.executeQuery( sql ) ).thenReturn( rows );
        when( rows.next() ).thenReturn( true );
        when( rows.getLong( 1 ) ).thenReturn( 1L );

        long revision = migration.appliedRevision( session, schema );

        assertThat( revision ).isOne();
        verify( session ).createStatement();
        verifyNoMoreInteractions( session );
        verify( statement ).executeQuery( sql );
        verify( statement ).close();
        verify( rows ).close();
    }

    private static Stream<String> schemasLongos() {
        return Stream.of( "tenant_" + "a".repeat( 57 ), "tenant_" + "a".repeat( 64 ) );
    }

    private static Stream<String> schemasCanonicos() {
        return Stream.of( "tenant_a", "tenant_alpha", "tenant_alpha_01", "tenant_" + "a".repeat( 56 ) );
    }

}
