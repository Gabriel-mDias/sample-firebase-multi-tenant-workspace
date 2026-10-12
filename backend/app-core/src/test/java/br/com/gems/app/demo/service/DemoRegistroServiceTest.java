package br.com.gems.app.demo.service;

import br.com.gems.app.demo.repository.DemoRegistroRepository;
import br.com.gems.app.model.dto.DemoRegistroDTO;
import br.com.gems.app.model.entity.demo.DemoRegistro;
import br.com.gems.app.model.params.DemoRegistroFilterParams;
import br.com.gems.app.model.response.DemoRegistroResponseDTO;
import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import br.com.gems.exception.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoRegistroServiceTest {
    private static final String ID_NAO_INFORMADO = "O id não foi informado!";
    private final DemoRegistroRepository repository = mock( DemoRegistroRepository.class );
    private final UsuarioAutenticadoService usuario = mock( UsuarioAutenticadoService.class );
    private DemoRegistroService service;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new DemoRegistroService( repository, usuario );
    }

    @Test
    void deveOmitirValorRestrito_quandoLeSemAcaoDeCampo() {
        when( repository.findById( id ) ).thenReturn( Optional.of( registro() ) );

        var response = service.findById( id );

        assertThat( response.valorRestrito() ).isNull();
    }

    @Test
    void devePreservarValorRestrito_quandoPayloadTentaSobrescreverSemAcao() {
        var existing = registro();
        when( repository.findById( id ) ).thenReturn( Optional.of( existing ) );
        when( repository.save( any() ) ).thenAnswer( call -> call.getArgument( 0 ) );

        var response = service.save( id, new DemoRegistroDTO( id, "Alterado", "Descrição", BigDecimal.TEN ) );

        assertThat( existing.getValorRestrito() ).isEqualByComparingTo( "123.45" );
        assertThat( response.valorRestrito() ).isNull();
        verify( repository ).save( existing );
    }

    @Test
    void deveIgnorarValorRestrito_quandoInsereSemAcaoDeCampo() {
        when( repository.save( any() ) ).thenAnswer( call -> call.getArgument( 0 ) );

        var response = service.insert( new DemoRegistroDTO( null, "Registro", "Descrição", BigDecimal.TEN ) );

        assertThat( response.valorRestrito() ).isNull();
    }

    @Test
    void devePermitirValorRestrito_quandoSnapshotSQLAutorizaCampo() {
        when( usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO ) ).thenReturn( true );
        when( repository.findById( id ) ).thenReturn( Optional.of( registro() ) );

        var response = service.findById( id );

        assertThat( response.valorRestrito() ).isEqualByComparingTo( "123.45" );
    }

    @Test
    void deveRestringirProjecaoNaBusca_quandoNaoPodeLerCampo() {
        var filter = new DemoRegistroFilterParams( "Registro" );
        var pageable = PageRequest.of( 0, 10 );
        when( repository.search( eq( filter ), eq( pageable ), eq( false ) ) ).thenReturn(
                new PageImpl<>( List.of( new DemoRegistroResponseDTO( id, "Registro", "Descrição", null ) ) ) );

        var page = service.search( filter, pageable );

        assertThat( page.getContent().getFirst().valorRestrito() ).isNull();
        verify( repository ).search( filter, pageable, false );
    }

    @Test
    void deveAcumularValidacao_quandoNomeEDescricaoSaoInvalidos() {
        var dto = new DemoRegistroDTO( id, "", "x".repeat( 1001 ), null );

        assertThatThrownBy( () -> service.insert( dto ) ).isInstanceOf( BusinessException.class )
                .satisfies( failure -> {
                    var business = (BusinessException) failure;
                    assertThat( business.getDetalhes() ).hasSizeGreaterThanOrEqualTo( 2 );
                } );
    }

    @Test
    void deveAcumularTodosOsErrosNaOrdemDoContrato_quandoTodosOsCamposSaoInvalidosNaInsercao() {
        when( usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO ) ).thenReturn( true );
        var dto = new DemoRegistroDTO( id, " ", "x".repeat( 1001 ), new BigDecimal( "1.001" ) );

        assertThatThrownBy( () -> service.insert( dto ) ).isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( "Informe o nome.", "Descrição deve ter no máximo 1000 caracteres.",
                        "ID é gerado pelo servidor.",
                        "Valor restrito deve ter até 17 dígitos inteiros e 2 decimais." ) );
        verifyNoInteractions( repository );
    }

    @Test
    void deveAcumularNomeLongoEValorExcedente_quandoAlteraRegistroSemInformarId() {
        when( usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO ) ).thenReturn( true );
        var dto = new DemoRegistroDTO( id, "n".repeat( 151 ), "ok", new BigDecimal( "123456789012345678.00" ) );

        assertThatThrownBy( () -> service.save( id, dto ) ).isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( "Nome deve ter no máximo 150 caracteres.",
                        "Valor restrito deve ter até 17 dígitos inteiros e 2 decimais." ) );
        verifyNoInteractions( repository );
    }

    @Test
    void deveIgnorarValorRestritoInvalido_quandoUsuarioNaoPodeEscreverCampo() {
        var dto = new DemoRegistroDTO( null, "Registro", "ok", new BigDecimal( "1.001" ) );
        when( repository.save( any() ) ).thenAnswer( call -> call.getArgument( 0 ) );

        var response = service.insert( dto );

        assertThat( response.nome() ).isEqualTo( "Registro" );
    }

    @Test
    void deveLancarExcecaoDeNegocio_quandoRegistroNaoFoiInformado() {
        assertThatThrownBy( () -> service.insert( null ) ).isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( "Informe o registro." ) );
        verifyNoInteractions( repository );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void deveLancarExcecaoDeNegocio_quandoIdTextoNaoInformado( String texto ) {
        assertThatThrownBy( () -> service.findById( texto ) ).isInstanceOf( BusinessException.class )
                .hasMessage( ID_NAO_INFORMADO );
        verifyNoInteractions( repository );
    }

    @Test
    void deveLancarExcecaoDeNegocio_quandoIdUuidNaoInformadoNaConsulta() {
        assertThatThrownBy( () -> service.findById( (UUID) null ) ).isInstanceOf( BusinessException.class )
                .hasMessage( ID_NAO_INFORMADO );
        verifyNoInteractions( repository );
    }

    @Test
    void deveLancarExcecaoDeNegocio_quandoIdUuidNaoInformadoNaExclusao() {
        assertThatThrownBy( () -> service.delete( null ) ).isInstanceOf( BusinessException.class )
                .hasMessage( ID_NAO_INFORMADO );
        verifyNoInteractions( repository );
    }

    @Test
    void deveLancarExcecaoDeNegocio_quandoIdUuidNaoInformadoNaAlteracao() {
        var dto = new DemoRegistroDTO( null, "Registro", "ok", null );

        assertThatThrownBy( () -> service.save( null, dto ) ).isInstanceOf( BusinessException.class )
                .hasMessage( ID_NAO_INFORMADO );
        verifyNoInteractions( repository );
    }

    @Test
    void deveConsultarComUuidConvertido_quandoIdTextoValido() {
        when( repository.findById( id ) ).thenReturn( Optional.of( registro() ) );

        var response = service.findById( id.toString() );

        assertThat( response.id() ).isEqualTo( id );
        verify( repository ).findById( id );
    }

    private DemoRegistro registro() {
        return DemoRegistro.builder().id( id ).nome( "Registro" ).descricao( "Descrição" )
                .valorRestrito( new BigDecimal( "123.45" ) ).build();
    }
}
