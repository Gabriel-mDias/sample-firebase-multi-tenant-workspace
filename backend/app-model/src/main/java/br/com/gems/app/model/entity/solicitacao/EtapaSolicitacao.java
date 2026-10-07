package br.com.gems.app.model.entity.solicitacao;

import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.util.UUID;

/**
 * Uma etapa da solicitação — a coleção filha ordenada.
 *
 * <p>
 * A ordem é <b>coluna</b>, não posição na lista: uma lista Java reordenada não persiste nada, e
 * o {@code @OrderBy} do lado do dono só funciona se houver por onde ordenar.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "ETAPA_SOLICITACAO")
@SQLDelete(sql = "UPDATE ETAPA_SOLICITACAO SET DT_EXCLUSAO = NOW() WHERE ID_ETAPA_SOLICITACAO = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class EtapaSolicitacao extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_ETAPA_SOLICITACAO")
    private UUID id;

    @Column(name = "NM_ETAPA", nullable = false)
    private String nome;

    @Column(name = "NR_ORDEM", nullable = false)
    private Integer ordem;

    @Column(name = "FL_CONCLUIDA", nullable = false)
    @Builder.Default
    private Boolean concluida = false;

    @Column(name = "DT_CONCLUSAO")
    private LocalDateTime dataConclusao;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SOLICITACAO", nullable = false)
    private Solicitacao solicitacao;

}
