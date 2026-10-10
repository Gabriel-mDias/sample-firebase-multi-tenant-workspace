package br.com.gems.app.model.entity.demo;

import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.auditing.SensitiveField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Exemplo empresarial removível, sem schema fixo. */
@Entity
@Table(name = "DEMO_REGISTRO")
@SQLDelete(sql = "UPDATE DEMO_REGISTRO SET DT_EXCLUSAO = NOW() WHERE ID_DEMO_REGISTRO = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class DemoRegistro extends EntidadeAuditavel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_DEMO_REGISTRO")
    private UUID id;
    @Column(name = "NM_REGISTRO", nullable = false, length = 150)
    private String nome;
    @Column(name = "DS_REGISTRO", length = 1000)
    private String descricao;
    @SensitiveField
    @ToString.Exclude
    @Column(name = "VL_RESTRITO", precision = 19, scale = 2)
    private BigDecimal valorRestrito;
}
