package br.com.gems.app.usuario.event;

import br.com.gems.app.model.entity.seguranca.Usuario;

/**
 * Publicado ao encerrar o acesso de um usuário.
 *
 * <p>
 * Desativar não é excluir: a conta é <b>desabilitada</b> no provedor de identidade, impedindo
 * novos logins, mas permanece lá — assim como a linha permanece no banco. É o que permite reativar
 * o mesmo acesso, com o mesmo login e a mesma senha, quando o usuário retorna à equipe.
 *
 * @param usuario
 *            credencial a desativar.
 */
public record UsuarioDesativadoEvent(Usuario usuario) {
}
