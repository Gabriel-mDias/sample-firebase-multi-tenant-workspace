package br.com.gems.app.model.entity.base;

import br.com.gems.auditing.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * @implSpec
 *           A classe filha <b>deve</b> ser anotada com o script de atualização apontando para a sua respectiva tabela e
 *           chave primária:
 *
 *           <pre>
 * {@code
 * &#64;Entity
 * &#64;Table(name = "TB_EXEMPLO")
 * &#64;SQLDelete(sql = "UPDATE TB_EXEMPLO SET DT_EXCLUSAO = NOW() WHERE ID_EXEMPLO = ?")
 * &#64;SQLRestriction("DT_EXCLUSAO IS NULL")
 * public class Exemplo extends EntidadeAuditavel { ... }
 * }
 *           </pre>
 *
 * @see org.hibernate.annotations.SQLDelete
 * @see org.hibernate.annotations.SQLRestriction
 */
@Getter
@Setter
@MappedSuperclass
@Auditable
public abstract class EntidadeAuditavel {

    @CreationTimestamp
    @Column(name = "DT_CRIACAO", updatable = false, nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "DT_EXCLUSAO", insertable = false)
    private LocalDateTime dataExclusao;

}
