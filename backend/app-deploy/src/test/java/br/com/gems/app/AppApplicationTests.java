package br.com.gems.app;

import br.com.gems.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Gate de paridade entre JPA e Liquibase.
 *
 * <p>
 * Não é um teste vazio, apesar da aparência: subir o contexto aplica todos os changesets no banco
 * do container e roda o {@code ddl-auto: validate} do Hibernate sobre o resultado. Uma coluna que
 * exista na entidade e não no changeset — ou o contrário — derruba este teste, e é o único lugar do
 * projeto onde essa divergência é pega.
 */
class AppApplicationTests extends AbstractIntegrationTest {

    @Test
    void deveCarregarContexto_quandoAplicacaoInicia() {
    }

}
