package br.com.gems.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Par constante/rótulo de um enum, para alimentar os campos de seleção da interface.
 *
 * <p>
 * O backend é a fonte única dos rótulos: o frontend nunca duplica as listas.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OpcaoDTO {

    private String valor;
    private String descricao;

}
