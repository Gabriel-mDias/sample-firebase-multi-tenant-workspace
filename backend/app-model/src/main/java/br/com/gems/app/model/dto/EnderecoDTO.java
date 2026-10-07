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
public class EnderecoDTO {

    private UUID id;

    /** Sempre obrigatório. */
    private PaisDTO pais;

    /** Obrigatório apenas quando o país é o Brasil. */
    private EstadoDTO estado;

    /** Obrigatório apenas quando o país é o Brasil. */
    private String cep;
    private String cidade;
    private String bairro;
    private String logradouro;
    private String numero;
    private String complemento;

}
