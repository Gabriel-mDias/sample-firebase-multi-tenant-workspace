package br.com.gems.app.tenant;

import br.com.gems.security.authorization.AuthorizationContextAware;
import br.com.gems.security.authorization.GlobalEndpoint;
import br.com.gems.security.authorization.TenantEndpoint;
import br.com.gems.tenant.JpaTenantContext;
import br.com.gems.tenant.TenantScope;
import br.com.gems.utils.ObjectUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Instala scope antes do controller/service e nunca troca dentro de EM/transação. */
public class TenantRequestScopeInterceptor implements HandlerInterceptor {

    private static final String ATTRIBUTE = TenantRequestScopeInterceptor.class.getName();

    @Override
    public boolean preHandle( HttpServletRequest request, HttpServletResponse response, Object handler ) {
        if ( TransactionSynchronizationManager.isActualTransactionActive()
                || ObjectUtil.isNotNullAndNotEmpty( TransactionSynchronizationManager.getResourceMap().keySet() ) ) {
            throw new IllegalStateException( "Scope deve preceder EntityManager" );
        }
        JpaTenantContext.clear();
        if ( !(handler instanceof HandlerMethod method) ) {
            return true;
        }
        boolean tenant = AnnotatedElementUtils.hasAnnotation( method.getBeanType(), TenantEndpoint.class )
                || AnnotatedElementUtils.hasAnnotation( method.getMethod(), TenantEndpoint.class );
        boolean global = AnnotatedElementUtils.hasAnnotation( method.getBeanType(), GlobalEndpoint.class )
                || AnnotatedElementUtils.hasAnnotation( method.getMethod(), GlobalEndpoint.class );
        if ( !tenant && !global ) {
            return true;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( !(authentication instanceof AuthorizationContextAware aware) || !authentication.isAuthenticated() ) {
            throw new AccessDeniedException( "Contexto autenticado obrigatório" );
        }
        if ( tenant ) {
            String alias = aware.authorizationContext().tenantAlias()
                    .orElseThrow( () -> new AccessDeniedException( "Contexto de empresa obrigatório" ) );
            request.setAttribute( ATTRIBUTE, TenantScope.forTenant( alias ) );
        } else {
            request.setAttribute( ATTRIBUTE, TenantScope.global() );
        }
        return true;
    }

    @Override
    public void afterCompletion( HttpServletRequest request, HttpServletResponse response, Object handler, Exception failure ) {
        try {
            if ( request.getAttribute( ATTRIBUTE ) instanceof TenantScope scope ) {
                scope.close();
                request.removeAttribute( ATTRIBUTE );
            }
        } finally {
            JpaTenantContext.clear();
        }
    }

}
