package br.com.gems.app.usuario.event;

import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;

/**
 * Publicado quando um acesso novo precisa ser provisionado no provedor de identidade.
 *
 * <p>
 * O listener é <b>síncrono e roda na mesma transação</b> do cadastro: se a criação no Keycloak
 * falhar, a exceção sobe e nada é gravado; se a transação cair depois, uma compensação remove a
 * conta órfã. É o que sustenta a exigência de zero usuários órfãos.
 *
 * <p>
 * O evento carrega o {@link Usuario} ainda <b>transiente</b> — o listener preenche nele o
 * identificador devolvido pelo Keycloak, e só então o serviço persiste a linha. Assim o
 * {@code CD_KEYCLOAK_ID} nunca precisa ser nulo no banco, nem por um instante.
 *
 * <p>
 * A senha trafega aqui e morre no listener: ela nunca é persistida.
 *
 * @param usuario
 *            credencial em construção, ainda não persistida.
 * @param senha
 *            senha em texto limpo, definitiva.
 * @param perfil
 *            perfil de atuação, que determina o grupo do Keycloak.
 * @param email
 *            e-mail do usuário.
 * @param primeiroNome
 *            primeiro nome do usuário.
 * @param sobrenome
 *            sobrenome do usuário.
 */
public record UsuarioCriadoEvent(Usuario usuario,
        String senha,
        PerfilAtuacao perfil,
        String email,
        String primeiroNome,
        String sobrenome) {
}
