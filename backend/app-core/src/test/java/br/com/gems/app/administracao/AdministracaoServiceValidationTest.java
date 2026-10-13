package br.com.gems.app.administracao;

import br.com.gems.app.administracao.service.AdministracaoService;
import br.com.gems.app.model.dto.TenantCadastroDTO;
import br.com.gems.app.model.dto.UsuarioCadastroDTO;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.firebase.auth.GrantAdministration;
import br.com.gems.firebase.auth.SecurityAdministrationReader;
import br.com.gems.firebase.auth.TenantAdministration;
import br.com.gems.firebase.auth.TenantAdministratorBootstrap;
import br.com.gems.firebase.auth.UserAdministration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdministracaoServiceValidationTest {

    private static final String NOME_USUARIO = "Informe nome de até 150 caracteres.";
    private static final String EMAIL_USUARIO = "Informe e-mail válido de até 254 caracteres.";
    private UserAdministration users;
    private TenantAdministration tenants;
    private GrantAdministration grants;
    private SecurityAdministrationReader reader;
    private TenantAdministratorBootstrap bootstrap;
    private AdministracaoService service;

    @BeforeEach
    void prepare() {
        users = mock( UserAdministration.class );
        tenants = mock( TenantAdministration.class );
        grants = mock( GrantAdministration.class );
        reader = mock( SecurityAdministrationReader.class );
        bootstrap = mock( TenantAdministratorBootstrap.class );
        service = new AdministracaoService( users, tenants, grants, reader, bootstrap );
    }

    @ParameterizedTest
    @CsvSource({ "GOOGLE,@", "PASSWORD,@", "GOOGLE,local@", "PASSWORD,local@", "GOOGLE,local..part@example.test", "PASSWORD,local..part@example.test", "GOOGLE,local@example", "PASSWORD,local@example" })
    void deveValidarEmailAntesDaPortaSQL_quandoUsuarioTemEnderecoMalformado( UsuarioCadastroDTO.Method method, String email ) {
        var dto = new UsuarioCadastroDTO( "Válido", email, method );

        assertThatThrownBy( () -> service.createUser( dto ) )
                .isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( "Informe e-mail válido de até 254 caracteres." ) );
        verifyNoInteractions( users, tenants, grants, reader, bootstrap );
    }

    @Test
    void deveAcumularNomeEmailEMetodoNaOrdemDoContrato_quandoTodosOsCamposDoUsuarioSaoInvalidos() {
        var dto = new UsuarioCadastroDTO( " ", "@", null );

        assertThatThrownBy( () -> service.createUser( dto ) ).isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( NOME_USUARIO, EMAIL_USUARIO, "Informe Google ou senha." ) );
        verifyNoInteractions( users, tenants, grants, reader, bootstrap );
    }

    @Test
    void deveAcumularNomeLongoEEmailLongo_quandoLimitesDoUsuarioSaoExcedidos() {
        var email = "a".repeat( 250 ) + "@b.co";
        var dto = new UsuarioCadastroDTO( "n".repeat( 151 ), email, UsuarioCadastroDTO.Method.PASSWORD );

        assertThatThrownBy( () -> service.createUser( dto ) ).isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( NOME_USUARIO, EMAIL_USUARIO ) );
        verifyNoInteractions( users, tenants, grants, reader, bootstrap );
    }

    @Test
    void deveInformarUsuarioAusente_quandoCadastroNaoFoiEnviado() {
        assertThatThrownBy( () -> service.createUser( null ) ).isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( "Informe o usuário." ) );
        verifyNoInteractions( users, tenants, grants, reader, bootstrap );
    }

    @Test
    void deveAcumularNomeEEmail_quandoAtualizacaoDoUsuarioTemCamposInvalidos() {
        var user = UUID.randomUUID();

        assertThatThrownBy( () -> service.updateUser( user, "", "local..part@example.test" ) )
                .isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() )
                .isEqualTo( List.of( NOME_USUARIO, EMAIL_USUARIO ) );
        verifyNoInteractions( users, tenants, grants, reader, bootstrap );
    }

    @Test
    void deveDelegarAPortaSQL_quandoUsuarioGoogleEValido() {
        var dto = new UsuarioCadastroDTO( "Válido", "valido@example.test", UsuarioCadastroDTO.Method.GOOGLE );
        var created = UUID.randomUUID();
        when( users.preProvisionGoogle( "Válido", "valido@example.test", "CONSULTAR_PROPRIA_CONTA" ) )
                .thenReturn( created );

        var result = service.createUser( dto );

        assertThat( result ).isEqualTo( created );
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void deveAcumularValidacaoAntesDaPortaSQL_quandoCamposAdministrativosSaoInvalidos( String operation, String codeOrAlias, String name, List<String> expected ) {
        var tenant = UUID.randomUUID();

        assertThatThrownBy( () -> {
            switch ( operation ) {
            case "tenant" -> service.createTenant( new TenantCadastroDTO( codeOrAlias, name ) );
            case "rename" -> service.renameTenant( tenant, name );
            case "profile" -> service.createProfile( tenant, codeOrAlias, name );
            case "group" -> service.createGroup( tenant, codeOrAlias, name );
            default -> throw new AssertionError( operation );
            }
        } ).isInstanceOf( BusinessException.class )
                .extracting( failure -> ((BusinessException) failure).getDetalhes() ).isEqualTo( expected );
        verifyNoInteractions( users, tenants, grants, reader, bootstrap );
    }

    private static Stream<Arguments> invalidInputs() {
        var name = "Informe nome de até 255 caracteres.";
        var alias = "Informe alias de até 56 caracteres.";
        var code = "Informe código de até 128 caracteres.";
        var cases = new java.util.ArrayList<Arguments>();
        cases.add( Arguments.of( "tenant", "valido", "n".repeat( 256 ), List.of( name ) ) );
        cases.add( Arguments.of( "tenant", "a".repeat( 57 ), "n".repeat( 256 ), List.of( alias, name ) ) );
        for ( String missing : new String[] { null, "", " " } ) {
            cases.add( Arguments.of( "tenant", missing, missing, List.of( alias, name ) ) );
            cases.add( Arguments.of( "rename", "unused", missing, List.of( name ) ) );
        }
        cases.add( Arguments.of( "rename", "unused", "n".repeat( 256 ), List.of( name ) ) );
        for ( String operation : new String[] { "profile", "group" } ) {
            cases.add( Arguments.of( operation, "c".repeat( 129 ), "n".repeat( 256 ), List.of( code, name ) ) );
            cases.add( Arguments.of( operation, "c".repeat( 129 ), "", List.of( code, name ) ) );
            cases.add( Arguments.of( operation, "", "n".repeat( 256 ), List.of( code, name ) ) );
            for ( String missing : new String[] { null, "", " " } ) {
                cases.add( Arguments.of( operation, missing, missing, List.of( code, name ) ) );
            }
        }
        return cases.stream();
    }

}
