package br.com.gems.app;

import br.com.gems.app.administracao.service.AdministracaoService;
import br.com.gems.app.demo.service.DemoRegistroService;
import br.com.gems.firebase.auth.FirebaseAuthenticationFilter;
import br.com.gems.security.authorization.TenantAuthorizationInterceptor;
import java.util.Arrays;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

/** Prova a fiação real: filtro único, interceptor no MVC e separação entre os managers JDBC e JPA. */
@SpringBootTest
@Testcontainers
@Import(FirebaseBackendIntegrationTest.Ports.class)
class FirebaseWiringIntegrationTest {
    private static final String SECURITY_MANAGER = "firebaseSecurityTransactionManager";
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" );
    @DynamicPropertySource
    static void database( DynamicPropertyRegistry registry ) {
        registry.add( "spring.datasource.url", DATABASE::getJdbcUrl );
        registry.add( "spring.datasource.username", DATABASE::getUsername );
        registry.add( "spring.datasource.password", DATABASE::getPassword );
    }
    @Autowired
    ApplicationContext context;
    @Autowired
    FilterChainProxy securityFilterChains;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mvcMapping;

    @Test
    void deveInstalarFiltroFirebaseUmaUnicaVezNaCadeia_quandoContextoWebInicia() {
        var chainsWithFirebase = securityFilterChains.getFilterChains().stream()
                .filter( chain -> chain.getFilters().stream()
                        .anyMatch( FirebaseAuthenticationFilter.class::isInstance ) )
                .toList();

        assertThat( chainsWithFirebase ).hasSize( 1 );
        var filters = chainsWithFirebase.getFirst().getFilters();
        assertThat( filters.stream().filter( FirebaseAuthenticationFilter.class::isInstance ) ).hasSize( 1 );
        var firebase = filters.stream().filter( FirebaseAuthenticationFilter.class::isInstance ).findFirst()
                .orElseThrow();
        assertThat( firebase ).isSameAs( context.getBean( FirebaseAuthenticationFilter.class ) );
        assertThat( filters.indexOf( firebase ) )
                .isLessThan( filters.indexOf( filters.stream().filter( AuthorizationFilter.class::isInstance )
                        .findFirst().orElseThrow() ) );
    }

    @Test
    void deveDesabilitarRegistrationServletDoFiltro_quandoChainDoConsumidorOInstala() {
        var registrations = context.getBeansOfType( FilterRegistrationBean.class ).values().stream()
                .filter( registration -> registration.getFilter() instanceof FirebaseAuthenticationFilter ).toList();

        assertThat( context.getBeansOfType( FirebaseAuthenticationFilter.class ) ).hasSize( 1 );
        assertThat( registrations ).hasSize( 1 );
        assertThat( registrations.getFirst().isEnabled() ).isFalse();
    }

    @Test
    void deveRegistrarInterceptorDeTenantNoMVC_quandoEndpointExigeEmpresa() throws Exception {
        for ( String path : new String[]{"/api/auth/context", "/api/demo/tenant/read",
                "/api/demo-registros/" + UUID.randomUUID()} ) {
            var request = new MockHttpServletRequest( "GET", path );
            ServletRequestPathUtils.parseAndCache( request );

            var chain = mvcMapping.getHandler( request );

            assertThat( chain ).as( "handler de %s", path ).isNotNull();
            assertThat( Arrays.stream( chain.getInterceptors() )
                    .filter( TenantAuthorizationInterceptor.class::isInstance ) ).as( "interceptor em %s", path )
                    .hasSize( 1 );
        }
    }

    @Test
    void deveSepararManagerJdbcDeSegurancaDoManagerJpa_quandoContextoInicia() {
        var jpa = context.getBean( "transactionManager", PlatformTransactionManager.class );
        var security = context.getBean( SECURITY_MANAGER, PlatformTransactionManager.class );
        var securitySource = context.getBean( "firebaseSecurityDataSource", DataSource.class );

        assertThat( jpa ).isInstanceOf( JpaTransactionManager.class );
        assertThat( security ).isInstanceOf( JdbcTransactionManager.class ).isNotSameAs( jpa );
        assertThat( context.getBean( PlatformTransactionManager.class ) ).isSameAs( jpa );
        assertThat( ((JdbcTransactionManager) security).getDataSource() ).isSameAs( securitySource );
        assertThat( ((JpaTransactionManager) jpa).getDataSource() ).isNotSameAs( securitySource );
        assertThat( context.getBean( DataSource.class ) ).isNotSameAs( securitySource );
        assertThat( context.getBean( "firebaseSecurityTransactions", TransactionTemplate.class )
                .getTransactionManager() ).isSameAs( security );
    }

    @Test
    void deveUsarQualificadoresEsperados_quandoServicosEscrevem() {
        var securityWrites = Arrays.stream( AdministracaoService.class.getDeclaredMethods() )
                .filter( method -> AnnotatedElementUtils.hasAnnotation( method, Transactional.class ) ).toList();
        var businessWrites = Arrays.stream( DemoRegistroService.class.getDeclaredMethods() )
                .filter( method -> AnnotatedElementUtils.hasAnnotation( method, Transactional.class ) ).toList();

        assertThat( securityWrites ).isNotEmpty();
        assertThat( securityWrites ).allSatisfy( method -> {
            var transactional = AnnotatedElementUtils.findMergedAnnotation( method, Transactional.class );
            assertThat( transactional.transactionManager() ).as( method.getName() ).isEqualTo( SECURITY_MANAGER );
            assertThat( transactional.rollbackFor() ).as( method.getName() ).contains( Exception.class );
        } );
        assertThat( businessWrites ).isNotEmpty();
        assertThat( businessWrites ).allSatisfy( method -> assertThat(
                AnnotatedElementUtils.findMergedAnnotation( method, Transactional.class ).transactionManager() )
                .as( method.getName() ).isEmpty() );
    }
}
