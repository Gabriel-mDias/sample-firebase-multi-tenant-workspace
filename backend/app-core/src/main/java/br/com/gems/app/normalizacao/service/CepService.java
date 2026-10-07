package br.com.gems.app.normalizacao.service;

import br.com.gems.app.commons.exception.ProvedorExternoException;
import br.com.gems.app.localidade.repository.EstadoRepository;
import br.com.gems.app.localidade.repository.PaisRepository;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.dto.EstadoDTO;
import br.com.gems.app.model.dto.PaisDTO;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.normalizacao.dto.ViaCepResponse;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

/**
 * Consulta de endereço por CEP.
 *
 * <p>
 * <b>Por que no backend, e não na SPA.</b> O de-para entre a sigla da UF que o provedor devolve e
 * o {@code ID_ESTADO} do cadastro de referência precisa acontecer em algum lugar. Aqui acontece uma
 * vez; no frontend, aconteceria em cada tela que coleta endereço — hoje três.
 *
 * <p>
 * <b>A consulta é uma conveniência, nunca um bloqueio</b>. Não há retry: insistir só
 * faria o operador esperar mais para chegar ao mesmo lugar, e o preenchimento manual continua
 * disponível em qualquer cenário de falha.
 *
 * <p>
 * Os três modos de falha são deliberadamente distintos:
 * <ul>
 * <li><b>400</b> — CEP fora do formato. Nem chega ao provedor.</li>
 * <li><b>400</b> — CEP inexistente. Segue a convenção do projeto, em que "não encontrado" é
 * {@link BusinessException} (ver o {@code buscarAtivo} do {@code ParceiroService}).</li>
 * <li><b>502</b> — provedor fora do ar, lento ou com erro. {@link ProvedorExternoException}.</li>
 * </ul>
 */
@Slf4j
@Service
public class CepService {

    private static final int TAMANHO_CEP = 8;
    private static final String PAIS_DO_CEP = "Brasil";

    private final RestClient restClient;
    private final EstadoRepository estadoRepository;
    private final PaisRepository paisRepository;

    /**
     * O {@link RestClient} nasce aqui, da fábrica estática, em vez de vir injetado como
     * {@code RestClient.Builder}: esse bean <b>não é auto-configurado</b> neste setup, e depender
     * dele derrubava a subida da aplicação. Montá-lo internamente também mantém a configuração de
     * timeout ao lado de quem a usa.
     *
     * <p>
     * {@code @Autowired} explícito porque a classe tem <b>dois</b> construtores — sem ele o Spring
     * procura o construtor padrão e falha. Ambos os defeitos foram pegos pelo {@code contextLoads},
     * que sobe a aplicação de verdade.
     */
    @Autowired
    public CepService( EstadoRepository estadoRepository,
            PaisRepository paisRepository,
            @Value("${app.cep.viacep.url}") String url,
            @Value("${app.cep.viacep.timeout}") Duration timeout ) {
        this.restClient = RestClient.builder()
                .baseUrl( url )
                .requestFactory( requestFactory( timeout ) )
                .build();
        this.estadoRepository = estadoRepository;
        this.paisRepository = paisRepository;
    }

    /**
     * Construtor de teste: recebe o {@link RestClient} já montado, para uso com
     * {@code MockRestServiceServer}.
     */
    CepService( RestClient restClient, EstadoRepository estadoRepository, PaisRepository paisRepository ) {
        this.restClient = restClient;
        this.estadoRepository = estadoRepository;
        this.paisRepository = paisRepository;
    }

    public EnderecoDTO buscarPorCep( String cep ) {
        var digitos = apenasDigitos( cep );

        if ( digitos.length() != TAMANHO_CEP ) {
            throw new BusinessException( "O CEP deve conter 8 dígitos." );
        }

        var resposta = consultar( digitos );

        if ( ObjectUtil.isNullOrEmpty( resposta ) || resposta.naoEncontrado() ) {
            throw new BusinessException( "CEP não encontrado." );
        }

        return montar( resposta, digitos );
    }

    /**
     * Qualquer falha de transporte, timeout ou status de erro vira {@link ProvedorExternoException}.
     * O {@code catch} é largo de propósito: o que interessa ao chamador é que o provedor não
     * respondeu, e não qual das muitas exceções do cliente HTTP ocorreu.
     */
    private ViaCepResponse consultar( String cep ) {
        try {
            return restClient.get()
                    .uri( "/{cep}/json/", cep )
                    .retrieve()
                    .body( ViaCepResponse.class );
        } catch ( Exception ex ) {
            log.warn( "Falha ao consultar o CEP {} no provedor externo: {}", cep, ex.getMessage() );
            throw new ProvedorExternoException(
                    "Não foi possível consultar o CEP no momento. Preencha o endereço manualmente.", ex );
        }
    }

    /**
     * O {@code estado} pode voltar nulo: uma UF que o provedor conhece mas que não está no cadastro
     * de referência não invalida o resto do endereço — os demais campos são preenchidos e a seleção
     * do estado fica para o operador.
     */
    private EnderecoDTO montar( ViaCepResponse resposta, String cep ) {
        var dto = new EnderecoDTO();
        dto.setCep( cep );
        dto.setCidade( resposta.localidade() );
        dto.setBairro( resposta.bairro() );
        dto.setLogradouro( resposta.logradouro() );
        dto.setPais( paisDoCep() );
        resolverEstado( resposta.uf() ).ifPresent( estado -> dto.setEstado( estadoDTO( estado ) ) );
        return dto;
    }

    private Optional<Estado> resolverEstado( String uf ) {
        if ( ObjectUtil.isNullOrEmpty( uf ) ) {
            return Optional.empty();
        }
        return estadoRepository.findFirstBySiglaEstadoIgnoreCaseAndPaisNomeIgnoreCase( uf, PAIS_DO_CEP );
    }

    private PaisDTO paisDoCep() {
        return paisRepository.findByNomeIgnoreCase( PAIS_DO_CEP )
                .map( pais -> new PaisDTO( pais.getId(), pais.getNome() ) )
                .orElse( null );
    }

    private EstadoDTO estadoDTO( Estado estado ) {
        var dto = new EstadoDTO();
        dto.setId( estado.getId() );
        dto.setNome( estado.getNome() );
        dto.setSiglaEstado( estado.getSiglaEstado() );
        return dto;
    }

    private SimpleClientHttpRequestFactory requestFactory( Duration timeout ) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout( timeout );
        factory.setReadTimeout( timeout );
        return factory;
    }

    private String apenasDigitos( String valor ) {
        return ObjectUtil.isNullOrEmpty( valor ) ? "" : valor.replaceAll( "\\D", "" );
    }

}
