package br.com.gems.app.normalizacao.service;

import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.normalizacao.mapper.ContatoMapper;
import br.com.gems.app.normalizacao.repository.ContatoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobertura das regras V8 e V9 do {@code data-model.md} da feature 007-vinculos-de-servico.
 *
 * <p>
 * O {@link ContatoMapper} é mockado com o comportamento de atualização necessário para proteger a
 * preservação da identidade: o payload não pode sobrescrever o id do contato já persistido.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContatoServiceTest {

    private static final UUID ID_CONTATO = UUID.fromString( "11111111-1111-1111-1111-111111111111" );
    private static final UUID ID_OUTRO_CONTATO = UUID.fromString( "22222222-2222-2222-2222-222222222222" );

    @Mock
    private ContatoRepository repository;

    private ContatoMapper mapper;
    private ContatoService service;

    @BeforeEach
    void setUp() {
        mapper = mock( ContatoMapper.class );
        service = new ContatoService( mapper, repository );

        doAnswer( invocation -> {
            var dto = invocation.getArgument( 0, ContatoDTO.class );
            var entity = invocation.getArgument( 1, Contato.class );
            entity.setEmail( dto.getEmail() );
            entity.setTelefone( dto.getTelefone() );
            return null;
        } ).when( mapper ).atualizar( any( ContatoDTO.class ), any( Contato.class ) );

        when( repository.save( any( Contato.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
    }

    @Test
    @DisplayName("ContatoDTO nulo acusa que os dados de contato são obrigatórios")
    void deveComprovarContatoNuloFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( null, errors );

        assertThat( errors ).containsExactly( "Os dados de contato são obrigatórios." );
    }

    @Test
    @DisplayName("V8 e V9: e-mail e telefone ausentes acumulam duas mensagens distintas")
    void deveComprovarEmailETelefoneAusentesAcumulamMensagens_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( new ContatoDTO(), errors );

        assertThat( errors ).containsExactlyInAnyOrder(
                "O e-mail é obrigatório.",
                "O telefone é obrigatório." );
    }

    @Test
    @DisplayName("ContatoDTO válido não acumula nenhuma mensagem")
    void deveComprovarContatoValidoNaoAcumulaMensagem_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( contatoValido(), errors );

        assertThat( errors ).isEmpty();
    }

    @Test
    @DisplayName("insert devolve instância gerenciada")
    void deveComprovarInsertDevolveInstanciaGerenciada_quandoExecutaOCenario() {
        var salvo = service.insert( contatoValido() );

        assertThat( salvo.getEmail() ).isEqualTo( "consolato@esteri.it" );
        assertThat( salvo.getTelefone() ).isEqualTo( "+55 21 3534-1300" );
        verify( repository ).save( any( Contato.class ) );
    }

    @Test
    @DisplayName("save sobre instância existente preserva o id, mesmo com id diferente no DTO")
    void deveComprovarSavePreservaId_quandoExecutaOCenario() {
        var existente = Contato.builder().id( ID_CONTATO ).email( "antigo@esteri.it" ).telefone( "0000" ).build();
        var dto = contatoValido();
        dto.setId( ID_OUTRO_CONTATO );

        var salvo = service.save( existente, dto );

        assertThat( salvo.getId() ).isEqualTo( ID_CONTATO );
        assertThat( salvo.getEmail() ).isEqualTo( "consolato@esteri.it" );
    }

    @Test
    @DisplayName("delete com argumento nulo não estoura e não chama o repositório")
    void deveComprovarDeleteComArgumentoNuloNaoEstoura_quandoExecutaOCenario() {
        assertThatCode( () -> service.delete( null ) ).doesNotThrowAnyException();

        verify( repository, never() ).delete( any() );
    }

    private ContatoDTO contatoValido() {
        var dto = new ContatoDTO();
        dto.setEmail( "consolato@esteri.it" );
        dto.setTelefone( "+55 21 3534-1300" );
        return dto;
    }

}
