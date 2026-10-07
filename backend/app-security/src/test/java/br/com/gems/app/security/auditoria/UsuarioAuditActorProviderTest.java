package br.com.gems.app.security.auditoria;

import br.com.gems.auditing.AuditActorType;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioAuditActorProviderTest {

    private final UsuarioAuditActorProvider provider = new UsuarioAuditActorProvider( new UsuarioAutenticadoService() );

    @AfterEach
    void limparContextos() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void devePreservarNomeIdEstavelECorrelacao_quandoJwtEstaAutenticado() {
        var id = UUID.randomUUID();
        var jwt = Jwt.withTokenValue( "token" )
                .header( "alg", "none" )
                .subject( id.toString() )
                .claim( "preferred_username", "maria.operadora" )
                .issuedAt( Instant.now() )
                .expiresAt( Instant.now().plusSeconds( 300 ) )
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication( new JwtAuthenticationToken( jwt, List.of() ) );
        MDC.put( "correlationId", "corr-42" );

        assertThat( provider.currentActor().name() ).isEqualTo( "maria.operadora" );
        assertThat( provider.currentActor().type() ).isEqualTo( AuditActorType.USUARIO );
        assertThat( provider.currentContext().actorId() ).isEqualTo( id.toString() );
        assertThat( provider.currentContext().correlationId() ).isEqualTo( "corr-42" );
    }

    @Test
    void deveUsarAutorTecnicoSemInventarContexto_quandoNaoHaJwt() {
        assertThat( provider.currentActor().name() ).isEqualTo( "SISTEMA" );
        assertThat( provider.currentActor().type() ).isEqualTo( AuditActorType.TECNICO );
        assertThat( provider.currentContext().actorId() ).isNull();
        assertThat( provider.currentContext().correlationId() ).isNull();
    }
}
