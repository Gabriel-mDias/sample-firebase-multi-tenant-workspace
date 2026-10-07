package br.com.gems.app.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.keycloak.client-id}")
    private String keycloakClientId;

    /**
     * Origens liberadas para requisições cross-origin (SPA Angular). Parametrizável por ambiente —
     * em produção deve conter apenas o domínio do frontend; nunca use {@code *} com credenciais.
     */
    @Value("${app.security.cors.allowed-origins:http://localhost:4200}")
    private List<String> corsAllowedOrigins;

    @Bean
    public SecurityFilterChain filterChain( HttpSecurity http ) throws Exception {
        http.csrf( AbstractHttpConfigurer::disable );

        // Habilita CORS na cadeia do Security (senão o preflight é barrado antes dos headers CORS
        // serem escritos). Usa o CorsConfigurationSource declarado abaixo.
        http.cors( cors -> cors.configurationSource( corsConfigurationSource() ) );

        http.authorizeHttpRequests( auth -> {
            // Preflight
            auth.requestMatchers( HttpMethod.OPTIONS, "/**" ).permitAll();

            // Actuator: SOMENTE o health é anônimo.
            //
            // Antes desta rodada o padrão era `/actuator/**` com permitAll(), e a exposição default
            // do application.yml incluía `metrics`. Publicado na internet, isso entrega
            // /actuator/metrics/http.server.requests a qualquer um — e esse endpoint ENUMERA todos
            // os URIs da API, com contagem e latência por rota. É reconhecimento gratuito para quem
            // não tem nem token.
            //
            // Reduzir ACTUATOR_EXPOSURE resolveria o sintoma, mas dependeria de o .env estar certo
            // em todo ambiente, para sempre. Aqui a rota passa a exigir autenticação, e a redução da
            // exposição fica como segunda camada — se alguém reexpuser `metrics` por variável de
            // ambiente, ainda assim é preciso um token para chegar nele.
            //
            // O health continua anônimo por dois motivos concretos: o HEALTHCHECK do
            // backend/Dockerfile bate nele de dentro do contêiner, e `show-details` está no default
            // `never`, então a resposta é {"status":"UP"} — não vaza estado de banco nem de disco.
            auth.requestMatchers( "/actuator/health", "/actuator/health/**" ).permitAll();

            // Bloqueado deliberadamente. O S3Controller do gems-aws-web está publicado neste
            // classpath (app-deploy depende de gems-aws-web) mas não tem nenhum @PreAuthorize, e
            // assina URL para QUALQUER chave do bucket, sem saber a quem o arquivo pertence —
            // inclusive delete-file, que é exclusão física via s3Client.deleteObject, sem
            // versionamento e irreversível. Como a anotação teria de estar na classe da SDK (jar de
            // terceiro), o bloqueio só pode vir daqui. É denyAll(), não hasRole('ADMIN'): nem um
            // ADMIN deveria ter acesso a uma porta que ignora a regra de propriedade do arquivo. O
            // acesso ao S3 é feito pelos endpoints do próprio domínio, que validam
            // a quem o arquivo pertence antes de assinar.
            //
            // A POSIÇÃO NÃO É ESTÉTICA: no Spring Security o primeiro matcher que casa decide.
            // Por isso o denyAll vem antes do laço de endpoints públicos, e não apenas antes do
            // anyRequest() — assim nem um @PublicEndpoint colocado por engano numa rota de S3
            // conseguiria reabrir a porta.
            auth.requestMatchers( "/api/aws/s3/**" ).denyAll();

            auth.requestMatchers( "/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" )
                    .permitAll();

            auth.anyRequest().authenticated();
        } );

        http.sessionManagement( session -> session
                .sessionCreationPolicy( org.springframework.security.config.http.SessionCreationPolicy.STATELESS ) );

        http.exceptionHandling( exceptions -> exceptions
                .authenticationEntryPoint( new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                        org.springframework.http.HttpStatus.UNAUTHORIZED ) ) );

        http.oauth2ResourceServer( oauth2 -> oauth2
                .jwt( jwt -> jwt.jwtAuthenticationConverter( jwtAuthenticationConverter() ) ) );

        return http.build();
    }

    /**
     * Configuração de CORS para a SPA. Expõe o header de correlação para o cliente e permite os
     * cabeçalhos/métodos usados pelo frontend (incluindo o preflight OPTIONS). As origens vêm de
     * {@code app.security.cors.allowed-origins} (default: localhost:4200 para dev).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins( corsAllowedOrigins );
        config.setAllowedMethods( List.of( "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS" ) );
        config.setAllowedHeaders( List.of( "Authorization", "Content-Type", "X-Correlation-Id" ) );
        config.setExposedHeaders( List.of( "X-Correlation-Id" ) );
        // NÃO habilitamos allowCredentials: a SPA autentica por `Authorization: Bearer`, que é um
        // header comum liberado logo acima — não é "credencial" no sentido do CORS (cookie, TLS
        // client cert, HTTP auth). Verificado: zero ocorrências de `withCredentials` no
        // frontend e na SDK. Deixá-lo ligado não fazia nada hoje e viraria falha real no dia em que
        // alguém trocasse a lista de origens por um curinga.
        config.setMaxAge( 3600L );

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration( "/**", config );
        return source;
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            var authorities = extractAuthorities( jwt );
            return new JwtAuthenticationToken( jwt, authorities );
        };
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> extractAuthorities( Jwt jwt ) {
        var authorities = new ArrayList<SimpleGrantedAuthority>();

        // 1. Realm Roles
        if ( jwt.hasClaim( "realm_access" ) ) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap( "realm_access" );
            if ( realmAccess != null && realmAccess.containsKey( "roles" ) ) {
                var roles = (List<String>) realmAccess.get( "roles" );
                roles.forEach( role -> authorities.add( new SimpleGrantedAuthority( "ROLE_" + role.toUpperCase() ) ) );
            }
        }

        // 2. Resource Roles (Client Roles dinâmicos do Keycloak)
        if ( jwt.hasClaim( "resource_access" ) ) {
            Map<String, Object> resourceAccess = jwt.getClaimAsMap( "resource_access" );
            String clientId = keycloakClientId;
            if ( resourceAccess != null && resourceAccess.containsKey( clientId ) ) {
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get( clientId );
                if ( clientAccess != null && clientAccess.containsKey( "roles" ) ) {
                    var roles = (List<String>) clientAccess.get( "roles" );
                    roles.forEach(
                            role -> authorities.add( new SimpleGrantedAuthority( "ROLE_" + role.toUpperCase() ) ) );
                }
            }
        }

        return authorities;
    }
}
