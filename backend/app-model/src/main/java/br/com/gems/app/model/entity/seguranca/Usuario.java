package br.com.gems.app.model.entity.seguranca;

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

/**
 * Credencial de acesso de um usuário do sistema.
 *
 * <p>
 * Única entidade do projeto fora do schema {@code public}. A separação é exigência do Princípio
 * II da constituição: dado de segurança não se mistura com dado de negócio.
 *
 * <p>
 * <b>A senha nunca é coluna desta entidade</b> — ela trafega do formulário até o
 * Keycloak e morre ali. O que se guarda aqui é o {@code CD_KEYCLOAK_ID}, o {@code sub} do usuário no
 * provedor de identidade, gravado na <b>mesma transação</b> em que a conta é criada.
 *
 * <p>
 * Note que o ciclo de vida útil desta entidade é governado por {@code FL_ATIVO}, não por
 * {@code DT_EXCLUSAO}: encerrar um vínculo <b>desativa</b> o acesso, nunca o exclui. O
 * {@code @SQLDelete} abaixo existe por convenção do Princípio II e não é acionado por nenhum caminho
 * desta feature.
 *
 * @implNote O {@code @SQLDelete} é SQL nativo e <b>não</b> recebe a reescrita de schema que o
 *           Hibernate aplica ao mapeamento — daí o {@code security.USUARIO} por extenso. Escrever apenas
 *           {@code USUARIO} ali compila, passa nos testes e só falha no primeiro delete, em runtime.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "USUARIO", schema = "security")
@SQLDelete(sql = "UPDATE security.USUARIO SET DT_EXCLUSAO = NOW() WHERE ID_USUARIO = ?")
@SQLRestriction("DT_EXCLUSAO IS NULL")
public class Usuario extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID_USUARIO")
    private UUID id;

    @Column(name = "NM_LOGIN", nullable = false)
    private String login;

    /** Identificador do usuário no Keycloak ({@code sub}). */
    @Column(name = "CD_KEYCLOAK_ID", nullable = false)
    private String keycloakId;

    @Column(name = "FL_ATIVO", nullable = false)
    private Boolean ativo;

}
