package br.com.gems.app.normalizacao.repository;

import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Acesso aos dados de contato normalizados.
 *
 * <p>
 * Sem busca paginada: {@code Contato} nunca é listado por si só — ele é sempre parte de um
 * agregado (parceiro, usuário e, nas ondas seguintes, cliente).
 */
@Repository
public interface ContatoRepository extends JpaRepository<Contato, UUID>, BaseCustomJpaRepository<Contato> {
}
