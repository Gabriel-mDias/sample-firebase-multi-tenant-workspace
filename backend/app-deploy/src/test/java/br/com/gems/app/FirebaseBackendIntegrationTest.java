package br.com.gems.app;

import br.com.gems.app.config.JobsConfiguration;
import br.com.gems.app.demo.repository.DemoRegistroRepository;
import br.com.gems.app.events.SegurancaAlteradaEvent;
import br.com.gems.app.model.entity.demo.DemoRegistro;
import br.com.gems.app.tenant.SampleTenantMigration;
import br.com.gems.firebase.auth.AuthorizationResolver;
import br.com.gems.firebase.auth.FirebaseAdminGateway;
import br.com.gems.firebase.auth.FirebaseAuthException;
import br.com.gems.firebase.auth.FirebaseAuthenticationToken;
import br.com.gems.firebase.auth.IdTokenVerifier;
import br.com.gems.firebase.auth.SecurityActorProvider;
import br.com.gems.firebase.auth.SecurityEventSink;
import br.com.gems.firebase.auth.SecurityEventPublisher;
import br.com.gems.firebase.auth.TenantAliasValidator;
import br.com.gems.firebase.auth.VerifiedIdentity;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.jdbc.JdbcGrantService;
import br.com.gems.firebase.auth.jdbc.JdbcTenantService;
import br.com.gems.firebase.auth.jdbc.JdbcUserService;
import br.com.gems.firebase.auth.jdbc.JdbcTenantProvisioner;
import br.com.gems.firebase.auth.jdbc.JdbcIdentityCommandWorker;
import br.com.gems.firebase.auth.jdbc.JdbcSecurityEventOutbox;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.firebase.auth.DelegationPolicy;
import br.com.gems.tenant.JpaTenantContext;
import br.com.gems.tenant.TenantScope;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PostgreSQL real; verifier/Admin são portas determinísticas, sem credenciais de nuvem. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(FirebaseBackendIntegrationTest.Ports.class)
@RecordApplicationEvents
class FirebaseBackendIntegrationTest {

    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" );

    @DynamicPropertySource
    static void database( DynamicPropertyRegistry registry ) {
        registry.add( "spring.datasource.url", DATABASE::getJdbcUrl );
        registry.add( "spring.datasource.username", DATABASE::getUsername );
        registry.add( "spring.datasource.password", DATABASE::getPassword );
    }

    static final Map<String, VerifiedIdentity> IDENTITIES = new ConcurrentHashMap<>();
    static final AtomicBoolean REMOTE_FAILURE = new AtomicBoolean();
    static final String RAW_PROVIDER_SECRET = "raw-provider-secret-sentinel";
    @Autowired
    MockMvc mvc;
    @Autowired
    AuthorizationResolver resolver;
    @Autowired
    AuthorizationCatalog catalog;
    @Autowired
    Clock clock;
    @Autowired
    SecurityEventSink events;
    @Autowired
    SecurityEventPublisher publisher;
    @Autowired
    ApplicationEvents applicationEvents;
    @Autowired
    TenantAliasValidator aliases;
    @Autowired
    DelegationPolicy delegation;
    @Autowired
    JdbcTenantProvisioner provisioner;
    @Autowired
    JdbcIdentityCommandWorker worker;
    @Autowired
    DemoRegistroRepository repository;
    @Autowired
    @Qualifier("firebaseSecurityJdbcClient")
    JdbcClient jdbc;
    @Autowired
    @Qualifier("firebaseSecurityTransactions")
    TransactionTemplate tx;
    @Autowired
    @Qualifier("firebaseSecurityDataSource")
    DataSource source;
    private JdbcUserService users;
    private JdbcTenantService tenants;
    private JdbcGrantService grants;
    private String alpha, beta, aliceUid, bobUid;
    private UUID alphaId, betaId, aliceId, bobId, aliceMember;
    private final UUID sameId = UUID.fromString( "00000000-0000-0000-0000-000000000123" );

    @BeforeEach
    void prepare() throws Exception {
        REMOTE_FAILURE.set( false );
        JpaTenantContext.clear();
        SecurityContextHolder.clearContext();
        jdbc.sql( "UPDATE security.comando_identidade SET dt_proxima_tentativa=now()+interval '1 day' WHERE cd_estado IN ('PENDING','RETRY')" )
                .update();
        var actor = SecurityActorProvider.trustedBootstrap( UUID.randomUUID() );
        users = new JdbcUserService( jdbc, tx, events, clock, "sample-isolated-test", catalog, actor );
        tenants = new JdbcTenantService( jdbc, tx, events, clock, aliases, catalog, actor );
        grants = new JdbcGrantService( jdbc, tx, events, catalog, delegation, clock, actor );
        var suffix = UUID.randomUUID().toString().replace( "-", "" ).substring( 0, 8 );
        alpha = "alpha" + suffix;
        beta = "beta" + suffix;
        aliceUid = "alice" + suffix;
        bobUid = "bob" + suffix;
        aliceId = bind( aliceUid );
        bobId = bind( bobUid );
        alphaId = tenants.create( alpha, "Alpha" );
        betaId = tenants.create( beta, "Beta" );
        provisioner.provision( alphaId, 1, new SampleTenantMigration() );
        provisioner.provision( betaId, 1, new SampleTenantMigration() );
        aliceMember = tenants.addMember( alphaId, aliceId );
        var bobAlpha = tenants.addMember( alphaId, bobId );
        var bobBeta = tenants.addMember( betaId, bobId );
        for ( String action : catalog.tenantActions() ) {
            grants.grant( GrantAdministration.Target.MEMBER, alphaId, bobAlpha, action );
            grants.grant( GrantAdministration.Target.MEMBER, betaId, bobBeta, action );
            if ( !action.equals( "VISUALIZAR_VALOR_RESTRITO" ) ) {
                grants.grant( GrantAdministration.Target.MEMBER, alphaId, aliceMember, action );
            }
        }
        for ( String action : catalog.globalActions() ) {
            grants.grantGlobal( bobId, action );
        }
        jdbc.sql( "INSERT INTO tenant_" + alpha + ".demo_registro VALUES(:id,'Alpha','Alpha somente',123.45,now(),NULL)" )
                .param( "id", sameId ).update();
        jdbc.sql( "INSERT INTO tenant_" + beta + ".demo_registro VALUES(:id,'Beta','Beta somente',999.99,now(),NULL)" )
                .param( "id", sameId ).update();
    }

    @AfterEach
    void clear() {
        JpaTenantContext.clear();
        SecurityContextHolder.clearContext();
        REMOTE_FAILURE.set( false );
    }

