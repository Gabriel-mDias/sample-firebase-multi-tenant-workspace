package br.com.gems.app.model.params;

import br.com.gems.app.model.enums.SituacaoSolicitacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SolicitacaoFilterParams {

    private UUID id;
    private String titulo;
    private SituacaoSolicitacao situacao;
    private UUID parceiroId;

}
