package br.com.gems.app.convencoes;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * N-06 — o gate da exceção escrita ({@code conformance-exceptions.md}): o inventário bate com a
 * fonte, nenhuma exceção está vencida, nenhuma é órfã, toda regra citada existe. As quatro
 * afirmações de {@code contracts/gates.md} §3.6, cada uma no seu teste.
 *
 * <p>
 * <strong>A órfã só é detectável no Java.</strong> Detectar exige reavaliar a regra ignorando a
 * própria supressão ({@code Modo.BRUTO}) e conferir que a linha coberta desvia mesmo — e as regras
 * que este módulo sabe avaliar são as de {@link RegrasDeFonte}. Exceção sobre regra do ArchUnit é
 * sempre órfã por construção: aquele gate não conhece linha, e o contrato só lhe dá baseline.
 * Exceção em TypeScript, HTML ou CSS entra no inventário e é conferida quanto a prazo e regra;
 * a órfã dela é assunto do {@code verify-conventions.mjs}, que é quem avalia as regras do frontend.
 *
 * <p>
 * Regenerar o inventário: {@code -Dgems.excecoes.gerar=true}. Nunca à mão.
 */
class ExcecoesEscritasTest {

    private static final Path RAIZ_DAS_REGRAS = LocalizadorDeFonte.RAIZ_DO_REPOSITORIO.resolve( ".gems-ai" )
            .resolve( "rules" );

    private static List<ExcecoesEscritas.Excecao> excecoes;
    private static LocalDate hoje;

    @BeforeAll
    static void ler() {
        excecoes = InventarioDeExcecoes.lerDoRepositorio();
        hoje = LocalDate.now();
    }

    @Test
    void deveManterOInventarioIgualAoGerado_quandoOCodigoEAFonte() {
        var gerado = InventarioDeExcecoes.gerar( excecoes, hoje );

        if ( Boolean.getBoolean( InventarioDeExcecoes.PROPRIEDADE_DE_GERACAO ) ) {
            InventarioDeExcecoes.escrever( gerado );
        }

        var versionado = InventarioDeExcecoes.lerArquivo();
        if ( !gerado.equals( versionado ) ) {
            throw new AssertionError( """
                    .gems-ai/conformance-exceptions.json
                      regra:    .gems-ai/rules/conformance-exceptions.md#3
                      violação: o inventário versionado %s do que o código contém (%d exceção(ões) lida(s) da fonte)
                      esperado: o inventário é DERIVADO — regenere com \
                    `.\\mvnw.cmd -pl app-deploy test -Dtest=ExcecoesEscritasTest -Dgems.excecoes.gerar=true` \
                    e versione o resultado; nunca o edite à mão"""
                    .formatted( versionado == null ? "não existe, e difere" : "difere", excecoes.size() ) );
        }
    }

    @Test
    void deveReprovar_quandoUmaExcecaoEstaVencida() {
        reprovarSeHouver( "exceção vencida", excecoes.stream().filter( excecao -> excecao.vencidaEm( hoje ) ).toList(),
                excecao -> "expirou em " + excecao.expira() + " (hoje é " + hoje + "), regra " + excecao.regra(),
                "corrigir o desvio, ou revisar o motivo e assumir uma data nova por escrito — a data que "
                        + "chegou reprova, e o desvio volta a reprovar por conta própria" );
    }

    @Test
    void deveReprovar_quandoAExcecaoEstaMalformada() {
        reprovarSeHouver( "exceção malformada",
                excecoes.stream().filter( excecao -> !excecao.bemFormada() ).toList(),
                ExcecoesEscritas.Excecao::problema,
                "GEMS-EXCEPTION(<arquivo-de-regra>#<seção>): <motivo>; expira AAAA-MM-DD — os quatro campos "
                        + "são obrigatórios" );
    }

