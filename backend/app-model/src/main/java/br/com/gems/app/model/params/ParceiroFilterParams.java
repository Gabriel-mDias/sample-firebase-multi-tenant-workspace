package br.com.gems.app.model.params;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ParceiroFilterParams {

    private UUID id;
    private String nome;
    private String cpf;
    private UUID estadoId;

}
