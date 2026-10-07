package br.com.gems.app.model.response;

import br.com.gems.app.model.dto.CampoAlteradoDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Projeção de histórico servida por {@code GET /api/auditoria/{entidade}/{id}} e
 * {@code POST /api/auditoria/search}.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistroAuditoriaResponseDTO {

    private UUID id;
    private String entidade;
    private UUID registroAfetado;

    /** INSERCAO | ALTERACAO | EXCLUSAO */
    private String operacao;
    private String operacaoDescricao;

    private LocalDateTime dataOperacao;
    private String autorNome;

    /** Vazia em INSERCAO e EXCLUSAO. */
    private List<CampoAlteradoDTO> camposAlterados;

}
