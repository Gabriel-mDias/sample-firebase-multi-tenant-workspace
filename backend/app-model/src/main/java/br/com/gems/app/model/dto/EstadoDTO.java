package br.com.gems.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EstadoDTO {

    private UUID id;
    private PaisDTO pais;
    private String nome;
    private String siglaEstado;

}
