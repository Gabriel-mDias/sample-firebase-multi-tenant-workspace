package br.com.gems.app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Sexo {

    MASCULINO("Masculino"), FEMININO("Feminino"), NAO_INFORMADO("Não informado");

    private final String descricao;

}
