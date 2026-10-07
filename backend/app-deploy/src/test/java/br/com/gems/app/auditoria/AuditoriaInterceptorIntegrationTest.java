package br.com.gems.app.auditoria;

import br.com.gems.app.auditoria.repository.RegistroAuditoriaRepository;
import br.com.gems.app.model.entity.auditoria.RegistroAuditoria;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.enums.OperacaoAuditoria;
import br.com.gems.app.model.params.AuditoriaFilterParams;
import br.com.gems.app.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O mecanismo da trilha de auditoria, provado contra o banco.
 *
 * <h2>Por que integração, e não teste unitário com Mockito</h2>
 * O que se quer provar aqui é <b>comportamento de persistência</b>: que o interceptador enxerga o
 * flush, que ele participa da mesma transação, e que o soft delete chega nele como remoção. Nenhuma
 * dessas coisas existe num teste com repository mockado — lá o Hibernate nunca roda. É a lição que o
 * projeto já pagou mais de uma vez: os testes unitários de um service com repository mockado
 * passavam enquanto o endereço correspondente respondia 500 em produção.
 *
 * <h2>Por que {@code TransactionTemplate} em vez de {@code @Transactional} na classe</h2>
 * A trilha é gravada no <b>beforeCommit</b>. Um teste transacional nunca commita, então ele não veria
 * registro nenhum — e o teste de rollback seria impossível de escrever, porque a própria
 * anotação já desfaz tudo ao final. Controlar a transação à mão é o que torna os dois observáveis.
 *
 * <h2>Disciplina de fixtures</h2>
 * Todo dado deste teste carrega um sufixo único por execução, e toda asserção é restrita a ele
 * A trilha é uma tabela que todas as outras classes de teste também alimentam, sem querer:
 * asserção ampla aqui quebraria por dado alheio legítimo.
 */
class AuditoriaInterceptorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RegistroAuditoriaRepository repository;

    /** Único por execução — é o que restringe toda asserção ao dado desta classe. */
    private final String sufixo = "Auditoria-" + UUID.randomUUID();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    // ------------------------------------------------------------------ T010

    @Test
    @DisplayName("T010: inserir uma entidade de negócio produz INSERCAO com autor e correlação")
    void deveComprovarInsercaoProduzRegistro_quandoExecutaOCenario() {
        MDC.put( "correlationId", "corr-" + sufixo );
        autenticarComo( UUID.randomUUID(), "maria.operadora" );

        var id = transactionTemplate.execute( status -> gravarPessoa( "Ana" ).getId() );

        var registros = trilhaDe( "Pessoa", id );

        assertThat( registros ).hasSize( 1 );
        assertThat( registros.getFirst().getOperacao() ).isEqualTo( OperacaoAuditoria.INSERCAO );
        assertThat( registros.getFirst().getAutorNome() ).isEqualTo( "maria.operadora" );
        assertThat( registros.getFirst().getCorrelacaoId() ).isEqualTo( "corr-" + sufixo );
        assertThat( registros.getFirst().getDataOperacao() ).isNotNull();
    }

    // ------------------------------------------------------------- T011/T013

    @Test
    @DisplayName("T011/T013: alterar dois campos registra EXATAMENTE dois; o que não mudou fica de fora")
    void deveComprovarAlteracaoRegistraSomenteOsCamposQueMudaram_quandoExecutaOCenario() {
        var id = transactionTemplate.execute( status -> gravarPessoa( "Bruno" ).getId() );

        transactionTemplate.executeWithoutResult( status -> {
            var pessoa = entityManager.find( Pessoa.class, id );
            pessoa.setPrimeiroNome( "Bruna" );
            pessoa.setRg( "MG-1234567" );
            // sobrenome e dataAproximada permanecem como estavam — é o T013.
        } );

        var alteracoes = trilhaDe( "Pessoa", id ).stream()
                .filter( r -> r.getOperacao() == OperacaoAuditoria.ALTERACAO )
                .toList();

        assertThat( alteracoes ).as( "registros ALTERACAO da trilha: %s", alteracoes ).hasSize( 1 );

        var campos = alteracoes.getFirst().getCamposAlterados();

        assertThat( campos ).hasSize( 2 );
        assertThat( campos ).extracting( c -> c.getCampo() )
                .containsExactlyInAnyOrder( "primeiroNome", "rg" );
        assertThat( campos ).filteredOn( c -> "primeiroNome".equals( c.getCampo() ) )
                .singleElement()
                .satisfies( c -> {
                    assertThat( c.getValorAnterior() ).isEqualTo( "Bruno" );
                    assertThat( c.getValorNovo() ).isEqualTo( "Bruna" );
                } );
    }

    // ------------------------------------------------------------------ T012

    @Test
    @DisplayName("T012: exclusão LÓGICA é interceptada como EXCLUSAO")
    void deveComprovarExclusaoLogicaProduzRegistro_quandoExecutaOCenario() {
        var id = transactionTemplate.execute( status -> gravarPessoa( "Carlos" ).getId() );

        transactionTemplate.executeWithoutResult(
                status -> entityManager.remove( entityManager.find( Pessoa.class, id ) ) );

        assertThat( trilhaDe( "Pessoa", id ) )
                .extracting( RegistroAuditoria::getOperacao )
                .contains( OperacaoAuditoria.EXCLUSAO );
    }

    // ------------------------------------------------------------------ rollback

    @Test
    @DisplayName("rollback do dado é rollback da trilha")
    void deveComprovarRollbackNaoDeixaRegistro_quandoExecutaOCenario() {
        var id = UUID.randomUUID();
        MDC.put( "correlationId", "rollback-" + sufixo );

        try {
            transactionTemplate.executeWithoutResult( status -> {
                gravarPessoa( "Daniela" );
                entityManager.flush();
                throw new IllegalStateException( "falha proposital para forçar rollback" );
            } );
        } catch ( IllegalStateException esperado ) {
            // é o ponto do teste
        }

        // Nenhuma Pessoa desta classe sobreviveu, e nenhuma trilha dela tampouco.
        assertThat( trilhaDoSufixo() ).isEmpty();
        assertThat( trilhaDe( "Pessoa", id ) ).isEmpty();
    }

    // ------------------------------------------------------------------ T015

    @Test
    @DisplayName("T015: a trilha não audita a si mesma — não existe trilha da trilha")
    void deveComprovarTrilhaDaTrilhaNaoExiste_quandoExecutaOCenario() {
        transactionTemplate.execute( status -> gravarPessoa( "Eduarda" ).getId() );

        var trilhaDaTrilha = entityManager.createQuery(
                "SELECT COUNT(r) FROM RegistroAuditoria r WHERE r.entidade IN "
                        + "('RegistroAuditoria', 'CampoAlterado')",
                Long.class )
                .getSingleResult();

        assertThat( trilhaDaTrilha )
                .as( "gravar a trilha não pode gerar trilha — seria recursão sem fim" )
                .isZero();
    }

    // ------------------------------------------------------------------ T016

    @Test
    @DisplayName("T016: sem usuário autenticado, o autor é o sistema e o id fica nulo")
    void deveComprovarSemAutenticadoAutorEhSistema_quandoExecutaOCenario() {
        SecurityContextHolder.clearContext();

        var id = transactionTemplate.execute( status -> gravarPessoa( "Fabio" ).getId() );

        var registro = trilhaDe( "Pessoa", id ).getFirst();

        assertThat( registro.getAutorNome() ).isEqualTo( "SISTEMA" );
        assertThat( registro.getAutorId() ).isNull();
    }

    // ------------------------------------------------------------------ T017

    @Test
    @DisplayName("T017: com autenticado, grava o sub em ID_AUTOR e o login em NM_AUTOR")
    void deveComprovarComAutenticadoGravaSubELogin_quandoExecutaOCenario() {
        var sub = UUID.randomUUID();
        autenticarComo( sub, "joao.operador" );

        var id = transactionTemplate.execute( status -> gravarPessoa( "Gabriela" ).getId() );

        var registro = trilhaDe( "Pessoa", id ).getFirst();

        assertThat( registro.getAutorId() ).isEqualTo( sub );
        assertThat( registro.getAutorNome() ).isEqualTo( "joao.operador" );
    }

    @Test
    @DisplayName("filtros preservam id estável e data final inclusiva")
    void deveComprovarFiltrosPorIdEDataInclusiva_quandoExecutaOCenario() {
        var sub = UUID.randomUUID();
        autenticarComo( sub, "filtro.operador" );
        var id = transactionTemplate.execute( status -> gravarPessoa( "Isabela" ).getId() );

        var filtro = new AuditoriaFilterParams();
        filtro.setEntidade( "Pessoa" );
        filtro.setRegistroAfetado( id );
        filtro.setOperacao( "INSERCAO" );
        filtro.setAutorId( sub );
        filtro.setDataInicio( LocalDate.now() );
        filtro.setDataFim( LocalDate.now() );

        assertThat( repository.search( filtro, PageRequest.of( 0, 10 ) ).getContent() )
                .singleElement()
                .satisfies( registro -> assertThat( registro.getRegistroAfetado() ).isEqualTo( id ) );
    }

    // ------------------------------------------------------------------ apoio

    private Pessoa gravarPessoa( String primeiroNome ) {
        var pessoa = Pessoa.builder()
                .primeiroNome( primeiroNome )
                .sobrenome( sufixo )
                .dataAproximada( false )
                .build();

        entityManager.persist( pessoa );
        entityManager.flush();

        return pessoa;
    }

    private List<RegistroAuditoria> trilhaDe( String entidade, UUID registroAfetado ) {
        return entityManager.createQuery(
                "SELECT DISTINCT r FROM RegistroAuditoria r LEFT JOIN FETCH r.camposAlterados "
                        + "WHERE r.entidade = :entidade AND r.registroAfetado = :id "
                        + "ORDER BY r.dataOperacao",
                RegistroAuditoria.class )
                .setParameter( "entidade", entidade )
                .setParameter( "id", registroAfetado )
                .getResultList();
    }

    /** Toda a trilha tentada pela transação revertida — restrita pela correlação única. */
    private List<RegistroAuditoria> trilhaDoSufixo() {
        return entityManager.createQuery(
                "SELECT r FROM RegistroAuditoria r WHERE r.correlacaoId = :correlacao",
                RegistroAuditoria.class )
                .setParameter( "correlacao", "rollback-" + sufixo )
                .getResultList();
    }

    private void autenticarComo( UUID sub, String login ) {
        var jwt = Jwt.withTokenValue( "token-de-teste" )
                .header( "alg", "none" )
                .subject( sub.toString() )
                .claim( "preferred_username", login )
                .issuedAt( Instant.now() )
                .expiresAt( Instant.now().plusSeconds( 300 ) )
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication( new TestingAuthenticationToken( jwt, null, List.of() ) );
    }
}
