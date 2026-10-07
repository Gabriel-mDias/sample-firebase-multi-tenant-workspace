package br.com.gems.app.autorizacao;

import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.support.AbstractIntegrationTest;
import br.com.gems.security.authorization.AuthorizationCatalog;
import br.com.gems.security.authorization.EndpointAuthorizationScan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Varredura de cobertura: todo endpoint de todo {@code @RestController} do sistema precisa declarar
 * {@code @PreAuthorize} — direto, ou por meio de {@code @PublicEndpoint} quando não exigir ação.
 *
 * <p>
 * <b>Por que este teste existe.</b> Sem ele, a única falha silenciosa possível nesta rodada é uma
 * anotação esquecida: o endpoint não quebra em lugar nenhum, apenas cai em
 * {@code anyRequest().authenticated()} do {@code SecurityConfig} e fica aberto a qualquer autenticado,
 * sem que nenhum teste de negócio o note (FR-015).
 *
 * <p>
 * <b>Não há lista de isenção por classe.</b> {@code LocalidadeController} e {@code CepController}
 * são dado de referência consumido por todo formulário do sistema, mas ainda assim carregam
 * {@code @PreAuthorize("isAuthenticated()")} em cada método — então nada precisa ser isento: exigir a
 * anotação em <b>todo</b> método mapeado é mais simples de manter do que uma lista que precisaria ser
 * atualizada a cada controller novo. A única saída deliberada é {@code @PublicEndpoint}, que já é o
 * mecanismo do projeto para "sem autenticação".
 *
 * <h2>Por que a varredura se restringe ao pacote do projeto</h2>
 * A primeira versão deste teste varria <b>todos</b> os {@code @RestController} do contexto e falhou
 * apontando seis métodos — três do {@code S3Controller} (do {@code gems-aws-web}) e três do
 * springdoc. O apontamento estava tecnicamente correto e mesmo assim era a pergunta errada: esses
 * controllers vêm de <b>jar de terceiro</b>, e não há como acrescentar {@code @PreAuthorize} a eles
 * sem forkar a biblioteca.
 *
 * <p>
 * Eles não estão desprotegidos — são tratados por <b>regra de caminho</b> no {@code SecurityConfig},
 * que é o único lugar de onde se pode alcançá-los:
 * <ul>
 * <li>{@code /api/aws/s3/**} tem {@code denyAll()}, e quem prova isso é o
 * {@code S3EndpointsSecurityIntegrationTest} — com 403 verificado nos três endpoints;</li>
 * <li>{@code /v3/api-docs/**} e {@code /swagger-ui/**} são deliberadamente públicos, declarados
 * entre os padrões fixos de {@code getPublicEndpoints()}.</li>
 * </ul>
 *
 * <p>
 * Restringir ao pacote {@code br.com.gems.app} mantém o teste respondendo à pergunta que ele
 * consegue responder — "algum endpoint <b>nosso</b> ficou sem regra?" —, em vez de exigir de terceiros
 * uma anotação que eles nunca terão e que só produziria uma lista de exceções crescendo a cada
 * dependência nova.
 */
class CoberturaPreAuthorizeTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * Só os controllers escritos por este projeto. Controllers de terceiros são cobertos por regra de
     * caminho no {@code SecurityConfig} — ver o javadoc da classe.
     */
    private static final String PACOTE_DO_PROJETO = "br.com.gems.app";

    @Test
    @DisplayName("a varredura publicada aceita todos os endpoints do projeto")
    void deveComprovarTodoEndpointAtendePoliticaPublicada_quandoExecutaOCenario() {
        var metodosDoProjeto = requestMappingHandlerMapping.getHandlerMethods().values().stream()
                .filter( metodo -> metodo.getBeanType().getName().startsWith( PACOTE_DO_PROJETO ) )
                .toList();
        var referenciaAutenticada = metodosDoProjeto.stream()
                .filter( metodo -> {
                    var preAuthorize = metodo.getMethodAnnotation( PreAuthorize.class );
                    return preAuthorize != null && "isAuthenticated()".equals( preAuthorize.value() );
                } )
                .toList();

        assertThat( referenciaAutenticada )
                .extracting( metodo -> metodo.getBeanType().getSimpleName() + "#" + metodo.getMethod().getName() )
                .containsExactlyInAnyOrder(
                        "CepController#buscarPorCep",
                        "LocalidadeController#listarEstadosPorPais",
                        "LocalidadeController#listarPaises" );

        EndpointAuthorizationScan.assertProtected(
                metodosDoProjeto.stream().filter( metodo -> !referenciaAutenticada.contains( metodo ) ).toList(),
                AuthorizationCatalog.of( AcaoSistema.class ) );
    }

    @Test
    @DisplayName("a varredura de fato encontra os 13 controllers do projeto — senão ela passaria vazia")
    void deveComprovarAVarreduraNaoPassaVazia_quandoExecutaOCenario() {
        var controllersDoProjeto = context.getBeansWithAnnotation( RestController.class ).values()
                .stream()
                .map( AopUtils::getTargetClass )
                .filter( classe -> classe.getName().startsWith( PACOTE_DO_PROJETO ) )
                .map( Class::getSimpleName )
                .sorted()
                .toList();

        // Sem esta asserção, um erro no filtro de pacote faria o teste acima passar por não olhar
        // nada — a pior forma de teste verde. Os cinco controllers abaixo são a lista fechada de
        // hoje; um controller novo quebra aqui, e quebrar aqui é a intenção: ele precisa entrar na
        // conta.
        //
        // Atualizar esta lista é tarefa do roteiro da rodada que acrescentou o controller, não
        // conserto de teste quebrado.
        assertThat( controllersDoProjeto ).containsExactly(
                "AuditoriaController", "CepController", "LocalidadeController",
                "ParceiroController", "SolicitacaoController" );
    }

}
