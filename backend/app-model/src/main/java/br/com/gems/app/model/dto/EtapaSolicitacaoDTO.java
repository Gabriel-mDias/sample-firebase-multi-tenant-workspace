package br.com.gems.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EtapaSolicitacaoDTO {

    private UUID id;
    private String nome;
    private Integer ordem;
    private Boolean concluida;
    private LocalDateTime dataConclusao;

}
