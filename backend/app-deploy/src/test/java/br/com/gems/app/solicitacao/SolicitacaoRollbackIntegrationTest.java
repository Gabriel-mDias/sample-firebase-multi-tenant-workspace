package br.com.gems.app.solicitacao;

import br.com.gems.app.model.dto.SolicitacaoDTO;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.entity.solicitacao.EtapaSolicitacao;
import br.com.gems.app.model.entity.solicitacao.Solicitacao;
import br.com.gems.app.model.enums.SituacaoSolicitacao;
import br.com.gems.app.solicitacao.mapper.SolicitacaoMapper;
import br.com.gems.app.solicitacao.service.SolicitacaoService;
import br.com.gems.app.support.AbstractIntegrationTest;
import br.com.gems.utils.ObjectUtil;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Fixtures têm transações próprias; a operação de negócio é chamada somente pelo proxy externo. */
class SolicitacaoRollbackIntegrationTest extends AbstractIntegrationTest {

    private static final String FALHA = "Falha checked após flush da solicitação C03.";

    @Autowired
    private SolicitacaoService service;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private SolicitacaoMapper mapper;

    private final String correlacao = "C03-solicitacao-" + UUID.randomUUID();
    private final List<Registro> registros = new ArrayList<>();
    private UUID parceiroId;
    private UUID solicitacaoId;
    private UUID etapaId;

    @AfterEach
    void limparDadosProprios() {
        jdbc.update( "DELETE FROM auditoria.AUDIT_CHANGE WHERE ID_AUDIT_OPERATION IN "
                + "(SELECT ID_AUDIT_OPERATION FROM auditoria.AUDIT_OPERATION WHERE CD_CORRELATION IN (?, ?))",
                correlacao, correlacao + "-fixture" );
        jdbc.update( "DELETE FROM auditoria.AUDIT_OPERATION WHERE CD_CORRELATION IN (?, ?)", correlacao,
                correlacao + "-fixture" );
        if ( ObjectUtil.isNotNullAndNotEmpty( etapaId ) ) {
            jdbc.update( "DELETE FROM ETAPA_SOLICITACAO WHERE ID_ETAPA_SOLICITACAO = ?", etapaId );
        }
        if ( ObjectUtil.isNotNullAndNotEmpty( solicitacaoId ) ) {
            jdbc.update( "DELETE FROM SOLICITACAO WHERE ID_SOLICITACAO = ?", solicitacaoId );
        }
        registros.forEach( registro -> jdbc
                .update( "DELETE FROM " + registro.tabela() + " WHERE " + registro.chave() + " = ?", registro.id() ) );
        MDC.clear();
    }

    @Test
    void deveReverterSolicitacaoEtapaETrilha_quandoCheckedNoInsert() {
        // Arrange — a etapa é fixture de hook; o insert de produção não cria etapas.
        criarParceiro();
        var baseline = estadoDoParceiro();
        prepararMapeamento( true, true );
        var dto = payload();
        verificarFronteira();

        // Act
        var excecao = assertThrows( Exception.class, () -> service.insert( dto ) );

        // Assert — IDs foram comprovados no flush; estas leituras estão fora da transação encerrada.
        verificarChecked( excecao );
        assertThat( quantidadeFisica( "SOLICITACAO", "ID_SOLICITACAO", solicitacaoId ) )
                .as( "solicitação indevidamente persistida após checked no insert" ).isZero();
        assertThat( quantidadeFisica( "ETAPA_SOLICITACAO", "ID_ETAPA_SOLICITACAO", etapaId ) ).isZero();
        assertThat( trilha( correlacao ) ).isEmpty();
        assertThat( estadoDoParceiro() ).isEqualTo( baseline );
    }

    @Test
    void devePersistirSolicitacaoEtapaETrilha_quandoInsertSemFalha() {
        // Arrange
        criarParceiro();
        var baseline = estadoDoParceiro();
        prepararMapeamento( false, true );
        var dto = payload();

        // Act
        var salva = service.insert( dto );

        // Assert — mesmo hook, sem falha, torna observáveis raiz, etapa e trilha.
        assertThat( salva.getId() ).isEqualTo( solicitacaoId );
        assertThat( quantidadeFisica( "SOLICITACAO", "ID_SOLICITACAO", solicitacaoId ) ).isEqualTo( 1 );
        assertThat( quantidadeFisica( "ETAPA_SOLICITACAO", "ID_ETAPA_SOLICITACAO", etapaId ) ).isEqualTo( 1 );
        assertThat( jdbc.queryForObject( "SELECT CD_SITUACAO FROM SOLICITACAO WHERE ID_SOLICITACAO = ?", String.class,
                solicitacaoId ) ).isEqualTo( "RASCUNHO" );
        assertThat( trilha( correlacao ) ).hasSize( 2 );
        assertThat( trilha( correlacao ) ).extracting( linha -> linha.get( "ds_entity_id" ) )
                .containsExactlyInAnyOrder( solicitacaoId.toString(), etapaId.toString() );
        assertThat( estadoDoParceiro() ).isEqualTo( baseline );
    }

