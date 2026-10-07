package br.com.gems.app.model.dto;

import br.com.gems.app.model.enums.OperacaoAuditoria;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Um registro de auditoria, completo. Espelha {@code RegistroAuditoria}.
 *
 * <p>
 * Não há endpoint de escrita para este DTO: quem grava é sempre o mecanismo de
 * persistência, nunca a API — um endpoint de escrita permitiria forjar trilha.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistroAuditoriaDTO {

    private UUID id;
    private String entidade;
    private UUID registroAfetado;
    private OperacaoAuditoria operacao;
    private LocalDateTime dataOperacao;

    /** Nulo quando a operação é do próprio sistema. */
    private UUID autorId;
    private String autorNome;

    /** Amarra todas as entidades gravadas no mesmo {@code save}. */
    private String correlacaoId;

    /** Vazia em INSERCAO e EXCLUSAO. */
    private List<CampoAlteradoDTO> camposAlterados;

}
