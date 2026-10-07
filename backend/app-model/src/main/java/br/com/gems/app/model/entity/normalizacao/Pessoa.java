package br.com.gems.app.model.entity.normalizacao;

import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.app.model.enums.Sexo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Identidade civil de uma pessoa física.
 *
 * <p>
 * Dado normalizado, compartilhado: é reaproveitado por todo domínio que precise de identidade civil —
 * parceiro, usuário, e o que vier. Por isso vive em {@code normalizacao}, ao lado de {@link Contato} e
 * {@link Endereco}, e não no pacote de nenhum domínio de negócio.
 *
 * <p>
 * O CPF identifica a pessoa de forma única <b>entre os registros ativos</b>. Não existe
 * {@code UNIQUE} físico na coluna: soft delete não compõe com constraint de banco sem índice
 * parcial, então a regra vive inteiramente no {@code PessoaService} — mesmo padrão já adotado em
 * {@code NM_PARCEIRO}.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "PESSOA")
@SQLDelete(sql = "UPDATE PESSOA SET DT_EXCLUSAO = NOW() WHERE ID_PESSOA = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class Pessoa extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_PESSOA")
    private UUID id;

    @Column(name = "NM_PRIMEIRO_NOME", nullable = false)
    private String primeiroNome;

    @Column(name = "NM_SOBRENOME", nullable = false)
    private String sobrenome;

    @Column(name = "DT_NASCIMENTO")
    private LocalDate dataNascimento;

    /** Apenas dígitos, sem máscara. */
    @Column(name = "CD_CPF", length = 11)
    private String cpf;

    @Column(name = "CD_RG", length = 20)
    private String rg;

    @Enumerated(EnumType.STRING)
    @Column(name = "CD_SEXO", length = 20)
    private Sexo sexo;

    @Column(name = "DT_OBITO")
    private LocalDate dataObito;

    @Builder.Default
    @Column(name = "FL_DATA_APROXIMADA", nullable = false)
    private Boolean dataAproximada = false;

}
