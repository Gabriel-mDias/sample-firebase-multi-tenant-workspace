package br.com.gems.app.model.params;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo de {@code POST /api/auditoria/search}.
 *
 * <p>
 * {@code dataFim} é inclusivo até o fim do dia: o filtro é {@code LocalDate} e
 * {@code DT_OPERACAO} é {@code TIMESTAMP}, então o repository precisa comparar contra
 * {@code dataFim.atTime(LocalTime.MAX)} — comparar direto descartaria tudo que aconteceu depois da
 * meia-noite do próprio dia pedido.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuditoriaFilterParams {

    private String entidade;
    private UUID registroAfetado;
    private String operacao;
    private UUID autorId;
    private LocalDate dataInicio;
    private LocalDate dataFim;

}
