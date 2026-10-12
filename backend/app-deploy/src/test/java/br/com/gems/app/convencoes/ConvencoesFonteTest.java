package br.com.gems.app.convencoes;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * N-05 — o gate de fonte da rodada 008: o que só a <strong>árvore sintática</strong> sabe afirmar
 * sobre a assinatura de {@code .gems-ai/rules/backend-java/}, na ordem de valor do FR-029. As regras
 * vivem em {@link RegrasDeFonte}; aqui cada uma ganha um teste, o relatório e a exceção escrita.
 *
 * <p>
 * <strong>O que este gate NÃO cobre dos princípios que invoca</strong> (FR-029a — cobertura parcial
 * não declarada é lida como cobertura total, e é assim que um princípio deixa de ser verificado sem
 * que ninguém perceba):
 * <ul>
 * <li>a proibição de {@code != null} <em>fora</em> de {@code appendFilters} — o Princípio IV a
 * estende a toda validação de service, e o gate só olha a montagem do filtro;</li>
 * <li>a exigência de <em>validação acumulada</em> ({@code services.md} §7: uma única
 * {@code BusinessException( errors )} ao fim do {@code validate}) — MUST do Princípio IV, sem
 * verificação mecânica hoje;</li>
 * <li>o repository como <em>segundo</em> campo da service — a regra prescreve, o FR-029 só exige o
 * primeiro.</li>
 * </ul>
 * Cada um destes fica a cargo da revisão humana até que uma rodada decida cobri-lo.
 *
 * <p>
 * Escopo das regras de service: classes {@code @Service} cujo nome termina em {@code Service} — o
 * {@code OrganizacaoProvisioningWorker} e o {@code StepExecutor} são auxiliares, e {@code services.md}
 * descreve a service do domínio, não eles.
 */
class ConvencoesFonteTest {

    private static ExcecoesEscritas excecoes;

    @BeforeAll
    static void lerExcecoes() {
        excecoes = ExcecoesEscritas.doReator();
    }

    @Test
    void deveRecusarQualquerSupressao_quandoOBaselineCheckstyleEstaZerado() throws java.io.IOException {
        var atual = java.nio.file.Files
                .readAllLines( java.nio.file.Path.of( "../config/checkstyle/suppressions.xml" ) );
        var supressoes = atual.stream().map( String::strip ).filter( linha -> linha.startsWith( "<suppress " ) )
                .toList();
        if ( !supressoes.isEmpty() ) {
            throw new AssertionError( "Baseline Checkstyle deve permanecer zerado: " + supressoes );
        }
    }

    @Test
    void deveManterJoinsIdenticos_quandoCountQueryESearchQueryCoexistem() {
        avaliar( "Fonte · JOINs idênticos", RegrasDeFonte::joinsIdenticos );
    }

    @Test
    void deveRecusarComparacaoComNulo_quandoOFiltroEMontadoEmAppendFilters() {
        avaliar( "Fonte · != null em appendFilters", RegrasDeFonte::semComparacaoComNuloEmAppendFilters );
    }

    @Test
    void deveTerOMapperComoPrimeiroCampo_quandoAServiceTemMapperDoDominio() {
        avaliar( "Fonte · primeiro campo da service", RegrasDeFonte::mapperComoPrimeiroCampo );
    }

    @Test
    void deveComecarComWhereUmIgualUm_quandoAppendFiltersMontaOFiltro() {
        avaliar( "Fonte · WHERE 1=1", RegrasDeFonte::clausulaDePartida );
    }

    @Test
    void deveEstenderJpaRepositoryAntesDeBaseCustomJpaRepository_quandoORepositorioDeclaraOsDois() {
        avaliar( "Fonte · ordem do extends", RegrasDeFonte::ordemDaHeranca );
    }

    @Test
    void deveProjetarPorConstrutor_quandoSearchQueryDevolveDTO() {
        avaliar( "Fonte · projeção por construtor", RegrasDeFonte::projecaoPorConstrutor );
    }

    @Test
    void deveSeguirAOrdemCanonicaDeMetodos_quandoAClasseEUmaService() {
        avaliar( "Fonte · ordem dos métodos da service", RegrasDeFonte::ordemDosMetodosDaService );
    }

    @Test
    void deveNomearComoDeveXQuandoY_quandoOMetodoEAnotadoComoTeste() {
        avaliar( "Fonte · nome de método de teste", RegrasDeFonte::nomeDeMetodoDeTeste );
    }

    private static void avaliar( String gate, Consumer<RelatorioDeConformidade> regra ) {
        var relatorio = new RelatorioDeConformidade( gate, excecoes, RelatorioDeConformidade.Modo.SUPRIMIR );
        regra.accept( relatorio );
        relatorio.concluir();
    }

}
