package br.com.gems.app.security.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Primeiro teste do {@code app-security} — o módulo não tinha nenhum, e a ausência teve preço.
 *
 * <p>
 * O caso do token <b>sem {@code sub}</b> derrubou o registro de autoria na homologação da
 * homologação com {@code NullPointerException}: {@code UUID.fromString(null)} lança NPE, e o
 * {@code catch} existente só cobria {@code IllegalArgumentException}. O token vinha completo de
 * roles — o {@code @PreAuthorize} passava — e faltava apenas a identidade, porque o cliente do
 * realm declarava {@code defaultClientScopes} sem o escopo {@code basic}, que e quem carrega o
 * mapper do {@code sub} para o access token.
 */
class UsuarioAutenticadoServiceTest {

    private final UsuarioAutenticadoService service = new UsuarioAutenticadoService();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwtCom( Map<String, Object> claims ) {
        var builder = Jwt.withTokenValue( "token" )
                .header( "alg", "none" )
                .issuedAt( Instant.now() )
                .expiresAt( Instant.now().plusSeconds( 300 ) );
        claims.forEach( builder::claim );
        return builder.build();
    }

    private void autenticarCom( Jwt jwt ) {
        SecurityContextHolder.getContext()
                .setAuthentication( new JwtAuthenticationToken( jwt, List.of() ) );
    }

    @Test
    @DisplayName("devolve o sub do token quando ele existe")
    void deveComprovarDevolveOSub_quandoExecutaOCenario() {
        var id = UUID.randomUUID();
        autenticarCom( jwtCom( Map.of( "sub", id.toString() ) ) );

        assertThat( service.idDoUsuarioAutenticado() ).contains( id );
    }

    @Test
    @DisplayName("token sem sub devolve vazio, e NÃO estoura NullPointerException")
    void deveComprovarTokenSemSubNaoEstoura_quandoExecutaOCenario() {
        // Exatamente o token da homologação: roles presentes, identidade ausente.
        autenticarCom( jwtCom( Map.of( "realm_access", Map.of( "roles", List.of( "APROVAR_ARQUIVO" ) ) ) ) );

        assertThat( service.idDoUsuarioAutenticado() ).isEmpty();
    }

    @Test
    @DisplayName("sub fora do formato UUID devolve vazio em vez de gravar lixo")
    void deveComprovarSubForaDoFormato_quandoExecutaOCenario() {
        autenticarCom( jwtCom( Map.of( "sub", "nao-e-um-uuid" ) ) );

        assertThat( service.idDoUsuarioAutenticado() ).isEmpty();
    }

    @Test
    @DisplayName("sem autenticacao no contexto devolve vazio")
    void deveComprovarSemAutenticacao_quandoExecutaOCenario() {
        assertThat( service.idDoUsuarioAutenticado() ).isEmpty();
    }

    @Test
    @DisplayName("autenticacao que nao e JWT devolve vazio")
    void deveComprovarAutenticacaoQueNaoEJwt_quandoExecutaOCenario() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken( "alguem", "senha", List.of() ) );

        assertThat( service.idDoUsuarioAutenticado() ).isEmpty();
    }
}