    @Test
    void devePreservarEstadoDataEtapaContadorETrilha_quandoCheckedNaConclusao() {
        // Arrange — solicitação EM_ANDAMENTO com etapa já concluída antes da operação.
        criarParceiro();
        criarSolicitacaoEmAndamento();
        var baseline = estadoDaConclusao();
        prepararMapeamento( true, false );
        verificarFronteira();

        // Act
        var excecao = assertThrows( Exception.class, () -> service.concluir( solicitacaoId ) );

        // Assert — fixture deve continuar existindo, exatamente como antes, incluindo trilha.
        verificarChecked( excecao );
        assertThat( estadoDaConclusao() )
                .as( "estado/data/etapa/contador/trilha não podem sobreviver à conclusão abortada" )
                .isEqualTo( baseline );
        assertThat( trilha( correlacao ) ).isEmpty();
    }

    @Test
    void deveConcluirEIncrementarContadorComTrilha_quandoConclusaoSemFalha() {
        // Arrange
        criarParceiro();
        criarSolicitacaoEmAndamento();
        prepararMapeamento( false, false );

        // Act
        var concluida = service.concluir( solicitacaoId );

        // Assert — o listener BEFORE_COMMIT real deve ter consumido o evento em memória.
        assertThat( concluida.getSituacao() ).isEqualTo( SituacaoSolicitacao.CONCLUIDA );
        assertThat( concluida.getDataConclusao() ).isNotNull();
        assertThat( jdbc.queryForObject( "SELECT CD_SITUACAO FROM SOLICITACAO WHERE ID_SOLICITACAO = ?", String.class,
                solicitacaoId ) ).isEqualTo( "CONCLUIDA" );
        assertThat( jdbc.queryForObject( "SELECT DT_CONCLUSAO FROM SOLICITACAO WHERE ID_SOLICITACAO = ?",
                LocalDateTime.class, solicitacaoId ) ).isNotNull();
        assertThat( jdbc.queryForObject( "SELECT NR_SOLICITACOES_CONCLUIDAS FROM PARCEIRO WHERE ID_PARCEIRO = ?",
                Integer.class, parceiroId ) ).isEqualTo( 1 );
        assertThat( trilha( correlacao ) ).extracting( linha -> linha.get( "ds_entity_id" ) )
                .contains( solicitacaoId.toString(), parceiroId.toString(), etapaId.toString() );
        assertThat( jdbc.queryForObject( "SELECT FL_CONCLUIDA FROM ETAPA_SOLICITACAO WHERE ID_ETAPA_SOLICITACAO = ?",
                Boolean.class, etapaId ) ).isTrue();
    }

    private void prepararMapeamento( boolean falhar, boolean criarEtapa ) {
        MDC.put( "correlationId", correlacao );
        doAnswer( chamada -> {
            var dto = chamada.callRealMethod();
            var solicitacao = chamada.getArgument( 0, Solicitacao.class );
            solicitacaoId = solicitacao.getId();
            if ( criarEtapa ) {
                var etapa = EtapaSolicitacao.builder().solicitacao( solicitacao ).nome( "Etapa fixture de hook C03" )
                        .ordem( 1 ).build();
                entityManager.persist( etapa );
                etapaId = etapa.getId();
            } else {
                // Efeito auxiliar explícito de fixture/hook, para provar também rollback da etapa.
                entityManager.find( EtapaSolicitacao.class, etapaId ).setNome( "Etapa alterada no hook C03" );
            }
            entityManager.flush();
            verificarEscrita( "SOLICITACAO", "ID_SOLICITACAO", solicitacaoId );
            verificarEscrita( "ETAPA_SOLICITACAO", "ID_ETAPA_SOLICITACAO", etapaId );
            assertThat( solicitacao.getSituacao() )
                    .isEqualTo( criarEtapa ? SituacaoSolicitacao.RASCUNHO : SituacaoSolicitacao.CONCLUIDA );
            if ( falhar ) {
                throw new IOException( FALHA );
            }
            return dto;
        } ).when( mapper ).toDTO( any( Solicitacao.class ) );
    }

    private void verificarEscrita( String tabela, String chave, UUID id ) {
        assertThat( id ).isNotNull();
        assertThat( ((Number) entityManager
                .createNativeQuery( "SELECT COUNT(*) FROM " + tabela + " WHERE " + chave + " = :id" )
                .setParameter( "id", id ).getSingleResult()).longValue() ).isEqualTo( 1 );
    }