    @Test
    void deveExigirIdentidadeEAcoesConcretas_quandoEndpointsSaoProtegidos() throws Exception {
        mvc.perform( get( "/api/demo/public" ) ).andExpect( status().isOk() );
        mvc.perform( get( "/api/auth/me" ) ).andExpect( status().isUnauthorized() );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer invalid" ) )
                .andExpect( status().isUnauthorized() );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer unavailable" ) )
                .andExpect( status().isServiceUnavailable() );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.user.id" ).value( aliceId.toString() ) )
                .andExpect( jsonPath( "$.user.uid" ).doesNotExist() )
                .andExpect( jsonPath( "$.tenants[0].alias" ).value( alpha ) );
        mvc.perform( get( "/api/demo/global" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isForbidden() );
        mvc.perform( get( "/api/demo/global" ).header( "Authorization", "Bearer " + bobUid ) )
                .andExpect( status().isOk() );
        mvc.perform( get( "/actuator/metrics" ) ).andExpect( status().isUnauthorized() );
        mvc.perform( get( "/api/aws/s3/presigned-url" ).header( "Authorization", "Bearer " + bobUid ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void deveIsolarMesmaPKESpoof_quandoUsuarioSelecionaEmpresa() throws Exception {
        mvc.perform( get( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.nome" ).value( "Alpha" ) );
        mvc.perform( get( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", beta ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.nome" ).value( "Beta" ) );
        mvc.perform( get( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", beta ) ).andExpect( status().isForbidden() );
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isForbidden() );
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.tenant.name" ).value( "Alpha" ) );
    }

    @Test
    void deveOcultarCampoNaLeituraBuscaEGravacao_quandoAcaoRestritaFalta() throws Exception {
        mvc.perform( get( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.valorRestrito" ).isEmpty() );
        mvc.perform( post( "/api/demo-registros/search" ).contentType( "application/json" ).content( "{}" )
                .header( "Authorization", "Bearer " + aliceUid ).header( "X-Tenant-Alias", alpha ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.content[0].valorRestrito" ).isEmpty() );
        mvc.perform( put( "/api/demo-registros/" + sameId ).contentType( "application/json" )
                .content( "{\"nome\":\"Alterado\",\"descricao\":\"Editado\",\"valorRestrito\":0,\"tenant\":\"" + beta + "\"}" )
                .header( "Authorization", "Bearer " + aliceUid ).header( "X-Tenant-Alias", alpha ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.valorRestrito" ).isEmpty() );
        assertThat( jdbc.sql( "SELECT vl_restrito FROM tenant_" + alpha + ".demo_registro WHERE id_demo_registro=:id" )
                .param( "id", sameId ).query( BigDecimal.class ).single() ).isEqualByComparingTo( "123.45" );
        assertThat( jdbc.sql( "SELECT nm_registro FROM tenant_" + beta + ".demo_registro WHERE id_demo_registro=:id" )
                .param( "id", sameId ).query( String.class ).single() ).isEqualTo( "Beta" );
    }

    @Test
    void deveValidarESoftDeleteAuditarPorTenant_quandoExecutaCRUD() throws Exception {
        mvc.perform( post( "/api/demo-registros" ).contentType( "application/json" )
                .content( "{\"nome\":\"\",\"descricao\":\"" + "x".repeat( 1001 ) + "\"}" )
                .header( "Authorization", "Bearer " + bobUid ).header( "X-Tenant-Alias", alpha ) )
                .andExpect( status().isBadRequest() );
        mvc.perform( delete( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isNoContent() );
        assertThat( jdbc
                .sql( "SELECT dt_exclusao IS NOT NULL FROM tenant_" + alpha + ".demo_registro WHERE id_demo_registro=:id" )
                .param( "id", sameId ).query( Boolean.class ).single() ).isTrue();
        assertThat( jdbc.sql( "SELECT count(*) FROM tenant_" + alpha + ".audit_operation" ).query( Long.class ).single() )
                .isPositive();
        assertThat( jdbc.sql( "SELECT count(*) FROM tenant_" + beta + ".audit_operation" ).query( Long.class ).single() )
                .isZero();
    }

    @Test
    void deveRecusarPendenteERevisaoAtrasada_quandoSchemaNaoEstaPronto() throws Exception {
        var pending = tenants.create( "pending" + UUID.randomUUID().toString().substring( 0, 8 ), "Pendente" );
        tenants.addMember( pending, aliceId );
        String alias = jdbc.sql( "SELECT cd_alias FROM security.tenant WHERE id_tenant=:id" ).param( "id", pending )
                .query( String.class ).single();
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alias ) ).andExpect( status().isForbidden() );
        jdbc.sql( "UPDATE security.tenant SET nr_revisao_aplicada=0 WHERE id_tenant=:id" ).param( "id", alphaId )
                .update();
        mvc.perform( get( "/api/demo/tenant/read" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isForbidden() );
        jdbc.sql( "UPDATE security.tenant SET nr_revisao_aplicada=1 WHERE id_tenant=:id" ).param( "id", alphaId )
                .update();
        tenants.suspend( alphaId );
        assertThat( tenants.expectedSchemas() ).contains( "tenant_" + alpha );
    }

    @Test
    void deveRevogarNoProximoRequestSemDesabilitarConta_quandoMembershipEncerra() throws Exception {
        tenants.endMembership( alphaId, aliceMember );
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isForbidden() );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isOk() );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.comando_identidade WHERE id_usuario=:user" )
                .param( "user", aliceId ).query( Long.class ).single() ).isZero();
    }

    @Test
    void deveBloquearSQLMesmoComProviderFalho_quandoSuspendeGlobalmente() throws Exception {
        users.suspend( aliceId );
        REMOTE_FAILURE.set( true );
        assertThat( worker.runOnce() ).isTrue();
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isForbidden() );
        assertThat( jdbc
                .sql( "SELECT count(*) FROM security.comando_identidade WHERE id_usuario=:user AND cd_estado='RETRY'" )
                .param( "user", aliceId ).query( Long.class ).single() ).isPositive();
    }

    @Test
    void deveRecusarEscaladaEOutroTenant_quandoAdministradorLocalConcedeGrant() throws Exception {
        String body = "{\"ownerId\":\"" + aliceMember + "\",\"action\":\"VISUALIZAR_VALOR_RESTRITO\"}";
        mvc.perform( post( "/api/admin/tenants/" + alphaId + "/grants" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" ).content( body ) )
                .andExpect( status().isForbidden() );
        mvc.perform( post( "/api/admin/tenants/" + betaId + "/memberships" )
                .header( "Authorization", "Bearer " + aliceUid ).header( "X-Tenant-Alias", alpha )
                .contentType( "application/json" ).content( "{\"userId\":\"" + aliceId + "\"}" ) )
                .andExpect( status().isForbidden() );
        mvc.perform( post( "/api/admin/central/tenants" ).header( "Authorization", "Bearer " + aliceUid )
                .contentType( "application/json" ).content( "{\"alias\":\"evil\",\"name\":\"Evil\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void deveLimparThreadMesmoAntesDeMVC_quandoAutenticacaoFalha() throws Exception {
        JpaTenantContext.setCurrentTenant( beta );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer invalid" ) )
                .andExpect( status().isUnauthorized() );
        assertThat( JpaTenantContext.current() ).isEmpty();
        assertThat( SecurityContextHolder.getContext().getAuthentication() ).isNull();
        mvc.perform( get( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.nome" ).value( "Alpha" ) );
        assertThat( JpaTenantContext.current() ).isEmpty();
    }

    @Test
    void deveRollbackNegocioEAuditoria_quandoTransacaoFalha() {
        assertThatThrownBy( () -> TenantScope.runInTenant( alpha, () -> {
            var business = new TransactionTemplate( new org.springframework.orm.jpa.JpaTransactionManager( repositoryEntityManagerFactory() ) );
            business.executeWithoutResult( status -> {
                repository.saveAndFlush( DemoRegistro.builder().nome( "Rollback" ).descricao( "Rollback" ).build() );
                throw new IllegalStateException( "rollback expected" );
            } );
        } ) ).isInstanceOf( IllegalStateException.class );
        assertThat( jdbc.sql( "SELECT count(*) FROM tenant_" + alpha + ".demo_registro WHERE nm_registro='Rollback'" )
                .query( Long.class ).single() ).isZero();
        assertThat( jdbc.sql( "SELECT count(*) FROM tenant_" + alpha + ".audit_operation" ).query( Long.class ).single() )
                .isZero();
    }

    @Autowired
    jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired
    FirebaseAdminGateway admin;
    @Autowired
    org.springframework.context.ApplicationContext applicationContext;

    @Test
    void deveConservarCommitSeguranca_quandoTransacaoEmpresarialRollback() {
        String email = "isolated" + UUID.randomUUID() + "@example.test";
        assertThatThrownBy( () -> TenantScope.runInTenant( alpha, () -> {
            var business = new TransactionTemplate( new org.springframework.orm.jpa.JpaTransactionManager( entityManagerFactory ) );
            business.executeWithoutResult( status -> {
                users.preProvisionGoogle( "Isolado", email, "CONSULTAR_PROPRIA_CONTA" );
                repository.saveAndFlush( DemoRegistro.builder().nome( "Empresarial cancelado" ).build() );
                throw new IllegalStateException( "rollback empresarial" );
            } );
        } ) ).isInstanceOf( IllegalStateException.class );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario WHERE cd_email=:email" ).param( "email", email )
                .query( Long.class ).single() ).isOne();
        assertThat( jdbc
                .sql( "SELECT count(*) FROM tenant_" + alpha + ".demo_registro WHERE nm_registro='Empresarial cancelado'" )
                .query( Long.class ).single() ).isZero();
    }

    @Test
    void deveExecutarWorkerSemServidorHTTP_quandoJobExplicito() {
        var properties = new java.util.HashMap<String, Object>();
        properties.put( "spring.datasource.url", DATABASE.getJdbcUrl() );
        properties.put( "spring.datasource.username", DATABASE.getUsername() );
        properties.put( "spring.datasource.password", DATABASE.getPassword() );
        properties.put( "app.jobs.mode", "identity-worker" );
        try (var context = new org.springframework.boot.builder.SpringApplicationBuilder( AppApplication.class, Ports.class )
                .web( org.springframework.boot.WebApplicationType.NONE )
                .initializers( application -> application.getEnvironment().getPropertySources().addFirst( new org.springframework.core.env.MapPropertySource( "isolated-job-test", properties ) ) )
                .run()) {
            assertThat( context )
                    .isNotInstanceOf( org.springframework.web.context.WebApplicationContext.class );
            assertThat( context.isActive() ).isFalse();
        }
    }

    @Test
    void deveBootstrapIdempotenteAuditadoSemCapacidadeHTTP_quandoOperadorFoiVerificado() {
        String uid = "operator" + UUID.randomUUID();
        var identity = new VerifiedIdentity( "sample-isolated-test", "https://securetoken.google.com/sample-isolated-test", uid, uid + "@example.test", true, "google.com", "Operador", clock.instant() );
        var operator = br.com.gems.app.administracao.BootstrapOperator.verify( "sample-isolated-test", uid, "ephemeral-proof", ignored -> identity, admin );
        var operatorUsers = new JdbcUserService( jdbc, tx, events, clock, "sample-isolated-test", catalog, operator.actor() );
        var operatorGrants = new JdbcGrantService( jdbc, tx, events, catalog, delegation, clock, operator.actor() );
        var workflow = new br.com.gems.app.administracao.CentralBootstrapWorkflow();
        var id = workflow.initialize( operator, operatorUsers, operatorGrants, resolver, catalog, clock, tx );
        assertThat( workflow.initialize( operator, operatorUsers, operatorGrants, resolver, catalog, clock, tx ) )
                .isEqualTo( id );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_role_global WHERE id_usuario=:id AND dt_fim IS NULL" )
                .param( "id", id ).query( Long.class ).single() )
                .isEqualTo( catalog.globalActions().size() );
        assertThat( jdbc
                .sql( "SELECT count(*) FROM security.evento_seguranca WHERE id_usuario=:id AND id_ator IS NOT NULL" )
                .param( "id", id ).query( Long.class ).single() ).isPositive();
        assertThat( applicationContext.getBeansOfType( br.com.gems.app.administracao.BootstrapOperator.class ) )
                .isEmpty();
    }

    @Test
    void deveRecusarEscaladaPorPerfilGrupoEEncerrarHeranca_quandoVigenciaMuda() throws Exception {
        UUID profile = grants.createProfile( alphaId, "restrito", "Restrito" );
        grants.grant( GrantAdministration.Target.PROFILE, alphaId, profile, "VISUALIZAR_VALOR_RESTRITO" );
        UUID group = grants.createGroup( alphaId, "restrito", "Restrito" );
        grants.link( GrantAdministration.Link.GROUP_PROFILE, alphaId, group, profile );
        var profileBody = "{\"leftId\":\"" + aliceMember + "\",\"rightId\":\"" + profile + "\"}";
        mvc.perform( post( "/api/admin/tenants/" + alphaId + "/grants/links/MEMBER_PROFILE" )
                .header( "Authorization", "Bearer " + aliceUid ).header( "X-Tenant-Alias", alpha )
                .contentType( "application/json" ).content( profileBody ) ).andExpect( status().isForbidden() );
        var groupBody = "{\"leftId\":\"" + group + "\",\"rightId\":\"" + aliceMember + "\"}";
        mvc.perform( post( "/api/admin/tenants/" + alphaId + "/grants/links/GROUP_MEMBER" )
                .header( "Authorization", "Bearer " + aliceUid ).header( "X-Tenant-Alias", alpha )
                .contentType( "application/json" ).content( groupBody ) ).andExpect( status().isForbidden() );
        var link = grants.link( GrantAdministration.Link.GROUP_MEMBER, alphaId, group, aliceMember );
        mvc.perform( get( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.valorRestrito" ).value( 123.45 ) );
        grants.revoke( GrantAdministration.Link.GROUP_MEMBER, alphaId, link );
        mvc.perform( get( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.valorRestrito" ).isEmpty() );
        assertThatThrownBy( () -> jdbc.sql( "DELETE FROM security.grupo_usuario WHERE id_grupo_usuario=:id" )
                .param( "id", link ).update() ).isInstanceOf( org.springframework.dao.DataAccessException.class )
                .satisfies( failure -> assertThat( ((java.sql.SQLException) ((org.springframework.dao.DataAccessException) failure)
                .getMostSpecificCause()).getSQLState() )
                .isEqualTo( "23514" ) );
        UUID foreignProfile = grants.createProfile( betaId, "outro", "Outro" );
        assertThatThrownBy( () -> jdbc.sql( "INSERT INTO security.grupo_perfil VALUES(:id,:tenant,:group,:profile,now(),NULL)" )
                .param( "id", UUID.randomUUID() ).param( "tenant", alphaId ).param( "group", group )
                .param( "profile", foreignProfile ).update() )
                .isInstanceOf( org.springframework.dao.DataAccessException.class )
                .satisfies( failure -> assertThat( ((java.sql.SQLException) ((org.springframework.dao.DataAccessException) failure)
                .getMostSpecificCause()).getSQLState() )
                .isEqualTo( "23503" ) );
    }

    @Test
    void deveMigrarComLockPinadoERecuperarFalha_quandoRevisaoIncompleta() throws Exception {
        UUID id = tenants.create( "retry" + UUID.randomUUID().toString().substring( 0, 8 ), "Retry" );
        assertThatThrownBy( () -> provisioner.provision( id, 2, new SampleTenantMigration() ) )
                .isInstanceOf( IllegalStateException.class ).hasMessage( "TENANT_PROVISIONING_FAILED" );
        assertThat( jdbc.sql( "SELECT cd_provisionamento FROM security.tenant WHERE id_tenant=:id" ).param( "id", id )
                .query( String.class ).single() ).isEqualTo( "FAILED" );
        var seenSession = new java.util.concurrent.atomic.AtomicInteger();
        var migration = new SampleTenantMigration();
        provisioner.provision( id, 1, new JdbcTenantProvisioner.Migration() {

            public void apply( java.sql.Connection session, String schema ) throws Exception {
                try (var statement = session.createStatement(); var rows = statement.executeQuery( "SELECT pg_backend_pid(), EXISTS(SELECT 1 FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='advisory' AND granted)" )) {
                    assertThat( rows.next() ).isTrue();
                    seenSession.set( rows.getInt( 1 ) );
                    assertThat( rows.getBoolean( 2 ) ).isTrue();
                }
                migration.apply( session, schema );
            }

            public long appliedRevision( java.sql.Connection session, String schema ) throws Exception {
                assertThat( session.isClosed() ).isFalse();
                try (var statement = session.createStatement(); var rows = statement.executeQuery( "SELECT pg_backend_pid()" )) {
                    assertThat( rows.next() ).isTrue();
                    assertThat( rows.getInt( 1 ) ).isEqualTo( seenSession.get() );
                }
                return migration.appliedRevision( session, schema );
            }

        } );
        assertThat( jdbc.sql( "SELECT cd_provisionamento FROM security.tenant WHERE id_tenant=:id" ).param( "id", id )
                .query( String.class ).single() ).isEqualTo( "READY" );
        assertThat( jdbc.sql( "SELECT count(*) FROM pg_locks WHERE pid=:pid AND locktype='advisory'" )
                .param( "pid", seenSession.get() ).query( Long.class ).single() ).isZero();
    }

    @Test
    void deveSerializarProvisionamento_quandoDuasSessoesDisputamAlias() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch( 1 );
        var release = new java.util.concurrent.CountDownLatch( 1 );
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var delegate = new SampleTenantMigration();
        var migration = new JdbcTenantProvisioner.Migration() {

            public void apply( java.sql.Connection session, String schema ) throws Exception {
                calls.incrementAndGet();
                entered.countDown();
                if ( !release.await( 5, java.util.concurrent.TimeUnit.SECONDS ) ) {
                    throw new IllegalStateException( "Teste de lock excedeu limite" );
                }
                delegate.apply( session, schema );
            }

            public long appliedRevision( java.sql.Connection session, String schema ) throws Exception {
                return delegate.appliedRevision( session, schema );
            }

        };
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var first = pool.submit( () -> provisioner.provision( alphaId, 1, migration ) );
            assertThat( entered.await( 5, java.util.concurrent.TimeUnit.SECONDS ) ).isTrue();
            var second = pool.submit( () -> provisioner.provision( alphaId, 1, migration ) );
            try {
                assertThatThrownBy( () -> second.get( 200, java.util.concurrent.TimeUnit.MILLISECONDS ) )
                        .isInstanceOf( java.util.concurrent.TimeoutException.class );
                assertThat( calls.get() ).isOne();
            } finally {
                release.countDown();
            }
            first.get( 10, java.util.concurrent.TimeUnit.SECONDS );
            second.get( 10, java.util.concurrent.TimeUnit.SECONDS );
            assertThat( calls.get() ).isEqualTo( 2 );
        }
    }

    @Test
    void deveCriarSenhaComWorkerReiniciado_quandoCadastroCentralTemGrantAtomico() throws Exception {
        jdbc.sql( "UPDATE security.comando_identidade SET dt_proxima_tentativa=now()+interval '1 day' WHERE cd_estado IN ('PENDING','RETRY')" )
                .update();
        UUID id = users.provisionPassword( "Senha", "password" + UUID.randomUUID() + "@example.test", "CONSULTAR_PROPRIA_CONTA" );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_role_global WHERE id_usuario=:id" ).param( "id", id )
                .query( Long.class ).single() )
                .isOne();
        String uid = jdbc.sql( "SELECT cd_uid_planejado FROM security.usuario WHERE id_usuario=:id" ).param( "id", id )
                .query( String.class ).single();
        String email = jdbc.sql( "SELECT cd_email FROM security.usuario WHERE id_usuario=:id" ).param( "id", id )
                .query( String.class ).single();
        var restarted = new JdbcIdentityCommandWorker( jdbc, tx, events, source, admin, clock, java.time.Duration.ofMinutes( 2 ), catalog );
        assertThat( restarted.runOnce() ).isTrue();
        var identity = new VerifiedIdentity( "sample-isolated-test", "https://securetoken.google.com/sample-isolated-test", uid, email, true, "password", "Senha", clock.instant() );
        IDENTITIES.put( uid, identity );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + uid ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.user.id" ).value( id.toString() ) );
    }

    @Test
    void deveRecusarACKAntigo_quandoSuspensaoOcorreDuranteCriacaoRemota() {
        jdbc.sql( "UPDATE security.comando_identidade SET dt_proxima_tentativa=now()+interval '1 day' WHERE cd_estado IN ('PENDING','RETRY')" )
                .update();
        UUID id = users.provisionPassword( "Race", "race" + UUID.randomUUID() + "@example.test" );
        var racingAdmin = new FirebaseAdminGateway() {

            public User getUser( String uid ) {
                return admin.getUser( uid );
            }

            public User createUser( String uid, String email, String name ) {
                // Fixture adversarial: versão durável avançou durante a chamada remota.
                jdbc.sql( "UPDATE security.usuario SET nr_versao_identidade=nr_versao_identidade+1,dt_fim=now() WHERE id_usuario=:id" )
                        .param( "id", id ).update();
                return admin.createUser( uid, email, name );
            }

            public void setDisabled( String uid, boolean disabled ) {
                admin.setDisabled( uid, disabled );
            }

            public void revokeRefreshTokens( String uid ) {
                admin.revokeRefreshTokens( uid );
            }

            public String passwordResetLink( String email ) {
                throw new UnsupportedOperationException();
            }

        };
        var restarted = new JdbcIdentityCommandWorker( jdbc, tx, events, source, racingAdmin, clock, java.time.Duration.ofMinutes( 2 ), catalog );
        assertThat( restarted.runOnce() ).isTrue();
        assertThat( jdbc
                .sql( "SELECT cd_estado FROM security.comando_identidade WHERE id_usuario=:id AND cd_tipo='CREATE'" )
                .param( "id", id ).query( String.class ).single() ).isEqualTo( "OBSOLETE" );
        assertThat( jdbc.sql( "SELECT dt_fim IS NOT NULL FROM security.usuario WHERE id_usuario=:id" ).param( "id", id )
                .query( Boolean.class ).single() ).isTrue();
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_identidade WHERE id_usuario=:id" ).param( "id", id )
                .query( Long.class ).single() )
                .isZero();
    }

    @Test
    void devePublicarCadaEventoUmaVez_quandoJobIdentityWorkerProcessaEDrenaOutbox() throws Exception {
        var previous = jdbc.sql( "SELECT id_evento FROM security.evento_seguranca WHERE id_usuario=:id" )
                .param( "id", aliceId ).query( UUID.class ).list();
        var outbox = new JdbcSecurityEventOutbox( jdbc, tx, clock );
        applicationEvents.clear();

        users.suspend( aliceId );
        runIdentityJob( outbox );
        users.requestReactivation( aliceId );
        runIdentityJob( outbox );

        var expected = jdbc.sql( "SELECT id_evento FROM security.evento_seguranca WHERE id_usuario=:id" )
                .param( "id", aliceId ).query( UUID.class ).list().stream().filter( id -> !previous.contains( id ) ).toList();
        assertThat( expected ).hasSize( 6 );
        assertThat( applicationEvents.stream( SegurancaAlteradaEvent.class ).filter( event -> aliceId.equals( event.usuarioId() ) ).map( SegurancaAlteradaEvent::id ).toList() )
                .as( "O publisher do sample recebe cada UUID durável uma vez, inclusive os ACKs e a devolução." )
                .containsExactlyInAnyOrderElementsOf( expected );
        assertThat( applicationEvents.stream( SegurancaAlteradaEvent.class ).filter( event -> aliceId.equals( event.usuarioId() ) ).map( SegurancaAlteradaEvent::tipo ).toList() )
                .contains( "IDENTITY_COMMAND_ACKED", "GLOBAL_SELF_ACTION_RESTORED" );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.entrega_evento d JOIN security.evento_seguranca e ON e.id_evento=d.id_evento " + "WHERE e.id_usuario=:id AND d.cd_estado<>'ACKED'" ).param( "id", aliceId ).query( Long.class ).single() ).isZero();
        assertThat( outbox.publishNext( publisher ) ).isFalse();
    }

    private void runIdentityJob( JdbcSecurityEventOutbox outbox ) throws Exception {
        // O runner real fecha seu contexto; usa-se um contexto separado do fixture web.
        try (var jobContext = new GenericApplicationContext()) {
            jobContext.refresh();
            new JobsConfiguration().identityWorkerRunner( worker, outbox, publisher, jobContext ).run( new DefaultApplicationArguments() );
        }
    }

    @Test
    void deveReentregarEventoComMesmoUUID_quandoOutboxReiniciaAposFalha() {
        jdbc.sql( "UPDATE security.entrega_evento SET dt_proxima_tentativa=now()+interval '1 day' WHERE cd_estado='PENDING'" )
                .update();
        SecurityEventSink failingSink = event -> {
            throw new IllegalStateException( "destination unavailable" );
        };
        var failingUsers = new JdbcUserService( jdbc, tx, failingSink, clock, "sample-isolated-test", catalog, SecurityActorProvider.trustedBootstrap( UUID.randomUUID() ) );
        UUID id = failingUsers.preProvisionGoogle( "Evento", "event" + UUID.randomUUID() + "@example.test" );
        UUID eventId = jdbc.sql( "SELECT id_evento FROM security.evento_seguranca WHERE id_usuario=:id AND cd_evento LIKE '%USER_PREPROVISIONED'" )
                .param( "id", id ).query( UUID.class ).single();
        assertThat( jdbc.sql( "SELECT cd_estado FROM security.entrega_evento WHERE id_evento=:id" ).param( "id", eventId ).query( String.class ).single() ).isEqualTo( "PENDING" );
        assertThat( jdbc.sql( "SELECT nr_tentativas FROM security.entrega_evento WHERE id_evento=:id" ).param( "id", eventId ).query( Long.class ).single() ).isOne();
        jdbc.sql( "UPDATE security.entrega_evento SET dt_proxima_tentativa=now()-interval '1 day' WHERE id_evento=:id" )
                .param( "id", eventId ).update();
        var first = new br.com.gems.firebase.auth.jdbc.JdbcSecurityEventOutbox( jdbc, tx, clock );
        assertThat( first.publishNext( event -> {
            assertThat( event.id() ).isEqualTo( eventId );
            throw new IllegalStateException( "destination unavailable" );
        } ) ).isTrue();
        assertThat( jdbc.sql( "SELECT cd_estado FROM security.entrega_evento WHERE id_evento=:id" ).param( "id", eventId ).query( String.class ).single() ).isEqualTo( "PENDING" );
        jdbc.sql( "UPDATE security.entrega_evento SET dt_proxima_tentativa=now()-interval '1 day' WHERE id_evento=:id" )
                .param( "id", eventId ).update();
        var restarted = new br.com.gems.firebase.auth.jdbc.JdbcSecurityEventOutbox( jdbc, tx, clock );
        assertThat( restarted.publishNext( event -> assertThat( event.id() ).isEqualTo( eventId ) ) ).isTrue();
        assertThat( jdbc.sql( "SELECT cd_estado FROM security.entrega_evento WHERE id_evento=:id" ).param( "id", eventId )
                .query( String.class ).single() )
                .isEqualTo( "ACKED" );
        assertThat( jdbc.sql( "SELECT nr_tentativas FROM security.entrega_evento WHERE id_evento=:id" ).param( "id", eventId ).query( Long.class ).single() ).isEqualTo( 3 );
    }

    private jakarta.persistence.EntityManagerFactory repositoryEntityManagerFactory() {
        return entityManagerFactory;
    }

    @ParameterizedTest
    @CsvSource({ "12345678901234567.89,0.01", "99999999999999999.99,-99999999999999999.99", "-99999999999999999.99,0.00" })
    void devePreservarDecimalExatoNoCRUD_quandoValorUsaContratoDeDezesseteInteirosEDoisDecimais( String inserted, String updated ) throws Exception {
        var response = mvc.perform( post( "/api/demo-registros" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Decimal exato\",\"valorRestrito\":\"" + inserted + "\"}" ) )
                .andExpect( status().isCreated() ).andExpect( jsonPath( "$.valorRestrito" ).isString() )
                .andExpect( jsonPath( "$.valorRestrito" ).value( inserted ) )
                .andReturn().getResponse();
        String id = com.jayway.jsonpath.JsonPath.read( response.getContentAsString(), "$.id" );
        mvc.perform( get( "/api/demo-registros/" + id ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.valorRestrito" ).isString() )
                .andExpect( jsonPath( "$.valorRestrito" ).value( inserted ) );
        mvc.perform( post( "/api/demo-registros/search" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Decimal exato\"}" ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].valorRestrito" ).isString() )
                .andExpect( jsonPath( "$.content[0].valorRestrito" ).value( inserted ) );
        mvc.perform( put( "/api/demo-registros/" + id ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Decimal atualizado\",\"valorRestrito\":\"" + updated + "\"}" ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.valorRestrito" ).isString() )
                .andExpect( jsonPath( "$.valorRestrito" ).value( updated ) );
        mvc.perform( get( "/api/demo-registros/" + id ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.valorRestrito" ).isString() )
                .andExpect( jsonPath( "$.valorRestrito" ).value( updated ) );
        mvc.perform( post( "/api/demo-registros/search" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Decimal atualizado\"}" ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].valorRestrito" ).isString() )
                .andExpect( jsonPath( "$.content[0].valorRestrito" ).value( updated ) );
        assertThat( jdbc.sql( "SELECT vl_restrito FROM tenant_" + alpha + ".demo_registro WHERE id_demo_registro=:id" )
                .param( "id", UUID.fromString( id ) ).query( BigDecimal.class ).single() )
                .isEqualTo( new BigDecimal( updated ) );
        mvc.perform( get( "/api/demo-registros/" + id ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.valorRestrito" ).isEmpty() );
        mvc.perform( post( "/api/demo-registros/search" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Decimal atualizado\"}" ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content[0].valorRestrito" ).isEmpty() );
        mvc.perform( put( "/api/demo-registros/" + id ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Sem valor permitido\",\"valorRestrito\":\"0.99\"}" ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.valorRestrito" ).isEmpty() );
        assertThat( jdbc.sql( "SELECT vl_restrito FROM tenant_" + alpha + ".demo_registro WHERE id_demo_registro=:id" )
                .param( "id", UUID.fromString( id ) ).query( BigDecimal.class ).single() )
                .isEqualTo( new BigDecimal( updated ) );
        mvc.perform( post( "/api/demo-registros" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Sem valor permitido\",\"valorRestrito\":\"" + inserted + "\"}" ) )
                .andExpect( status().isCreated() ).andExpect( jsonPath( "$.valorRestrito" ).isEmpty() );
    }

    @ParameterizedTest
    @CsvSource({ "100000000000000000.00", "-100000000000000000.00", "0.001" })
    void deveRecusarDecimalForaDoLimiteSemAlterarSQL_quandoValorChegaComoString( String decimal ) throws Exception {
        mvc.perform( post( "/api/demo-registros" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Fora do limite\",\"valorRestrito\":\"" + decimal + "\"}" ) )
                .andExpect( status().isBadRequest() );
        mvc.perform( put( "/api/demo-registros/" + sameId ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"nome\":\"Fora do limite\",\"valorRestrito\":\"" + decimal + "\"}" ) )
                .andExpect( status().isBadRequest() );
        assertThat( jdbc.sql( "SELECT vl_restrito FROM tenant_" + alpha + ".demo_registro WHERE id_demo_registro=:id" )
                .param( "id", sameId ).query( BigDecimal.class ).single() ).isEqualTo( new BigDecimal( "123.45" ) );
        assertThat( jdbc.sql( "SELECT count(*) FROM tenant_" + alpha + ".demo_registro" ).query( Long.class ).single() )
                .isOne();
    }

    @ParameterizedTest
    @CsvSource({ "GOOGLE,@", "PASSWORD,@", "GOOGLE,local@", "PASSWORD,local@", "GOOGLE,local..part@example.test", "PASSWORD,local..part@example.test", "GOOGLE,local@example", "PASSWORD,local@example" })
    void deveRecusarEmailMalformadoSemPersistir_quandoCentralCriaUsuario( String method, String email ) throws Exception {
        assertInvalid( post( "/api/admin/central/users" )
                .content( "{\"name\":\"Válido\",\"email\":\"" + email + "\",\"method\":\"" + method + "\"}" ), "Informe e-mail válido de até 254 caracteres." );
    }

    @Test
    void deveAcumularNomeEmailEMetodoSemPersistir_quandoCentralRecebeUsuarioInvalido() throws Exception {
        assertInvalid( post( "/api/admin/central/users" ).content( "{\"name\":\" \",\"email\":\"@\"}" ), "Informe nome de até 150 caracteres.", "Informe e-mail válido de até 254 caracteres.", "Informe Google ou senha." );
        assertInvalid( put( "/api/admin/central/users/" + bobId ).content( "{\"name\":\" \",\"email\":\"@\"}" ), "Informe nome de até 150 caracteres.", "Informe e-mail válido de até 254 caracteres." );
    }

    @Test
    void deveRecusarEmailSemAlterarUsuario_quandoCentralAtualizaContaValida() throws Exception {
        assertInvalid( put( "/api/admin/central/users/" + bobId )
                .content( "{\"name\":\"Válido\",\"email\":\"local..part@example.test\"}" ), "Informe e-mail válido de até 254 caracteres." );
    }

    @Test
    void deveAcumularCamposEmpresaSemPersistir_quandoNomeOuAliasExcedeContrato() throws Exception {
        assertInvalid( post( "/api/admin/central/tenants" )
                .content( "{\"alias\":\"novo" + UUID.randomUUID().toString().replace( "-", "" ) + "\",\"name\":\"" + "x".repeat( 256 ) + "\"}" ), "Informe nome de até 255 caracteres." );
        assertInvalid( post( "/api/admin/central/tenants" ).content( "{\"alias\":\" \",\"name\":\" \"}" ), "Informe alias de até 56 caracteres.", "Informe nome de até 255 caracteres." );
        assertInvalid( post( "/api/admin/central/tenants" ).content( "{}" ), "Informe alias de até 56 caracteres.", "Informe nome de até 255 caracteres." );
        assertInvalid( post( "/api/admin/central/tenants" )
                .content( "{\"alias\":\"" + "a".repeat( 57 ) + "\",\"name\":\"" + "x".repeat( 256 ) + "\"}" ), "Informe alias de até 56 caracteres.", "Informe nome de até 255 caracteres." );
    }

    @Test
    void deveRecusarNomeEmpresaSemAlterarSQL_quandoCentralRenomeiaComNomeInvalido() throws Exception {
        for ( String name : new String[] { "", " ", "x".repeat( 256 ) } ) {
            assertInvalid( put( "/api/admin/central/tenants/" + alphaId )
                    .content( "{\"name\":\"" + name + "\"}" ), "Informe nome de até 255 caracteres." );
        }
        assertInvalid( put( "/api/admin/central/tenants/" + alphaId ).content( "{}" ), "Informe nome de até 255 caracteres." );
    }

    @ParameterizedTest
    @CsvSource({ "profiles", "groups" })
    void deveAcumularCodigoENomeSemPersistir_quandoAdministradorCriaContainerInvalido( String kind ) throws Exception {
        var path = "/api/admin/tenants/" + alphaId + "/" + kind;
        assertInvalid( post( path ).header( "X-Tenant-Alias", alpha )
                .content( "{\"code\":\"" + "c".repeat( 129 ) + "\",\"name\":\"" + "x".repeat( 256 ) + "\"}" ), "Informe código de até 128 caracteres.", "Informe nome de até 255 caracteres." );
        assertInvalid( post( path ).header( "X-Tenant-Alias", alpha )
                .content( "{\"code\":\" \",\"name\":\" \"}" ), "Informe código de até 128 caracteres.", "Informe nome de até 255 caracteres." );
        assertInvalid( post( path ).header( "X-Tenant-Alias", alpha ).content( "{}" ), "Informe código de até 128 caracteres.", "Informe nome de até 255 caracteres." );
        assertInvalid( post( path ).header( "X-Tenant-Alias", alpha )
                .content( "{\"code\":\"\",\"name\":\"" + "x".repeat( 256 ) + "\"}" ), "Informe código de até 128 caracteres.", "Informe nome de até 255 caracteres." );
        assertInvalid( post( path ).header( "X-Tenant-Alias", alpha )
                .content( "{\"code\":\"" + "c".repeat( 129 ) + "\",\"name\":\"\"}" ), "Informe código de até 128 caracteres.", "Informe nome de até 255 caracteres." );
    }

    @Test
    void devePreservarFluxosGoogleESenha_quandoCentralRecebeEmailValido() throws Exception {
        for ( String method : new String[] { "GOOGLE", "PASSWORD" } ) {
            String email = "valid" + UUID.randomUUID() + "@example.test";
            var response = mvc.perform( post( "/api/admin/central/users" )
                    .header( "Authorization", "Bearer " + bobUid ).contentType( "application/json" )
                    .content( "{\"name\":\"Válido\",\"email\":\"" + email + "\",\"method\":\"" + method + "\"}" ) )
                    .andExpect( status().isCreated() ).andReturn().getResponse();
            UUID id = UUID.fromString( com.jayway.jsonpath.JsonPath.read( response.getContentAsString(), "$.id" ) );
            assertThat( jdbc.sql( "SELECT cd_email FROM security.usuario WHERE id_usuario=:id" ).param( "id", id )
                    .query( String.class ).single() ).isEqualTo( email );
            assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_role_global WHERE id_usuario=:id" )
                    .param( "id", id ).query( Long.class ).single() ).isOne();
            assertThat( jdbc
                    .sql( "SELECT count(*) FROM security.comando_identidade WHERE id_usuario=:id AND cd_tipo='CREATE'" )
                    .param( "id", id ).query( Long.class ).single() )
                    .isEqualTo( method.equals( "PASSWORD" ) ? 1L : 0L );
            if ( method.equals( "GOOGLE" ) ) {
                mvc.perform( put( "/api/admin/central/users/" + id ).header( "Authorization", "Bearer " + bobUid )
                        .contentType( "application/json" )
                        .content( "{\"name\":\"Atualizado\",\"email\":\"" + email + "\"}" ) )
                        .andExpect( status().isOk() );
            }
        }
    }

    @Test
    void deveAceitarLimitesPublicados_quandoEmpresaPerfilEGrupoUsamTamanhosMaximos() throws Exception {
        String alias = "limite" + UUID.randomUUID().toString().replace( "-", "" ) + "a".repeat( 18 );
        String name = "n".repeat( 255 );
        var response = mvc.perform( post( "/api/admin/central/tenants" ).header( "Authorization", "Bearer " + bobUid )
                .contentType( "application/json" )
                .content( "{\"alias\":\"" + alias + "\",\"name\":\"" + name + "\"}" ) )
                .andExpect( status().isCreated() ).andReturn().getResponse();
        UUID id = UUID.fromString( com.jayway.jsonpath.JsonPath.read( response.getContentAsString(), "$.id" ) );
        assertThat( jdbc.sql( "SELECT nm_tenant FROM security.tenant WHERE id_tenant=:id" ).param( "id", id )
                .query( String.class ).single() ).isEqualTo( name );
        mvc.perform( put( "/api/admin/central/tenants/" + id ).header( "Authorization", "Bearer " + bobUid )
                .contentType( "application/json" ).content( "{\"name\":\"" + "r".repeat( 255 ) + "\"}" ) )
                .andExpect( status().isOk() );
        for ( String kind : new String[] { "profiles", "groups" } ) {
            String code = UUID.randomUUID().toString() + "c".repeat( 92 );
            mvc.perform( post( "/api/admin/tenants/" + alphaId + "/" + kind )
                    .header( "Authorization", "Bearer " + bobUid ).header( "X-Tenant-Alias", alpha )
                    .contentType( "application/json" )
                    .content( "{\"code\":\"" + code + "\",\"name\":\"" + name + "\"}" ) )
                    .andExpect( status().isCreated() );
        }
    }

    @Test
    void deveRecusarMe403_quandoApenasConsultarPropriaContaEhRevogada() throws Exception {
        grants.grantGlobal( aliceId, "CONSULTAR_TENANT" );
        revokeGlobalAction( aliceId, "CONSULTAR_PROPRIA_CONTA" );

        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isForbidden() );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isForbidden() );

        assertThat( identityAndMembershipRemainValid( aliceId, alphaId ) ).isTrue();
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.tenant.alias" ).value( alpha ) );
    }

    @Test
    void deveRecusarContext403_quandoApenasConsultarProprioContextoTenantEhRevogada() throws Exception {
        revokeMemberAction( alphaId, aliceMember, "CONSULTAR_PROPRIO_CONTEXTO_TENANT" );

        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isForbidden() );

        assertThat( identityAndMembershipRemainValid( aliceId, alphaId ) ).isTrue();
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.user.id" ).value( aliceId.toString() ) );
        mvc.perform( get( "/api/demo/tenant/read" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() );
    }

    @Test
    void deveRetornarMeCompleto_quandoUsuarioTemUmaEmpresaOuVariasEAcoesGlobais() throws Exception {
        var aliceBody = json( mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andExpect( status().isOk() ).andReturn().getResponse().getContentAsString() );
        var bobBody = json( mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + bobUid ) )
                .andExpect( status().isOk() ).andReturn().getResponse().getContentAsString() );

        assertThat( aliceBody ).containsOnlyKeys( "user", "tenants", "globalActions" );
        assertThat( (Map<String, Object>) aliceBody.get( "user" ) ).containsOnlyKeys( "id", "name", "email" )
                .containsEntry( "id", aliceId.toString() ).containsEntry( "name", aliceUid )
                .containsEntry( "email", aliceUid + "@example.test" );
        assertThat( (java.util.List<Map<String, Object>>) aliceBody.get( "tenants" ) )
                .containsExactly( Map.of( "alias", alpha, "name", "Alpha" ) );
        assertThat( (java.util.List<String>) aliceBody.get( "globalActions" ) )
                .containsExactlyInAnyOrder( "CONSULTAR_PROPRIA_CONTA" );
        assertThat( (Map<String, Object>) bobBody.get( "user" ) ).containsEntry( "id", bobId.toString() )
                .containsEntry( "name", bobUid ).containsEntry( "email", bobUid + "@example.test" );
        assertThat( (java.util.List<Map<String, Object>>) bobBody.get( "tenants" ) ).containsExactlyInAnyOrder( Map.of( "alias", alpha, "name", "Alpha" ), Map.of( "alias", beta, "name", "Beta" ) );
        assertThat( (java.util.List<String>) bobBody.get( "globalActions" ) )
                .containsExactlyInAnyOrderElementsOf( catalog.globalActions() );
    }

    @Test
    void deveRetornarContextCompleto_quandoMembroTemPerfilGrupoEAcoesProprias() throws Exception {
        var semContainers = json( mvc
                .perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) )
                .andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString() );
        var profile = grants.createProfile( alphaId, "perfil-a", "Perfil A" );
        grants.grant( GrantAdministration.Target.PROFILE, alphaId, profile, "VISUALIZAR_VALOR_RESTRITO" );
        grants.link( GrantAdministration.Link.MEMBER_PROFILE, alphaId, aliceMember, profile );
        var group = grants.createGroup( alphaId, "grupo-a", "Grupo A" );
        grants.link( GrantAdministration.Link.GROUP_MEMBER, alphaId, group, aliceMember );

        var completo = json( mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() ).andReturn().getResponse()
                .getContentAsString() );

        var ownActions = new HashSet<>( catalog.tenantActions() );
        ownActions.remove( "VISUALIZAR_VALOR_RESTRITO" );
        assertThat( semContainers ).containsOnlyKeys( "tenant", "profiles", "groups", "actions" );
        assertThat( (Map<String, Object>) semContainers.get( "tenant" ) ).containsOnlyKeys( "alias", "name" )
                .containsEntry( "alias", alpha ).containsEntry( "name", "Alpha" );
        assertThat( (java.util.List<?>) semContainers.get( "profiles" ) ).isEmpty();
        assertThat( (java.util.List<?>) semContainers.get( "groups" ) ).isEmpty();
        assertThat( (java.util.List<String>) semContainers.get( "actions" ) )
                .containsExactlyInAnyOrderElementsOf( ownActions );
        assertThat( completo ).containsOnlyKeys( "tenant", "profiles", "groups", "actions" );
        assertThat( (java.util.List<Map<String, Object>>) completo.get( "profiles" ) )
                .containsExactly( Map.of( "code", "perfil-a", "name", "Perfil A" ) );
        assertThat( (java.util.List<Map<String, Object>>) completo.get( "groups" ) )
                .containsExactly( Map.of( "code", "grupo-a", "name", "Grupo A" ) );
        assertThat( (java.util.List<String>) completo.get( "actions" ) )
                .containsExactlyInAnyOrderElementsOf( catalog.tenantActions() );
    }

    @Test
    void deveManterCorposSemSegredos_quandoRespostasSao401403E503() throws Exception {
        String sentinel = "sentinela-" + UUID.randomUUID();
        var forbiddenFragments = java.util.List.of( sentinel, RAW_PROVIDER_SECRET, aliceUid, bobUid, aliceUid + "@example.test", bobUid + "@example.test", DATABASE.getJdbcUrl(), DATABASE.getPassword(), "Bearer", "Exception", "stackTrace", "org.springframework", "org.postgresql", "SQL" );
        revokeGlobalAction( aliceId, "CONSULTAR_PROPRIA_CONTA" );
        var unauthorized = mvc.perform( get( "/api/auth/me" ).header( "X-Tenant-Alias", sentinel ) )
                .andReturn().getResponse();
        var invalid = mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer invalid-" + sentinel )
                .header( "X-Tenant-Alias", sentinel ) ).andReturn().getResponse();
        var unavailable = mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer unavailable" )
                .header( "X-Tenant-Alias", sentinel ) ).andReturn().getResponse();
        var remoteFailure = mvc.perform( get( "/api/auth/me" )
                .header( "Authorization", "Bearer provider-failure-" + sentinel )
                .header( "X-Tenant-Alias", sentinel ) ).andReturn().getResponse();
        var foreignAlias = mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid )
                .header( "X-Tenant-Alias", beta ) ).andReturn().getResponse();
        var withoutAlias = mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andReturn().getResponse();
        var missingAction = mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + aliceUid ) )
                .andReturn().getResponse();

        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat( unauthorized.getStatus() ).isEqualTo( 401 );
            assertions.assertThat( invalid.getStatus() ).isEqualTo( 401 );
            assertions.assertThat( unavailable.getStatus() ).isEqualTo( 503 );
            assertions.assertThat( remoteFailure.getStatus() ).isEqualTo( 503 );
            assertions.assertThat( foreignAlias.getStatus() ).isEqualTo( 403 );
            assertions.assertThat( withoutAlias.getStatus() ).isEqualTo( 403 );
            assertions.assertThat( missingAction.getStatus() ).isEqualTo( 403 );
            for ( var response : java.util.List.of( unauthorized, invalid, unavailable, remoteFailure, foreignAlias, withoutAlias, missingAction ) ) {
                String body = responseBody( response );
                for ( String fragment : forbiddenFragments ) {
                    assertions.assertThat( body ).as( "corpo %d sem '%s'", response.getStatus(), fragment )
                            .doesNotContain( fragment );
                }
                assertions.assertThat( response.getHeaderNames() ).as( "cabecalhos %d", response.getStatus() )
                        .noneMatch( name -> String.join( ",", response.getHeaders( name ) ).contains( sentinel ) );
            }
        } );
    }

    @Test
    void devePermitirLeituraEGravacaoDoProprioTenant_quandoAlphaEBetaCompartilhamPK() throws Exception {
        mvc.perform( get( "/api/demo/tenant/read" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content.length()" ).value( 1 ) )
                .andExpect( jsonPath( "$.content[0].id" ).value( sameId.toString() ) )
                .andExpect( jsonPath( "$.content[0].nome" ).value( "Alpha" ) );
        mvc.perform( get( "/api/demo/tenant/read" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", beta ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content.length()" ).value( 1 ) )
                .andExpect( jsonPath( "$.content[0].id" ).value( sameId.toString() ) )
                .andExpect( jsonPath( "$.content[0].nome" ).value( "Beta" ) );
        mvc.perform( post( "/api/demo/tenant/write" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", alpha ).contentType( "application/json" )
                .content( "{\"id\":\"" + sameId + "\",\"nome\":\"Alpha gravado\",\"descricao\":\"Via write\"}" ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.nome" ).value( "Alpha gravado" ) );

        assertThat( demoName( alpha ) ).isEqualTo( "Alpha gravado" );
        assertThat( demoName( beta ) ).isEqualTo( "Beta" );
        mvc.perform( post( "/api/demo/tenant/write" ).header( "Authorization", "Bearer " + bobUid )
                .header( "X-Tenant-Alias", beta ).contentType( "application/json" )
                .content( "{\"id\":\"" + sameId + "\",\"nome\":\"Beta gravado\",\"descricao\":\"Via write\"}" ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.nome" ).value( "Beta gravado" ) );
        assertThat( demoName( alpha ) ).isEqualTo( "Alpha gravado" );
        assertThat( demoName( beta ) ).isEqualTo( "Beta gravado" );
    }

    @ParameterizedTest
    @ValueSource(strings = { "CONSULTAR_DEMO", "INSERIR_DEMO", "ALTERAR_DEMO", "EXCLUIR_DEMO" })
    void deveRecusar403SemMutacaoNemAuditoria_quandoMembroValidoNaoTemAAcaoDemo( String missing ) throws Exception {
        String uid = "carol" + UUID.randomUUID().toString().replace( "-", "" ).substring( 0, 8 );
        var carolMember = tenants.addMember( alphaId, bind( uid ) );
        for ( String action : catalog.tenantActions() ) {
            if ( !action.equals( missing ) ) {
                grants.grant( GrantAdministration.Target.MEMBER, alphaId, carolMember, action );
            }
        }
        var valid = "{\"nome\":\"Nao deve gravar\",\"descricao\":\"x\"}";
        var forbidden = Map.<String, java.util.List<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder>>of( "CONSULTAR_DEMO", java.util.List.of( get( "/api/demo/tenant/read" ), get( "/api/demo-registros/" + sameId ), post( "/api/demo-registros/search" ).contentType( "application/json" ).content( "{}" ) ), "INSERIR_DEMO", java.util.List.of( post( "/api/demo-registros" ).contentType( "application/json" ).content( valid ) ), "ALTERAR_DEMO", java.util.List.of( put( "/api/demo-registros/" + sameId ).contentType( "application/json" ).content( valid ), post( "/api/demo/tenant/write" ).contentType( "application/json" )
                .content( "{\"id\":\"" + sameId + "\",\"nome\":\"Nao deve gravar\"}" ) ), "EXCLUIR_DEMO", java.util.List.of( delete( "/api/demo-registros/" + sameId ) ) );
        var before = java.util.List.of( businessState( alpha ), businessState( beta ) );

        for ( var request : forbidden.get( missing ) ) {
            mvc.perform( request.header( "Authorization", "Bearer " + uid ).header( "X-Tenant-Alias", alpha ) )
                    .andExpect( status().isForbidden() );
        }

        assertThat( java.util.List.of( businessState( alpha ), businessState( beta ) ) ).isEqualTo( before );
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + uid )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.actions" )
                .value( org.hamcrest.Matchers.not( org.hamcrest.Matchers.hasItem( missing ) ) ) )
                .andExpect( jsonPath( "$.actions.length()" ).value( catalog.tenantActions().size() - 1 ) );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json( String body ) {
        return com.jayway.jsonpath.JsonPath.read( body, "$" );
    }

    private static String responseBody( org.springframework.mock.web.MockHttpServletResponse response ) {
        return new String( response.getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8 );
    }

    private void revokeGlobalAction( UUID user, String action ) {
        UUID grant = jdbc.sql( "SELECT g.id_usuario_role_global FROM security.usuario_role_global g " + "JOIN security.role_seguranca r ON r.id_role=g.id_role " + "WHERE g.id_usuario=:user AND r.cd_acao=:action AND g.dt_fim IS NULL" )
                .param( "user", user ).param( "action", action ).query( UUID.class ).single();
        grants.revokeGlobal( user, grant );
    }

    private void revokeMemberAction( UUID tenant, UUID member, String action ) {
        UUID grant = jdbc.sql( "SELECT g.id_usuario_tenant_role FROM security.usuario_tenant_role g " + "JOIN security.role_seguranca r ON r.id_role=g.id_role " + "WHERE g.id_usuario_tenant=:member AND r.cd_acao=:action AND g.dt_fim IS NULL" )
                .param( "member", member ).param( "action", action ).query( UUID.class ).single();
        grants.revoke( GrantAdministration.Target.MEMBER, tenant, grant );
    }

    private boolean identityAndMembershipRemainValid( UUID user, UUID tenant ) {
        return jdbc.sql( "SELECT EXISTS(SELECT 1 FROM security.usuario WHERE id_usuario=:user AND dt_fim IS NULL) " + "AND EXISTS(SELECT 1 FROM security.usuario_tenant WHERE id_usuario=:user AND id_tenant=:tenant " + "AND dt_fim IS NULL)" ).param( "user", user ).param( "tenant", tenant ).query( Boolean.class )
                .single();
    }

    private String demoName( String alias ) {
        return jdbc.sql( "SELECT nm_registro FROM tenant_" + alias + ".demo_registro WHERE id_demo_registro=:id" )
                .param( "id", sameId ).query( String.class ).single();
    }

    private String businessState( String alias ) {
        return jdbc.sql( "SELECT coalesce(jsonb_agg(to_jsonb(row) ORDER BY to_jsonb(row)::text),'[]'::jsonb)::text " + "FROM tenant_" + alias + ".demo_registro row" ).query( String.class ).single() + "|audit=" + jdbc.sql( "SELECT count(*) FROM tenant_" + alias + ".audit_operation" ).query( Long.class )
                .single();
    }

    private void assertInvalid( org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String... details ) throws Exception {
        var before = securityState();
        var response = mvc.perform( request.header( "Authorization", "Bearer " + bobUid )
                .contentType( "application/json" ) ).andReturn().getResponse();
        Map<String, Object> body = response.getContentAsString().isBlank()
                ? Map.of()
                : com.jayway.jsonpath.JsonPath.read( response.getContentAsString(), "$" );
        var after = securityState();
        SoftAssertions.assertSoftly( assertions -> {
            assertions.assertThat( response.getStatus() ).isEqualTo( 400 );
            assertions.assertThat( body.get( "detalhes" ) ).isEqualTo( java.util.List.of( details ) );
            assertions.assertThat( after.keySet() ).isEqualTo( before.keySet() );
            assertions.assertThat( after.entrySet().stream()
                    .filter( entry -> !entry.getValue().equals( before.get( entry.getKey() ) ) )
                    .map( Map.Entry::getKey ).toList() )
                    .as( "changed tables: users, tenants, grants, commands and all durable outboxes" ).isEmpty();
        } );
    }

    private Map<String, String> securityState() {
        var state = new java.util.TreeMap<String, String>();
        var tables = jdbc.sql( "SELECT tablename FROM pg_tables WHERE schemaname='security' ORDER BY tablename" )
                .query( String.class ).list();
        for ( String table : tables ) {
            assertThat( table ).matches( "[a-z_]+" );
            state.put( table, jdbc.sql( "SELECT coalesce(jsonb_agg(to_jsonb(row) ORDER BY to_jsonb(row)::text),'[]'::jsonb)::text FROM security." + table + " row" )
                    .query( String.class ).single() );
        }
        return state;
    }

    private UUID bind( String uid ) {
        var email = uid + "@example.test";
        var id = users.preProvisionGoogle( uid, email );
        var identity = new VerifiedIdentity( "sample-isolated-test", "https://securetoken.google.com/sample-isolated-test", uid, email, true, "google.com", uid, clock.instant() );
        IDENTITIES.put( uid, identity );
        assertThat( resolver.resolve( identity, Optional.empty(), clock.instant() ).principal().userId() )
                .isEqualTo( id );
        return id;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Ports {

        @Bean
        IdTokenVerifier verifier() {
            return token -> {
                if ( token.equals( "unavailable" ) ) {
                    throw new FirebaseAuthException( FirebaseAuthException.Reason.PROVIDER_UNAVAILABLE );
                }
                if ( token.startsWith( "provider-failure-" ) ) {
                    throw new IllegalStateException( RAW_PROVIDER_SECRET + " " + token );
                }
                return Optional.ofNullable( IDENTITIES.get( token ) )
                        .orElseThrow( () -> new FirebaseAuthException( FirebaseAuthException.Reason.INVALID_TOKEN ) );
            };
        }

        @Bean
        FirebaseAdminGateway admin() {
            return new FirebaseAdminGateway() {

                public User getUser( String uid ) {
                    return new User( uid, uid + "@example.test", uid, false, true );
                }

                public User createUser( String uid, String email, String displayName ) {
                    return new User( uid, email, displayName, false, true );
                }

                public void setDisabled( String uid, boolean disabled ) {
                    if ( REMOTE_FAILURE.get() ) {
                        throw new FirebaseAuthException( FirebaseAuthException.Reason.PROVIDER_UNAVAILABLE );
                    }
                }

                public void revokeRefreshTokens( String uid ) {
                    if ( REMOTE_FAILURE.get() ) {
                        throw new FirebaseAuthException( FirebaseAuthException.Reason.PROVIDER_UNAVAILABLE );
                    }
                }

                public String passwordResetLink( String email ) {
                    throw new UnsupportedOperationException();
                }

            };
        }

    }

}
