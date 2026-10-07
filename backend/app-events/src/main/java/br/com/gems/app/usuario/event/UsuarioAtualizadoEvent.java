package br.com.gems.app.usuario.event;

import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;

/**
 * Publicado quando os dados cadastrais ou o perfil de atuação de um usuário mudam.
 *
 * <p>
 * Duas coisas podem precisar de sincronização: nome/sobrenome/e-mail no provedor de identidade,
 * e a filiação a grupo — trocar de perfil significa sair de um grupo e entrar em outro, o que muda
 * as permissões efetivas do usuário sem que o sistema atribua role nenhuma.
 *
 * <p>
 * O login <b>não</b> muda por este caminho: ele é a identidade da conta e está fora de escopo
 * desta entrega.
 *
 * @param usuario
 *            credencial já persistida, com o identificador do Keycloak preenchido.
 * @param perfil
 *            perfil de atuação corrente, que determina o grupo de destino.
 * @param email
 *            e-mail atualizado.
 * @param primeiroNome
 *            primeiro nome atualizado.
 * @param sobrenome
 *            sobrenome atualizado.
 */
public record UsuarioAtualizadoEvent(Usuario usuario,
        PerfilAtuacao perfil,
        String email,
        String primeiroNome,
        String sobrenome) {
}
