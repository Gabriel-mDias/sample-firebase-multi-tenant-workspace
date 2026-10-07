package br.com.gems.app.model.entity.parceiro;

import br.com.gems.auditing.SensitiveField;
import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cadastro de referência do sistema — e o <b>domínio canônico</b> deste projeto.
 *
 * <p>
 * Quando um domínio novo precisar de uma forma para copiar, é esta: entidade + agregados
 * normalizados sem cascade, soft delete herdado de {@link EntidadeAuditavel}, um campo sigiloso, e
 * o service com a ordem canônica de métodos. Os {@code AGENTS.md} de backend e frontend apontam
 * para cá literalmente.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "PARCEIRO")
@SQLDelete(sql = "UPDATE PARCEIRO SET DT_EXCLUSAO = NOW() WHERE ID_PARCEIRO = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
@NamedEntityGraph(name = "parceiro-entity-graph", attributeNodes = {
        @NamedAttributeNode(value = "pessoa"),
        @NamedAttributeNode(value = "contato"),
        @NamedAttributeNode(value = "endereco", subgraph = "endereco-subgraph"),
}, subgraphs = {
        @NamedSubgraph(name = "endereco-subgraph", attributeNodes = {
                @NamedAttributeNode(value = "pais"),
                @NamedAttributeNode(value = "estado", subgraph = "estado-subgraph"),
        }),
        @NamedSubgraph(name = "estado-subgraph", attributeNodes = {
                @NamedAttributeNode(value = "pais"),
        })
})
public class Parceiro extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_PARCEIRO")
    private UUID id;

    @Column(name = "NM_PARCEIRO", nullable = false)
    private String nome;

    @Column(name = "DS_PARCEIRO", length = 4000)
    private String descricao;

    /**
     * Percentual de comissão do parceiro.
     *
     * <p>
     * {@link SensitiveField} tira o <b>valor</b> da trilha de auditoria: a trilha registra que o
     * campo mudou, e não para quanto. Quem enxerga o número na API é decidido no
     * {@code ParceiroService}, por {@code possuiAcao(VISUALIZAR_COMISSAO)} — em {@code toDTO}, na
     * busca <b>e</b> na gravação, para que um payload malicioso não sobrescreva o que o chamador
     * não podia ler.
     */
    @SensitiveField
    @Column(name = "VL_COMISSAO", precision = 5, scale = 2)
    private BigDecimal comissao;

    /** Quantas solicitações deste parceiro já foram concluídas. Mantido pelo listener do evento. */
    @Column(name = "NR_SOLICITACOES_CONCLUIDAS", nullable = false)
    @Builder.Default
    private Integer solicitacoesConcluidas = 0;

    // Sem cascade, de propósito: a persistência e a exclusão de pessoa, contato e endereço são
    // responsabilidade explícita dos serviços de `normalizacao`, na ordem correta — agregado
    // primeiro no insert, dono primeiro no delete. Como a chave é nullable = false, cada instância
    // precisa estar GERENCIADA antes de o parceiro ser salvo.
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PESSOA", nullable = false)
    private Pessoa pessoa;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CONTATO", nullable = false)
    private Contato contato;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ENDERECO", nullable = false)
    private Endereco endereco;

}
