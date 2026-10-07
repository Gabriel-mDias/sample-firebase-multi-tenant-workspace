package br.com.gems.app.model.response;

import br.com.gems.app.model.enums.Sexo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PessoaExistenteDTO {

    private UUID id;
    private String primeiroNome;
    private String sobrenome;
    private String cpf;
    private String rg;
    private Sexo sexo;
    private LocalDate dataNascimento;
    private Boolean dataAproximada;
    private LocalDate dataObito;
    private Boolean parceiro;
    private Boolean usuario;
    private List<String> vinculos;

}
