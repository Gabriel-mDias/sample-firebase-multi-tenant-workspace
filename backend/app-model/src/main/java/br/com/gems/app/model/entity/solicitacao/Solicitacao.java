package br.com.gems.app.model.entity.solicitacao;

import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.enums.SituacaoSolicitacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O domínio de fluxo — o que o {@code parceiro} não demonstra.
 *
 * <p>
 * Três coisas vivem aqui e em nenhum outro lugar do esqueleto: uma máquina de estados cuja
 * situação <b>nunca</b> vem do corpo da requisição, uma coleção filha ordenada, e a publicação de
 * um evento de domínio consumido por outro domínio.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "SOLICITACAO")
@SQLDelete(sql = "UPDATE SOLICITACAO SET DT_EXCLUSAO = NOW() WHERE ID_SOLICITACAO = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
@NamedEntityGraph(
        // Grafo é por SENTIDO DE NAVEGAÇÃO, não por entidade: quem navega
        // `Solicitacao -> etapas` é esta consulta, então o grafo mora deste lado.
        name = "solicitacao-entity-graph", attributeNodes = {
                @NamedAttributeNode(value = "parceiro"),
                @NamedAttributeNode(value = "etapas"),
        })
public class Solicitacao extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_SOLICITACAO")
    private UUID id;

    @Column(name = "NM_SOLICITACAO", nullable = false)
    private String titulo;

    @Column(name = "DS_SOLICITACAO", length = 4000)
    private String descricao;

    /**
     * Nunca preenchida a partir de um DTO de entrada. O {@code SolicitacaoService} a define no
     * insert ({@code RASCUNHO}) e nos endpoints de transição; qualquer outro caminho é defeito.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "CD_SITUACAO", nullable = false, length = 20)
    private SituacaoSolicitacao situacao;

    /** Preenchido apenas no cancelamento, e obrigatório nele. */
    @Column(name = "DS_MOTIVO_CANCELAMENTO", length = 4000)
    private String motivoCancelamento;

    @Column(name = "DT_CONCLUSAO")
    private LocalDateTime dataConclusao;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PARCEIRO", nullable = false)
    private Parceiro parceiro;

    // Sem cascade (Princípio I): as etapas são gravadas e excluídas explicitamente pelo service,
    // agregado-primeiro no insert e dono-primeiro no delete.
    @ToString.Exclude
    @OneToMany(mappedBy = "solicitacao", fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<EtapaSolicitacao> etapas = new ArrayList<>();

}
