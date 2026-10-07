package br.com.gems.app.security;

import br.com.gems.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova, em nível HTTP, que a superfície de diagnóstico do backend não é anônima.
 *
 * <h2>Por que este teste existe</h2>
 * O default que se costuma escrever sem pensar libera {@code /actuator/**} inteiro com
 * {@code permitAll()}, e a exposição default do {@code application.yml} inclui {@code metrics}.
 * Publicado na internet, isso entrega {@code /actuator/metrics/http.server.requests} a quem não tem
 * token nenhum — e esse endpoint <b>enumera todos os URIs da API</b>, com contagem e latência por
 * rota. É o mapa da aplicação servido de graça, antes de qualquer tentativa de autenticação.
 *
 * <h2>Por que não bastava reduzir a exposição</h2>
 * Trocar {@code ACTUATOR_EXPOSURE} para {@code health} resolve o sintoma <b>enquanto o {@code .env}
 * estiver certo</b>, em todo ambiente, para sempre. Aqui a rota passa a exigir autenticação, e a
 * redução da exposição fica como segunda camada: quem reexpuser {@code metrics} por variável de
 * ambiente ainda vai precisar de um token para chegar nele. Este teste cobre a primeira camada, que
 * é a que não depende de configuração de ambiente.
 *
 * <h2>Por que o 401 aqui prova a regra, e não a ausência do endpoint</h2>
 * Com {@code ACTUATOR_EXPOSURE=health}, {@code /actuator/metrics} não está mapeado — e um endpoint
 * inexistente também devolveria erro. O que distingue os dois é o <b>código</b>: a cadeia de
 * segurança roda antes do dispatcher, então falta de credencial vira <b>401</b> e falta de handler
 * viraria <b>404</b>. {@link #comTokenAMesmaRotaDeixaDeSer401()} fecha o argumento pelo outro lado:
 * com um JWT válido, a mesma rota deixa de responder 401. Se o 401 anônimo viesse da ausência do
 * endpoint, ele não mudaria ao acrescentar credencial.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Espelha a produção: o .env desliga os dois. Sem isto o teste do 404 provaria o contrário
        // do que interessa — que o contrato da API está publicado.
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class ActuatorSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/actuator/health continua anônimo — é o que o HEALTHCHECK do contêiner usa")
    void deveComprovarHealthContinuaAnonimo_quandoExecutaOCenario() throws Exception {
        mockMvc.perform( get( "/actuator/health" ) )
                .andExpect( status().isOk() );
    }

    /**
     * O {@code HEALTHCHECK} do {@code backend/Dockerfile} bate em {@code /actuator/health} de dentro
     * do contêiner, sem token. Se esta rota deixasse de ser pública, o contêiner ficaria
     * {@code unhealthy} — e como o compose de produção <b>não</b> usa health do backend em nenhum
     * {@code depends_on}, a falha não derrubaria nada: ficaria visível apenas em
     * {@code docker compose ps}, que ninguém olha num deploy que "funcionou". Daí o teste.
     */
    @Test
    @DisplayName("as probes de liveness e readiness também continuam anônimas")
    void deveComprovarProbesContinuamAnonimas_quandoExecutaOCenario() throws Exception {
        mockMvc.perform( get( "/actuator/health/liveness" ) ).andExpect( status().isOk() );
        mockMvc.perform( get( "/actuator/health/readiness" ) ).andExpect( status().isOk() );
    }

    @Test
    @DisplayName("/actuator/metrics responde 401 sem token")
    void deveComprovarMetricsExigeAutenticacao_quandoExecutaOCenario() throws Exception {
        mockMvc.perform( get( "/actuator/metrics" ) )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    @DisplayName("/actuator/env e /actuator/beans também exigem token, mesmo não expostos hoje")
    void deveComprovarDemaisEndpointsDeDiagnosticoExigemAutenticacao_quandoExecutaOCenario() throws Exception {
        mockMvc.perform( get( "/actuator/env" ) ).andExpect( status().isUnauthorized() );
        mockMvc.perform( get( "/actuator/beans" ) ).andExpect( status().isUnauthorized() );
    }

    /**
     * O contraponto de {@link #metricsExigeAutenticacao()}: com credencial, a resposta deixa de ser
     * 401. Não afirmamos 200 — com {@code ACTUATOR_EXPOSURE=health} o endpoint não está exposto e a
     * resposta correta é 404. O que importa é que o 401 anterior vinha da <b>regra de autorização</b>,
     * e não de o endpoint não existir.
     */
    @Test
    @DisplayName("com token, /actuator/metrics deixa de responder 401 — o 401 vinha da autorização")
    void deveComprovarComTokenAMesmaRotaDeixaDeSer401_quandoExecutaOCenario() throws Exception {
        var resposta = mockMvc.perform( get( "/actuator/metrics" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_ADMIN" ) ) ) )
                .andReturn()
                .getResponse();

        assertThat( resposta.getStatus() ).isNotEqualTo( 401 );
    }

    /**
     * O {@code /v3/api-docs} continua na lista de rotas públicas do {@code SecurityConfig} — o que o
     * fecha em produção é o springdoc estar desligado, não uma regra de autorização.
     * Desligar por configuração em vez de por rota mantém o Swagger disponível em desenvolvimento,
     * que é onde ele serve.
     *
     * <p>
     * Asserta o código <b>e</b> o conteúdo. O 404 passou a ser confiável com o
     * {@code RequisicaoInvalidaExceptionHandler}; a checagem de conteúdo continua porque é ela que
     * expressa a propriedade de segurança — o documento OpenAPI não sai — independentemente do
     * status.
     */
    @Test
    @DisplayName("com o springdoc desligado, o contrato da API não é publicado")
    void deveComprovarContratoDaApiNaoEhPublicadoComSpringdocDesligado_quandoExecutaOCenario() throws Exception {
        var apiDocs = mockMvc.perform( get( "/v3/api-docs" ) ).andReturn().getResponse();
        var swagger = mockMvc.perform( get( "/swagger-ui/index.html" ) ).andReturn().getResponse();

        assertThat( apiDocs.getStatus() ).isEqualTo( 404 );
        assertThat( apiDocs.getContentAsString() )
                .as( "o documento OpenAPI não pode sair na resposta" )
                .doesNotContain( "\"openapi\"" )
                .doesNotContain( "\"paths\"" );

        assertThat( swagger.getStatus() ).isEqualTo( 404 );
        // Asserta o tipo do erro, não a ausência da string "swagger-ui": o corpo do 404 ecoa o
        // caminho pedido no campo `path`, então procurar por ela ali acusaria a própria requisição.
        // O que prova que a interface não foi servida é a resposta ser o erro de recurso ausente.
        assertThat( swagger.getContentAsString() )
                .contains( "RECURSO_NAO_ENCONTRADO" )
                .doesNotContain( "<html" );
    }

    /**
     * O defeito que este teste registrava foi <b>corrigido</b>, e o registro fica como história.
     *
     * <p>
     * Quando escrito, ele afirmava o contrário: qualquer caminho sem handler respondia
     * <b>500 {@code ERRO_NAO_ESPERADO}</b>, porque o {@code GlobalExceptionHandler} do
     * {@code gems-exception} trata {@code Exception} de forma ampla e alcançava a
     * {@code NoResourceFoundException} do Spring. Não vazava nada, mas deixava um endereço digitado
     * errado indistinguível de aplicação quebrada em qualquer monitoração por taxa de 5xx.
     *
     * <p>
     * O conserto é o {@code RequisicaoInvalidaExceptionHandler}, que segue o padrão que o projeto
     * já usava duas vezes para corrigir mapeamentos ruins da SDK — {@code AccessDeniedExceptionHandler}
     * e {@code IdentityProviderExceptionHandler}. O teste foi escrito para falhar exatamente neste
     * momento, e foi o que aconteceu.
     */
    @Test
    @DisplayName("caminho não mapeado responde 404, não 500")
    void deveComprovarCaminhoNaoMapeadoResponde404_quandoExecutaOCenario() throws Exception {
        var resposta = mockMvc.perform( get( "/caminho/que/nao/existe" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_ADMIN" ) ) ) )
                .andReturn()
                .getResponse();

        assertThat( resposta.getStatus() ).isEqualTo( 404 );
        assertThat( resposta.getContentAsString() ).contains( "RECURSO_NAO_ENCONTRADO" );
    }

    /**
     * O outro lado do mesmo conserto: {@code @PathVariable UUID} com valor malformado.
     *
     * <p>
     * O {@code FamiliaController} é o único do projeto que declara o id como {@code UUID} em vez
     * de {@code String} — os outros seis recebem {@code String} e convertem no service, o que produz
     * {@code IllegalArgumentException} e 400 pelo handler da SDK. Ali o Spring lançava
     * {@code MethodArgumentTypeMismatchException}, que não descende de {@code IllegalArgumentException}
     * e escapava como 500. Agora os dois caminhos convergem em 400.
     */
    @Test
    @DisplayName("UUID malformado na rota responde 400, não 500")
    void deveComprovarUuidMalformadoResponde400_quandoExecutaOCenario() throws Exception {
        var resposta = mockMvc.perform( post( "/api/solicitacoes/isto-nao-e-um-uuid/abrir" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_ABRIR_SOLICITACAO" ) ) ) )
                .andReturn()
                .getResponse();

        assertThat( resposta.getStatus() ).isEqualTo( 400 );
        assertThat( resposta.getContentAsString() ).contains( "PARAMETRO_INVALIDO" );
    }

}
