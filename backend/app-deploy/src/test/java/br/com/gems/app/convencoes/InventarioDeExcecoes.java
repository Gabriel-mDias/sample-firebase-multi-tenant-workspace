package br.com.gems.app.convencoes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * O inventário de exceções escritas ({@code data-model.md} §2): <strong>derivado</strong> dos
 * comentários no código e verificado no build — o mesmo desenho do catálogo de ações
 * ({@code AcaoSistema} → {@code authorization.actions.json} → gate Maven).
 *
 * <p>
 * Ele é escrito à mão em JSON, sem biblioteca, por uma razão medida na rodada 007: o Jackson quebra
 * linha com o separador do sistema, e um arquivo versionado que muda de CRLF para LF conforme a
 * máquina que o gerou é um gate que reprova no CI o que passou na IDE. Aqui o separador é sempre
 * {@code \n}, e a comparação normaliza o que o {@code autocrlf} do Git possa ter feito.
 *
 * <p>
 * <strong>Como regenerar</strong>: {@code .\mvnw.cmd -pl app-deploy test -Dtest=ExcecoesEscritasTest
 * -Dgems.excecoes.gerar=true}. Editar o arquivo à mão é o defeito, não o atalho.
 */
final class InventarioDeExcecoes {

    static final Path ARQUIVO = LocalizadorDeFonte.RAIZ_DO_REPOSITORIO.resolve( ".gems-ai" )
            .resolve( "conformance-exceptions.json" );

    static final String PROPRIEDADE_DE_GERACAO = "gems.excecoes.gerar";

    private static final List<String> EXTENSOES_DO_FRONTEND = List.of( ".ts", ".html", ".css" );

    private InventarioDeExcecoes() {
    }

    /** Todas as exceções do repositório: os {@code .java} do reator e os fontes do frontend. */
    static List<ExcecoesEscritas.Excecao> lerDoRepositorio() {
        var todas = new ArrayList<>( ExcecoesEscritas.doReator().todas() );
        todas.addAll( lerDoFrontend() );
        todas.sort( Comparator.comparing( ExcecoesEscritas.Excecao::caminho )
                .thenComparingInt( ExcecoesEscritas.Excecao::linhaCoberta ) );
        return todas;
    }

    static String gerar( List<ExcecoesEscritas.Excecao> excecoes, LocalDate hoje ) {
        var json = new StringBuilder();
        json.append( "{\n" );
        json.append(
                "  \"_origem\": \"Derivado dos comentários GEMS-EXCEPTION(...) do código pelo ExcecoesEscritasTest. " )
                .append( "Nunca editar à mão: regenerar com -Dgems.excecoes.gerar=true.\",\n" );
        json.append( "  \"excecoes\": [" );

        for ( int i = 0; i < excecoes.size(); i++ ) {
            var excecao = excecoes.get( i );
            json.append( i == 0 ? "\n" : ",\n" );
            json.append( "    {\n" );
            json.append( "      \"arquivo\": " ).append( texto( excecao.caminho() ) ).append( ",\n" );
            json.append( "      \"linha\": " ).append( excecao.linhaCoberta() ).append( ",\n" );
            json.append( "      \"regra\": " ).append( texto( excecao.regra() ) ).append( ",\n" );
            json.append( "      \"motivo\": " ).append( texto( excecao.motivo() ) ).append( ",\n" );
            json.append( "      \"expira\": " )
                    .append( texto( excecao.expira() == null ? null : excecao.expira().toString() ) )
                    .append( ",\n" );
            json.append( "      \"situacao\": " ).append( texto( situacao( excecao, hoje ) ) ).append( "\n" );
            json.append( "    }" );
        }

        json.append( excecoes.isEmpty() ? "]\n" : "\n  ]\n" );
        json.append( "}\n" );
        return json.toString();
    }

    static String situacao( ExcecoesEscritas.Excecao excecao, LocalDate hoje ) {
        if ( !excecao.bemFormada() ) {
            return "malformada";
        }
        return excecao.vencidaEm( hoje ) ? "vencida" : "valida";
    }

