package br.com.gems.app.usuario.service;

import br.com.gems.app.model.dto.UsuarioDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.security.PasswordUtil;
import br.com.gems.app.usuario.event.UsuarioAtualizadoEvent;
import br.com.gems.app.usuario.event.UsuarioCriadoEvent;
import br.com.gems.app.usuario.event.UsuarioDesativadoEvent;
import br.com.gems.app.usuario.event.UsuarioReativadoEvent;
import br.com.gems.app.usuario.repository.UsuarioRepository;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Regras da credencial de acesso de um usuário.
 *
 * <p>
 * <b>Este serviço nunca fala com o Keycloak.</b> Ele publica eventos; quem conversa com o
 * provedor de identidade é o {@code UsuarioKeycloakListener}, que roda de forma síncrona, na mesma
 * transação, e sabe se desfazer caso ela não seja confirmada. A separação existe para que a regra de
 * negócio não fique acoplada à API de administração — e para que o envio de e-mail, quando entrar,
 * seja apenas mais um ouvinte do mesmo evento.
 *
 * <p>
 * A ordem "publicar, depois persistir" no {@link #insert} não é acidental: é ela que permite ao
 * {@code CD_KEYCLOAK_ID} ser {@code NOT NULL} no banco. O listener preenche o identificador na
 * instância ainda transiente, e só então a linha nasce — completa.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final ApplicationEventPublisher publisher;

    public UsuarioDTO toDTO( Usuario usuario ) {
        if ( ObjectUtil.isNullOrEmpty( usuario ) ) {
            return null;
        }

        var dto = new UsuarioDTO();
        dto.setId( usuario.getId() );
        dto.setLogin( usuario.getLogin() );
        dto.setAtivo( usuario.getAtivo() );
        // A senha NUNCA sai daqui. O mapeamento é manual justamente para que essa omissão seja
        // explícita e testável: app-model não tem Jackson no classpath, então não há como marcar o
        // campo como somente-escrita por anotação.
        dto.setSenha( null );
        return dto;
    }

    /**
     * Cria a credencial e provisiona a conta no provedor de identidade.
     *
     * @param dto
     *            login e senha vindos do formulário.
     * @param perfil
     *            perfil de atuação, que determina o grupo do Keycloak.
     * @param pessoa
     *            dados civis, usados para nome e sobrenome da conta.
     * @param contato
     *            dados de contato, usados para o e-mail da conta.
     * @return instância <b>gerenciada</b>, pronta para ser referenciada pelo usuário.
     */
    public Usuario insert( UsuarioDTO dto, PerfilAtuacao perfil, Pessoa pessoa, Contato contato ) {
        var usuario = Usuario.builder()
                .login( dto.getLogin().trim() )
                .ativo( true )
                .build();

        // Publicar ANTES de salvar: o listener é síncrono e preenche o keycloakId na instância
        // transiente. Se o Keycloak recusar, a exceção sobe daqui e nenhuma linha chega a existir.
        publisher.publishEvent( new UsuarioCriadoEvent(
                usuario, dto.getSenha(), perfil, contato.getEmail(), pessoa.getPrimeiroNome(),
                pessoa.getSobrenome() ) );

        return repository.save( usuario );
    }

    /**
     * Reabilita uma credencial existente para um novo vínculo, sem pedir senha nova.
     *
     * <p>
     * É o caminho de quem retorna à equipe: o login e a senha continuam os mesmos, e o
     * usuário é recolocado no grupo do perfil do novo vínculo — que pode ser diferente do
     * anterior.
     */
    public Usuario reativar( Usuario usuario, PerfilAtuacao perfil ) {
        usuario.setAtivo( true );
        publisher.publishEvent( new UsuarioReativadoEvent( usuario, perfil ) );
        return repository.save( usuario );
    }

    /**
     * Propaga ao provedor de identidade os dados cadastrais e o perfil correntes.
     *
     * <p>
     * Chamado na edição de um vínculo. O login não muda por aqui — ele é a identidade da conta.
     */
    public void sincronizar( Usuario usuario, PerfilAtuacao perfil, Pessoa pessoa, Contato contato ) {
        publisher.publishEvent( new UsuarioAtualizadoEvent(
                usuario, perfil, contato.getEmail(), pessoa.getPrimeiroNome(), pessoa.getSobrenome() ) );
        repository.save( usuario );
    }

    /**
     * Desativa a credencial ao encerrar um vínculo. A conta não é excluída, aqui nem no Keycloak.
     */
    public void desativar( Usuario usuario ) {
        if ( ObjectUtil.isNullOrEmpty( usuario ) ) {
            return;
        }

        usuario.setAtivo( false );
        publisher.publishEvent( new UsuarioDesativadoEvent( usuario ) );
        repository.save( usuario );
    }

    /**
     * @param exigeSenha
     *            {@code true} quando o acesso está sendo criado; {@code false} quando ele já
     *            existe (edição ou usuário reaproveitado), caso em que login e senha são
     *            somente leitura e não precisam vir no payload.
     */
    public void validar( UsuarioDTO usuario, boolean exigeSenha, List<String> errors ) {
        if ( !exigeSenha ) {
            // Acesso já existente: nada a validar, o formulário nem envia os campos.
            return;
        }

        if ( ObjectUtil.isNullOrEmpty( usuario ) ) {
            errors.add( "Os dados de usuário são obrigatórios." );
            return;
        }

        if ( ObjectUtil.isNullOrEmpty( usuario.getLogin() ) ) {
            errors.add( "O login é obrigatório." );
        } else if ( repository.existsByLogin( usuario.getLogin().trim() ) ) {
            errors.add( "Já existe um usuário com este login." );
        }

        if ( ObjectUtil.isNullOrEmpty( usuario.getSenha() ) ) {
            errors.add( "A senha é obrigatória." );
        } else if ( !PasswordUtil.isValid( usuario.getSenha() ) ) {
            errors.add( "A senha não atende aos critérios de segurança exigidos." );
        }
    }

}
