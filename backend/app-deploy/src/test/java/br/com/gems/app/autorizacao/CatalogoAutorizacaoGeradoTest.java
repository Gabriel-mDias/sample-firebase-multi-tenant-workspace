package br.com.gems.app.autorizacao;

import br.com.gems.app.security.AcaoSistema;
import br.com.gems.security.authorization.FrontendActionCatalogGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoAutorizacaoGeradoTest {

    @Test
    @DisplayName("o catálogo gerado é determinístico e ordenado")
    void deveGerarCatalogoDeterministico_quandoExecutaMaisDeUmaVez() {
        var primeiraSaida = FrontendActionCatalogGenerator.generate( AcaoSistema.class );
        var segundaSaida = FrontendActionCatalogGenerator.generate( AcaoSistema.class );

        assertThat( segundaSaida ).isEqualTo( primeiraSaida );
        assertThat( primeiraSaida ).contains( "\"global\"", "\"tenant\": []" );
        assertThat( primeiraSaida.indexOf( "ABRIR_SOLICITACAO" ) )
                .isLessThan( primeiraSaida.indexOf( "VISUALIZAR_COMISSAO" ) );
    }
}
