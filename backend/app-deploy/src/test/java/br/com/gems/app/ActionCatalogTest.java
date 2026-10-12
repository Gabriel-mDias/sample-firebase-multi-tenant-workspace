package br.com.gems.app;

import br.com.gems.app.security.AcaoSistema;
import br.com.gems.security.authorization.FrontendActionCatalogGenerator;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ActionCatalogTest {

    @Test
    void deveVerificarCatalogoGeradoOficialmente_quandoBuildCompilaAcoes() {
        FrontendActionCatalogGenerator.verify( Path.of( "src/main/resources/authorization.actions.json" ), AcaoSistema.class );
    }

}
