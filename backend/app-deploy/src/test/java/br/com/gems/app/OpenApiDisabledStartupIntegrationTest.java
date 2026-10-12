package br.com.gems.app;

import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Boots the production YAML with isolated PostgreSQL and deterministic Firebase ports. */
@Testcontainers
class OpenApiDisabledStartupIntegrationTest {
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer( "postgres:16-alpine" );

    @Test
    void deveIniciarComDocumentacaoDesabilitada_quandoUsaConfiguracaoPadrao() {
        boot( false );
    }

    @Test
    void deveIgnorarFlagNaoSuportada_quandoAmbientePedeSpringdoc() {
        boot( true );
    }

    @Test
    void deveRecusarHabilitacaoDireta_quandoHandlersNaoTemClassificacao() {
        assertThatThrownBy( () -> boot( false, true ) )
                .hasMessageContaining( "endpoint sem classificação" )
                .hasMessageContaining( "OpenApiWebMvcResource" );
    }

    private void boot( boolean unsupportedFlag ) {
        boot( unsupportedFlag, false );
    }

    private void boot( boolean unsupportedFlag, boolean forceEnable ) {
        var properties = new HashMap<String, Object>();
        properties.put( "DB_URL", DATABASE.getJdbcUrl() );
        properties.put( "DB_USERNAME", DATABASE.getUsername() );
        properties.put( "DB_PASSWORD", DATABASE.getPassword() );
        properties.put( "FIREBASE_PROJECT_ID", "sample-isolated-test" );
        properties.put( "logging.level.root", "WARN" );
        if ( unsupportedFlag ) {
            properties.put( "SPRINGDOC_ENABLED", "true" );
        }
        if ( forceEnable ) {
            properties.put( "springdoc.api-docs.enabled", "true" );
            properties.put( "springdoc.swagger-ui.enabled", "true" );
        }
        var yaml = Path.of( "src/main/resources/application.yml" ).toAbsolutePath().toUri().toString();
        try ( var context = new SpringApplicationBuilder( AppApplication.class,
                FirebaseBackendIntegrationTest.Ports.class )
                .initializers( app -> app.getEnvironment().getPropertySources()
                        .addFirst( new MapPropertySource( "isolated-test-input", properties ) ) )
                .run( "--spring.config.location=" + yaml, "--server.port=0", "--spring.main.banner-mode=off" ) ) {
            assertThat( context.isActive() ).isTrue();
            assertThat( context.getEnvironment().getProperty( "springdoc.api-docs.enabled", Boolean.class ) ).isFalse();
            assertThat( context.getEnvironment().getProperty( "springdoc.swagger-ui.enabled", Boolean.class ) )
                    .isFalse();
            assertThat( context.getEnvironment().getProperty( "spring.jpa.hibernate.ddl-auto" ) )
                    .isEqualTo( "validate" );
            assertThat( context.getBeansOfType( RequestMappingHandlerMapping.class ).values() )
                    .allSatisfy( mappings -> assertThat( mappings.getHandlerMethods().keySet() )
                            .noneMatch( mapping -> mapping.toString().contains( "/v3/api-docs" )
                                    || mapping.toString().contains( "/swagger-ui" ) ) );
        }
    }
}
