package br.com.gems.app;

import br.com.gems.app.support.FirebaseContractSupport;
import br.com.gems.firebase.auth.FirebaseAuthException;
import br.com.gems.firebase.auth.VerifiedIdentity;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(FirebaseBackendIntegrationTest.Ports.class)
class IdentityContractIntegrationTest extends FirebaseContractSupport {
    @ParameterizedTest
    @ValueSource(strings = {"cd_sujeito_externo", "cd_emissor", "id_provedor_identidade", "id_usuario"})
    void deveRecusarUpdateDiretoEPreservarLinha_quandoChaveOuDonoDaIdentidadeMuda( String column ) {
        String before = identityRow( subject.id() );
        Object replacement = switch ( column ) {
            case "cd_sujeito_externo" -> unique( "uidnovo" );
            case "cd_emissor" -> "https://securetoken.google.com/outro-projeto";
            case "id_provedor_identidade" -> UUID.randomUUID();
            case "id_usuario" -> operator.id();
            default -> throw new IllegalArgumentException( "Coluna fora do contrato." );
        };

        assertThatThrownBy( () -> jdbc.sql( "UPDATE security.usuario_identidade SET " + column
                + "=:value WHERE id_usuario=:id" ).param( "value", replacement ).param( "id", subject.id() ).update() )
                .isInstanceOf( DataIntegrityViolationException.class )
                .satisfies( failure -> assertThat( ((SQLException) ((DataIntegrityViolationException) failure)
                        .getMostSpecificCause()).getSQLState() )
                        .as( "Imutabilidade 23514; rejeição por FK 23503 não atende o contrato." )
                        .isEqualTo( "23514" ) );

        assertThat( identityRow( subject.id() ) ).isEqualTo( before );
    }

