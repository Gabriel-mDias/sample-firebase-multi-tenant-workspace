package br.com.gems.app.model.response;

import br.com.gems.app.model.enums.SituacaoSolicitacao;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Projeção da busca paginada de solicitações. Ver a nota de contrato em {@link ParceiroResponseDTO}. */
@Getter
@Setter
public class SolicitacaoResponseDTO {

    private UUID id;
    private String titulo;
    private SituacaoSolicitacao situacao;
    private UUID parceiroId;
    private String parceiroNome;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataConclusao;

    public SolicitacaoResponseDTO( UUID id, String titulo, SituacaoSolicitacao situacao, UUID parceiroId,
            String parceiroNome, LocalDateTime dataCriacao, LocalDateTime dataConclusao ) {
        this.id = id;
        this.titulo = titulo;
        this.situacao = situacao;
        this.parceiroId = parceiroId;
        this.parceiroNome = parceiroNome;
        this.dataCriacao = dataCriacao;
        this.dataConclusao = dataConclusao;
    }

}
