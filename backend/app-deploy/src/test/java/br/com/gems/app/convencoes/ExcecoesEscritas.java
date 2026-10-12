package br.com.gems.app.convencoes;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * O leitor da exceção escrita ({@code conformance-exceptions.md}) nos comentários que o JavaParser
 * entrega — o portador de T077 ao lado do {@code SuppressWithNearbyCommentFilter} do Checkstyle. O
 * {@code toggleOffOn} do Spotless saiu com o próprio Spotless, na rodada 006.
 *
 * <p>
 * A forma canônica é {@code GEMS-EXCEPTION(<arquivo-de-regra>#<seção>): <motivo>; expira AAAA-MM-DD},
 * na linha <strong>imediatamente acima</strong> do desvio. O comentário pode continuar por mais
 * linhas {@code //} consecutivas (o exemplo da própria regra tem duas), e a linha coberta é a
 * seguinte à última delas. A leitura é por posição de texto, sem expressão regular — a mesma
 * exigência que o FR-029 faz ao gate.
 *
 * <p>
 * A fonte única é o comentário no código; o inventário que o {@code ExcecoesEscritasTest} confere é
 * derivado daqui.
 */
final class ExcecoesEscritas {

    static final String MARCA = "GEMS-EXCEPTION(";
    private static final String EXPIRA = "expira";

    /**
     * Uma exceção lida do código. {@code problema} é {@code null} quando os quatro campos estão
     * presentes e bem formados; caso contrário diz o que faltou, e a exceção não suprime nada.
     */
    record Excecao(String caminho, int linhaDoComentario, int linhaCoberta, String regra, String motivo, LocalDate expira, String problema) {

        boolean bemFormada() {
            return problema == null;
        }

        boolean vencidaEm( LocalDate hoje ) {
            return bemFormada() && !expira.isAfter( hoje );
        }

        /** A regra como o relatório a nomeia: sem o prefixo {@code backend-java/}. */
        boolean cobre( String regraDoRelatorio ) {
            return regra != null && regra.equals( "backend-java/" + regraDoRelatorio );
        }

    }

    private final List<Excecao> excecoes;
    private final LocalDate hoje;

    private ExcecoesEscritas( List<Excecao> excecoes, LocalDate hoje ) {
        this.excecoes = List.copyOf( excecoes );
        this.hoje = hoje;
    }

    static ExcecoesEscritas nenhuma() {
        return new ExcecoesEscritas( List.of(), LocalDate.now() );
    }

    static ExcecoesEscritas de( List<Excecao> excecoes, LocalDate hoje ) {
        return new ExcecoesEscritas( excecoes, hoje );
    }

    /** Todas as exceções escritas nos {@code .java} de produção e de teste do reator, com a data de hoje. */
    static ExcecoesEscritas doReator() {
        var encontradas = new ArrayList<Excecao>();
        FontesJava.producao().forEach( ( caminho, unidade ) -> encontradas.addAll( lerDe( caminho, unidade ) ) );
        FontesJava.teste().forEach( ( caminho, unidade ) -> encontradas.addAll( lerDe( caminho, unidade ) ) );
        return new ExcecoesEscritas( encontradas, LocalDate.now() );
    }

    List<Excecao> todas() {
        return excecoes;
    }

    /**
     * A exceção que suprime a violação desta regra nesta linha — bem formada, para a mesma regra,
     * e ainda dentro do prazo. Vencida não suprime: é a "falha duas vezes de propósito" da §3.
     */
    Optional<Excecao> valida( String caminho, int linha, String regra ) {
        return excecoes.stream()
                .filter( excecao -> excecao.caminho().equals( caminho ) && excecao.linhaCoberta() == linha )
                .filter( excecao -> excecao.bemFormada() && excecao.cobre( regra ) && !excecao.vencidaEm( hoje ) )
                .findFirst();
    }

    /** Lê todas as exceções de uma unidade de compilação, na ordem em que aparecem. */
    static List<Excecao> lerDe( String caminho, CompilationUnit unidade ) {
        var comentarios = unidade.getAllContainedComments().stream()
                .filter( comentario -> comentario.getBegin().isPresent() )
                .sorted( Comparator.comparingInt( comentario -> comentario.getBegin().get().line ) )
                .toList();

        var encontradas = new ArrayList<Excecao>();
        for ( int i = 0; i < comentarios.size(); i++ ) {
            var comentario = comentarios.get( i );
            var texto = comentario.getContent().strip();
            if ( !texto.startsWith( MARCA ) ) {
                continue;
            }

            var inicio = comentario.getBegin().get().line;
            var fim = comentario.getEnd().get().line;
            var acumulado = new StringBuilder( texto );

            // Continuação: linhas `//` imediatamente seguintes, até a que traz o prazo.
            while ( comentario instanceof LineComment && !contem( acumulado, EXPIRA ) && i + 1 < comentarios.size() ) {
                Comment proximo = comentarios.get( i + 1 );
                if ( !(proximo instanceof LineComment) || proximo.getBegin().get().line != fim + 1 ) {
                    break;
                }
                i++;
                comentario = proximo;
                fim = proximo.getEnd().get().line;
                acumulado.append( ' ' ).append( proximo.getContent().strip() );
            }

            encontradas.add( interpretar( caminho, inicio, fim + 1, acumulado.toString() ) );
        }
        return encontradas;
    }

    /**
     * Interpreta o texto canônico. Público ao pacote para que a leitura de TypeScript, HTML e CSS —
     * que não têm JavaParser — reutilize a mesma gramática.
     */
    static Excecao interpretar( String caminho, int linhaDoComentario, int linhaCoberta, String texto ) {
        var fechaRegra = texto.indexOf( ')' );
        if ( !texto.startsWith( MARCA ) || fechaRegra < 0 ) {
            return malformada( caminho, linhaDoComentario, linhaCoberta, "não tem a forma GEMS-EXCEPTION(<regra>#<seção>)" );
        }
        var regra = texto.substring( MARCA.length(), fechaRegra ).strip();
        if ( regra.isEmpty() || regra.indexOf( '#' ) < 0 ) {
            return malformada( caminho, linhaDoComentario, linhaCoberta, "a regra precisa ser <arquivo-de-regra>#<seção>" );
        }

        var depoisDaRegra = texto.substring( fechaRegra + 1 ).strip();
        if ( !depoisDaRegra.startsWith( ":" ) ) {
            return malformada( caminho, linhaDoComentario, linhaCoberta, "falta o ':' antes do motivo" );
        }
        var corpo = depoisDaRegra.substring( 1 ).strip();

        var posicaoDoPrazo = corpo.lastIndexOf( ";" );
        if ( posicaoDoPrazo < 0 ) {
            return malformada( caminho, linhaDoComentario, linhaCoberta, "falta '; expira AAAA-MM-DD'" );
        }
        var motivo = corpo.substring( 0, posicaoDoPrazo ).strip();
        var prazo = corpo.substring( posicaoDoPrazo + 1 ).strip();
        if ( motivo.isEmpty() ) {
            return malformada( caminho, linhaDoComentario, linhaCoberta, "o motivo está vazio" );
        }
        if ( !prazo.startsWith( EXPIRA ) ) {
            return malformada( caminho, linhaDoComentario, linhaCoberta, "falta '; expira AAAA-MM-DD'" );
        }

        var data = prazo.substring( EXPIRA.length() ).strip();
        try {
            return new Excecao( caminho, linhaDoComentario, linhaCoberta, regra, motivo, LocalDate.parse( data ), null );
        } catch ( DateTimeParseException e ) {
            return malformada( caminho, linhaDoComentario, linhaCoberta, "a data '" + data + "' não está no formato AAAA-MM-DD" );
        }
    }

    private static Excecao malformada( String caminho, int linhaDoComentario, int linhaCoberta, String problema ) {
        return new Excecao( caminho, linhaDoComentario, linhaCoberta, null, null, null, problema );
    }

    private static boolean contem( StringBuilder texto, String trecho ) {
        return texto.indexOf( trecho ) >= 0;
    }

}
