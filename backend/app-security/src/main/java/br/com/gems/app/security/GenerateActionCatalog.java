package br.com.gems.app.security;

import br.com.gems.security.authorization.FrontendActionCatalogGenerator;
import java.nio.file.Path;

/** CLI do catálogo chama somente o gerador oficial, com a mesma fonte do backend. */
public final class GenerateActionCatalog {

    private GenerateActionCatalog() {
    }

    public static void main( String[] args ) {
        if ( args.length != 1 ) {
            throw new IllegalArgumentException( "Informe o arquivo de saída." );
        }
        FrontendActionCatalogGenerator.writeTo( Path.of( args[0] ), AcaoSistema.class );
    }

}
