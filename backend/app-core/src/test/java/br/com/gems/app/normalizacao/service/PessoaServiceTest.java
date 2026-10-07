package br.com.gems.app.normalizacao.service;

import br.com.gems.app.model.dto.PessoaDTO;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.enums.Sexo;
import br.com.gems.app.normalizacao.mapper.PessoaMapper;
import br.com.gems.app.normalizacao.repository.PessoaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobertura das regras V1 a V7 do {@code data-model.md} da feature 007-vinculos-de-servico.
 *
 * <p>
 * O {@link PessoaMapper} é mockado com o comportamento de atualização necessário para proteger a
 * preservação da identidade: o payload não pode sobrescrever o id da pessoa já persistida.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PessoaServiceTest {

    private static final UUID ID_PESSOA = UUID.fromString( "11111111-1111-1111-1111-111111111111" );
    private static final UUID ID_OUTRA_PESSOA = UUID.fromString( "22222222-2222-2222-2222-222222222222" );

    @Mock
    private PessoaRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PessoaMapper mapper;
    private PessoaService service;

    @BeforeEach
    void setUp() {
        mapper = mock( PessoaMapper.class );
        service = new PessoaService( mapper, repository, eventPublisher );

        doAnswer( invocation -> {
            var dto = invocation.getArgument( 0, PessoaDTO.class );
            var entity = invocation.getArgument( 1, Pessoa.class );
            entity.setPrimeiroNome( dto.getPrimeiroNome() );
            entity.setSobrenome( dto.getSobrenome() );
            entity.setDataNascimento( dto.getDataNascimento() );
            entity.setCpf( dto.getCpf() );
            entity.setRg( dto.getRg() );
            entity.setSexo( dto.getSexo() );
            entity.setDataObito( dto.getDataObito() );
            entity.setDataAproximada( dto.getDataAproximada() );
            return null;
        } ).when( mapper ).atualizar( any( PessoaDTO.class ), any( Pessoa.class ) );

        when( mapper.toDTO( any( Pessoa.class ) ) ).thenAnswer( invocation -> {
            var entity = invocation.getArgument( 0, Pessoa.class );
            return new PessoaDTO( entity.getId(), entity.getPrimeiroNome(), entity.getSobrenome(),
                    entity.getDataNascimento(), entity.getCpf(), entity.getRg(), entity.getSexo(),
                    entity.getDataObito(), entity.getDataAproximada() );
        } );

        when( repository.save( any( Pessoa.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
    }

    @Test
    @DisplayName("V1 a V5: payload vazio acumula todas as mensagens de uma vez, nunca a primeira")
    void deveComprovarValidacaoEhAcumuladaNuncaFailFast_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( new PessoaDTO(), null, true, errors );

        assertThat( errors )
                .contains(
                        "O primeiro nome é obrigatório.",
                        "O sobrenome é obrigatório.",
                        "A data de nascimento é obrigatória.",
                        "O CPF é obrigatório." );
    }

    @Test
    @DisplayName("PessoaDTO nulo acusa que os dados pessoais são obrigatórios")
    void deveComprovarPessoaNulaFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( null, null, true, errors );

        assertThat( errors ).containsExactly( "Os dados pessoais são obrigatórios." );
    }

    @Test
    @DisplayName("V4: data de nascimento no futuro é recusada")
    void deveComprovarDataNascimentoNoFuturoFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setDataNascimento( LocalDate.now().plusDays( 1 ) );

        service.validar( dto, null, errors );

        assertThat( errors ).contains( "A data de nascimento deve estar no passado." );
    }

    @Test
    @DisplayName("V4: data de nascimento igual a hoje também é recusada")
    void deveComprovarDataNascimentoHojeFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setDataNascimento( LocalDate.now() );

        service.validar( dto, null, errors );

        assertThat( errors ).contains( "A data de nascimento deve estar no passado." );
    }

    @Test
    @DisplayName("V4: data de nascimento no passado passa sem erro")
    void deveComprovarDataNascimentoNoPassadoPassa_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setDataNascimento( LocalDate.now().minusYears( 30 ) );
        when( repository.existsByCpf( "12345678901" ) ).thenReturn( false );

        service.validar( dto, null, errors );

        assertThat( errors ).doesNotContain( "A data de nascimento deve estar no passado." );
    }

    @Test
    @DisplayName("V6: CPF com menos de 11 dígitos é recusado")
    void deveComprovarCpfComMenosDeOnzeDigitosFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setCpf( "123456789" );

        service.validar( dto, null, errors );

        assertThat( errors ).containsExactly( "O CPF deve conter 11 dígitos." );
        verify( repository, never() ).existsByCpf( any() );
    }

    @Test
    @DisplayName("V6: CPF com mais de 11 dígitos é recusado")
    void deveComprovarCpfComMaisDeOnzeDigitosFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setCpf( "123456789012" );

        service.validar( dto, null, errors );

        assertThat( errors ).containsExactly( "O CPF deve conter 11 dígitos." );
        verify( repository, never() ).existsByCpf( any() );
    }

    @Test
    @DisplayName("CPF com máscara é normalizado para dígitos antes de validar e consultar o repositório")
    void deveComprovarCpfComMascaraEhNormalizadoAntesDaConsulta_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setCpf( "123.456.789-01" );
        when( repository.existsByCpf( "12345678901" ) ).thenReturn( false );

        service.validar( dto, null, errors );

        assertThat( errors ).isEmpty();
        verify( repository ).existsByCpf( "12345678901" );
    }

    @Test
    @DisplayName("V7: CPF duplicado sem idExistente é recusado")
    void deveComprovarCpfDuplicadoSemIdExistenteFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        when( repository.existsByCpf( "12345678901" ) ).thenReturn( true );

        service.validar( dto, null, errors );

        assertThat( errors ).containsExactly( "Já existe uma pessoa cadastrada com este CPF." );
        verify( repository, never() ).existsByCpfAndIdNot( any(), any() );
    }

    @Test
    @DisplayName("V7: CPF duplicado com idExistente não acusa quando o único registro é a própria pessoa")
    void deveComprovarCpfDuplicadoComIdExistenteDaPropriaPessoaPassa_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        when( repository.existsByCpfAndIdNot( "12345678901", ID_PESSOA ) ).thenReturn( false );

        service.validar( dto, ID_PESSOA, errors );

        assertThat( errors ).isEmpty();
        verify( repository ).existsByCpfAndIdNot( "12345678901", ID_PESSOA );
        verify( repository, never() ).existsByCpf( any() );
    }

    @Test
    @DisplayName("V7: CPF duplicado com idExistente ainda acusa quando pertence a outra pessoa")
    void deveComprovarCpfDuplicadoComIdExistenteDeOutraPessoaFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        when( repository.existsByCpfAndIdNot( "12345678901", ID_PESSOA ) ).thenReturn( true );

        service.validar( dto, ID_PESSOA, errors );

        assertThat( errors ).containsExactly( "Já existe uma pessoa cadastrada com este CPF." );
    }

    @Test
    @DisplayName("Genealogia aceita CPF e nascimento ausentes sem consultar duplicidade")
    void deveComprovarGenealogiaAceitaCpfENascimentoAusentes_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setCpf( null );
        dto.setDataNascimento( null );

        service.validar( dto, null, false, errors );

        assertThat( errors ).isEmpty();
        verify( repository, never() ).existsByCpf( any() );
        verify( repository, never() ).existsByCpfAndIdNot( any(), any() );
    }

    @Test
    @DisplayName("Dois CPFs ausentes não são tratados como identidade duplicada")
    void deveComprovarDoisCpfsAusentesNaoSaoDuplicados_quandoExecutaOCenario() {
        var primeira = pessoaValida();
        primeira.setCpf( null );
        var segunda = pessoaValida();
        segunda.setCpf( null );
        var errorsPrimeira = new ArrayList<String>();
        var errorsSegunda = new ArrayList<String>();

        service.validar( primeira, null, false, errorsPrimeira );
        service.validar( segunda, null, false, errorsSegunda );

        assertThat( errorsPrimeira ).isEmpty();
        assertThat( errorsSegunda ).isEmpty();
        verify( repository, never() ).existsByCpf( any() );
    }

    @Test
    @DisplayName("Genealogia ainda valida formato e unicidade quando o CPF é informado")
    void deveComprovarGenealogiaValidaCpfInformado_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        when( repository.existsByCpf( "12345678901" ) ).thenReturn( true );

        service.validar( dto, null, false, errors );

        assertThat( errors ).containsExactly( "Já existe uma pessoa cadastrada com este CPF." );
    }

    @Test
    @DisplayName("Cliente e usuário continuam exigindo CPF e nascimento")
    void deveComprovarFluxoLegadoContinuaExigindoCpfENascimento_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setCpf( null );
        dto.setDataNascimento( null );

        service.validar( dto, null, true, errors );

        assertThat( errors ).containsExactly(
                "A data de nascimento é obrigatória.",
                "O CPF é obrigatório." );
    }

    @Test
    @DisplayName("Óbito anterior ao nascimento é recusado")
    void deveComprovarObitoAnteriorAoNascimentoFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();
        var dto = pessoaValida();
        dto.setDataObito( dto.getDataNascimento().minusDays( 1 ) );

        service.validar( dto, null, false, errors );

        assertThat( errors ).containsExactly(
                "A data de óbito não pode ser anterior à data de nascimento." );
    }

    @Test
    @DisplayName("buscarPorCpf normaliza a máscara antes de consultar o repositório")
    void deveComprovarBuscarPorCpfNormalizaMascara_quandoExecutaOCenario() {
        var pessoa = pessoaPersistida();
        when( repository.findByCpf( "12345678901" ) ).thenReturn( Optional.of( pessoa ) );

        var encontrada = service.buscarPorCpf( "123.456.789-01" );

        assertThat( encontrada ).contains( pessoa );
        verify( repository ).findByCpf( "12345678901" );
    }

    @Test
    @DisplayName("buscarPorCpf devolve vazio para CPF nulo sem tocar o repositório")
    void deveComprovarBuscarPorCpfNuloDevolveVazio_quandoExecutaOCenario() {
        var encontrada = service.buscarPorCpf( null );

        assertThat( encontrada ).isEmpty();
        verify( repository, never() ).findByCpf( any() );
    }

    @Test
    @DisplayName("buscarPorCpf devolve vazio para CPF em branco sem tocar o repositório")
    void deveComprovarBuscarPorCpfVazioDevolveVazio_quandoExecutaOCenario() {
        var encontrada = service.buscarPorCpf( "" );

        assertThat( encontrada ).isEmpty();
        verify( repository, never() ).findByCpf( any() );
    }

    @Test
    @DisplayName("insert devolve instância gerenciada e grava o CPF só com dígitos")
    void deveComprovarInsertDevolveInstanciaGerenciadaComCpfSemMascara_quandoExecutaOCenario() {
        var dto = pessoaValida();
        dto.setCpf( "123.456.789-01" );

        var salva = service.insert( dto );

        assertThat( salva.getCpf() ).isEqualTo( "12345678901" );
        assertThat( salva.getPrimeiroNome() ).isEqualTo( "Giulia" );
        assertThat( salva.getSobrenome() ).isEqualTo( "Rossi" );

        var captor = ArgumentCaptor.forClass( Pessoa.class );
        verify( repository ).save( captor.capture() );
        assertThat( captor.getValue().getCpf() ).isEqualTo( "12345678901" );
    }

    @Test
    @DisplayName("insert preserva CPF nulo e aplica false como padrão de data aproximada")
    void deveComprovarInsertAceitaCpfNuloEAplicaPadraoDaEstimativa_quandoExecutaOCenario() {
        var dto = pessoaValida();
        dto.setCpf( null );
        dto.setDataAproximada( null );

        var salva = service.insert( dto );

        assertThat( salva.getCpf() ).isNull();
        assertThat( salva.getDataAproximada() ).isFalse();
    }

    @Test
    @DisplayName("save sobre instância existente preserva o id, mesmo com id diferente no DTO")
    void deveComprovarSavePreservaId_quandoExecutaOCenario() {
        var existente = Pessoa.builder().id( ID_PESSOA ).primeiroNome( "Antigo" ).build();
        var dto = pessoaValida();
        dto.setId( ID_OUTRA_PESSOA );

        var salva = service.save( existente, dto );

        assertThat( salva.getId() ).isEqualTo( ID_PESSOA );
        assertThat( salva.getPrimeiroNome() ).isEqualTo( "Giulia" );
    }

    @Test
    @DisplayName("toDTO mapeia todos os campos da pessoa")
    void deveComprovarToDtoMapeiaTodosOsCampos_quandoExecutaOCenario() {
        var pessoa = pessoaPersistida();

        var dto = service.toDTO( pessoa );

        assertThat( dto.getId() ).isEqualTo( ID_PESSOA );
        assertThat( dto.getPrimeiroNome() ).isEqualTo( "Giulia" );
        assertThat( dto.getSobrenome() ).isEqualTo( "Rossi" );
        assertThat( dto.getDataNascimento() ).isEqualTo( LocalDate.of( 1990, 5, 20 ) );
        assertThat( dto.getCpf() ).isEqualTo( "12345678901" );
        assertThat( dto.getRg() ).isEqualTo( "1234567" );
        assertThat( dto.getSexo() ).isEqualTo( Sexo.FEMININO );
        assertThat( dto.getDataObito() ).isEqualTo( LocalDate.of( 2025, 1, 2 ) );
        assertThat( dto.getDataAproximada() ).isTrue();
    }

    private PessoaDTO pessoaValida() {
        var dto = new PessoaDTO();
        dto.setPrimeiroNome( "Giulia" );
        dto.setSobrenome( "Rossi" );
        dto.setDataNascimento( LocalDate.of( 1990, 5, 20 ) );
        dto.setCpf( "12345678901" );
        dto.setRg( "1234567" );
        dto.setSexo( Sexo.FEMININO );
        dto.setDataObito( LocalDate.of( 2025, 1, 2 ) );
        dto.setDataAproximada( true );
        return dto;
    }

    private Pessoa pessoaPersistida() {
        return Pessoa.builder()
                .id( ID_PESSOA )
                .primeiroNome( "Giulia" )
                .sobrenome( "Rossi" )
                .dataNascimento( LocalDate.of( 1990, 5, 20 ) )
                .cpf( "12345678901" )
                .rg( "1234567" )
                .sexo( Sexo.FEMININO )
                .dataObito( LocalDate.of( 2025, 1, 2 ) )
                .dataAproximada( true )
                .build();
    }

}
