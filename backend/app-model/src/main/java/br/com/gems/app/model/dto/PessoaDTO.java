package br.com.gems.app.model.dto;

import br.com.gems.app.model.enums.Sexo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PessoaDTO {

    private UUID id;
    private String primeiroNome;
    private String sobrenome;
    private LocalDate dataNascimento;
    private String cpf;
    private String rg;
    private Sexo sexo;
    private LocalDate dataObito;
    private Boolean dataAproximada;

}
