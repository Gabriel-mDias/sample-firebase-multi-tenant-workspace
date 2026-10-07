package br.com.gems.app.model.entity.auditoria;

import br.com.gems.app.model.enums.OperacaoAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma linha por operação ({@link OperacaoAuditoria#INSERCAO}, {@link OperacaoAuditoria#ALTERACAO}
 * ou {@link OperacaoAuditoria#EXCLUSAO}) sobre uma entidade de negócio.
 *
 * <h2>Por que não estende {@code EntidadeAuditavel} e não tem {@code @SQLDelete}</h2>
 * O MUST de soft delete do Princípio II é escopado a
 * entidades <b>de negócio</b>, e um registro de trilha não é uma. Auditar a auditoria seria
 * recursão sem fim, e o registro não é editável nem excluível.
 *
 * <h2>{@code registroAfetado} e {@code autorId} não são {@code @ManyToOne}</h2>
 * {@code registroAfetado} precisa sobreviver à exclusão do alvo e aponta para tabelas diferentes
 * conforme {@code entidade}; {@code autorId} precisa continuar legível se o usuário for removido —
 * por isso {@code autorNome} é desnormalizado, com origem em {@code loginDoUsuarioAutenticado()}.
 * Nenhum dos dois é FK no banco.
 *
 * <h2>O {@code @NamedEntityGraph} mora aqui, não em {@link CampoAlterado}</h2>
 * Grafo é por <b>sentido de navegação</b>, não por entidade: a consulta navega
 * {@code RegistroAuditoria → camposAlterados}. Pôr o grafo no lado errado foi o defeito que fez
 * um endpoint responder 500 para toda entrada real — reaproveitar o
 * grafo do irmão em vez de declarar um por sentido de navegação.
 *
 * <h2>Sem {@code cascade}</h2>
 * Como em todo o projeto: quem grava e exclui {@link CampoAlterado} é o mecanismo de persistência
 * (fase seguinte desta rodada), explicitamente e na mesma transação do registro-raiz.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "AUDIT_OPERATION", schema = "auditoria")
@NamedEntityGraph(name = "registro-auditoria-entity-graph", attributeNodes = {
        @NamedAttributeNode(value = "camposAlterados"),
})
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_AUDIT_OPERATION")
    private UUID id;

    @Column(name = "NM_ENTITY", nullable = false, length = 100)
    private String entidade;

    /** Não é FK: precisa sobreviver à exclusão do alvo e aponta para tabelas diferentes. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "DS_ENTITY_ID", nullable = false, length = 255)
    private UUID registroAfetado;

    @Convert(converter = OperacaoAuditoria.PersistenciaConverter.class)
    @Column(name = "CD_OPERATION", nullable = false, length = 20)
    private OperacaoAuditoria operacao;

    @Column(name = "DT_OPERATION", nullable = false)
    private LocalDateTime dataOperacao;

    /** Não é FK: a trilha precisa continuar legível se o usuário for removido. Nulo = sistema. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ID_ACTOR", length = 200)
    private UUID autorId;

    /** Desnormalizado de propósito — ver javadoc da classe. O login, ou "sistema". */
    @Column(name = "NM_ACTOR", nullable = false, length = 150)
    private String autorNome;

    /** Amarra todas as entidades gravadas no mesmo {@code save}. */
    @Column(name = "CD_CORRELATION", length = 200)
    private String correlacaoId;

    @ToString.Exclude
    @OneToMany(mappedBy = "registroAuditoria", fetch = FetchType.LAZY)
    @Builder.Default
    private List<CampoAlterado> camposAlterados = new ArrayList<>();

}
