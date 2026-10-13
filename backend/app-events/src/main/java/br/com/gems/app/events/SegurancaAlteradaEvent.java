package br.com.gems.app.events;

import java.time.Instant;
import java.util.UUID;

/** Contrato sanitizado de integração; UUID permite deduplicação no destino. */
public record SegurancaAlteradaEvent(UUID id, String tipo, UUID usuarioId, UUID tenantId, Instant ocorridoEm) {

}
