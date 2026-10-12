package br.com.gems.app;

import br.com.gems.app.support.FirebaseContractSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(FirebaseBackendIntegrationTest.Ports.class)
class IdentityLifecycleContractIntegrationTest extends FirebaseContractSupport {

    @Test
    void deveRecusarAckObsoletoEProcessarDisableNovo_quandoEnableAntigoEstaEmVoo() throws Exception {
        suspendAndDrain();
        users.requestReactivation( subject.id() );
        var command = enableCommand();
        long disableVersion = identityVersion() + 1;
        UUID disableId = UUID.randomUUID();
        var reached = new CountDownLatch( 1 );
        var release = new CountDownLatch( 1 );
        var forwarding = new ForwardingGateway( firebase ) {

            @Override
            public void setDisabled( String uid, boolean disabled ) {
                super.setDisabled( uid, disabled );
                if ( !disabled && uid.equals( subject.uid() ) ) {
                    reached.countDown();
                    try {
                        if ( !release.await( 10, TimeUnit.SECONDS ) ) {
                            throw new IllegalStateException( "O teste excedeu o limite antes do ACK." );
                        }
                    } catch ( InterruptedException failure ) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException( "O teste foi interrompido antes do ACK.", failure );
                    }
                }
            }

        };
        var oldWorker = worker( forwarding );
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var inFlight = pool.submit( oldWorker::runOnce );
            try {
                assertThat( reached.await( 5, TimeUnit.SECONDS ) ).as( "ENABLE remoto concluído antes do ACK." )
                        .isTrue();
                assertThat( firebase.getUser( subject.uid() ).disabled() ).isFalse();
                advanceDurableDisable( command, disableId );
                assertThat( blocked() ).isTrue();
                mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + subject.uid() ) )
                        .andExpect( status().isForbidden() );
            } finally {
                release.countDown();
            }
            assertThat( inFlight.get( 10, TimeUnit.SECONDS ) ).isTrue();
        }

        assertThat( enableState() ).isEqualTo( "OBSOLETE" );
        assertThat( blocked() ).isTrue();
        assertThat( identityVersion() ).isEqualTo( disableVersion );
        var restarted = worker( firebase );
        assertThat( restarted.runOnce() ).isTrue();
        assertThat( jdbc.sql( "SELECT cd_estado FROM security.comando_identidade WHERE " + commandPrimaryKey() + "=:id AND cd_tipo='DISABLE'" )
                .param( "id", disableId ).query( String.class ).single() ).isEqualTo( "ACKED" );
        assertThat( firebase.getUser( subject.uid() ).disabled() ).isTrue();
        assertThat( blocked() ).isTrue();
        assertThat( identityVersion() ).isEqualTo( disableVersion );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + subject.uid() ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void deveAtivarSomenteUsuario_quandoEnableVigenteRecebeAck() throws Exception {
        suspendAndDrain();
        users.requestReactivation( subject.id() );
        closeAuthorizationChains();
        var closed = closedChains();
        assertThat( blocked() ).isTrue();
        assertThat( enableState() ).isEqualTo( "PENDING" );

        assertThat( worker( firebase ).runOnce() ).isTrue();

        assertThat( enableState() ).isEqualTo( "ACKED" );
        assertThat( blocked() ).isFalse();
        assertThat( firebase.getUser( subject.uid() ).disabled() ).isFalse();
        assertThat( closedChains() ).as( "ACK não reabre identidade, membership nem grants." ).isEqualTo( closed );
        mvc.perform( get( "/api/auth/me" ).header( "Authorization", "Bearer " + subject.uid() ) )
                .andExpect( status().isForbidden() );
    }

    @ParameterizedTest
    @ValueSource(strings = { "RETRY", "CLAIMED" })
    void deveRecuperarMesmoComandoSemAtivarAntesDoAck_quandoFalhaRemotaEWorkerReinicia( String state ) {
        suspendAndDrain();
        users.requestReactivation( subject.id() );
        closeAuthorizationChains();
        var closed = closedChains();
        var initial = enableCommand();
        firebase.failure.set( true );

        assertThat( worker( firebase ).runOnce() ).isTrue();

        assertThat( enableState() ).isEqualTo( "RETRY" );
        assertThat( blocked() ).isTrue();
        assertThat( firebase.getUser( subject.uid() ).disabled() ).isTrue();
        assertThat( closedChains() ).isEqualTo( closed );
        firebase.failure.set( false );
        jdbc.sql( "UPDATE security.comando_identidade SET cd_estado=:state,cd_claim=:claim," + "dt_lease=now()-interval '1 minute',dt_proxima_tentativa=now()-interval '1 day' " + "WHERE id_usuario=:id AND cd_tipo='ENABLE'" )
                .param( "state", state ).param( "claim", UUID.randomUUID() ).param( "id", subject.id() ).update();
        var restarted = worker( firebase );
        assertThat( restarted.runOnce() ).isTrue();
        assertThat( enableState() ).isEqualTo( "ACKED" );
        assertThat( blocked() ).isFalse();
        assertThat( firebase.getUser( subject.uid() ).disabled() ).isFalse();
        assertThat( closedChains() ).isEqualTo( closed );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.comando_identidade " + "WHERE id_usuario=:id AND cd_tipo='ENABLE'" ).param( "id", subject.id() )
                .query( Long.class ).single() ).isOne();
        String primaryKey = commandPrimaryKey();
        assertThat( enableCommand().get( primaryKey ) ).isEqualTo( initial.get( primaryKey ) );
        assertThat( restarted.runOnce() ).isFalse();
    }

    private void suspendAndDrain() {
        users.suspend( subject.id() );
        var current = worker( firebase );
        int processed = 0;
        while ( current.runOnce() ) {
            assertThat( ++processed ).as( "Limite de comandos do usuário sintético." ).isLessThan( 10 );
        }
        assertThat( blocked() ).isTrue();
        assertThat( firebase.getUser( subject.uid() ).disabled() ).isTrue();
        assertThat( jdbc.sql( "SELECT cd_elegibilidade FROM security.usuario WHERE id_usuario=:id" )
                .param( "id", subject.id() ).query( String.class ).single() ).isEqualTo( "BOUND" );
        assertThat( jdbc.sql( "SELECT count(*) FROM security.usuario_identidade " + "WHERE id_usuario=:id AND dt_fim IS NULL" ).param( "id", subject.id() )
                .query( Long.class ).single() ).as( "Pré-condição da solicitação de ENABLE." ).isOne();
    }

    private boolean blocked() {
        return jdbc.sql( "SELECT dt_fim IS NOT NULL FROM security.usuario WHERE id_usuario=:id" )
                .param( "id", subject.id() ).query( Boolean.class ).single();
    }

    private long identityVersion() {
        return jdbc.sql( "SELECT nr_versao_identidade FROM security.usuario WHERE id_usuario=:id" )
                .param( "id", subject.id() ).query( Long.class ).single();
    }

    private Map<String, Object> enableCommand() {
        return jdbc.sql( "SELECT * FROM security.comando_identidade WHERE id_usuario=:id AND cd_tipo='ENABLE'" )
                .param( "id", subject.id() ).query().singleRow();
    }

    private String enableState() {
        return (String) enableCommand().get( "cd_estado" );
    }

    private void closeAuthorizationChains() {
        // Encerra a identidade após solicitar ENABLE: o serviço exige identidade aberta para obter o UID.
        assertThat( enableState() ).isEqualTo( "PENDING" );
        for ( String table : List.of( "usuario_identidade", "usuario_tenant", "usuario_role_global" ) ) {
            jdbc.sql( "UPDATE security." + table + " SET dt_fim=dt_inicio WHERE id_usuario=:id AND dt_fim IS NULL" )
                    .param( "id", subject.id() ).update();
        }
        jdbc.sql( "UPDATE security.usuario_tenant_role SET dt_fim=dt_inicio " + "WHERE id_usuario_tenant=:id AND dt_fim IS NULL" ).param( "id", subjectMember ).update();
    }

    private Map<String, String> closedChains() {
        var all = securityState();
        return Map.of( "identities", all.get( "usuario_identidade" ), "memberships", all.get( "usuario_tenant" ), "globalGrants", all.get( "usuario_role_global" ), "tenantGrants", all.get( "usuario_tenant_role" ) );
    }

    private String commandPrimaryKey() {
        return jdbc.sql( "SELECT a.attname FROM pg_index i JOIN pg_attribute a " + "ON a.attrelid=i.indrelid AND a.attnum=ANY(i.indkey) " + "WHERE i.indrelid='security.comando_identidade'::regclass AND i.indisprimary" )
                .query( String.class ).single();
    }

    /** Escritor SQL adversarial contorna o lock do serviço, como no teste CREATE já existente. */
    private void advanceDurableDisable( Map<String, Object> enable, UUID disableId ) {
        String primaryKey = commandPrimaryKey();
        var versionKeys = enable.keySet().stream().filter( key -> key.startsWith( "nr_versao" ) ).toList();
        var idempotencyKeys = enable.keySet().stream().filter( key -> key.contains( "idempot" ) ).toList();
        assertThat( versionKeys ).as( "Versão durável da API publicada." ).hasSize( 1 );
        assertThat( idempotencyKeys ).as( "Chave de idempotência durável." ).hasSize( 1 );
        tx.executeWithoutResult( status -> {
            long version = jdbc.sql( "UPDATE security.usuario SET nr_versao_identidade=nr_versao_identidade+1," + "dt_fim=coalesce(dt_fim,now()) WHERE id_usuario=:id RETURNING nr_versao_identidade" )
                    .param( "id", subject.id() ).query( Long.class ).single();
            assertThat( version ).isGreaterThan( ((Number) enable.get( versionKeys.getFirst() )).longValue() );
            jdbc.sql( "INSERT INTO security.comando_identidade SELECT (jsonb_populate_record(" + "NULL::security.comando_identidade,to_jsonb(c)||jsonb_build_object(" + "CAST(:pk AS text),CAST(:newId AS text),CAST(:versionKey AS text),CAST(:version AS bigint)," + "CAST(:idempotencyKey AS text),CAST(:idempotency AS text),'cd_tipo','DISABLE'," + "'cd_estado','PENDING','cd_claim',NULL,'dt_lease',NULL,'nr_tentativas',0," + "'dt_criacao',now(),'dt_proxima_tentativa',now()))).* " + "FROM security.comando_identidade c WHERE id_usuario=:id AND cd_tipo='ENABLE'" )
                    .param( "pk", primaryKey ).param( "newId", disableId.toString() )
                    .param( "versionKey", versionKeys.getFirst() ).param( "version", version )
                    .param( "idempotencyKey", idempotencyKeys.getFirst() )
                    .param( "idempotency", UUID.randomUUID().toString() ).param( "id", subject.id() ).update();
        } );
    }

}
