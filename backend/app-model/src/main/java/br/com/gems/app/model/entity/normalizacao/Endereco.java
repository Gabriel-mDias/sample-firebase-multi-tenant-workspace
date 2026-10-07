package br.com.gems.app.model.entity.normalizacao;

import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
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

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "ENDERECO")
@SQLDelete(sql = "UPDATE ENDERECO SET DT_EXCLUSAO = NOW() WHERE ID_ENDERECO = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class Endereco extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_ENDERECO")
    private UUID id;

    /**
     * País do endereço. <b>Sempre obrigatório</b> — é ele, e não mais {@code estado.pais}, que
     * responde em que país o endereço fica.
     *
     * <p>
     * A coluna nasceu do backfill em {@code changelog-endereco-internacional.xml}: todo endereço
     * já gravado recebeu o país do estado que referenciava.
     */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PAIS", nullable = false)
    private Pais pais;

    /**
     * Unidade federativa. <b>Opcional no banco</b>, obrigatória apenas quando o país é o Brasil —
     * regra que vive no {@code EnderecoService}, porque uma constraint não sabe
     * condicionar-se ao valor de outra coluna.
     */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ESTADO")
    private Estado estado;

    /** Opcional pelo mesmo motivo do {@link #estado}: CEP é exigência brasileira, não universal. */
    @Column(name = "CD_CEP")
    private String cep;

    @Column(name = "DS_CIDADE", nullable = false)
    private String cidade;

    @Column(name = "DS_BAIRRO", nullable = false)
    private String bairro;

    @Column(name = "DS_LOGRADOURO", nullable = false)
    private String logradouro;

    @Column(name = "NR_NUMERO")
    private String numero;

    @Column(name = "DS_COMPLEMENTO")
    private String complemento;

}
