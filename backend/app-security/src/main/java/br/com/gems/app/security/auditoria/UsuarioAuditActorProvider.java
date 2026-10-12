package br.com.gems.app.security.auditoria;

import br.com.gems.auditing.AuditActor;
import br.com.gems.auditing.AuditActorProvider;
import br.com.gems.auditing.AuditContext;
import br.com.gems.auditing.AuditContextProvider;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Ator estável comprovado pelo resolver SQL da SDK. */
@Component
public class UsuarioAuditActorProvider implements AuditActorProvider, AuditContextProvider {
    private final UsuarioAutenticadoService usuario;
    public UsuarioAuditActorProvider( UsuarioAutenticadoService usuario ) {
        this.usuario = usuario;
    }
    @Override
    public AuditActor currentActor() {
        return usuario.loginDoUsuarioAutenticado().map( AuditActor::usuario ).orElseGet( AuditActor::tecnico );
    }
    @Override
    public AuditContext currentContext() {
        return new AuditContext( usuario.idDoUsuarioAutenticado().map( Object::toString ).orElse( null ),
                MDC.get( "correlationId" ) );
    }
}
