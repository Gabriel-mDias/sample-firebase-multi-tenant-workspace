package br.com.gems.app;

import br.com.gems.app.support.FirebaseContractSupport;
import br.com.gems.firebase.auth.AuthorizationResolver;
import br.com.gems.firebase.auth.FirebaseAuthenticationFilter;
import br.com.gems.firebase.auth.FirebaseAuthenticationToken;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.TenantAdministration;
import br.com.gems.firebase.auth.TenantAdministratorBootstrap;
import br.com.gems.firebase.auth.UserAdministration;
import br.com.gems.firebase.auth.jdbc.JdbcIdentityCommandWorker;
import br.com.gems.firebase.auth.jdbc.JdbcSecurityCatalog;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.security.authorization.AuthorizationContextAware;
import br.com.gems.security.authorization.JwtAuthorizationContext;
import br.com.gems.security.authorization.TenantAuthorizationInterceptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(FirebaseBackendIntegrationTest.Ports.class)
class FirebaseAdoptionContractIntegrationTest extends FirebaseContractSupport {
    @Autowired
    ApplicationContext context;
    @Autowired
    FilterChainProxy chains;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapping;

    @Test
    void deveConsultarSqlNoProximoRequest_quandoTokenContemClaimsAdministrativos() throws Exception {
        jdbc.sql( "UPDATE security.usuario_role_global SET dt_fim=dt_inicio WHERE id_usuario=:id" )
                .param( "id", subject.id() ).update();
        jdbc.sql( "UPDATE security.usuario_tenant_role SET dt_fim=dt_inicio WHERE id_usuario_tenant=:id" )
                .param( "id", subjectMember ).update();
        String token = firebase.administrativeToken( subject.uid() );
        String payload = new String( Base64.getUrlDecoder().decode( token.split( "\\." )[1] ),
                StandardCharsets.UTF_8 );
        assertThat( (Boolean) com.jayway.jsonpath.JsonPath.read( payload, "$.admin" ) ).isTrue();
        assertThat( (java.util.List<String>) com.jayway.jsonpath.JsonPath.read( payload, "$.roles" ) )
                .contains( "ADMIN", "CONSULTAR_TENANT" );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_role_global "
                + "WHERE id_usuario=:id AND dt_fim IS NULL" )
                .param( "id", subject.id() ).query( Long.class ).single() ).isZero();
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_tenant_role "
                + "WHERE id_usuario_tenant=:id AND dt_fim IS NULL" )
                .param( "id", subjectMember ).query( Long.class ).single() ).isZero();

        mvc.perform( get( "/api/demo/global" ).header( "Authorization", "Bearer " + token ) )
                .andExpect( status().isForbidden() );
        var grant = grants.grantGlobal( subject.id(), "CONSULTAR_TENANT" );
        mvc.perform( get( "/api/demo/global" ).header( "Authorization", "Bearer " + token ) )
                .andExpect( status().isOk() );
        grants.revokeGlobal( subject.id(), grant );
        mvc.perform( get( "/api/demo/global" ).header( "Authorization", "Bearer " + token ) )
                .andExpect( status().isForbidden() );

        var resolution = resolver.resolve( firebase.identities.get( token ), java.util.Optional.empty(),
                clock.instant() );
        var authentication = new FirebaseAuthenticationToken( resolution );
        assertThat( authentication ).isInstanceOf( AuthorizationContextAware.class );
        assertThat( authentication.authorizationContext() ).isInstanceOf( JwtAuthorizationContext.class );
        assertThat( authentication.authorizationContext().globalActions() ).doesNotContain( "CONSULTAR_TENANT" );
        assertThat( authentication.getAuthorities() ).extracting( "authority" )
                .doesNotContain( "ROLE_ADMIN", "ROLE_CONSULTAR_TENANT" );
    }

    @Test
    void deveUsarJarMavenDoFiltroEfetivo_quandoContextoInstalaSdk() throws Exception {
        var firebaseFilter = context.getBean( FirebaseAuthenticationFilter.class );
        var location = firebaseFilter.getClass().getProtectionDomain().getCodeSource().getLocation();
        var path = Path.of( location.toURI() );

        assertThat( path.getFileName().toString() ).isEqualTo( "gems-firebase-auth-3.4.1.jar" );
        try ( var jar = new JarFile( path.toFile() ) ) {
            var metadata = jar.getJarEntry( "META-INF/maven/br.com.gems/gems-firebase-auth/pom.properties" );
            assertThat( metadata ).isNotNull();
            var properties = new Properties();
            try ( var stream = jar.getInputStream( metadata ) ) {
                properties.load( stream );
            }
            assertThat( properties ).containsEntry( "groupId", "br.com.gems" )
                    .containsEntry( "artifactId", "gems-firebase-auth" ).containsEntry( "version", "3.4.1" );
            assertThat( FirebaseAuthenticationFilter.class.getResource( "FirebaseAuthenticationFilter.class" )
                    .toExternalForm() ).startsWith( "jar:" + location.toExternalForm() + "!/" );
        }
        var effective = chains.getFilterChains().stream()
                .filter( chain -> chain.getFilters().contains( firebaseFilter ) ).toList();
        assertThat( effective ).hasSize( 1 );
        var filters = effective.getFirst().getFilters();
        assertThat( filters.stream().filter( FirebaseAuthenticationFilter.class::isInstance ) ).hasSize( 1 );
        assertThat( filters.indexOf( firebaseFilter ) ).isLessThan( filters.indexOf( filters.stream()
                .filter( AuthorizationFilter.class::isInstance ).findFirst().orElseThrow() ) );
        var registrations = context.getBeansOfType( FilterRegistrationBean.class ).values().stream()
                .filter( registration -> registration.getFilter() == firebaseFilter ).toList();
        assertThat( registrations ).hasSize( 1 );
        assertThat( registrations.getFirst().isEnabled() ).isFalse();
        for ( Class<?> port : List.of( AuthorizationResolver.class, UserAdministration.class,
                TenantAdministration.class, GrantAdministration.class, TenantAdministratorBootstrap.class,
                JdbcIdentityCommandWorker.class ) ) {
            var implementation = AopUtils.getTargetClass( context.getBean( port ) );
            assertThat( implementation.getProtectionDomain().getCodeSource().getLocation() )
                    .as( "Garantia %s ligada à implementação no JAR adotado.", port.getSimpleName() )
                    .isEqualTo( location );
        }
    }

    @ParameterizedTest
    @CsvSource({"invalid,401", "unavailable,503"})
    void deveFalharFechadoComStatusDistinto_quandoVerificadorRecusaToken( String token, int expected )
            throws Exception {
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + token ) )
                .andExpect( status().is( expected ) );
    }

    @Test
    void deveRecusarTrocaDeEscopoNoCatalogo_quandoWiringDaSdkERealmenteMutado() {
        var global = new HashSet<>( catalog.globalActions() );
        var tenant = new HashSet<>( catalog.tenantActions() );
        tenant.remove( "CONSULTAR_DEMO" );
        global.add( "CONSULTAR_DEMO" );
        var mutated = new JdbcSecurityCatalog( jdbc, tx, events, new AuthorizationCatalog( global, tenant ),
                () -> tenant, clock, PROJECT );

        try {
            assertThatThrownBy( mutated::initialize ).isInstanceOf( IllegalStateException.class );
        } finally {
            context.getBean( JdbcSecurityCatalog.class ).initialize();
        }
    }

    @Test
    void deveRecusarEndpointSemClassificacao_quandoScannerExaminaWiringMutado() throws Exception {
        var scanner = context.getBean( "firebaseEndpointAuthorizationScan", SmartInitializingSingleton.class );
        var route = RequestMappingInfo.paths( "/api/be2-unclassified" ).build();
        mapping.registerMapping( route, new UnclassifiedEndpoint(), UnclassifiedEndpoint.class.getMethod( "read" ) );

        try {
            assertThatThrownBy( scanner::afterSingletonsInstantiated )
                    .hasMessageContaining( "endpoint sem classifica" );
        } finally {
            mapping.unregisterMapping( route );
        }
        scanner.afterSingletonsInstantiated();
    }

    @Test
    void deveExigirAliasSomenteNoEndpointTenant_quandoMesmoAtorTemAmbosEscopos() throws Exception {
        mvc.perform( get( "/api/demo/global" ).header( "Authorization", "Bearer " + operator.uid() ) )
                .andExpect( status().isOk() );
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + operator.uid() ) )
                .andExpect( status().isForbidden() );
        mvc.perform( get( "/api/auth/context" ).header( "Authorization", "Bearer " + operator.uid() )
                .header( "X-Tenant-Alias", alpha ) ).andExpect( status().isOk() );
        var request = new MockHttpServletRequest( "GET", "/api/auth/context" );
        ServletRequestPathUtils.parseAndCache( request );
        var handler = mapping.getHandler( request );
        assertThat( handler ).isNotNull();
        assertThat( Arrays.stream( handler.getInterceptors() )
                .filter( TenantAuthorizationInterceptor.class::isInstance ) ).hasSize( 1 );
    }

    public static class UnclassifiedEndpoint {
        public String read() {
            return "Sem classificação.";
        }
    }
}