    private void verificarFronteira() {
        assertThat( AopUtils.isAopProxy( service ) ).isTrue();
        assertThat( TransactionSynchronizationManager.isActualTransactionActive() ).isFalse();
    }

    private void verificarChecked( Exception excecao ) {
        assertThat( excecao ).hasRootCauseInstanceOf( IOException.class ).hasRootCauseMessage( FALHA );
        assertThat( TransactionSynchronizationManager.isActualTransactionActive() ).isFalse();
    }

    private SolicitacaoDTO payload() {
        var dto = new SolicitacaoDTO();
        dto.setTitulo( correlacao );
        dto.setParceiroId( parceiroId );
        return dto;
    }

    private void criarParceiro() {
        MDC.put( "correlationId", correlacao + "-fixture" );
        transactionTemplate.executeWithoutResult( status -> {
            var pais = Pais.builder().nome( correlacao ).build();
            entityManager.persist( pais );
            var pessoa = Pessoa.builder().primeiroNome( "Fixture" ).sobrenome( correlacao ).build();
            entityManager.persist( pessoa );
            var contato = Contato.builder().email( correlacao + "@example.test" ).telefone( "11999999999" ).build();
            entityManager.persist( contato );
            var endereco = Endereco.builder().pais( pais ).cidade( "Cidade C03" ).bairro( "Bairro C03" )
                    .logradouro( "Rua C03" ).build();
            entityManager.persist( endereco );
            var parceiro = Parceiro.builder().nome( correlacao ).pessoa( pessoa ).contato( contato )
                    .endereco( endereco ).build();
            entityManager.persist( parceiro );
            entityManager.flush();
            parceiroId = parceiro.getId();
            registros.addAll( List.of(
                    new Registro( "PARCEIRO", "ID_PARCEIRO", parceiroId ),
                    new Registro( "ENDERECO", "ID_ENDERECO", endereco.getId() ),
                    new Registro( "CONTATO", "ID_CONTATO", contato.getId() ),
                    new Registro( "PESSOA", "ID_PESSOA", pessoa.getId() ),
                    new Registro( "PAIS", "ID_PAIS", pais.getId() ) ) );
        } );
    }

    private void criarSolicitacaoEmAndamento() {
        transactionTemplate.executeWithoutResult( status -> {
            var solicitacao = Solicitacao.builder().titulo( correlacao )
                    .parceiro( entityManager.find( Parceiro.class, parceiroId ) )
                    .situacao( SituacaoSolicitacao.EM_ANDAMENTO ).build();
            entityManager.persist( solicitacao );
            var etapa = EtapaSolicitacao.builder().solicitacao( solicitacao ).nome( "Etapa concluída fixture C03" )
                    .ordem( 1 ).concluida( true ).dataConclusao( LocalDateTime.of( 2026, 1, 1, 12, 0 ) ).build();
            entityManager.persist( etapa );
            entityManager.flush();
            solicitacaoId = solicitacao.getId();
            etapaId = etapa.getId();
        } );
    }

    private List<Object> estadoDoParceiro() {
        return List.of( jdbc.queryForList( "SELECT * FROM PARCEIRO WHERE ID_PARCEIRO = ?", parceiroId ),
                trilha( correlacao + "-fixture" ) );
    }

    private List<Object> estadoDaConclusao() {
        return List.of( jdbc.queryForList( "SELECT * FROM SOLICITACAO WHERE ID_SOLICITACAO = ?", solicitacaoId ),
                jdbc.queryForList( "SELECT * FROM ETAPA_SOLICITACAO WHERE ID_ETAPA_SOLICITACAO = ?", etapaId ),
                estadoDoParceiro(),
                jdbc.queryForList(
                        "SELECT c.* FROM auditoria.AUDIT_CHANGE c JOIN auditoria.AUDIT_OPERATION r ON r.ID_AUDIT_OPERATION = c.ID_AUDIT_OPERATION WHERE r.CD_CORRELATION = ? ORDER BY c.ID_AUDIT_CHANGE",
                        correlacao + "-fixture" ) );
    }

    private List<Map<String, Object>> trilha( String codigo ) {
        return jdbc.queryForList(
                "SELECT * FROM auditoria.AUDIT_OPERATION WHERE CD_CORRELATION = ? ORDER BY ID_AUDIT_OPERATION",
                codigo );
    }

    private long quantidadeFisica( String tabela, String chave, UUID id ) {
        return jdbc.queryForObject( "SELECT COUNT(*) FROM " + tabela + " WHERE " + chave + " = ?", Long.class, id );
    }

    private record Registro(String tabela, String chave, UUID id) {
    }
}
