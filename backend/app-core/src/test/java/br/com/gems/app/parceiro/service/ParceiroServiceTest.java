package br.com.gems.app.parceiro.service;

import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.response.ParceiroResponseDTO;
import br.com.gems.app.normalizacao.service.ContatoService;
import br.com.gems.app.normalizacao.service.EnderecoService;
import br.com.gems.app.normalizacao.service.PessoaService;
import br.com.gems.app.parceiro.mapper.ParceiroMapper;
import br.com.gems.app.parceiro.repository.ParceiroRepository;
import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import br.com.gems.exception.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A autorização de campo, atacada nos três caminhos.
 *
 * <p>
 * Ela é a regra mais fácil de implementar pela metade: quase todo mundo cobre o {@code toDTO} e
 * esquece a busca e a gravação. Os três testes abaixo existem porque cada um deles falharia sozinho,
 * sem que os outros dois acusassem nada.
 */
class ParceiroServiceTest {

    private ParceiroMapper mapper;
    private ParceiroRepository repository;
    private PessoaService pessoaService;
    private ContatoService contatoService;
    private EnderecoService enderecoService;
    private UsuarioAutenticadoService usuarioAutenticadoService;
    private ParceiroService service;

    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        mapper = mock( ParceiroMapper.class );
        repository = mock( ParceiroRepository.class );
        pessoaService = mock( PessoaService.class );
        contatoService = mock( ContatoService.class );
        enderecoService = mock( EnderecoService.class );
        usuarioAutenticadoService = mock( UsuarioAutenticadoService.class );
        service = new ParceiroService( mapper, repository, pessoaService, contatoService, enderecoService,
                usuarioAutenticadoService );

        when( mapper.toDTO( any( Parceiro.class ) ) ).thenAnswer( invocation -> {
            var entity = invocation.getArgument( 0, Parceiro.class );
            var dto = new ParceiroDTO();
            dto.setId( entity.getId() );
            dto.setNome( entity.getNome() );
            dto.setDescricao( entity.getDescricao() );
            dto.setComissao( entity.getComissao() );
            dto.setSolicitacoesConcluidas( entity.getSolicitacoesConcluidas() );
            return dto;
        } );
        when( repository.save( any( Parceiro.class ) ) ).thenAnswer( i -> i.getArgument( 0 ) );
    }

    @Nested
    @DisplayName("O sigilo da comissão")
    class SigiloDaComissao {

        @Test
        @DisplayName("quem tem a ação enxerga o valor na leitura")
        void deveComprovarLeituraComAcao_quandoExecutaOCenario() {
            comAcao( true );
            when( repository.findById( id ) ).thenReturn( Optional.of( parceiroComComissao() ) );

            assertThat( service.findById( id ).getComissao() ).isEqualByComparingTo( "12.50" );
        }

        @Test
        @DisplayName("quem não tem a ação recebe null na leitura")
        void deveComprovarLeituraSemAcao_quandoExecutaOCenario() {
            comAcao( false );
            when( repository.findById( id ) ).thenReturn( Optional.of( parceiroComComissao() ) );

            assertThat( service.findById( id ).getComissao() ).isNull();
        }

        @Test
        @DisplayName("a busca também apaga o valor — filtrar só no findById deixaria a listagem vazando")
        void deveComprovarBuscaSemAcao_quandoExecutaOCenario() {
            comAcao( false );
            var linha = new ParceiroResponseDTO( id, "Parceiro", "12345678909", "a@b.c", "SP",
                    new BigDecimal( "12.50" ), 3 );
            when( repository.search( any(), any() ) )
                    .thenReturn( new PageImpl<>( List.of( linha ), PageRequest.of( 0, 10 ), 1 ) );

            var pagina = service.search( new br.com.gems.app.model.params.ParceiroFilterParams(),
                    PageRequest.of( 0, 10 ) );

            assertThat( pagina.getContent() ).singleElement()
                    .extracting( ParceiroResponseDTO::getComissao ).isNull();
        }

        @Test
        @DisplayName("a gravação NÃO sobrescreve a comissão de quem não pode lê-la")
        void deveComprovarGravacaoSemAcaoNaoApaga_quandoExecutaOCenario() {
            comAcao( false );
            var existente = parceiroComComissao();
            when( repository.findById( id ) ).thenReturn( Optional.of( existente ) );

            // O payload de quem não vê o campo o traz null, porque foi assim que ele o recebeu.
            var dto = new ParceiroDTO();
            dto.setNome( "Parceiro" );
            dto.setComissao( null );

            service.save( id, dto );

            // Sem a guarda no aplicar(), a autorização de LEITURA viraria uma autorização de
            // ESCRITA ao contrário, e o valor sumiria no primeiro salvamento.
            assertThat( existente.getComissao() ).isEqualByComparingTo( "12.50" );
        }
    }

    @Nested
    @DisplayName("A validação")
    class Validacao {

        @Test
        @DisplayName("é acumulada: todas as violações saem de uma vez")
        void deveComprovarAcumulaViolacoes_quandoExecutaOCenario() {
            comAcao( true );
            var dto = new ParceiroDTO();
            dto.setNome( null );
            dto.setComissao( new BigDecimal( "-1" ) );

            assertThatThrownBy( () -> service.insert( dto ) )
                    .isInstanceOf( BusinessException.class );

            // Validação fail-fast pararia na primeira e o usuário descobriria um erro por submit.
            // Os services de normalização acumulam na mesma lista e nunca lançam sozinhos.
            verify( pessoaService ).validar( any(), any(), any() );
            verify( contatoService ).validar( any(), any() );
            verify( enderecoService ).validar( any(), any() );
        }

        @Test
        @DisplayName("recusa nome duplicado")
        void deveComprovarRecusaNomeDuplicado_quandoExecutaOCenario() {
            comAcao( true );
            when( repository.existsByNomeIgnoreCase( "Parceiro" ) ).thenReturn( true );

            var dto = new ParceiroDTO();
            dto.setNome( "Parceiro" );

            assertThatThrownBy( () -> service.insert( dto ) )
                    .isInstanceOf( BusinessException.class );
        }
    }

    @Test
    @DisplayName("parceiro inexistente é erro de negócio, não NullPointerException")
    void deveComprovarInexistenteEhErroDeNegocio_quandoExecutaOCenario() {
        when( repository.findById( id ) ).thenReturn( Optional.empty() );

        assertThatThrownBy( () -> service.findById( id ) )
                .isInstanceOf( BusinessException.class )
                .hasMessageContaining( "não encontrado" );
    }

    private void comAcao( boolean possui ) {
        when( usuarioAutenticadoService.possuiAcao( AcaoSistema.VISUALIZAR_COMISSAO ) ).thenReturn( possui );
    }

    private Parceiro parceiroComComissao() {
        return Parceiro.builder()
                .id( id )
                .nome( "Parceiro" )
                .comissao( new BigDecimal( "12.50" ) )
                .solicitacoesConcluidas( 3 )
                .build();
    }
}
