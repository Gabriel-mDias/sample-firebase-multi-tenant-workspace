package br.com.gems.app.security.auditoria;

import br.com.gems.auditing.AuditActor;
import br.com.gems.auditing.AuditActorProvider;
import br.com.gems.auditing.AuditContext;
import br.com.gems.auditing.AuditContextProvider;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Fornece à auditoria a identidade estável do JWT e a correlação da requisição. */
@Component
public class UsuarioAuditActorProvider implements AuditActorProvider, AuditContextProvider {

    private static final String CORRELATION_ID = "correlationId";

    private final UsuarioAutenticadoService usuarioAutenticado;

    public UsuarioAuditActorProvider( UsuarioAutenticadoService usuarioAutenticado ) {
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Override
    public AuditActor currentActor() {
        try {
            return usuarioAutenticado.loginDoUsuarioAutenticado()
                    .map( AuditActor::usuario )
                    .orElseGet( AuditActor::tecnico );
        } catch ( RuntimeException falhaDeContexto ) {
            return AuditActor.tecnico();
        }
    }

    @Override
    public AuditContext currentContext() {
        try {
            var actorId = usuarioAutenticado.idDoUsuarioAutenticado()
                    .map( Object::toString )
                    .orElse( null );
            return new AuditContext( actorId, MDC.get( CORRELATION_ID ) );
        } catch ( RuntimeException falhaDeContexto ) {
            return AuditContext.empty();
        }
    }
}
