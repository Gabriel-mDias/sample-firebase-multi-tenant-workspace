package br.com.gems.app.convencoes;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConvencoesControllerTest {

    @Test
    void deveSeguirOrdemCanonica_quandoControllerDeclaraOperacoesCRUD() throws Exception {
        var canonical = List.of( "findById", "findAll", "search", "insert", "save", "delete" );
        try (var sources = Files.walk( Path.of( "../app-core/src/main/java" ) )) {
            for ( var source : sources.filter( p -> p.toString().endsWith( "Controller.java" ) ).toList() ) {
                var unit = StaticJavaParser.parse( source );
                for ( var type : unit.findAll( ClassOrInterfaceDeclaration.class ) ) {
                    if ( type.isAnnotationPresent( "RestController" ) ) {
                        var actual = type.getMethods().stream().map( method -> method.getNameAsString() )
                                .filter( canonical::contains ).toList();
                        var expected = actual.stream().sorted( java.util.Comparator.comparingInt( canonical::indexOf ) )
                                .toList();
                        assertThat( actual ).as( "java.controllers section 2: %s", source )
                                .containsExactlyElementsOf( expected );
                    }
                }
            }
        }
    }

}
