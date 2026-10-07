package br.com.gems.app.normalizacao.repository;

import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso à identidade civil normalizada.
 *
 * <p>
 * Todos os métodos abaixo enxergam apenas registros ativos: o {@code @SQLRestriction} de
 * {@link Pessoa} já filtra os excluídos, que é exatamente a semântica da regra "CPF único entre os
 * ativos".
 */
@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, UUID>, BaseCustomJpaRepository<Pessoa> {

    /** Base da busca da primeira etapa do cadastro. */
    Optional<Pessoa> findByCpf( String cpf );

    @Query("SELECT p FROM Pessoa p "
            + "WHERE LOWER(concat(p.primeiroNome, ' ', p.sobrenome)) LIKE LOWER(concat('%', :nome, '%')) "
            + "ORDER BY p.primeiroNome, p.sobrenome")
    List<Pessoa> findByParteDoNomeCompleto( @Param("nome") String nome, Pageable pageable );

    boolean existsByCpf( String cpf );

    boolean existsByCpfAndIdNot( String cpf, UUID id );

}
