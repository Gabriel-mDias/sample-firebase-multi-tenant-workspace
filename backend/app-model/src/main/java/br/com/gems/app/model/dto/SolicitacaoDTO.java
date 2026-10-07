package br.com.gems.app.model.dto;

import br.com.gems.app.model.enums.SituacaoSolicitacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SolicitacaoDTO {

    private UUID id;
    private String titulo;
    private String descricao;
    private UUID parceiroId;
    private String parceiroNome;

    /**
     * <b>Somente leitura.</b> O service ignora este campo na gravação — a situação avança apenas
     * pelos endpoints de transição, cada um com a sua própria ação. Ele viaja no DTO para que a
     * tela saiba o que exibir, não para que alguém a defina.
     */
    private SituacaoSolicitacao situacao;

    /** Somente leitura. Preenchido pelo endpoint de cancelamento. */
    private String motivoCancelamento;

    /** Somente leitura. Preenchido pelo endpoint de conclusão. */
    private LocalDateTime dataConclusao;

    private List<EtapaSolicitacaoDTO> etapas;

}
