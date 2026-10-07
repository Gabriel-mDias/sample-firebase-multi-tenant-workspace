package br.com.gems.app.auditoria.interceptor;

import br.com.gems.auditing.AuditPolicy;
import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.app.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A varredura de cobertura da trilha de auditoria.
 *
 * <h2>Ela parte do metamodelo JPA, nunca de uma lista escrita à mão</h2>
 * Este é o ponto inteiro da fase, e o projeto já pagou por errá-lo: o
 * {@code routes-coverage.spec.ts} foi escrito para pegar rota sem autorização declarada, enumerando
 * as tabelas de rota <b>à mão</b> — e ignorou uma feature inteira, porque a tabela dela nunca entrou
 * na lista. A rede montada para pegar o esquecimento foi vítima do mesmo esquecimento.
 *
 * <p>
 * Aqui a fonte é o {@code Metamodel} do Hibernate: tudo que é entidade mapeada aparece, queira o
 * autor ou não. Uma entidade criada em qualquer rodada futura entra nesta varredura no instante em
 * que for mapeada, sem que ninguém precise lembrar de nada.
 *
 * <h2>O que a varredura prova, e o que ela deliberadamente não tenta provar</h2>
 * Ela prova que <b>toda entidade de negócio está dentro da fronteira</b> do interceptador e que
 * <b>nenhuma delas tem chave que o mecanismo não saiba registrar</b>. Ela não persiste cada
 * entidade para observar os três registros de todas elas: isso exigiria montar o grafo de chaves
 * estrangeiras de todas elas, e o teste ficaria caro e quebradiço sem cobrir risco novo — as três
 * operações compartilham a <b>mesma</b> guarda, e o caminho de cada uma já é provado ponta a ponta
 * em {@code AuditoriaInterceptorIntegrationTest}.
 */
class AuditoriaCoberturaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /** As duas tabelas puramente referenciais, que o Princípio II isenta de soft delete. */
    private static final List<String> REFERENCIAIS = List.of( "Pais", "Estado" );

    /** As duas da própria trilha, que não estendem a base para não auditar a si mesmas. */
    private static final List<String> DA_TRILHA = List.of( "RegistroAuditoria", "CampoAlterado" );

    @Test
    @DisplayName("toda entidade de negócio mapeada está coberta pela trilha")
    void deveComprovarTodaEntidadeDeNegocioEstaCoberta_quandoExecutaOCenario() {
        var descobertas = entidadesDeNegocio().stream()
                .filter( tipo -> !AuditPolicy.isAuditable( tipo ) )
                .map( Class::getSimpleName )
                .toList();

        assertThat( descobertas )
                .as( "entidade de negócio fora da cobertura da auditoria — ela estende "
                        + "EntidadeAuditavel mas o interceptador a recusa" )
                .isEmpty();

        // A varredura só vale alguma coisa se de fato enxergar as entidades. Um metamodelo vazio
        // faria o teste acima passar sem examinar nada. O piso é ordem de grandeza, não contagem:
        // suba-o quando o projeto crescer.
        assertThat( entidadesDeNegocio() )
                .as( "a varredura precisa enxergar as entidades de negócio do projeto" )
                .hasSizeGreaterThanOrEqualTo( 5 );
    }

    @Test
    @DisplayName("SC-140: nenhuma entidade de negócio tem chave que o mecanismo não saiba registrar")
    void deveComprovarTodaEntidadeDeNegocioTemChaveUuid_quandoExecutaOCenario() {
        var forasteiras = entidadesDeNegocio().stream()
                .filter( tipo -> !UUID.class.equals( tipoDaChave( tipo ) ) )
                .map( tipo -> tipo.getSimpleName() + " (" + tipoDaChave( tipo ) + ")" )
                .toList();

        assertThat( forasteiras )
                .as( "o interceptador descarta silenciosamente operação cuja chave não é UUID. "
                        + "Uma entidade assim seria persistida SEM trilha, e sem erro nenhum — é o "
                        + "modo de falha mais perigoso que esta rodada tem" )
                .isEmpty();
    }

    @Test
    @DisplayName("A trilha e as tabelas referenciais ficam FORA da cobertura, e isso é deliberado")
    void deveComprovarOQueFicaDeForaContinuaDeFora_quandoExecutaOCenario() {
        var mapeadas = entityManagerFactory.getMetamodel().getEntities().stream()
                .map( EntityType::getJavaType )
                .collect( Collectors.toMap( Class::getSimpleName, tipo -> tipo, ( a, b ) -> a ) );

        for ( var referencial : REFERENCIAIS ) {
            assertThat( mapeadas ).containsKey( referencial );
            assertThat( AuditPolicy.isAuditable( mapeadas.get( referencial ) ) )
                    .as( "%s é tabela puramente referencial: auditá-la encheria a trilha com a "
                            + "carga de seed", referencial )
                    .isFalse();
        }

        for ( var daTrilha : DA_TRILHA ) {
            assertThat( mapeadas ).containsKey( daTrilha );
            assertThat( AuditPolicy.isAuditable( mapeadas.get( daTrilha ) ) )
                    .as( "auditar %s seria recursão sem fim", daTrilha )
                    .isFalse();
        }
    }

    @Test
    @DisplayName("nenhuma entidade mapeada escapa sem classificação")
    void deveComprovarNenhumaEntidadeMapeadaFicaSemClassificacao_quandoExecutaOCenario() {
        var semClassificacao = entityManagerFactory.getMetamodel().getEntities().stream()
                .map( EntityType::getJavaType )
                .filter( tipo -> !EntidadeAuditavel.class.isAssignableFrom( tipo ) )
                .filter( tipo -> !REFERENCIAIS.contains( tipo.getSimpleName() ) )
                .filter( tipo -> !DA_TRILHA.contains( tipo.getSimpleName() ) )
                .map( Class::getSimpleName )
                .toList();

        assertThat( semClassificacao )
                .as( """
                        Entidade mapeada que não é nem de negócio, nem referencial, nem da trilha.

                        Este é o buraco que os outros testes desta classe NÃO pegam: eles partem das \
                        entidades que estendem EntidadeAuditavel, então uma entidade nova que \
                        simplesmente não estenda a base sai do universo examinado e passa \
                        despercebida — sem auditoria e sem soft delete.

                        Se a entidade é de negócio, faça-a estender EntidadeAuditavel. Se é \
                        puramente referencial, acrescente-a a REFERENCIAIS aqui, de forma \
                        deliberada e revisável.""" )
                .isEmpty();
    }

    // ------------------------------------------------------------------ apoio

    /** A fonte da verdade: o metamodelo, filtrado pela fronteira do interceptador. */
    private List<Class<?>> entidadesDeNegocio() {
        return entityManagerFactory.getMetamodel().getEntities().stream()
                .map( EntityType::getJavaType )
                .filter( EntidadeAuditavel.class::isAssignableFrom )
                .sorted( java.util.Comparator.comparing( Class::getSimpleName ) )
                .collect( Collectors.toList() );
    }

    private Class<?> tipoDaChave( Class<?> tipo ) {
        return entityManagerFactory.getMetamodel().getEntities().stream()
                .filter( entidade -> entidade.getJavaType().equals( tipo ) )
                .findFirst()
                .map( entidade -> entidade.getIdType().getJavaType() )
                .orElse( null );
    }

}
