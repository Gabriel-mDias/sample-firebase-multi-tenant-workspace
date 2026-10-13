package br.com.gems.app.administracao.event;

import br.com.gems.app.events.SegurancaAlteradaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Destino demonstrativo; entrega durável pertence à outbox, sem payload remoto. */
@Component
@Slf4j
public class SecurityEventListener {

    @EventListener
    public void onSecurityChange( SegurancaAlteradaEvent event ) {
        log.info( "Evento de segurança entregue: id={} tipo={} tenant={}", event.id(), event.tipo(), event.tenantId() );
    }

}
