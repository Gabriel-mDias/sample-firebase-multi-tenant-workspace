package br.com.gems.app.usuario.event;

import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;

/**
 * Publicado quando um usuário que já teve acesso retorna à equipe.
 *
 * <p>
 * Nenhuma conta nova é criada e nenhuma senha é pedida: a conta existente é reabilitada e
 * recolocada no grupo do perfil de atuação do novo vínculo, que pode ser diferente do anterior.
 *
 * @param usuario
 *            credencial existente, a reativar.
 * @param perfil
 *            perfil de atuação do novo vínculo.
 */
public record UsuarioReativadoEvent(Usuario usuario, PerfilAtuacao perfil) {
}