    @Test
    void deveReprovar_quandoARegraCitadaNaoExiste() {
        reprovarSeHouver( "regra inexistente",
                excecoes.stream().filter( ExcecoesEscritas.Excecao::bemFormada )
                        .filter( excecao -> !regraExiste( excecao.regra() ) ).toList(),
                excecao -> "a regra " + excecao.regra() + " não resolve em .gems-ai/rules/",
                "<arquivo-de-regra>#<seção> apontando para um arquivo existente e para um título dele — "
                        + "exceção que aponta para o nada não foi revisada por ninguém" );
    }

    /**
     * A detecção em dois modos da §3: as regras rodam em {@code Modo.BRUTO}, e a exceção cuja linha
     * coberta não aparece entre as violações brutas da sua regra perdeu o objeto.
     */
    @Test
    void deveReprovar_quandoUmaExcecaoEOrfa() {
        var brutas = new ArrayList<RelatorioDeConformidade.Violacao>();
        for ( Consumer<RelatorioDeConformidade> regra : todasAsRegrasDeFonte() ) {
            var relatorio = new RelatorioDeConformidade( "bruto", ExcecoesEscritas.nenhuma(),
                    RelatorioDeConformidade.Modo.BRUTO );
            regra.accept( relatorio );
            brutas.addAll( relatorio.violacoes() );
        }

        var orfas = excecoes.stream()
                .filter( ExcecoesEscritas.Excecao::bemFormada )
                .filter( excecao -> excecao.caminho().endsWith( ".java" ) )
                .filter( excecao -> brutas.stream()
                        .noneMatch( violacao -> violacao.caminho().equals( excecao.caminho() )
                                && violacao.linha() == excecao.linhaCoberta() && excecao.cobre( violacao.regra() ) ) )
                .toList();

        reprovarSeHouver( "exceção órfã", orfas,
                excecao -> "a linha coberta já está conforme a " + excecao.regra() + " — nada há para suprimir",
                "remover a exceção: o que sobrou é ruído com aparência de autorização (INV-1)" );
    }

    // ---------------------------------------------------------------- a gramática do texto canônico

    @Test
    void deveLerOsQuatroCampos_quandoOTextoEstaNaFormaCanonica() {
        var excecao = ExcecoesEscritas.interpretar( "a/B.java", 10, 11,
                "GEMS-EXCEPTION(backend-java/repositories.md#2.4): compara duas colunas entre si; expira 2030-12-31" );

        assertThat( excecao.bemFormada() ).isTrue();
        assertThat( excecao.regra() ).isEqualTo( "backend-java/repositories.md#2.4" );
        assertThat( excecao.motivo() ).isEqualTo( "compara duas colunas entre si" );
        assertThat( excecao.expira() ).isEqualTo( LocalDate.of( 2030, 12, 31 ) );
        assertThat( excecao.cobre( "repositories.md#2.4" ) ).isTrue();
        assertThat( excecao.vencidaEm( LocalDate.of( 2030, 12, 31 ) ) ).isTrue();
        assertThat( excecao.vencidaEm( LocalDate.of( 2030, 12, 30 ) ) ).isFalse();
    }

    @Test
    void deveApontarOCampoQueFalta_quandoOTextoNaoTemPrazo() {
        var semPrazo = ExcecoesEscritas.interpretar( "a/B.java", 1, 2,
                "GEMS-EXCEPTION(backend-java/repositories.md#2.4): compara duas colunas entre si" );
        var semMotivo = ExcecoesEscritas.interpretar( "a/B.java", 1, 2,
                "GEMS-EXCEPTION(backend-java/repositories.md#2.4): ; expira 2030-01-01" );
        var semSecao = ExcecoesEscritas.interpretar( "a/B.java", 1, 2,
                "GEMS-EXCEPTION(backend-java/repositories.md): motivo; expira 2030-01-01" );

        assertThat( semPrazo.problema() ).contains( "expira" );
        assertThat( semMotivo.problema() ).contains( "motivo" );
        assertThat( semSecao.problema() ).contains( "#" );
    }

