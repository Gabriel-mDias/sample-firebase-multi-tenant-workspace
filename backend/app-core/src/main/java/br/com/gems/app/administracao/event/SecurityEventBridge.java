package br.com.gems.app.administracao.event;

import br.com.gems.app.events.SegurancaAlteradaEvent;
import br.com.gems.firebase.auth.SecurityEventPublisher;
import br.com.gems.firebase.auth.SecurityEventSink;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** A outbox SDK publica o POJO somente depois do commit original. */
@Component
@RequiredArgsConstructor
public class SecurityEventBridge implements SecurityEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void emit( SecurityEventSink.Event event ) {
        publisher.publishEvent( new SegurancaAlteradaEvent( event.id(), event.type(), event.userId(), event.tenantId(), event.occurredAt() ) );
    }

}
