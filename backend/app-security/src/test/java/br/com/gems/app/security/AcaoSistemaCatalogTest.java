package br.com.gems.app.security;

import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.security.authorization.AuthorizationScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcaoSistemaCatalogTest {

    @Test
    @DisplayName("as 21 ações são globais e derivadas do enum")
    void deveDerivarVinteEUmaAcoesGlobais_quandoGeraCatalogo() {
        var catalogo = AuthorizationCatalog.of( AcaoSistema.class );

        assertThat( AcaoSistema.values() ).hasSize( 21 );
        assertThat( catalogo.globalActions() )
                .hasSize( 21 )
                .containsExactlyInAnyOrder(
                        java.util.Arrays.stream( AcaoSistema.values() ).map( Enum::name ).toArray( String[]::new ) );
        assertThat( catalogo.tenantActions() ).isEmpty();
        assertThat( java.util.Arrays.stream( AcaoSistema.values() ) )
                .allSatisfy( acao -> assertThat( acao.scope() ).isEqualTo( AuthorizationScope.GLOBAL ) );
    }
}
