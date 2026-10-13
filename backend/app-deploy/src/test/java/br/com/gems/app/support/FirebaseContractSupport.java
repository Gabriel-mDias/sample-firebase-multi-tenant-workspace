package br.com.gems.app.support;

import br.com.gems.app.AppApplication;
import br.com.gems.app.tenant.SampleTenantMigration;
import br.com.gems.firebase.auth.AdministrationViews;
import br.com.gems.firebase.auth.AuthorizationResolver;
import br.com.gems.firebase.auth.DelegationPolicy;
import br.com.gems.firebase.auth.FirebaseAdminGateway;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.SecurityEventSink;
import br.com.gems.firebase.auth.TenantAliasValidator;
import br.com.gems.firebase.auth.VerifiedIdentity;
import br.com.gems.firebase.auth.jdbc.JdbcGrantService;
import br.com.gems.firebase.auth.jdbc.JdbcIdentityCommandWorker;
import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import br.com.gems.firebase.auth.jdbc.JdbcTenantService;
import br.com.gems.firebase.auth.jdbc.JdbcUserService;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.tenant.JpaTenantContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

/** Reutiliza as portas do fixture original; nunca conecta a um provedor externo. */
@SpringBootTest(classes = AppApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class FirebaseContractSupport {

    protected static final String PROJECT = "sample-isolated-test";
    protected static final String ISSUER = "https://securetoken.google.com/" + PROJECT;
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" );

    @DynamicPropertySource
    static void database( DynamicPropertyRegistry registry ) {
        registry.add( "spring.datasource.url", DATABASE::getJdbcUrl );
        registry.add( "spring.datasource.username", DATABASE::getUsername );
        registry.add( "spring.datasource.password", DATABASE::getPassword );
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected AuthorizationResolver resolver;
    @Autowired
    protected AuthorizationCatalog catalog;
    @Autowired
    protected Clock clock;
    @Autowired
    protected SecurityEventSink events;
    @Autowired
    protected TenantAliasValidator aliases;
    @Autowired
    protected DelegationPolicy delegation;
    @Autowired
    protected JdbcTenantProvisioner provisioner;
    protected DeterministicFirebase firebase;
    @Autowired
    protected FirebaseAdminGateway originalAdmin;
    @Autowired
    @Qualifier("firebaseSecurityJdbcClient")
    protected JdbcClient jdbc;
    @Autowired
    @Qualifier("firebaseSecurityTransactions")
    protected TransactionTemplate tx;
    @Autowired
    @Qualifier("firebaseSecurityDataSource")
    protected DataSource source;
    protected JdbcUserService users;
    protected JdbcTenantService tenants;
    protected JdbcGrantService grants;
    protected Account operator;
    protected Account subject;
    protected Account tenantAdmin;
    protected UUID alphaId;
    protected UUID betaId;
    protected UUID operatorMember;
    protected UUID subjectMember;
    protected String alpha;
    protected String beta;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void prepareContract() throws ClassNotFoundException {
        // O fixture original é package-private; o acesso fica restrito ao suporte de testes.
        var fixture = Class.forName( "br.com.gems.app.FirebaseBackendIntegrationTest" );
        firebase = new DeterministicFirebase( originalAdmin, (Map<String, VerifiedIdentity>) ReflectionTestUtils.getField( fixture, "IDENTITIES" ), (AtomicBoolean) ReflectionTestUtils.getField( fixture, "REMOTE_FAILURE" ) );
        firebase.reset();
        JpaTenantContext.clear();
        SecurityContextHolder.clearContext();
        postponeCommands();
        var actor = SecurityActorProvider.trustedBootstrap( UUID.randomUUID() );
        users = new JdbcUserService( jdbc, tx, events, clock, PROJECT, catalog, actor );
        tenants = new JdbcTenantService( jdbc, tx, events, clock, aliases, catalog, actor );
        grants = new JdbcGrantService( jdbc, tx, events, catalog, delegation, clock, actor );
        operator = bind( "central" );
        subject = bind( "destinatario" );
        tenantAdmin = bind( "local" );
        alpha = unique( "alpha" );
        beta = unique( "beta" );
        alphaId = tenants.create( alpha, "Alpha" );
        betaId = tenants.create( beta, "Beta" );
        provisioner.provision( alphaId, 1, new SampleTenantMigration() );
        provisioner.provision( betaId, 1, new SampleTenantMigration() );
        operatorMember = tenants.addMember( alphaId, operator.id(), "CONSULTAR_PROPRIO_CONTEXTO_TENANT" );
        subjectMember = tenants.addMember( alphaId, subject.id(), "CONSULTAR_PROPRIO_CONTEXTO_TENANT" );
        var localMember = tenants.addMember( alphaId, tenantAdmin.id(), "CONSULTAR_PROPRIO_CONTEXTO_TENANT" );
        var betaMember = tenants.addMember( betaId, operator.id(), "CONSULTAR_PROPRIO_CONTEXTO_TENANT" );
        for ( String action : catalog.tenantActions() ) {
            if ( !action.equals( "CONSULTAR_PROPRIO_CONTEXTO_TENANT" ) ) {
                grants.grant( GrantAdministration.Target.MEMBER, alphaId, operatorMember, action );
                grants.grant( GrantAdministration.Target.MEMBER, alphaId, localMember, action );
                grants.grant( GrantAdministration.Target.MEMBER, betaId, betaMember, action );
            }
        }
        for ( String action : catalog.globalActions() ) {
            if ( !action.equals( "CONSULTAR_PROPRIA_CONTA" ) ) {
                grants.grantGlobal( operator.id(), action );
            }
        }
    }

    @AfterEach
    void clearContract() {
        firebase.reset();
        JpaTenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    protected Account bind( String prefix ) {
        String uid = unique( prefix );
        String email = uid + "@example.test";
        UUID id = users.preProvisionGoogle( uid, email, "CONSULTAR_PROPRIA_CONTA" );
        var identity = identity( uid, email );
        firebase.identities.put( uid, identity );
        firebase.remote.put( uid, new FirebaseAdminGateway.User( uid, email, uid, false, true ) );
        assertThat( resolver.resolve( identity, Optional.empty(), clock.instant() ).principal().userId() )
                .isEqualTo( id );
        return new Account( id, uid, email );
    }

    protected VerifiedIdentity identity( String uid, String email ) {
        return new VerifiedIdentity( PROJECT, ISSUER, uid, email, true, "google.com", uid, clock.instant() );
    }

    protected String unique( String prefix ) {
        return prefix + UUID.randomUUID().toString().replace( "-", "" ).substring( 0, 12 );
    }

    protected JdbcIdentityCommandWorker worker( FirebaseAdminGateway gateway ) {
        return new JdbcIdentityCommandWorker( jdbc, tx, events, source, gateway, clock, Duration.ofMinutes( 2 ) );
    }

    protected void postponeCommands() {
        jdbc.sql( "UPDATE security.comando_identidade SET dt_proxima_tentativa=now()+interval '1 day' " + "WHERE cd_estado IN ('PENDING','RETRY')" ).update();
    }

    protected void revokeAction( String action, boolean tenant ) {
        String table = tenant ? "usuario_tenant_role" : "usuario_role_global";
        String owner = tenant ? "id_usuario_tenant" : "id_usuario";
        UUID id = jdbc.sql( "SELECT g.id_" + table + " FROM security." + table + " g " + "JOIN security.role_seguranca r ON r.id_role=g.id_role WHERE g." + owner + "=:owner AND r.cd_acao=:action AND g.dt_fim IS NULL" )
                .param( "owner", tenant ? operatorMember : operator.id() ).param( "action", action )
                .query( UUID.class ).single();
        if ( tenant ) {
            grants.revoke( GrantAdministration.Target.MEMBER, alphaId, id );
        } else {
            grants.revokeGlobal( operator.id(), id );
        }
    }

    protected Map<String, String> securityState() {
        var state = new TreeMap<String, String>();
        for ( String table : jdbc.sql( "SELECT tablename FROM pg_tables WHERE schemaname='security'" )
                .query( String.class ).list() ) {
            assertThat( table ).matches( "[a-z_]+" );
            state.put( table, jdbc.sql( "SELECT coalesce(jsonb_agg(to_jsonb(row) " + "ORDER BY to_jsonb(row)::text),'[]'::jsonb)::text FROM security." + table + " row" )
                    .query( String.class ).single() );
        }
        return state;
    }

    protected List<Map<String, Object>> securityEvents() {
        return jdbc.sql( "SELECT * FROM security.evento_seguranca" ).query().listOfRows();
    }

    /** A paginação usa os nomes do DTO carregado, sem inventar uma assinatura da SDK. */
    protected String queryBody() {
        assertThat( AdministrationViews.Query.class.isRecord() ).isTrue();
        var values = new TreeMap<String, Object>();
        for ( var component : AdministrationViews.Query.class.getRecordComponents() ) {
            var type = component.getType();
            Object value = null;
            if ( type == String.class ) {
                value = "";
            } else if ( type == int.class || type == long.class || Number.class.isAssignableFrom( type ) ) {
                value = component.getName().toLowerCase( java.util.Locale.ROOT ).matches( ".*(size|limit).*" ) ? 20 : 0;
            } else if ( type == boolean.class || type == Boolean.class ) {
                value = false;
            }
            values.put( component.getName(), value );
        }
        return com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().toJson( values );
    }

    protected long identities( UUID user ) {
        return jdbc.sql( "SELECT count(*) FROM security.usuario_identidade WHERE id_usuario=:id" )
                .param( "id", user ).query( Long.class ).single();
    }

    protected record Account(UUID id, String uid, String email) {

    }

    /** Observa o estado remoto e sempre encaminha comandos ao gateway fake já existente. */
    public static class DeterministicFirebase implements FirebaseAdminGateway {

        private final FirebaseAdminGateway delegate;
        public final Map<String, VerifiedIdentity> identities;
        public final Map<String, User> remote = new ConcurrentHashMap<>();
        public final AtomicBoolean failure;

        DeterministicFirebase( FirebaseAdminGateway delegate, Map<String, VerifiedIdentity> identities, AtomicBoolean failure ) {
            this.delegate = delegate;
            this.identities = identities;
            this.failure = failure;
        }

        public void reset() {
            identities.clear();
            remote.clear();
            failure.set( false );
        }

        public String administrativeToken( String uid ) {
            String payload = "{\"sub\":\"" + uid + "\",\"aud\":\"" + PROJECT + "\",\"iss\":\"" + ISSUER + "\",\"admin\":true,\"roles\":[\"ADMIN\",\"CONSULTAR_TENANT\"]," + "\"realm_access\":{\"roles\":[\"ADMIN\",\"CONSULTAR_TENANT\"]}," + "\"firebase\":{\"sign_in_provider\":\"google.com\"}}";
            String token = encode( "{\"alg\":\"RS256\",\"typ\":\"JWT\"}" ) + "." + encode( payload ) + ".assinatura-do-verificador-fake";
            identities.put( token, identities.get( uid ) );
            return token;
        }

        private String encode( String value ) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString( value.getBytes( StandardCharsets.UTF_8 ) );
        }

        @Override
        public User getUser( String uid ) {
            return Optional.ofNullable( remote.get( uid ) )
                    .orElseGet( () -> delegate.getUser( uid ) );
        }

        @Override
        public User createUser( String uid, String email, String displayName ) {
            var created = delegate.createUser( uid, email, displayName );
            remote.putIfAbsent( uid, created );
            return remote.get( uid );
        }

        @Override
        public void setDisabled( String uid, boolean disabled ) {
            delegate.setDisabled( uid, disabled );
            var user = getUser( uid );
            remote.put( uid, new User( uid, user.email(), uid, disabled, true ) );
        }

        @Override
        public void revokeRefreshTokens( String uid ) {
            delegate.revokeRefreshTokens( uid );
        }

        @Override
        public String passwordResetLink( String email ) {
            throw new UnsupportedOperationException( "O contrato não envia links." );
        }

    }

    protected static class ForwardingGateway implements FirebaseAdminGateway {

        protected final FirebaseAdminGateway delegate;

        protected ForwardingGateway( FirebaseAdminGateway delegate ) {
            this.delegate = delegate;
        }

        @Override
        public User getUser( String uid ) {
            return delegate.getUser( uid );
        }

        @Override
        public User createUser( String uid, String email, String name ) {
            return delegate.createUser( uid, email, name );
        }

        @Override
        public void setDisabled( String uid, boolean disabled ) {
            delegate.setDisabled( uid, disabled );
        }

        @Override
        public void revokeRefreshTokens( String uid ) {
            delegate.revokeRefreshTokens( uid );
        }

        @Override
        public String passwordResetLink( String email ) {
            throw new UnsupportedOperationException( "O contrato não envia links." );
        }

    }

}
