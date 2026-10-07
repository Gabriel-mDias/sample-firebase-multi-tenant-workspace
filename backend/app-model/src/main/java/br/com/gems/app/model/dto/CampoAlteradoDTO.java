package br.com.gems.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um campo que mudou, dentro do histórico de um registro de auditoria.
 *
 * <p>
 * {@code valorAnterior} e {@code valorNovo} vêm nulos quando {@code sigiloso} é verdadeiro
 * — a tela mostra "valor não registrado".
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampoAlteradoDTO {

    private String campo;
    private String valorAnterior;
    private String valorNovo;
    private boolean sigiloso;

}
