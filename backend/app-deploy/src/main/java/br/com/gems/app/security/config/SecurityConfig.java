package br.com.gems.app.security.config;

import br.com.gems.firebase.auth.FirebaseAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Uma chain stateless; o filtro SDK tem registration servlet desabilitada. */
@Configuration
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain( HttpSecurity http, FirebaseAuthenticationFilter firebase, @Qualifier("corsConfigurationSource") CorsConfigurationSource cors, @Value("${springdoc.api-docs.enabled:false}") boolean docs ) throws Exception {
        http.csrf( AbstractHttpConfigurer::disable ).cors( config -> config.configurationSource( cors ) )
                .sessionManagement( config -> config.sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
                .exceptionHandling( config -> config
                .authenticationEntryPoint( new HttpStatusEntryPoint( HttpStatus.UNAUTHORIZED ) ) )
                .authorizeHttpRequests( auth -> {
                    auth.requestMatchers( "/api/aws/s3/**" ).denyAll();
                    auth.requestMatchers( HttpMethod.OPTIONS, "/**" ).permitAll();
                    auth.requestMatchers( "/actuator/health", "/actuator/health/**", "/api/demo/public" ).permitAll();
                    if ( docs ) {
                        auth.requestMatchers( "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html" ).permitAll();
                    }
                    auth.anyRequest().authenticated();
                } ).addFilterBefore( firebase, UsernamePasswordAuthenticationFilter.class );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource( @Value("${app.security.cors.allowed-origins:http://localhost:4200}") List<String> origins ) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins( origins );
        config.setAllowedMethods( List.of( "GET", "POST", "PUT", "DELETE", "OPTIONS" ) );
        config.setAllowedHeaders( List.of( "Authorization", "Content-Type", "X-Correlation-Id", "X-Tenant-Alias" ) );
        config.setExposedHeaders( List.of( "X-Correlation-Id" ) );
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration( "/**", config );
        return source;
    }

}
