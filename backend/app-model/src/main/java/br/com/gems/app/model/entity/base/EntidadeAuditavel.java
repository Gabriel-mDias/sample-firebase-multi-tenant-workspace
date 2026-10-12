package br.com.gems.app.model.entity.base;

import br.com.gems.auditing.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Colunas comuns à exclusão lógica e à auditoria empresarial. */
@Getter
@Setter
@MappedSuperclass
@Auditable
public abstract class EntidadeAuditavel {
    @CreationTimestamp
    @Column(name = "DT_CRIACAO", updatable = false, nullable = false)
    private Instant dataCriacao;
    @Column(name = "DT_EXCLUSAO", insertable = false)
    private Instant dataExclusao;
}
