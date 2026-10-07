package br.com.gems.app.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração do sistema.
 *
 * <p>
 * Antes desta classe, os {@code @SpringBootTest} do projeto subiam contra o mesmo Postgres
 * que a homologação manual vinha preenchendo. A consequência não era teórica: um cadastro feito pela
 * tela podia derrubar um teste sem que nada tivesse regredido, e toda asserção precisava se defender
 * de dado alheio por disciplina de revisão, não por ferramenta.
 *
 * <p>
 * <b>O container é estático e nunca é parado.</b> Isso é deliberado e é o ponto mais importante
 * daqui. Se ele fosse um campo de instância anotado com {@code @Container} — o exemplo mais comum na
 * documentação do Testcontainers —, o JUnit subiria <b>um Postgres por classe de teste</b>. Com
 * poucas classes hoje e mais chegando a cada rodada, a suíte sairia de segundos para minutos, e uma
 * suíte lenta é uma suíte que as pessoas param de rodar. Um único container, iniciado no primeiro
 * carregamento da classe e derrubado pelo Ryuk ao fim da JVM, é o que mantém o custo constante.
 *
 * <p>
 * Pelo mesmo motivo não há {@code @Testcontainers}: aquela extensão gerencia o ciclo de vida por
 * classe, que é exatamente o que se quer evitar aqui.
 *
 * <p>
 * <b>O schema nasce do Liquibase</b>, dos mesmos changesets da produção, e o {@code ddl-auto}
 * continua {@code validate}. Não é detalhe de configuração: é o que dá sentido ao
 * {@code AppApplicationTests.contextLoads}, cujo trabalho é justamente provar que as entidades JPA
 * e os changesets descrevem o mesmo banco. Trocar por {@code create-drop} destruiria a verificação.
 *
 * <p>
 * <b>A disciplina de fixtures continua valendo.</b> Restringir cada asserção ao dado que
 * só o próprio teste cria — sufixo de e-mail próprio, {@code "Integracao-" + UUID.randomUUID()}, CPF
 * derivado de {@code System.nanoTime()} — segue sendo boa prática. Isolamento é rede de segurança,
 * não licença para asserção ampla.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    /**
     * Mesma major do compose de desenvolvimento ({@code postgres:16}). Divergir aqui reintroduziria,
     * pela porta dos fundos, o risco que esta classe existe para eliminar: um teste verde contra uma
     * versão que a produção não usa.
     */
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer( "postgres:16" )
            .withDatabaseName( "app-test-db" )
            .withUsername( "app-test" )
            .withPassword( "app-test" );

    static {
        POSTGRES.start();
    }

    /**
     * Só o datasource é dinâmico — todo o resto da configuração de teste é estático e vive em
     * {@code src/test/resources/application.yml}. A porta do container só é conhecida depois de ele
     * subir, e é por isso que estes três valores não podem estar lá.
     */
    @DynamicPropertySource
    static void datasourceDoContainer( DynamicPropertyRegistry registry ) {
        registry.add( "spring.datasource.url", POSTGRES::getJdbcUrl );
        registry.add( "spring.datasource.username", POSTGRES::getUsername );
        registry.add( "spring.datasource.password", POSTGRES::getPassword );
    }
}