    @Test
    void deveLerOComentarioDeVariasLinhas_quandoOPortadorEHtml() {
        var lidas = InventarioDeExcecoes.lerDeTexto( "frontend/src/x.html", List.of(
                "<div>",
                "<!-- GEMS-EXCEPTION(frontend-angular/lists.md#4): a tabela legada do relatório fiscal é substituída",
                "     na rodada 009; expira 2030-11-30 -->",
                "<table class=\"tabela-legada\">" ) );

        assertThat( lidas ).hasSize( 1 );
        assertThat( lidas.get( 0 ).bemFormada() ).isTrue();
        assertThat( lidas.get( 0 ).linhaDoComentario() ).isEqualTo( 2 );
        assertThat( lidas.get( 0 ).linhaCoberta() ).isEqualTo( 4 );
        assertThat( lidas.get( 0 ).motivo() ).isEqualTo(
                "a tabela legada do relatório fiscal é substituída na rodada 009" );
    }

    // ---------------------------------------------------------------- apoio

    private static List<Consumer<RelatorioDeConformidade>> todasAsRegrasDeFonte() {
        return List.of( RegrasDeFonte::joinsIdenticos, RegrasDeFonte::semComparacaoComNuloEmAppendFilters,
                RegrasDeFonte::mapperComoPrimeiroCampo, RegrasDeFonte::clausulaDePartida, RegrasDeFonte::ordemDaHeranca,
                RegrasDeFonte::projecaoPorConstrutor, RegrasDeFonte::ordemDosMetodosDaService,
                RegrasDeFonte::nomeDeMetodoDeTeste );
    }

    /**
     * {@code backend-java/repositories.md#2.4} → o arquivo existe e tem um título cujo número, ou cujo nome, é
     * {@code 2.4}.
     */
    private static boolean regraExiste( String regra ) {
        var cerquilha = regra.indexOf( '#' );
        var arquivo = RAIZ_DAS_REGRAS.resolve( regra.substring( 0, cerquilha ) );
        var secao = regra.substring( cerquilha + 1 ).strip();
        if ( !Files.isRegularFile( arquivo ) || secao.isEmpty() ) {
            return false;
        }
        try {
            return Files.readAllLines( arquivo ).stream()
                    .filter( linha -> linha.startsWith( "#" ) )
                    .map( linha -> linha.substring( linha.lastIndexOf( '#' ) + 1 ).strip() )
                    .anyMatch( titulo -> tituloResolve( titulo, secao ) );
        } catch ( java.io.IOException e ) {
            throw new java.io.UncheckedIOException( e );
        }
    }

    private static boolean tituloResolve( String titulo, String secao ) {
        var espaco = titulo.indexOf( ' ' );
        var primeiraPalavra = espaco < 0 ? titulo : titulo.substring( 0, espaco );
        if ( primeiraPalavra.endsWith( "." ) ) {
            primeiraPalavra = primeiraPalavra.substring( 0, primeiraPalavra.length() - 1 );
        }
        var comoAncora = titulo.toLowerCase().replace( ' ', '-' );
        return primeiraPalavra.equalsIgnoreCase( secao ) || comoAncora.startsWith( secao.toLowerCase() );
    }

    private static void reprovarSeHouver( String motivo, List<ExcecoesEscritas.Excecao> reprovadas,
            java.util.function.Function<ExcecoesEscritas.Excecao, String> violacao, String esperado ) {
        if ( reprovadas.isEmpty() ) {
            return;
        }
        var mensagens = reprovadas.stream().map( excecao -> """
                %s:%d
                  regra:    .gems-ai/rules/conformance-exceptions.md#3
                  violação: %s — %s
                  esperado: %s""".formatted( excecao.caminho(), excecao.linhaDoComentario(), motivo,
                violacao.apply( excecao ), esperado ) ).toList();
        throw new AssertionError( "Exceções escritas reprovou — %d %s(s):%n%n%s%n".formatted( reprovadas.size(), motivo,
                String.join( System.lineSeparator() + System.lineSeparator(), mensagens ) ) );
    }
}
