// origem: local; precedencia: local-vence; versao: 2.1.0
package br.com.gems.{app}.model.entity.{schema};

import br.com.gems.{app}.model.enums.{dominio}.{TipoEnum};
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

// Forma: rules/backend-java/architecture.md §2. Anotações nesta ordem; parêntese de anotação sem espaço interno.
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "{TABELA}", schema = "{schema}")
@SQLDelete(sql = "UPDATE {schema}.{TABELA} SET DT_EXCLUSAO = NOW() WHERE ID_{TABELA} = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class {Entidade}Entity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_{TABELA}")
    private UUID id;

    @Column(name = "DS_NOME", nullable = false, length = 255)
    private String nome;

    // Enumerações: sempre STRING — nunca ordinal.
    @Enumerated(EnumType.STRING)
    @Column(name = "CD_TIPO", nullable = false, length = 32)
    private {TipoEnum} tipo;

    // Texto longo: VARCHAR(4000) com prefixo DS_; TEXT (TX_) só quando a necessidade for explícita.
    @Column(name = "DS_OBSERVACAO", length = 4000)
    private String observacao;

    // Relacionamento: só do lado que tem a chave estrangeira, sempre LAZY. Sem cascade — a Service
    // delega a persistência do relacionado à Service dele (services.md §8).
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_{RELACIONADO}", nullable = false)
    private {Relacionado}Entity {relacionado};

    // Propriedade calculada pelo banco (não persistida) — evita recálculo na Service ou no DTO.
    @Formula(" CONCAT(DS_NOME, ' ', CD_TIPO) ")
    private String descricao;

    @Column(name = "DT_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "DT_EXCLUSAO")
    private LocalDateTime dataExclusao;
}
