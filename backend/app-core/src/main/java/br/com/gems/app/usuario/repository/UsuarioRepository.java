package br.com.gems.app.usuario.repository;

import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Acesso às credenciais dos usuárioes (tabela {@code security.USUARIO}).
 *
 * <p>
 * Sem busca paginada: não há tela de gestão de acesso nesta entrega — o usuário existe sempre
 * como parte do agregado do usuário.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID>, BaseCustomJpaRepository<Usuario> {

    boolean existsByLogin( String login );

    boolean existsByLoginAndIdNot( String login, UUID id );

}
