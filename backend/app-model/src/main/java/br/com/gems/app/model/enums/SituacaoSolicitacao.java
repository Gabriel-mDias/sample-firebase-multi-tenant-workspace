package br.com.gems.app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * A máquina de estados da solicitação.
 *
 * <p>
 * <b>A situação nunca entra pelo corpo da requisição</b> (Princípio V). Cada transição é um
 * endpoint de ação dedicado, com a sua própria ação de {@code AcaoSistema}. Se a situação viesse
 * num {@code PUT} genérico, quem pudesse editar o cadastro poderia concluir qualquer solicitação —
 * a máquina viraria sugestão e a autorização por transição, ficção.
 *
 * <p>
 * As transições permitidas vivem <b>aqui</b>, e não espalhadas em {@code if} pelo service: uma
 * transição nova é uma linha nesta tabela, e o teste que percorre o enum a cobre sem alteração.
 *
 * <pre>
 *   RASCUNHO ──abrir──▶ ABERTA ──iniciar──▶ EM_ANDAMENTO ──concluir──▶ CONCLUIDA
 *       │                  │                     │
 *       └──────────────────┴─────────cancelar────┴─────────────────▶ CANCELADA
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum SituacaoSolicitacao {

    /** Recém-criada, ainda editável e invisível para quem executa. */
    RASCUNHO("Rascunho"),

    /** Publicada, aguardando quem a assuma. */
    ABERTA("Aberta"),

    /** Alguém assumiu a execução. É a única situação em que as etapas avançam. */
    EM_ANDAMENTO("Em andamento"),

    /** Terminal. Publica o evento consumido pelo domínio {@code parceiro}. */
    CONCLUIDA("Concluída"),

    /** Terminal. Exige motivo — cancelar sem dizer por quê é perda de informação. */
    CANCELADA("Cancelada");

    private final String descricao;

    static {
        RASCUNHO.proximas = Set.of( ABERTA, CANCELADA );
        ABERTA.proximas = Set.of( EM_ANDAMENTO, CANCELADA );
        EM_ANDAMENTO.proximas = Set.of( CONCLUIDA, CANCELADA );
        CONCLUIDA.proximas = Set.of();
        CANCELADA.proximas = Set.of();
    }

    private Set<SituacaoSolicitacao> proximas;

    /** {@code true} se esta situação pode transitar para {@code destino}. */
    public boolean permiteTransicaoPara( SituacaoSolicitacao destino ) {
        return proximas.contains( destino );
    }

    /** {@code true} quando não há transição possível a partir daqui. */
    public boolean isTerminal() {
        return proximas.isEmpty();
    }
}
