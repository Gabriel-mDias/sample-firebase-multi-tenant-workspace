package br.com.gems.app.model.entity.normalizacao;

import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "CONTATO")
@SQLDelete(sql = "UPDATE CONTATO SET DT_EXCLUSAO = NOW() WHERE ID_CONTATO = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class Contato extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_CONTATO")
    private UUID id;

    @Column(name = "CD_EMAIL", nullable = false)
    private String email;

    @Column(name = "NR_TELEFONE", nullable = false)
    private String telefone;

}
