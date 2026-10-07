package br.com.gems.app.auditoria;

import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.enums.OperacaoAuditoria;
import br.com.gems.app.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O sigilo da trilha.
 *
 * <h2>Por que este teste existe separado do mecanismo</h2>
 * A comissão já é omitida na API por perfil, no {@code ParceiroService}. A trilha é uma <b>porta
 * lateral</b> para o mesmo dado: se ela gravasse o valor anterior e o novo, bastaria ter a ação de
 * consultar auditoria para ler o que o sigilo da tela fecha. Fechar a porta da frente e deixar a
 * lateral aberta é pior que não ter fechado nenhuma, porque dá a impressão de proteção.
 *
 * <h2>A asserção é feita contra o BANCO, não contra o objeto</h2>
 * Verificar o DTO provaria apenas que a leitura esconde. O que precisa ser provado é que o valor
 * <b>nunca foi gravado</b> — nem em claro, nem cifrado, nem em nenhuma outra coluna. Por isso a
 * consulta varre {@code DS_VALOR_ANTERIOR} e {@code DS_VALOR_NOVO} procurando o número.
 */
class AuditoriaSigiloIntegrationTest extends AbstractIntegrationTest {

    /** Improváveis de aparecer por acaso — é o que torna a varredura por texto confiável. */
    private static final BigDecimal COMISSAO_INICIAL = new BigDecimal( "13.57" );
    private static final BigDecimal COMISSAO_NOVA = new BigDecimal( "24.68" );

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final String sufixo = "Sigilo-" + UUID.randomUUID();

    @Test
    @DisplayName("o valor da comissão não aparece em nenhuma coluna da trilha")
    void deveComprovarValorDaComissaoNuncaEhGravadoNaTrilha_quandoExecutaOCenario() {
        var parceiroId = transactionTemplate.execute( status -> criarParceiro().getId() );

        transactionTemplate.executeWithoutResult( status -> {
            var parceiro = entityManager.find( Parceiro.class, parceiroId );
            parceiro.setComissao( COMISSAO_NOVA );
        } );

        // 1. A alteração FOI registrada: o sigilo esconde o valor, não o fato.
        var camposDaAlteracao = entityManager.createQuery(
                "SELECT c FROM CampoAlterado c WHERE c.registroAuditoria.entidade = 'Parceiro' "
                        + "AND c.registroAuditoria.registroAfetado = :id "
                        + "AND c.registroAuditoria.operacao = :operacao "
                        + "AND c.campo = 'comissao'",
                br.com.gems.app.model.entity.auditoria.CampoAlterado.class )
                .setParameter( "id", parceiroId )
                .setParameter( "operacao", OperacaoAuditoria.ALTERACAO )
                .getResultList();

        assertThat( camposDaAlteracao )
                .as( "a trilha precisa registrar QUE a comissão mudou" )
                .hasSize( 1 );

        var campo = camposDaAlteracao.getFirst();

        assertThat( campo.isSigiloso() ).isTrue();
        assertThat( campo.getValorAnterior() ).isNull();
        assertThat( campo.getValorNovo() ).isNull();

        // 2. E o número não aparece em lugar nenhum da tabela — nem neste registro, nem em outro.
        var ocorrencias = entityManager.createQuery(
                "SELECT COUNT(c) FROM CampoAlterado c WHERE "
                        + "c.valorAnterior LIKE :inicial OR c.valorNovo LIKE :inicial OR "
                        + "c.valorAnterior LIKE :nova OR c.valorNovo LIKE :nova",
                Long.class )
                .setParameter( "inicial", "%13.57%" )
                .setParameter( "nova", "%24.68%" )
                .getSingleResult();

        assertThat( ocorrencias )
                .as( "nenhum valor de comissão pode existir na trilha, em coluna nenhuma" )
                .isZero();
    }

    // ------------------------------------------------------------------ apoio

    private Parceiro criarParceiro() {
        var brasil = entityManager.createQuery(
                "SELECT p FROM Pais p WHERE p.nome = 'Brasil'", Pais.class )
                .getSingleResult();

        var pessoa = Pessoa.builder()
                .primeiroNome( "Helena" )
                .sobrenome( sufixo )
                .build();

        var contato = Contato.builder()
                .email( "helena@" + sufixo + ".test" )
                .telefone( "31999990000" )
                .build();

        var endereco = Endereco.builder()
                .pais( brasil )
                .cidade( "Belo Horizonte" )
                .bairro( "Savassi" )
                .logradouro( "Rua dos Testes" )
                .build();

        entityManager.persist( pessoa );
        entityManager.persist( contato );
        entityManager.persist( endereco );

        var parceiro = Parceiro.builder()
                .nome( "Parceiro " + sufixo )
                .pessoa( pessoa )
                .contato( contato )
                .endereco( endereco )
                .comissao( COMISSAO_INICIAL )
                .solicitacoesConcluidas( 0 )
                .build();

        entityManager.persist( parceiro );
        entityManager.flush();

        return parceiro;
    }
}
