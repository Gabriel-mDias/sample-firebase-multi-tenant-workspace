package br.com.gems.app.auth.service;

import br.com.gems.app.model.response.AuthMeResponse;
import br.com.gems.app.model.response.AuthContextResponse;
import br.com.gems.firebase.auth.FirebaseAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Usa os DTOs do snapshot resolvido pela SDK no início da requisição. */
@Service
public class AuthService {

    public AuthMeResponse me() {
        var view = authentication().me();
        var user = view.user();
        return new AuthMeResponse( new AuthMeResponse.User( user.userId(), user.name(), user.email() ), view.tenants().stream().map( t -> new AuthMeResponse.Tenant( t.alias(), t.name() ) ).toList(), view.globalActions() );
    }

    public AuthContextResponse context() {
        var view = authentication().tenantContext()
                .orElseThrow( () -> new AccessDeniedException( "Contexto de empresa obrigatório" ) );
        return new AuthContextResponse( new AuthContextResponse.Tenant( view.tenant().alias(), view.tenant().name() ), view.profiles().stream().map( p -> new AuthContextResponse.Container( p.code(), p.name() ) ).toList(), view.groups().stream().map( g -> new AuthContextResponse.Container( g.code(), g.name() ) ).toList(), view.actions() );
    }

    private FirebaseAuthenticationToken authentication() {
        if ( SecurityContextHolder.getContext().getAuthentication() instanceof FirebaseAuthenticationToken token
                && token.isAuthenticated() ) {
            return token;
        }
        throw new AccessDeniedException( "Snapshot autenticado obrigatório" );
    }

}
