package br.com.gems.app.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.UUID;

public record DemoRegistroDTO(UUID id, String nome, String descricao, @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorRestrito) {

}
