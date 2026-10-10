package br.com.gems.app.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.util.UUID;

public record DemoRegistroResponseDTO(UUID id, String nome, String descricao,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorRestrito) {
}
