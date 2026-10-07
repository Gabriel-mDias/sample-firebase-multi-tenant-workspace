package br.com.gems.app.normalizacao.repository;

import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Acesso aos endereços normalizados.
 *
 * <p>
 * Sem busca paginada, pelo mesmo motivo de {@link ContatoRepository}.
 */
@Repository
public interface EnderecoRepository extends JpaRepository<Endereco, UUID>, BaseCustomJpaRepository<Endereco> {
}
