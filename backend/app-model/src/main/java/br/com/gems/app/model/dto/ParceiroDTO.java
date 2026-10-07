package br.com.gems.app.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ParceiroDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private PessoaDTO pessoa;
    private ContatoDTO contato;
    private EnderecoDTO endereco;

    /**
     * Preenchido apenas para quem tem {@code VISUALIZAR_COMISSAO}, e zerado pelo service para os
     * demais — em {@code toDTO}, na busca <b>e</b> na gravação.
     *
     * <p>
     * Zerar na gravação não é excesso: sem isso, um payload de quem não pode ler o campo
     * sobrescreveria o valor existente com {@code null}, e a autorização de leitura viraria uma
     * autorização de escrita ao contrário.
     */
    private BigDecimal comissao;

    /** Somente leitura: mantido pelo listener do evento de conclusão. */
    private Integer solicitacoesConcluidas;

}
