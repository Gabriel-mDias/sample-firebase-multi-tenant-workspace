package br.com.gems.app.tenant;

import br.com.gems.firebase.auth.AuthorizationResolver;
import br.com.gems.firebase.auth.FirebaseAuthenticationToken;
import br.com.gems.firebase.auth.FirebasePrincipal;
import br.com.gems.security.authorization.JwtAuthorizationContext;
import br.com.gems.security.authorization.TenantEndpoint;
import br.com.gems.tenant.JpaTenantContext;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantRequestScopeInterceptorTest {

    private final TenantRequestScopeInterceptor interceptor = new TenantRequestScopeInterceptor();

    @AfterEach
    void limpar() {
        JpaTenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveInstalarAliasAutenticadoELimparThread_quandoHeaderTentaOutroTenant() throws Exception {
        autenticar( Optional.of( "alpha" ) );
        var request = new MockHttpServletRequest();
        request.addHeader( "X-Tenant-Alias", "beta" );
        var response = new MockHttpServletResponse();
        interceptor.preHandle( request, response, endpoint() );
        assertThat( JpaTenantContext.getCurrentTenant() ).isEqualTo( "alpha" );
        interceptor.afterCompletion( request, response, endpoint(), new IllegalStateException() );
        assertThat( JpaTenantContext.current() ).isEmpty();
    }

    @Test
    void devePreservarScopeENegarTroca_quandoEntityManagerJaEstaAberto() throws Exception {
        autenticar( Optional.of( "alpha" ) );
        JpaTenantContext.setCurrentTenant( "beta" );
        var resource = new Object();
        org.springframework.transaction.support.TransactionSynchronizationManager.bindResource( resource, new Object() );
        try {
            assertThatThrownBy( () -> interceptor.preHandle( new MockHttpServletRequest(), new MockHttpServletResponse(), endpoint() ) ).isInstanceOf( IllegalStateException.class );
            assertThat( JpaTenantContext.getCurrentTenant() ).isEqualTo( "beta" );
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource( resource );
        }
    }

    @Test
    void deveNegarSemAliasAutenticado_quandoEndpointETenant() throws Exception {
        autenticar( Optional.empty() );
        assertThatThrownBy( () -> interceptor.preHandle( new MockHttpServletRequest(), new MockHttpServletResponse(), endpoint() ) )
                .isInstanceOf( AccessDeniedException.class );
        assertThat( JpaTenantContext.current() ).isEmpty();
    }

    private HandlerMethod endpoint() throws Exception {
        return new HandlerMethod( new Endpoint(), Endpoint.class.getMethod( "read" ) );
    }

    private void autenticar( Optional<String> alias ) {
        var context = new JwtAuthorizationContext( Set.of(), Set.of(), Set.of(), Set.of( "CONSULTAR_DEMO" ), alias );
        var principal = new FirebasePrincipal( UUID.randomUUID(), "uid", "Nome", "name@example.test" );
        SecurityContextHolder.getContext().setAuthentication( new FirebaseAuthenticationToken( new AuthorizationResolver.Resolution( principal, context ) ) );
    }

    @TenantEndpoint
    static class Endpoint {

        public void read() {
        }

    }

}
