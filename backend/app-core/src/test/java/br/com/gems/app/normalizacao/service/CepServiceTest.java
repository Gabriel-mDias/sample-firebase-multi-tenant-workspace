package br.com.gems.app.normalizacao.service;

import br.com.gems.app.commons.exception.ProvedorExternoException;
import br.com.gems.app.localidade.repository.EstadoRepository;
import br.com.gems.app.localidade.repository.PaisRepository;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.exception.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Cobertura dos modos de resposta e de falha da consulta de CEP.
 *
 * <p>
 * Usa {@link MockRestServiceServer} em vez de mock do {@link RestClient}: a API do cliente é
 * fluente, e mocká-la elo a elo testaria a corrente de chamadas em vez do comportamento. Com o
 * servidor simulado, o que se exercita é o que de fato importa — como o serviço reage ao que chega
 * pela rede.
 *
 * <p>
 * O caso mais importante é {@link #cepInexistenteEhRecusado()}: o ViaCEP responde <b>200</b> com
 * corpo de erro, e um serviço que olhasse só o status devolveria um endereço vazio como se fosse
 * sucesso.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CepServiceTest {

    private static final String BASE_URL = "https://viacep.com.br/ws";
    private static final String CEP = "29010000";
    private static final String URL_CONSULTA = BASE_URL + "/" + CEP + "/json/";

    private static final UUID ID_BRASIL = UUID.fromString( "11111111-1111-1111-1111-111111111111" );
    private static final UUID ID_ES = UUID.fromString( "22222222-2222-2222-2222-222222222222" );

    private static final String CORPO_OK = """
            {
              "cep": "29010-000",
              "logradouro": "Rua Sete de Setembro",
              "complemento": "",
              "bairro": "Centro",
              "localidade": "Vitória",
              "uf": "ES"
            }
            """;

    private static final String CORPO_ERRO = """
            { "erro": "true" }
            """;

    @Mock
    private EstadoRepository estadoRepository;

    @Mock
    private PaisRepository paisRepository;

    private CepService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl( BASE_URL );
        server = MockRestServiceServer.bindTo( builder ).build();
        service = new CepService( builder.build(), estadoRepository, paisRepository );

        var brasil = Pais.builder().id( ID_BRASIL ).nome( "Brasil" ).build();
        var espiritoSanto = Estado.builder()
                .id( ID_ES ).nome( "Espírito Santo" ).siglaEstado( "ES" ).pais( brasil ).build();

        when( paisRepository.findByNomeIgnoreCase( "Brasil" ) ).thenReturn( Optional.of( brasil ) );
        when( estadoRepository.findFirstBySiglaEstadoIgnoreCaseAndPaisNomeIgnoreCase( "ES", "Brasil" ) )
                .thenReturn( Optional.of( espiritoSanto ) );
    }

    @Test
    @DisplayName("CEP encontrado devolve endereço com o estado já resolvido para o cadastro de referência")
    void deveComprovarCepEncontradoDevolveEnderecoResolvido_quandoExecutaOCenario() {
        server.expect( requestTo( URL_CONSULTA ) )
                .andRespond( withSuccess( CORPO_OK, MediaType.APPLICATION_JSON ) );

        var endereco = service.buscarPorCep( CEP );

        assertThat( endereco.getCidade() ).isEqualTo( "Vitória" );
        assertThat( endereco.getBairro() ).isEqualTo( "Centro" );
        assertThat( endereco.getLogradouro() ).isEqualTo( "Rua Sete de Setembro" );
        assertThat( endereco.getCep() ).isEqualTo( CEP );
        assertThat( endereco.getEstado().getId() ).isEqualTo( ID_ES );
        assertThat( endereco.getPais().getId() ).isEqualTo( ID_BRASIL );
        server.verify();
    }

    @Test
    @DisplayName("número e complemento nunca vêm preenchidos: são do endereço, não do CEP")
    void deveComprovarNumeroEComplementoNaoVemPreenchidos_quandoExecutaOCenario() {
        server.expect( requestTo( URL_CONSULTA ) )
                .andRespond( withSuccess( CORPO_OK, MediaType.APPLICATION_JSON ) );

        var endereco = service.buscarPorCep( CEP );

        assertThat( endereco.getNumero() ).isNull();
        assertThat( endereco.getComplemento() ).isNull();
    }

    @Test
    @DisplayName("ARMADILHA DO PROVEDOR: CEP inexistente chega como HTTP 200 com corpo de erro, e "
            + "precisa ser recusado — não devolvido como endereço vazio")
    void deveComprovarCepInexistenteEhRecusado_quandoExecutaOCenario() {
        server.expect( requestTo( URL_CONSULTA ) )
                .andRespond( withSuccess( CORPO_ERRO, MediaType.APPLICATION_JSON ) );

        assertThatThrownBy( () -> service.buscarPorCep( CEP ) )
                .isInstanceOf( BusinessException.class )
                .hasMessageContaining( "CEP não encontrado." );
    }

    @Test
    @DisplayName("CEP fora do formato é recusado sem sequer chamar o provedor")
    void deveComprovarCepMalformadoNaoChegaAoProvedor_quandoExecutaOCenario() {
        assertThatThrownBy( () -> service.buscarPorCep( "123" ) )
                .isInstanceOf( BusinessException.class )
                .hasMessageContaining( "O CEP deve conter 8 dígitos." );

        server.verify(); // nenhuma requisição esperada, nenhuma feita
    }

    @Test
    @DisplayName("CEP nulo ou vazio é recusado como formato inválido, sem NullPointerException")
    void deveComprovarCepNuloEhRecusado_quandoExecutaOCenario() {
        assertThatThrownBy( () -> service.buscarPorCep( null ) )
                .isInstanceOf( BusinessException.class )
                .hasMessageContaining( "O CEP deve conter 8 dígitos." );
    }

    @Test
    @DisplayName("máscara no CEP é irrelevante: 29010-000 é o mesmo que 29010000")
    void deveComprovarMascaraNoCepEhIgnorada_quandoExecutaOCenario() {
        server.expect( requestTo( URL_CONSULTA ) )
                .andRespond( withSuccess( CORPO_OK, MediaType.APPLICATION_JSON ) );

        var endereco = service.buscarPorCep( "29010-000" );

        assertThat( endereco.getCep() ).isEqualTo( CEP );
        server.verify();
    }

    @Test
    @DisplayName("UF que não existe no cadastro de referência não invalida o resto do endereço")
    void deveComprovarUfDesconhecidaDeixaEstadoNuloSemFalhar_quandoExecutaOCenario() {
        when( estadoRepository.findFirstBySiglaEstadoIgnoreCaseAndPaisNomeIgnoreCase( "ES", "Brasil" ) )
                .thenReturn( Optional.empty() );
        server.expect( requestTo( URL_CONSULTA ) )
                .andRespond( withSuccess( CORPO_OK, MediaType.APPLICATION_JSON ) );

        var endereco = service.buscarPorCep( CEP );

        assertThat( endereco.getEstado() ).isNull();
        assertThat( endereco.getCidade() ).isEqualTo( "Vitória" );
    }

    @Test
    @DisplayName("erro do provedor vira ProvedorExternoException (502), nunca 500 nem 400")
    void deveComprovarErroDoProvedorViraFalhaDeProvedorExterno_quandoExecutaOCenario() {
        server.expect( requestTo( URL_CONSULTA ) ).andRespond( withServerError() );

        assertThatThrownBy( () -> service.buscarPorCep( CEP ) )
                .isInstanceOf( ProvedorExternoException.class )
                .hasMessageContaining( "Preencha o endereço manualmente." );
    }

    @Test
    @DisplayName("indisponibilidade do provedor também vira ProvedorExternoException")
    void deveComprovarProvedorIndisponivelViraFalhaDeProvedorExterno_quandoExecutaOCenario() {
        server.expect( requestTo( URL_CONSULTA ) )
                .andRespond( withStatus( HttpStatus.SERVICE_UNAVAILABLE ) );

        assertThatThrownBy( () -> service.buscarPorCep( CEP ) )
                .isInstanceOf( ProvedorExternoException.class );
    }

    @Test
    @DisplayName("resposta ilegível do provedor não escapa como erro do sistema")
    void deveComprovarRespostaIlegivelViraFalhaDeProvedorExterno_quandoExecutaOCenario() {
        server.expect( requestTo( URL_CONSULTA ) )
                .andRespond( withSuccess( "isto não é json", MediaType.APPLICATION_JSON ) );

        assertThatThrownBy( () -> service.buscarPorCep( CEP ) )
                .isInstanceOf( ProvedorExternoException.class );
    }

}
