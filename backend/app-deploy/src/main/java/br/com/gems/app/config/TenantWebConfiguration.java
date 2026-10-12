package br.com.gems.app.config;

import br.com.gems.app.tenant.TenantRequestScopeInterceptor;
import br.com.gems.tenant.JpaTenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Limpeza também cobre 401/403/503 antes de MVC e threads previamente contaminadas. */
@Configuration
public class TenantWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors( InterceptorRegistry registry ) {
        registry.addInterceptor( new TenantRequestScopeInterceptor() ).order( Ordered.HIGHEST_PRECEDENCE + 10 );
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> tenantThreadBoundary() {
        var registration = new FilterRegistrationBean<OncePerRequestFilter>();
        registration.setFilter( new OncePerRequestFilter() {

            @Override
            protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain chain ) throws ServletException, IOException {
                if ( TransactionSynchronizationManager.isActualTransactionActive()
                        || br.com.gems.utils.ObjectUtil.isNotNullAndNotEmpty( TransactionSynchronizationManager.getResourceMap().keySet() ) ) {
                    throw new ServletException( "Autenticação deve preceder EntityManager/transação" );
                }
                JpaTenantContext.clear();
                MDC.remove( "actorId" );
                try {
                    chain.doFilter( request, response );
                } finally {
                    JpaTenantContext.clear();
                    MDC.remove( "actorId" );
                }
            }

        } );
        registration.setOrder( Ordered.HIGHEST_PRECEDENCE + 20 );
        return registration;
    }

}
