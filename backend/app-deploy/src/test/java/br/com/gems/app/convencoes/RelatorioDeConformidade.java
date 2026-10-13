package br.com.gems.app.convencoes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * O acumulador de violações dos dois gates de conformidade, e a catraca de baseline que os dois
 * compartilham.
 *
 * <p>
 * <strong>Por que os gates não usam as mensagens prontas do ArchUnit.</strong> O §2 de
 * {@code contracts/gates.md} exige três coisas de toda reprovação desta rodada: caminho relativo à
 * raiz, regra nomeada com âncora, e o que era <em>esperado</em>. A mensagem padrão de qualquer uma
 * das ferramentas atende no máximo à primeira — e a terceira é a que evita que cada pessoa
 * redescubra a forma certa por tentativa. Por isso as regras aqui avaliam com a ferramenta e
 * <strong>formatam com este relatório</strong>.
 *
 * <p>
 * <strong>A catraca (R-07, INV-4).</strong> O baseline congela a dívida que a rodada não alcançou, e
 * ele só encolhe: {@link #MAXIMO_DE_ENTRADAS} é o tamanho congelado no fechamento da fase 7, e
 * acrescentar uma linha ao arquivo reprova o build sozinha, antes de qualquer regra rodar. Hoje ele
 * vale <strong>zero</strong> — a fase 5 conformou o backend e não sobrou dívida a congelar.
 *
 * <p>
 * <strong>A exceção escrita (N-06).</strong> Uma violação com linha conhecida é suprimida quando a
 * linha imediatamente acima traz {@code // GEMS-EXCEPTION(<regra>): <motivo>; expira AAAA-MM-DD}
 * para a <em>mesma</em> regra — é o {@link ExcecoesEscritas} quem lê o comentário. O
 * {@link Modo#BRUTO} ignora a supressão de propósito: é comparando os dois modos que o gate de
 * exceções descobre a exceção órfã ({@code conformance-exceptions.md} §3).
 */
final class RelatorioDeConformidade {

    /**
     * O tamanho congelado do baseline. Reduzi-lo ao corrigir dívida é o caminho normal; aumentá-lo
     * exige decisão humana registrada, e é o que a mutação de T082 prova.
     */
    static final int MAXIMO_DE_ENTRADAS = 0;
    private static final Path BASELINE = Path.of( "src", "test", "resources", "archunit-baseline.txt" );

    /** Como o relatório trata a exceção escrita. */
    enum Modo {

        /** O normal do gate: exceção válida sobre a linha suprime a violação. */
        SUPRIMIR,
        /** Toda violação é registrada, com exceção ou sem — a leitura que revela a órfã. */
        BRUTO

    }

    /** Uma violação registrada, na forma que o §2 do contrato manda imprimir. */
    record Violacao(String chave, String caminho, int linha, String regra, String violacao, String esperado) {

        String formatada() {
            var cabecalho = linha > 0 ? caminho + ":" + linha : caminho;
            return """
                    %s
                      regra:    .gems-ai/rules/backend-java/%s
                      violação: %s
                      esperado: %s""".formatted( cabecalho, regra, violacao, esperado );
        }

    }

    private final String gate;
    private final Modo modo;
    private final ExcecoesEscritas excecoes;
    private final Set<String> baseline;
    private final List<Violacao> violacoes = new ArrayList<>();
    private final Set<String> baselineUsado = new LinkedHashSet<>();
    private final List<ExcecoesEscritas.Excecao> excecoesUsadas = new ArrayList<>();

    /** O relatório do gate ArchUnit: sem linha, e por isso sem exceção escrita — só baseline. */
    RelatorioDeConformidade( String gate ) {
        this( gate, ExcecoesEscritas.nenhuma(), Modo.SUPRIMIR );
    }

    RelatorioDeConformidade( String gate, ExcecoesEscritas excecoes, Modo modo ) {
        this.gate = gate;
        this.excecoes = excecoes;
        this.modo = modo;
        this.baseline = carregarBaseline();
    }

    /**
     * @param chave
     *            identidade estável da violação, comparada com o baseline
     * @param caminho
     *            relativo à raiz do repositório
     * @param linha
     *            1-indexada; {@code 0} quando a ferramenta não a conhece (o bytecode não a traz
     *            para declaração de tipo), e nesse caso ela é omitida em vez de inventada
     * @param regra
     *            arquivo de regra com âncora, ex.: {@code repositories.md#1}
     * @param violacao
     *            o que foi encontrado
     * @param esperado
     *            o que a regra prescreve
     */
    void registrar( String chave, String caminho, int linha, String regra, String violacao, String esperado ) {
        if ( baseline.contains( chave ) ) {
            baselineUsado.add( chave );
            return;
        }

        if ( modo == Modo.SUPRIMIR && linha > 0 ) {
            var excecao = excecoes.valida( caminho, linha, regra );
            if ( excecao.isPresent() ) {
                excecoesUsadas.add( excecao.get() );
                return;
            }
        }

        violacoes.add( new Violacao( chave, caminho, linha, regra, violacao, esperado ) );
    }

    /** Falha o teste listando tudo que foi registrado. Chamada ao fim de cada regra. */
    void concluir() {
        if ( Boolean.getBoolean( "gems.convencoes.relatorio" ) ) {
            try {
                var destino = Path.of( "target", "convencoes-medicao.txt" );
                Files.write( destino, violacoes.stream()
                        .map( v -> v.chave() + "\t" + v.formatada().replace( "\n", " " ).replace( "\r", " " ) )
                        .toList(), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND );
                System.out.println( gate + ": " + violacoes.size() + " violações (relatório)" );
                return;
            } catch ( IOException e ) {
                throw new UncheckedIOException( e );
            }
        }
        if ( !violacoes.isEmpty() ) {
            throw new AssertionError( "%s reprovou — %d violação(ões):%n%n%s%n".formatted( gate, violacoes.size(), String.join( System.lineSeparator() + System.lineSeparator(), violacoes.stream().map( Violacao::formatada ).toList() ) ) );
        }
    }

    /** O que foi registrado e não suprimido. No {@link Modo#BRUTO}, é tudo que as regras acharam. */
    List<Violacao> violacoes() {
        return List.copyOf( violacoes );
    }

    /** As exceções escritas que de fato suprimiram uma violação neste relatório. */
    List<ExcecoesEscritas.Excecao> excecoesUsadas() {
        return List.copyOf( excecoesUsadas );
    }

    /**
     * A catraca propriamente dita, verificada uma vez por gate: o arquivo não pode ter mais entradas
     * do que o número congelado.
     */
    static void verificarTamanhoDoBaseline() {
        var entradas = carregarBaseline();
        if ( entradas.size() > MAXIMO_DE_ENTRADAS ) {
            throw new AssertionError( """
                    backend/app-deploy/src/test/resources/archunit-baseline.txt
                      regra:    .gems-ai/rules/conformance-exceptions.md#4
                      violação: o baseline tem %d entrada(s), e o tamanho congelado é %d
                      esperado: o baseline SÓ ENCOLHE (INV-4). Desvio novo é corrigido ou vira exceção \
                    escrita GEMS-EXCEPTION(...) no ponto do código, com motivo e prazo — nunca uma linha nova aqui"""
                    .formatted( entradas.size(), MAXIMO_DE_ENTRADAS ) );
        }
    }

    private static Set<String> carregarBaseline() {
        if ( !Files.exists( BASELINE ) ) {
            return Set.of();
        }
        try {
            return Files.readAllLines( BASELINE, StandardCharsets.UTF_8 ).stream()
                    .map( String::strip )
                    .filter( linha -> !linha.isEmpty() && !linha.startsWith( "#" ) )
                    .collect( java.util.stream.Collectors.toCollection( LinkedHashSet::new ) );
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

}