    static String lerArquivo() {
        if ( !Files.exists( ARQUIVO ) ) {
            return null;
        }
        try {
            return Files.readString( ARQUIVO, StandardCharsets.UTF_8 ).replace( "\r\n", "\n" );
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    static void escrever( String conteudo ) {
        try {
            Files.writeString( ARQUIVO, conteudo, StandardCharsets.UTF_8 );
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    /**
     * TypeScript, HTML e CSS não têm JavaParser: a leitura é por linha, com o mesmo texto canônico.
     * O portador de cada linguagem ({@code //}, {@code <!-- -->}, {@code /* *&#47;}) é descartado
     * antes de interpretar.
     */
    static List<ExcecoesEscritas.Excecao> lerDoFrontend() {
        var raiz = LocalizadorDeFonte.RAIZ_DO_REPOSITORIO.resolve( "frontend" ).resolve( "src" );
        if ( !Files.isDirectory( raiz ) ) {
            return List.of();
        }
        try ( Stream<Path> caminhos = Files.walk( raiz ) ) {
            var encontradas = new ArrayList<ExcecoesEscritas.Excecao>();
            for ( var caminho : caminhos.filter( InventarioDeExcecoes::ehFonteDoFrontend ).sorted().toList() ) {
                var relativo = LocalizadorDeFonte.RAIZ_DO_REPOSITORIO.relativize( caminho ).toString().replace( '\\',
                        '/' );
                encontradas.addAll( lerDeTexto( relativo, Files.readAllLines( caminho, StandardCharsets.UTF_8 ) ) );
            }
            return encontradas;
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    static List<ExcecoesEscritas.Excecao> lerDeTexto( String caminho, List<String> linhas ) {
        var encontradas = new ArrayList<ExcecoesEscritas.Excecao>();
        for ( int i = 0; i < linhas.size(); i++ ) {
            var inicioDaMarca = linhas.get( i ).indexOf( ExcecoesEscritas.MARCA );
            if ( inicioDaMarca < 0 ) {
                continue;
            }

            var inicio = i;
            var acumulado = new StringBuilder( semPortador( linhas.get( inicio ).substring( inicioDaMarca ) ) );
            var ultima = inicio;
            while ( acumulado.indexOf( "expira" ) < 0 && ultima + 1 < linhas.size() ) {
                ultima++;
                acumulado.append( ' ' ).append( semPortador( linhas.get( ultima ) ) );
            }
            i = ultima;
            // Índices são 0-based; linhas do relatório são 1-based, e a coberta é a seguinte à última.
            encontradas.add(
                    ExcecoesEscritas.interpretar( caminho, inicio + 1, ultima + 2, acumulado.toString().strip() ) );
        }
        return encontradas;
    }

    private static boolean ehFonteDoFrontend( Path caminho ) {
        var nome = caminho.getFileName().toString();
        return Files.isRegularFile( caminho ) && EXTENSOES_DO_FRONTEND.stream().anyMatch( nome::endsWith );
    }

    /** Tira o que o comentário de cada linguagem põe em volta do texto canônico. */
    private static String semPortador( String linha ) {
        var texto = linha.strip();
        for ( var abertura : List.of( "//", "/*", "<!--", "*" ) ) {
            if ( texto.startsWith( abertura ) ) {
                texto = texto.substring( abertura.length() ).strip();
                break;
            }
        }
        for ( var fechamento : List.of( "-->", "*/" ) ) {
            if ( texto.endsWith( fechamento ) ) {
                texto = texto.substring( 0, texto.length() - fechamento.length() ).strip();
            }
        }
        return texto;
    }

    private static String texto( String valor ) {
        if ( valor == null ) {
            return "null";
        }
        var escapado = new StringBuilder( "\"" );
        for ( var caractere : valor.toCharArray() ) {
            switch ( caractere ) {
                case '"' -> escapado.append( "\\\"" );
                case '\\' -> escapado.append( "\\\\" );
                case '\n' -> escapado.append( "\\n" );
                case '\r' -> escapado.append( "\\r" );
                case '\t' -> escapado.append( "\\t" );
                default -> escapado.append( caractere );
            }
        }
        return escapado.append( '"' ).toString();
    }
}
