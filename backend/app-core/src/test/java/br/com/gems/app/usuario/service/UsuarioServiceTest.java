package br.com.gems.app.usuario.service;

import br.com.gems.app.model.dto.UsuarioDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.usuario.event.UsuarioAtualizadoEvent;
import br.com.gems.app.usuario.event.UsuarioCriadoEvent;
import br.com.gems.app.usuario.event.UsuarioDesativadoEvent;
import br.com.gems.app.usuario.event.UsuarioReativadoEvent;
import br.com.gems.app.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobertura das regras da credencial de acesso.
 *
 * <p>
 * Duas propriedades merecem destaque: a senha <b>nunca</b> sai em uma leitura, e o evento é
 * publicado <b>antes</b> da persistência no cadastro — é isso que permite gravar o identificador do
 * Keycloak na mesma transação, com a coluna sendo {@code NOT NULL}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    private static final UUID ID_USUARIO = UUID.fromString( "77777777-7777-7777-7777-777777777777" );

    @Mock
    private UsuarioRepository repository;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        when( repository.save( any( Usuario.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
    }

    @Test
    @DisplayName("toDTO NUNCA devolve a senha, em nenhuma circunstância")
    void deveComprovarToDtoNuncaDevolveSenha_quandoExecutaOCenario() {
        var dto = service.toDTO( usuarioPersistido() );

        assertThat( dto.getSenha() ).isNull();
        assertThat( dto.getLogin() ).isEqualTo( "ana.souza" );
        assertThat( dto.getAtivo() ).isTrue();
    }

    @Test
    @DisplayName("toDTO de usuário nulo devolve nulo, sem estourar")
    void deveComprovarToDtoDeNuloDevolveNulo_quandoExecutaOCenario() {
        assertThat( service.toDTO( null ) ).isNull();
    }

    @Test
    @DisplayName("insert publica o evento ANTES de persistir, para que o id do Keycloak já esteja na linha")
    void deveComprovarInsertPublicaAntesDePersistir_quandoExecutaOCenario() {
        service.insert( usuarioDTO( "ana.souza", "Senha@Forte123" ), PerfilAtuacao.OPERADOR, pessoa(), contato() );

        // Se a ordem fosse invertida, o INSERT tentaria gravar CD_KEYCLOAK_ID nulo — e a coluna é
        // NOT NULL.
        var ordem = inOrder( publisher, repository );
        ordem.verify( publisher ).publishEvent( any( UsuarioCriadoEvent.class ) );
        ordem.verify( repository ).save( any( Usuario.class ) );
    }

    @Test
    @DisplayName("insert monta o evento com senha, perfil, e-mail e nome vindos do agregado")
    void deveComprovarInsertMontaEventoCompleto_quandoExecutaOCenario() {
        service.insert( usuarioDTO( "ana.souza", "Senha@Forte123" ), PerfilAtuacao.CONSULTA, pessoa(), contato() );

        var captor = ArgumentCaptor.forClass( UsuarioCriadoEvent.class );
        verify( publisher ).publishEvent( captor.capture() );
        var evento = captor.getValue();

        assertThat( evento.senha() ).isEqualTo( "Senha@Forte123" );
        assertThat( evento.perfil() ).isEqualTo( PerfilAtuacao.CONSULTA );
        assertThat( evento.email() ).isEqualTo( "ana@exemplo.com.br" );
        assertThat( evento.primeiroNome() ).isEqualTo( "Ana" );
        assertThat( evento.sobrenome() ).isEqualTo( "Souza" );
        assertThat( evento.usuario().getLogin() ).isEqualTo( "ana.souza" );
        assertThat( evento.usuario().getAtivo() ).isTrue();
    }

    @Test
    @DisplayName("reativar reabilita a credencial existente sem pedir senha nova")
    void deveComprovarReativarNaoPedeSenhaNova_quandoExecutaOCenario() {
        var existente = usuarioPersistido();
        existente.setAtivo( false );

        service.reativar( existente, PerfilAtuacao.OPERADOR );

        assertThat( existente.getAtivo() ).isTrue();

        var captor = ArgumentCaptor.forClass( UsuarioReativadoEvent.class );
        verify( publisher ).publishEvent( captor.capture() );
        assertThat( captor.getValue().perfil() ).isEqualTo( PerfilAtuacao.OPERADOR );
        verify( publisher ).publishEvent( any( UsuarioReativadoEvent.class ) );
    }

    @Test
    @DisplayName("desativar marca a credencial como inativa e publica o evento de encerramento")
    void deveComprovarDesativarMarcaInativo_quandoExecutaOCenario() {
        var existente = usuarioPersistido();

        service.desativar( existente );

        assertThat( existente.getAtivo() ).isFalse();
        verify( publisher ).publishEvent( any( UsuarioDesativadoEvent.class ) );
        verify( repository ).save( existente );
    }

    @Test
    @DisplayName("desativar usuário nulo não faz nada e não estoura")
    void deveComprovarDesativarNuloNaoFazNada_quandoExecutaOCenario() {
        service.desativar( null );

        verify( repository, org.mockito.Mockito.never() ).save( any( Usuario.class ) );
    }

    @Test
    @DisplayName("sincronizar propaga os dados cadastrais correntes")
    void deveComprovarSincronizarPublicaAtualizacao_quandoExecutaOCenario() {
        service.sincronizar( usuarioPersistido(), PerfilAtuacao.OPERADOR, pessoa(), contato() );

        var captor = ArgumentCaptor.forClass( UsuarioAtualizadoEvent.class );
        verify( publisher ).publishEvent( captor.capture() );

        assertThat( captor.getValue().perfil() ).isEqualTo( PerfilAtuacao.OPERADOR );
        assertThat( captor.getValue().email() ).isEqualTo( "ana@exemplo.com.br" );
    }

    @Test
    @DisplayName("validação acumula login e senha ausentes numa submissão só")
    void deveComprovarValidacaoEhAcumulada_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( new UsuarioDTO(), true, errors );

        assertThat( errors ).containsExactlyInAnyOrder( "O login é obrigatório.", "A senha é obrigatória." );
    }

    @Test
    @DisplayName("login já usado por outro usuário ativo é recusado")
    void deveComprovarLoginDuplicadoFalha_quandoExecutaOCenario() {
        when( repository.existsByLogin( "ana.souza" ) ).thenReturn( true );
        var errors = new ArrayList<String>();

        service.validar( usuarioDTO( "ana.souza", "Senha@Forte123" ), true, errors );

        assertThat( errors ).containsExactly( "Já existe um usuário com este login." );
    }

    @Test
    @DisplayName("senha fraca é recusada pela política já vigente no projeto")
    void deveComprovarSenhaFracaFalha_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( usuarioDTO( "ana.souza", "123456" ), true, errors );

        assertThat( errors ).containsExactly( "A senha não atende aos critérios de segurança exigidos." );
    }

    @Test
    @DisplayName("senha forte passa: minúscula, maiúscula, especial, sem espaço e 12+ caracteres")
    void deveComprovarSenhaForteEhAceita_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( usuarioDTO( "ana.souza", "Senha@Forte123" ), true, errors );

        assertThat( errors ).isEmpty();
    }

    @Test
    @DisplayName("acesso já existente dispensa login e senha: o formulário nem os envia")
    void deveComprovarAcessoExistenteNaoExigeCredenciais_quandoExecutaOCenario() {
        var errors = new ArrayList<String>();

        service.validar( null, false, errors );

        assertThat( errors ).isEmpty();
    }

    private UsuarioDTO usuarioDTO( String login, String senha ) {
        var dto = new UsuarioDTO();
        dto.setLogin( login );
        dto.setSenha( senha );
        return dto;
    }

    private Usuario usuarioPersistido() {
        return Usuario.builder()
                .id( ID_USUARIO )
                .login( "ana.souza" )
                .keycloakId( "kc-0001" )
                .ativo( true )
                .build();
    }

    private Pessoa pessoa() {
        return Pessoa.builder().primeiroNome( "Ana" ).sobrenome( "Souza" ).build();
    }

    private Contato contato() {
        return Contato.builder().email( "ana@exemplo.com.br" ).telefone( "+55 21 99999-0000" ).build();
    }

}
