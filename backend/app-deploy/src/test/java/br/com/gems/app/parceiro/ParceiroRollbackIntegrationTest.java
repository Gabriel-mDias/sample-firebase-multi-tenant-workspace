package br.com.gems.app.parceiro;

import br.com.gems.app.localidade.service.LocalidadeService;
import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.dto.PessoaDTO;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.parceiro.mapper.ParceiroMapper;
import br.com.gems.app.parceiro.service.ParceiroService;
import br.com.gems.app.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Prova atomicidade do cadastro agregado, sem transação envolvendo a chamada do service. */
class ParceiroRollbackIntegrationTest extends AbstractIntegrationTest {

    private static final String FALHA = "Falha checked após flush do parceiro C03.";

    @Autowired
    private ParceiroService service;
    @Autowired
    private LocalidadeService localidadeService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private ParceiroMapper mapper;

    private final String correlacao = "C03-parceiro-" + UUID.randomUUID();
    private List<Registro> registros = List.of();

    @AfterEach
    void limparDadosProprios() {
        jdbc.update( "DELETE FROM auditoria.AUDIT_CHANGE WHERE ID_AUDIT_OPERATION IN "
                + "(SELECT ID_AUDIT_OPERATION FROM auditoria.AUDIT_OPERATION WHERE CD_CORRELATION = ?)",
                correlacao );
        jdbc.update( "DELETE FROM auditoria.AUDIT_OPERATION WHERE CD_CORRELATION = ?", correlacao );
        registros.forEach( registro -> jdbc
                .update( "DELETE FROM " + registro.tabela() + " WHERE " + registro.chave() + " = ?", registro.id() ) );
        MDC.clear();
    }

    @Test
    void deveReverterRaizFilhosContadorETrilha_quandoCheckedAposEscrita() {
        // Arrange
        var dto = payload();
        prepararMapeamento( true );
        assertThat( AopUtils.isAopProxy( service ) ).isTrue();
        assertThat( TransactionSynchronizationManager.isActualTransactionActive() ).isFalse();

        // Act
        var excecao = assertThrows( Exception.class, () -> service.insert( dto ) );

        // Assert — SQL físico novo, sem filtro de soft delete nem contexto JPA reutilizado.
        assertThat( excecao ).hasRootCauseInstanceOf( IOException.class ).hasRootCauseMessage( FALHA );
        assertThat( TransactionSynchronizationManager.isActualTransactionActive() ).isFalse();
        assertThat( registros ).hasSize( 4 );
        registros.forEach( registro -> {
            assertThat( quantidadeFisica( registro ) )
                    .as( "rollback físico de %s por id %s", registro.tabela(), registro.id() ).isZero();
            assertThat( quantidadeTrilha( registro.id() ) ).isZero();
        } );
        assertThat( jdbc.queryForObject( "SELECT COUNT(*) FROM auditoria.AUDIT_OPERATION WHERE CD_CORRELATION = ?",
                Long.class, correlacao ) ).isZero();
    }

    @Test
    void devePersistirRaizFilhosContadorETrilha_quandoSemFalha() {
        // Arrange
        var dto = payload();
        prepararMapeamento( false );

        // Act
        var salvo = service.insert( dto );

        // Assert — o mesmo flush e as mesmas consultas precisam observar o commit positivo.
        assertThat( salvo.getId() ).isEqualTo( registros.getFirst().id() );
        assertThat( registros ).hasSize( 4 );
        registros.forEach( registro -> {
            assertThat( quantidadeFisica( registro ) ).isEqualTo( 1 );
            assertThat( quantidadeTrilha( registro.id() ) ).isEqualTo( 1 );
        } );
        assertThat( jdbc.queryForObject( "SELECT NR_SOLICITACOES_CONCLUIDAS FROM PARCEIRO WHERE ID_PARCEIRO = ?",
                Integer.class, salvo.getId() ) ).isZero();
        assertThat( salvo.getPessoa().getId() ).isEqualTo( registros.get( 1 ).id() );
        assertThat( salvo.getContato().getId() ).isEqualTo( registros.get( 2 ).id() );
        assertThat( salvo.getEndereco().getId() ).isEqualTo( registros.get( 3 ).id() );
    }

    private void prepararMapeamento( boolean falhar ) {
        MDC.put( "correlationId", correlacao );
        doAnswer( chamada -> {
            var dto = chamada.callRealMethod();
            var parceiro = chamada.getArgument( 0, Parceiro.class );
            entityManager.flush();
            registros = List.of(
                    new Registro( "PARCEIRO", "ID_PARCEIRO", parceiro.getId() ),
                    new Registro( "PESSOA", "ID_PESSOA", parceiro.getPessoa().getId() ),
                    new Registro( "CONTATO", "ID_CONTATO", parceiro.getContato().getId() ),
                    new Registro( "ENDERECO", "ID_ENDERECO", parceiro.getEndereco().getId() ) );
            registros.forEach( registro -> {
                assertThat( registro.id() ).isNotNull();
                assertThat( ((Number) entityManager
                        .createNativeQuery(
                                "SELECT COUNT(*) FROM " + registro.tabela() + " WHERE " + registro.chave() + " = :id" )
                        .setParameter( "id", registro.id() ).getSingleResult()).longValue() ).isEqualTo( 1 );
            } );
            if ( falhar ) {
                // Answer lança checked diretamente; nenhum wrapper runtime marca rollback antes do interceptor.
                throw new IOException( FALHA );
            }
            return dto;
        } ).when( mapper ).toDTO( any( Parceiro.class ) );
    }

    private ParceiroDTO payload() {
        var pessoa = new PessoaDTO();
        pessoa.setPrimeiroNome( "Parceiro" );
        pessoa.setSobrenome( correlacao );
        pessoa.setDataNascimento( LocalDate.of( 1990, 1, 1 ) );
        pessoa.setCpf( String.format( "%011d", Math.floorMod( System.nanoTime(), 100000000000L ) ) );
        var contato = new ContatoDTO();
        contato.setEmail( correlacao + "@example.test" );
        contato.setTelefone( "11999999999" );
        var endereco = new EnderecoDTO();
        endereco.setPais( localidadeService.listarPaises().stream()
                .filter( pais -> "Brasil".equalsIgnoreCase( pais.getNome() ) ).findFirst().orElseThrow() );
        endereco.setEstado( localidadeService.listarEstadosPorPais( endereco.getPais().getId() ).getFirst() );
        endereco.setCep( "01001000" );
        endereco.setCidade( "Cidade C03" );
        endereco.setBairro( "Bairro C03" );
        endereco.setLogradouro( "Rua C03" );
        var dto = new ParceiroDTO();
        dto.setNome( correlacao );
        dto.setPessoa( pessoa );
        dto.setContato( contato );
        dto.setEndereco( endereco );
        return dto;
    }

    private long quantidadeFisica( Registro registro ) {
        return jdbc.queryForObject( "SELECT COUNT(*) FROM " + registro.tabela() + " WHERE " + registro.chave() + " = ?",
                Long.class, registro.id() );
    }

    private long quantidadeTrilha( UUID id ) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM auditoria.AUDIT_OPERATION WHERE DS_ENTITY_ID = ? AND CD_CORRELATION = ?",
                Long.class, id.toString(), correlacao );
    }

    private record Registro(String tabela, String chave, UUID id) {
    }
}