    @Test
    void deveRecusarEmailAmbiguoSemVinculo_quandoDoisPendingSaoElegiveis() {
        String email = unique( "ambiguo" ) + "@example.test";
        UUID first = users.preProvisionGoogle( "Primeiro", email );
        UUID second = users.preProvisionGoogle( "Segundo", unique( "segundo" ) + "@example.test" );
        jdbc.sql( "UPDATE security.usuario SET cd_email=:email WHERE id_usuario=:id" )
                .param( "email", email ).param( "id", second ).update();
        var identity = identity( unique( "google" ), email );
        var before = securityState();

        assertRefused( identity );

        assertThat( identities( first ) ).isZero();
        assertThat( identities( second ) ).isZero();
        assertThat( securityState() ).as( "Sem usuário bound, identidade ou evento parcial." ).isEqualTo( before );
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASSWORD_PROVISIONING", "BOUND", "SUSPENDED"})
    void deveRecusarNovoUidSemVinculo_quandoUsuarioNaoEElegivelParaGoogle( String state ) {
        String email = unique( "inelegivel" ) + "@example.test";
        UUID id;
        if ( state.equals( "PASSWORD_PROVISIONING" ) ) {
            id = users.provisionPassword( "Senha pendente", email );
        } else if ( state.equals( "BOUND" ) ) {
            id = subject.id();
            email = subject.email();
        } else {
            id = users.preProvisionGoogle( "Encerrado", email );
            users.suspend( id );
        }
        var candidate = identity( unique( "novouid" ), email );
        long count = identities( id );
        var before = securityState();

        assertRefused( candidate );

        assertThat( identities( id ) ).isEqualTo( count );
        assertThat( securityState() ).isEqualTo( before );
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROJECT", "ISSUER", "BOTH"})
    void deveRecusarIdentidadeSemMutacao_quandoProjetoOuEmissorDiverge( String mismatch ) {
        String email = unique( "emissor" ) + "@example.test";
        UUID pending = users.preProvisionGoogle( "Pendente", email );
        String project = mismatch.equals( "ISSUER" ) ? PROJECT : "outro-projeto";
        String issuer = mismatch.equals( "PROJECT" ) ? ISSUER : "https://securetoken.google.com/outro-projeto";
        String uid = unique( "externo" );
        var before = securityState();

        var failure = assertThrows( FirebaseAuthException.class, () -> {
            var candidate = new VerifiedIdentity( project, issuer, uid, email, true,
                    "google.com", "Pendente", clock.instant() );
            resolver.resolve( candidate, Optional.empty(), clock.instant() );
        } );

        assertThat( failure.reason() ).as( "Projeto/emissor incoerentes são recusados já no construtor." )
                .isEqualTo( mismatch.equals( "BOTH" )
                        ? FirebaseAuthException.Reason.LOCAL_DENIED
                        : FirebaseAuthException.Reason.INVALID_TOKEN );
        assertThat( identities( pending ) ).isZero();
        assertThat( jdbc.sql( "SELECT count(*) FROM security.evento_seguranca "
                + "WHERE id_usuario=:id AND cd_evento='GOOGLE_IDENTITY_LINKED'" )
                .param( "id", pending ).query( Long.class ).single() ).isZero();
        assertThat( securityState() ).isEqualTo( before );
    }

    @Test
    void deveManterHistoricoSemFallback_quandoUidEncerradoTemOutroPendingComMesmoEmail() {
        jdbc.sql( "UPDATE security.usuario_identidade SET dt_fim=dt_inicio WHERE id_usuario=:id" )
                .param( "id", subject.id() ).update();
        UUID pending = users.preProvisionGoogle( "Outro proprietário", unique( "historico" ) + "@example.test" );
        jdbc.sql( "UPDATE security.usuario SET cd_email=:email WHERE id_usuario=:id" )
                .param( "email", subject.email() ).param( "id", pending ).update();
        var before = securityState();

        assertRefused( firebase.identities.get( subject.uid() ) );

        assertThat( identities( pending ) ).isZero();
        assertThat( securityState() ).isEqualTo( before );
        assertThatThrownBy( () -> jdbc.sql( "INSERT INTO security.usuario_identidade SELECT :newId,:newUser,"
                + "id_provedor_identidade,cd_emissor,cd_sujeito_externo,cd_email_identidade,dt_inicio,NULL "
                + "FROM security.usuario_identidade WHERE id_usuario=:owner" )
                .param( "newId", UUID.randomUUID() ).param( "newUser", pending )
                .param( "owner", subject.id() ).update() )
                .isInstanceOf( DataIntegrityViolationException.class )
                .satisfies( failure -> assertThat( ((SQLException) ((DataIntegrityViolationException) failure)
                        .getMostSpecificCause()).getSQLState() ).isEqualTo( "23505" ) );
    }

    @Test
    void deveEsperarRowlockEVincularUmaVez_quandoDoisRequestsResolvemMesmoPending() throws Exception {
        String email = unique( "rowlock" ) + "@example.test";
        UUID pending = users.preProvisionGoogle( "Concorrente", email );
        var candidate = identity( unique( "concorrente" ), email );
        var started = new CountDownLatch( 2 );
        var beforeEvents = securityEvents();
        try ( var connection = source.getConnection(); var pool = Executors.newVirtualThreadPerTaskExecutor() ) {
            connection.setAutoCommit( false );
            try ( var lock = connection.prepareStatement(
                    "SELECT id_usuario FROM security.usuario WHERE id_usuario=? FOR UPDATE" ) ) {
                lock.setObject( 1, pending );
                try ( var rows = lock.executeQuery() ) {
                    assertThat( rows.next() ).isTrue();
                }
            }
            var first = pool.submit( () -> {
                started.countDown();
                return resolver.resolve( candidate, Optional.empty(), clock.instant() ).principal().userId();
            } );
            var second = pool.submit( () -> {
                started.countDown();
                return resolver.resolve( candidate, Optional.empty(), clock.instant() ).principal().userId();
            } );
            try {
                assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();
                assertThatThrownBy( () -> first.get( 200, TimeUnit.MILLISECONDS ) )
                        .isInstanceOf( TimeoutException.class );
                assertThatThrownBy( () -> second.get( 200, TimeUnit.MILLISECONDS ) )
                        .isInstanceOf( TimeoutException.class );
                assertThat( identities( pending ) ).isZero();
            } finally {
                connection.commit();
            }
            assertThat( first.get( 10, TimeUnit.SECONDS ) ).isEqualTo( pending );
            assertThat( second.get( 10, TimeUnit.SECONDS ) ).isEqualTo( pending );
        }

        assertThat( identities( pending ) ).isOne();
        var newEvents = securityEvents().stream().filter( event -> !beforeEvents.contains( event ) )
                .filter( event -> pending.equals( event.get( "id_usuario" ) ) ).toList();
        assertThat( newEvents ).isNotEmpty();
        assertThat( newEvents ).extracting( event -> event.get( "cd_evento" ) ).doesNotHaveDuplicates();
    }

    private void assertRefused( VerifiedIdentity candidate ) {
        assertThatThrownBy( () -> resolver.resolve( candidate, Optional.empty(), clock.instant() ) )
                .isInstanceOf( FirebaseAuthException.class );
    }

    private String identityRow( UUID user ) {
        return jdbc.sql( "SELECT to_jsonb(i)::text FROM security.usuario_identidade i WHERE id_usuario=:id" )
                .param( "id", user ).query( String.class ).single();
    }
}
