package br.com.gems.app.convencoes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Traduz um nome qualificado de classe no <strong>caminho relativo à raiz do repositório</strong> do
 * arquivo que a declara.
 *
 * <p>
 * Existe por causa da exigência 1 do §2 de {@code contracts/gates.md}: caminho absoluto de máquina de
 * CI não é clicável para quem lê o log, e gate cuja reprovação não se localiza vira obstáculo — e
 * obstáculo é o que alguém desliga.
 *
 * <p>
 * O ArchUnit lê <em>bytecode</em>, e o bytecode não sabe onde a fonte mora: a classe chega aqui vinda
 * do jar de outro módulo do reator. Por isso a tradução é uma busca no disco, e não uma consulta ao
 * {@code SourceCodeLocation} — que devolveria o caminho do {@code .class}.
 */
final class LocalizadorDeFonte {

    /**
     * O Surefire roda com o diretório do módulo como diretório de trabalho — {@code backend/app-deploy}
     * —, que é a mesma premissa do {@code FrontendActionCatalogGateTest}. Dois níveis acima está a raiz
     * do repositório.
     */
    static final Path RAIZ_DO_REPOSITORIO = Path.of( "..", ".." ).toAbsolutePath().normalize();
    private static final List<String> MODULOS = List.of( "app-model", "app-security", "app-events", "app-core", "app-deploy" );

    private LocalizadorDeFonte() {
    }

    /**
     * @return o caminho relativo com barras normais (o log é lido em Windows e em Linux), ou o próprio
     *         nome qualificado quando o arquivo não for encontrado — nunca um caminho absoluto.
     */
    static String caminhoRelativo( String nomeQualificado ) {
        var cifrao = nomeQualificado.indexOf( '$' );
        var nomeDeTopo = cifrao < 0 ? nomeQualificado : nomeQualificado.substring( 0, cifrao );
        var sufixo = nomeDeTopo.replace( '.', '/' ) + ".java";

        return MODULOS.stream()
                .map( modulo -> Path.of( "backend", modulo, "src", "main", "java" ).resolve( sufixo ) )
                .filter( relativo -> Files.exists( RAIZ_DO_REPOSITORIO.resolve( relativo ) ) )
                .findFirst()
                .map( relativo -> relativo.toString().replace( '\\', '/' ) )
                .orElse( nomeQualificado );
    }

    /** Todos os {@code .java} de produção do reator, em caminho relativo à raiz. É a entrada do gate de fonte. */
    static Stream<String> fontesDeProducao() {
        return MODULOS.stream()
                .map( modulo -> Path.of( "backend", modulo, "src", "main", "java" ) )
                .filter( relativo -> Files.isDirectory( RAIZ_DO_REPOSITORIO.resolve( relativo ) ) )
                .flatMap( LocalizadorDeFonte::arquivosJava );
    }

    /** Todos os {@code .java} de teste do reator. Escopo da regra de nome de método de teste. */
    static Stream<String> fontesDeTeste() {
        return MODULOS.stream()
                .map( modulo -> Path.of( "backend", modulo, "src", "test", "java" ) )
                .filter( relativo -> Files.isDirectory( RAIZ_DO_REPOSITORIO.resolve( relativo ) ) )
                .flatMap( LocalizadorDeFonte::arquivosJava );
    }

    private static Stream<String> arquivosJava( Path raizRelativa ) {
        try (var caminhos = Files.walk( RAIZ_DO_REPOSITORIO.resolve( raizRelativa ) )) {
            return caminhos.filter( caminho -> caminho.toString().endsWith( ".java" ) )
                    .map( caminho -> RAIZ_DO_REPOSITORIO.relativize( caminho ).toString().replace( '\\', '/' ) )
                    .toList()
                    .stream();
        } catch ( java.io.IOException e ) {
            throw new IllegalStateException( "Não foi possível varrer " + raizRelativa, e );
        }
    }

}
