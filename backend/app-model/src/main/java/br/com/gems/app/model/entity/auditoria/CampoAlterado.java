package br.com.gems.app.model.entity.auditoria;

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

import java.util.UUID;

/**
 * Um campo que mudou numa operação de {@link br.com.gems.app.model.enums.OperacaoAuditoria#ALTERACAO}
 * .
 *
 * <h2>Tabela filha, e não campo JSON no pai</h2>
 * "Quem mexeu no telefone deste parceiro" é consulta previsível, e sobre JSON custaria varredura; e
 * {@code FL_SIGILOSO} precisa ser consultável para provar, por teste, que nenhum valor sigiloso foi
 * gravado.
 *
 * <h2>Sem {@code EntidadeAuditavel} e sem {@code @SQLDelete}</h2>
 * Mesma razão de {@link RegistroAuditoria} — ver o javadoc de lá.
 *
 * <h2>O {@code @NamedEntityGraph} NÃO mora aqui</h2>
 * Está em {@link RegistroAuditoria}, cobrindo a coleção {@code camposAlterados}: grafo é por
 * sentido de navegação, e a consulta navega {@code RegistroAuditoria → camposAlterados},
 * nunca o inverso. Reaproveitar um grafo do lado errado foi o defeito que fez
 * um endpoint responder 500 para toda entrada real, sem que nenhum teste falhasse.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "AUDIT_CHANGE", schema = "auditoria")
public class CampoAlterado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_AUDIT_CHANGE")
    private UUID id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_AUDIT_OPERATION", nullable = false)
    private RegistroAuditoria registroAuditoria;

    @Column(name = "NM_FIELD", nullable = false, length = 100)
    private String campo;

    /**
     * Nulo quando o campo era vazio, ou quando {@code sigiloso} é verdadeiro.
     *
     * <p>
     * <b>Sem {@code columnDefinition = "TEXT"}, de propósito.</b> Estas são as duas primeiras
     * colunas {@code TEXT} do projeto, e a primeira versão deste arquivo as declarava também na
     * entidade. É inerte: com {@code ddl-auto: validate} o Hibernate nunca gera DDL, e quem declara
     * o tipo é o changeset Liquibase — Princípio II. Medido: o {@code contextLoads}, que é o gate de
     * paridade JPA ↔ Liquibase, passa igual com e sem o atributo.
     *
     * <p>
     * Removê-lo evita cravar sintaxe de PostgreSQL dentro do {@code app-model}, num projeto
     * onde {@code columnDefinition} não aparece em nenhuma outra entidade. O comprimento não é
     * limitado aqui pelo mesmo motivo: um valor anterior pode ser qualquer campo de qualquer
     * entidade auditada.
     */
    @Column(name = "DS_OLD_VALUE")
    private String valorAnterior;

    /** Nulo quando o campo passou a vazio, ou quando {@code sigiloso} é verdadeiro. */
    @Column(name = "DS_NEW_VALUE")
    private String valorNovo;

    /** Quando verdadeiro, os dois valores acima são nulos e a tela mostra "valor não registrado". */
    @Column(name = "FL_SIGILOSO", nullable = false)
    private boolean sigiloso;

}
