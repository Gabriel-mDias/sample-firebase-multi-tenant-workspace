package br.com.gems.app.convencoes;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * As unidades de compilação do reator, lidas uma vez por execução de teste. Chave: o caminho
 * relativo à raiz do repositório, que é o mesmo que o relatório imprime.
 */
final class FontesJava {

    private static final JavaParser PARSER = new JavaParser( new ParserConfiguration()
            .setLanguageLevel( ParserConfiguration.LanguageLevel.JAVA_21 )
            .setCharacterEncoding( StandardCharsets.UTF_8 ) );
    private static Map<String, CompilationUnit> producao;
    private static Map<String, CompilationUnit> teste;

    private FontesJava() {
    }

    static synchronized Map<String, CompilationUnit> producao() {
        if ( producao == null ) {
            producao = carregar( LocalizadorDeFonte.fontesDeProducao().toList() );
        }
        return producao;
    }

    static synchronized Map<String, CompilationUnit> teste() {
        if ( teste == null ) {
            teste = carregar( LocalizadorDeFonte.fontesDeTeste().toList() );
        }
        return teste;
    }

    private static Map<String, CompilationUnit> carregar( List<String> caminhos ) {
        var unidades = new LinkedHashMap<String, CompilationUnit>();
        for ( var caminho : caminhos ) {
            var absoluto = LocalizadorDeFonte.RAIZ_DO_REPOSITORIO.resolve( caminho );
            try {
                var resultado = PARSER.parse( absoluto );
                var unidade = resultado.getResult()
                        .orElseThrow( () -> new IllegalStateException( caminho + " não parseou: " + resultado.getProblems() ) );
                unidades.put( caminho, unidade );
            } catch ( IOException e ) {
                throw new UncheckedIOException( e );
            }
        }
        return unidades;
    }

}
