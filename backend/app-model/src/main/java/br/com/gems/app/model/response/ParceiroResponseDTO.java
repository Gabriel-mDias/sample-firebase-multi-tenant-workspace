package br.com.gems.app.model.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projeção da busca paginada de parceiros — construída por construtor na HQL, nunca por reflexão.
 *
 * <p>
 * O construtor é o contrato: mudar a ordem dos parâmetros aqui sem mudar o {@code SELECT new}
 * do repository compila e falha em runtime. Os dois andam juntos, sempre.
 */
@Getter
@Setter
public class ParceiroResponseDTO {

    private UUID id;
    private String nome;
    private String cpf;
    private String email;
    private String estadoSigla;
    private BigDecimal comissao;
    private Integer solicitacoesConcluidas;

    public ParceiroResponseDTO( UUID id, String nome, String cpf, String email, String estadoSigla,
            BigDecimal comissao, Integer solicitacoesConcluidas ) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.estadoSigla = estadoSigla;
        this.comissao = comissao;
        this.solicitacoesConcluidas = solicitacoesConcluidas;
    }

}
