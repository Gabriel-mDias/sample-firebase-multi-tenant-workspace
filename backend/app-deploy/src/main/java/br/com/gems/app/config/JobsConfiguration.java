package br.com.gems.app.config;

import br.com.gems.app.administracao.BootstrapOperator;
import br.com.gems.app.administracao.CentralBootstrapWorkflow;
import br.com.gems.app.tenant.SampleTenantMigration;
import br.com.gems.firebase.auth.AuthorizationResolver;
import br.com.gems.firebase.auth.FirebaseAdminGateway;
import br.com.gems.firebase.auth.FirebaseAuthProperties;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.IdTokenVerifier;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.UserAdministration;
import br.com.gems.firebase.auth.jdbc.JdbcIdentityCommandWorker;
import br.com.gems.firebase.auth.jdbc.JdbcSecurityEventOutbox;
import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import br.com.gems.firebase.auth.SecurityEventPublisher;
import br.com.gems.security.authorization.AuthorizationCatalog;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionTemplate;

/** Jobs exigem processo não HTTP; nunca registram capacidade de bootstrap no servidor web. */
@Configuration
@ConditionalOnNotWebApplication
public class JobsConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.jobs.mode", havingValue = "bootstrap")
    public BootstrapOperator bootstrapOperator( @Value("${app.jobs.operator-uid}") String uid, IdTokenVerifier verifier, FirebaseAdminGateway admin, FirebaseAuthProperties properties ) {
        return BootstrapOperator.verify( properties.projectId(), uid, System.getenv( "GEMS_BOOTSTRAP_ID_TOKEN" ), verifier, admin );
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.mode", havingValue = "bootstrap")
    public SecurityActorProvider bootstrapActor( BootstrapOperator operator ) {
        return operator.actor();
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.mode", havingValue = "bootstrap")
    public ApplicationRunner bootstrapRunner( BootstrapOperator operator, UserAdministration users, GrantAdministration grants, AuthorizationResolver resolver, AuthorizationCatalog catalog, Clock clock, @Qualifier("firebaseSecurityTransactions") TransactionTemplate tx, ConfigurableApplicationContext context ) {
        return args -> {
            try {
                new CentralBootstrapWorkflow().initialize( operator, users, grants, resolver, catalog, clock, tx );
            } finally {
                context.close();
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.mode", havingValue = "provision")
    public ApplicationRunner provisionRunner( @Value("${app.jobs.tenant-id}") UUID tenant, @Value("${gems.firebase.auth.minimum-applied-migration}") long revision, JdbcTenantProvisioner provisioner, IdTokenVerifier verifier, AuthorizationResolver resolver, Clock clock, ConfigurableApplicationContext context ) {
        return args -> {
            try {
                var resolution = resolver.resolve( verifier.verify( System.getenv( "GEMS_JOB_ID_TOKEN" ) ), Optional.empty(), clock.instant() );
                if ( !resolution.context().globalActions().contains( "ALTERAR_TENANT" ) ) {
                    throw new AccessDeniedException( "Provisionamento exige operador central" );
                }
                provisioner.provision( tenant, revision, new SampleTenantMigration() );
            } finally {
                context.close();
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.jobs.mode", havingValue = "identity-worker")
    public ApplicationRunner identityWorkerRunner( JdbcIdentityCommandWorker worker, JdbcSecurityEventOutbox events, SecurityEventPublisher publisher, ConfigurableApplicationContext context ) {
        return args -> {
            try {
                for ( int processed = 0; processed < 1000 && worker.runOnce(); processed++ ) {
                }
                for ( int delivered = 0; delivered < 1000 && events.publishNext( publisher ); delivered++ ) {
                }
            } finally {
                context.close();
            }
        };
    }

}
