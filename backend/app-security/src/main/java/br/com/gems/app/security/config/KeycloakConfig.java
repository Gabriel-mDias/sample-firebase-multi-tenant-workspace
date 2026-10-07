package br.com.gems.app.security.config;

import br.com.gems.keycloak.admin.KeycloakAdminProperties;
import br.com.gems.keycloak.admin.KeycloakAdminRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class KeycloakConfig {

    @Bean
    public KeycloakAdminProperties keycloakAdminProperties(
            @Value("${app.security.keycloak.server-url}") String baseUrl,
            @Value("${app.security.keycloak.realm}") String realm,
            @Value("${app.security.keycloak.client-id}") String clientId,
            @Value("${app.security.keycloak.client-secret}") String clientSecret,
            @Value("${app.security.keycloak.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.security.keycloak.read-timeout:10s}") Duration readTimeout ) {
        return new KeycloakAdminProperties(
                baseUrl, realm, clientId, clientSecret, connectTimeout, readTimeout );
    }

    @Bean
    public KeycloakAdminRestClient keycloakAdminRestClient( KeycloakAdminProperties properties ) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout( properties.connectTimeout() )
                .build();
        var requestFactory = new JdkClientHttpRequestFactory( httpClient );
        requestFactory.setReadTimeout( properties.readTimeout() );
        var restClient = RestClient.builder()
                .baseUrl( properties.baseUrl() )
                .requestFactory( requestFactory )
                .build();
        return new KeycloakAdminRestClient( restClient, properties );
    }
}
