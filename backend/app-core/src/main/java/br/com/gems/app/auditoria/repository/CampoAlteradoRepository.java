package br.com.gems.app.auditoria.repository;

import br.com.gems.app.model.entity.auditoria.CampoAlterado;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Os campos alterados de um lote de registros, buscados de uma vez.
 *
 * <p>
 * Existe por causa da <b>busca ampla</b>. O histórico de um registro resolve a coleção pelo
 * {@code @EntityGraph}, mas a busca pode devolver muitas linhas, e juntar {@code JOIN FETCH} com
 * paginação faz o Hibernate recortar em memória. Uma segunda consulta por lote de identificadores
 * custa uma ida ao banco e não tem esse problema.
 *
 * <p>
 * O que ela <b>não</b> faz é uma consulta por registro dentro de um laço: isso seria N+1, e numa
 * página de vinte registros seriam vinte idas.
 */
@Repository
public interface CampoAlteradoRepository
        extends
            JpaRepository<CampoAlterado, UUID>,
            BaseCustomJpaRepository<CampoAlterado> {

    List<CampoAlterado> findByRegistroAuditoriaIdIn( Collection<UUID> registroAuditoriaIds );
}
