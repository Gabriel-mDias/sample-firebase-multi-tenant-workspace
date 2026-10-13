package br.com.gems.app.config;

import br.com.gems.app.security.AcaoSistema;
import br.com.gems.auditing.AuditTrailDestination;
import br.com.gems.firebase.auth.DelegationPolicy;
import br.com.gems.firebase.auth.TenantAdministration;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.tenant.JpaTenantContext;
import br.com.gems.tenant.TenantSchemaNaming;
import br.com.gems.tenant.migration.TenantSchemaSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FoundationConfiguration {

    /** PostgreSQL timestamps must preserve the request instant used for first linking. */
    @Bean
    public java.time.Clock securityClock() {
        return java.time.Clock.tickMillis( java.time.ZoneOffset.UTC );
    }

    @Bean
    public AuthorizationCatalog authorizationCatalog() {
        return AuthorizationCatalog.of( AcaoSistema.class );
    }

    @Bean
    public DelegationPolicy delegationPolicy( AuthorizationCatalog catalog ) {
        return catalog::tenantActions;
    }

    @Bean
    public TenantSchemaSource tenantSchemaSource( TenantAdministration administration ) {
        return administration::expectedSchemas;
    }

    @Bean
    public AuditTrailDestination auditTrailDestination( TenantSchemaNaming naming ) {
        return entity -> JpaTenantContext.current().filter( snapshot -> !snapshot.global() )
                .map( snapshot -> naming.schemaFor( snapshot.alias() ) )
                .orElseThrow( () -> new IllegalStateException( "Auditoria empresarial exige tenant" ) );
    }

}
