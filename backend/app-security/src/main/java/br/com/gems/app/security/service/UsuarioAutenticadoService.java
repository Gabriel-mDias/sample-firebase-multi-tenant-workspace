package br.com.gems.app.security.service;

import br.com.gems.app.security.AcaoSistema;
import br.com.gems.firebase.auth.FirebaseAuthenticationToken;
import br.com.gems.security.authorization.AuthorizationScope;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Consulta o snapshot SQL autenticado, nunca claims de permissões do provedor. */
@Service
public class UsuarioAutenticadoService {

    public Optional<UUID> idDoUsuarioAutenticado() {
        return authentication().map( token -> token.getPrincipal().userId() );
    }

    public Optional<String> loginDoUsuarioAutenticado() {
        return authentication().map( token -> token.getPrincipal().uid() );
    }

    public boolean possuiAcao( AcaoSistema action ) {
        return authentication().map( token -> action.scope() == AuthorizationScope.TENANT
                ? token.authorizationContext().tenantActions().contains( action.name() )
                : token.authorizationContext().globalActions().contains( action.name() ) ).orElse( false );
    }

    private Optional<FirebaseAuthenticationToken> authentication() {
        var current = SecurityContextHolder.getContext().getAuthentication();
        return current instanceof FirebaseAuthenticationToken token && token.isAuthenticated()
                ? Optional.of( token )
                : Optional.empty();
    }

}
