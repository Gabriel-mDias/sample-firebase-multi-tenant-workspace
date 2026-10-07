package br.com.gems.app.autorizacao;

import br.com.gems.app.localidade.repository.EstadoRepository;
import br.com.gems.app.localidade.repository.PaisRepository;
import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.dto.EstadoDTO;
import br.com.gems.app.model.dto.PaisDTO;
import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.dto.PessoaDTO;
import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.model.enums.Sexo;
import br.com.gems.app.parceiro.repository.ParceiroRepository;
import br.com.gems.app.parceiro.service.ParceiroService;
import br.com.gems.app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova, em nível HTTP, que a autorização por ação <b>morde</b> — não apenas que a anotação existe
 * (isso já é responsabilidade do {@link CoberturaPreAuthorizeTest}).
 *
 * <p>
 * As credenciais simuladas carregam <b>exatamente</b> as ações de um perfil, montadas a partir de
 * {@link MapaPerfilAcoes#acoesDe(PerfilAtuacao)} e prefixadas com {@code ROLE_} — o mesmo cálculo que
 * o Keycloak faria ao expandir a composta {@code PERFIL_*} dentro de {@code realm_access.roles}. Não
 * há {@code ADMIN} em nenhuma delas: o objetivo é justamente provar o comportamento de um perfil que
 * não é o administrador.
 */
@AutoConfigureMockMvc
class AutorizacaoPorAcaoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Instanciado, e não injetado: o contexto de teste não registra um ObjectMapper como bean, e
    // aqui ele serve só para montar o corpo da requisição.
    //
    // O JavaTimeModule é obrigatório: sem ele o Jackson recusa LocalDate com uma mensagem que
    // parece defeito de DTO (Java 8 date/time type not supported by default), e não de mapper.
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule( new JavaTimeModule() );

    @Autowired
    private ParceiroService parceiroService;

    @Autowired
    private ParceiroRepository parceiroRepository;

    @Autowired
    private PaisRepository paisRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    private UUID parceiroId;

    @AfterEach
    void tearDown() {
        // Limpeza chama o service diretamente, sem passar pelo MockMvc: aqui não se está testando
        // autorização, só devolvendo o banco ao estado anterior ao teste.
        if ( parceiroId != null && parceiroRepository.existsById( parceiroId ) ) {
            parceiroService.delete( parceiroId );
        }
    }

    @Test
    @DisplayName("(a) Operador tenta excluir parceiro -> 403, e o parceiro CONTINUA existindo")
    void deveComprovarOperadorNaoExcluiParceiro_quandoExecutaOCenario() throws Exception {
        parceiroId = criarParceiroDeTeste();

        mockMvc.perform( delete( "/api/parceiros/{id}", parceiroId )
                .with( jwt().authorities( authoritiesDoPerfil( PerfilAtuacao.OPERADOR ) ) ) )
                .andExpect( status().isForbidden() );

        // Asserção sobre o DADO, não só sobre o código HTTP: prova que a exclusão nunca foi
        // alcançada. Um 403 que ainda assim excluísse o registro — por uma ordem errada entre a
        // interceptação de segurança e a execução do método — passaria despercebido se só o status
        // fosse conferido.
        assertThat( parceiroRepository.findById( parceiroId ) ).isPresent();
    }

    @Test
    @DisplayName("(b) Consulta tenta cadastrar parceiro -> 403 (não possui INSERIR_PARCEIRO)")
    void deveComprovarConsultaNaoCadastra_quandoExecutaOCenario() throws Exception {
        mockMvc.perform( post( "/api/parceiros" )
                .with( jwt().authorities( authoritiesDoPerfil( PerfilAtuacao.CONSULTA ) ) )
                .contentType( MediaType.APPLICATION_JSON )
                .content( objectMapper.writeValueAsString( dtoDeTeste( "Parceiro Recusado" ) ) ) )
                .andExpect( status().isForbidden() )
                .andExpect( jsonPath( "$.occurrenceTime" ).exists() )
                .andExpect( jsonPath( "$.errorType" ).value( "ACESSO_NEGADO" ) )
                .andExpect( jsonPath( "$.message" ).isNotEmpty() )
                .andExpect( jsonPath( "$.path" ).value( "/api/parceiros" ) )
                .andExpect( jsonPath( "$.method" ).value( "POST" ) );
    }

    @Test
    @DisplayName("(c) Consulta PESQUISA parceiros -> 200 (possui CONSULTAR_PARCEIRO)")
    void deveComprovarConsultaPesquisa_quandoExecutaOCenario() throws Exception {
        mockMvc.perform( post( "/api/parceiros/search" )
                .with( jwt().authorities( authoritiesDoPerfil( PerfilAtuacao.CONSULTA ) ) )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "{}" ) )
                .andExpect( status().isOk() );
    }

    @Test
    @DisplayName("(d) Operador conclui solicitação -> não é 403 (possui CONCLUIR_SOLICITACAO)")
    void deveComprovarOperadorPodeConcluir_quandoExecutaOCenario() throws Exception {
        // Id inexistente de propósito: aqui interessa que NÃO seja 403. Um 400 de negócio prova que
        // a requisição atravessou a camada de segurança e chegou ao service — que é o ponto.
        mockMvc.perform( post( "/api/solicitacoes/{id}/concluir", UUID.randomUUID() )
                .with( jwt().authorities( authoritiesDoPerfil( PerfilAtuacao.OPERADOR ) ) ) )
                .andExpect( result -> assertThat( result.getResponse().getStatus() ).isNotEqualTo( 403 ) );
    }

    @Test
    @DisplayName("(e) Consulta tenta concluir solicitação -> 403 (transição é ação própria)")
    void deveComprovarConsultaNaoConclui_quandoExecutaOCenario() throws Exception {
        // O núcleo do desenho: a situação não vem do corpo, e cada transição tem a sua ação. Sem
        // isso, quem pudesse editar concluiria.
        mockMvc.perform( post( "/api/solicitacoes/{id}/concluir", UUID.randomUUID() )
                .with( jwt().authorities( authoritiesDoPerfil( PerfilAtuacao.CONSULTA ) ) ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @DisplayName("(f) Token sem ação nenhuma -> 403 em tudo. O guard falha FECHADO.")
    void deveComprovarTokenVazioNaoAlcancaNada_quandoExecutaOCenario() throws Exception {
        mockMvc.perform( post( "/api/parceiros/search" )
                .with( jwt().authorities( List.of() ) )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "{}" ) )
                .andExpect( status().isForbidden() );
    }

    /**
     * As authorities de um perfil, calculadas do mesmo mapa que alimenta o Keycloak.
     *
     * <p>
     * Derivar em vez de listar à mão é deliberado: uma ação nova entra no teste sozinha, e o
     * teste continua descrevendo o perfil real em vez de uma fotografia dele.
     */
    private List<GrantedAuthority> authoritiesDoPerfil( PerfilAtuacao perfil ) {
        return MapaPerfilAcoes.acoesDe( perfil ).stream()
                .map( acao -> (GrantedAuthority) new SimpleGrantedAuthority( "ROLE_" + acao.name() ) )
                .toList();
    }

    private UUID criarParceiroDeTeste() {
        return parceiroService.insert( dtoDeTeste( "Parceiro " + UUID.randomUUID() ) ).getId();
    }

    private ParceiroDTO dtoDeTeste( String nome ) {
        var pais = paisRepository.findAll().stream()
                .filter( p -> "Brasil".equalsIgnoreCase( p.getNome() ) )
                .findFirst().orElseThrow();
        var estado = estadoRepository.findAll().stream().findFirst().orElseThrow();

        var pessoa = new PessoaDTO();
        pessoa.setPrimeiroNome( "Fulano" );
        pessoa.setSobrenome( "de Teste" );
        pessoa.setCpf( gerarCpf() );
        pessoa.setDataNascimento( LocalDate.of( 1990, 1, 1 ) );
        pessoa.setSexo( Sexo.MASCULINO );

        var contato = new ContatoDTO();
        contato.setEmail( "parceiro-" + UUID.randomUUID() + "@teste.local" );
        contato.setTelefone( "+55 27 99999-0000" );

        var endereco = new EnderecoDTO();
        var paisDto = new PaisDTO();
        paisDto.setId( pais.getId() );
        paisDto.setNome( pais.getNome() );

        var estadoDto = new EstadoDTO();
        estadoDto.setId( estado.getId() );
        estadoDto.setNome( estado.getNome() );
        estadoDto.setSiglaEstado( estado.getSiglaEstado() );

        endereco.setPais( paisDto );
        endereco.setEstado( estadoDto );
        endereco.setCep( "29000-000" );
        endereco.setCidade( "Vitória" );
        endereco.setBairro( "Centro" );
        endereco.setLogradouro( "Rua de Teste" );
        endereco.setNumero( "100" );

        var dto = new ParceiroDTO();
        dto.setNome( nome );
        dto.setPessoa( pessoa );
        dto.setContato( contato );
        dto.setEndereco( endereco );
        return dto;
    }

    /** CPF sintético válido para o dígito verificador, gerado por teste. */
    private String gerarCpf() {
        var base = String.format( "%09d", (int) (Math.random() * 1_000_000_000L) );
        var digitos = base.chars().map( c -> c - '0' ).toArray();

        var d1 = digitoVerificador( digitos, 10 );
        var d2 = digitoVerificador( java.util.Arrays.copyOf( somar( digitos, d1 ), 10 ), 11 );

        return base + d1 + d2;
    }

    private int[] somar( int[] base, int digito ) {
        var novo = java.util.Arrays.copyOf( base, base.length + 1 );
        novo[base.length] = digito;
        return novo;
    }

    private int digitoVerificador( int[] digitos, int pesoInicial ) {
        var soma = 0;
        var peso = pesoInicial;
        for ( int digito : digitos ) {
            soma += digito * peso--;
        }
        var resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
