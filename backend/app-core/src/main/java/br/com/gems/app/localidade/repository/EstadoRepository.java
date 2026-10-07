package br.com.gems.app.localidade.repository;

import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso ao catálogo de unidades federativas / províncias.
 *
 * <p>
 * Mesma natureza de {@link PaisRepository}: dado de referência, sem soft delete e sem busca
 * paginada.
 */
@Repository
public interface EstadoRepository extends JpaRepository<Estado, UUID>, BaseCustomJpaRepository<Estado> {

    /**
     * O {@code estado-entity-graph} traz o {@code pais} junto. É obrigatório aqui porque o
     * {@code ParceiroService} lê {@code estado.getPais().getNome()} para validar V11 (sede em
     * território brasileiro) — com {@code open-in-view: false} e o {@code ManyToOne} LAZY, resolver
     * isso pelo proxy dependeria de a chamada estar dentro da transação. O graph elimina a
     * dependência e a consulta extra.
     */
    @Override
    @EntityGraph(value = "estado-entity-graph")
    Optional<Estado> findById( UUID id );

    @EntityGraph(value = "estado-entity-graph")
    List<Estado> findAllByPaisIdOrderByNomeAsc( UUID paisId );

    /**
     * Resolve a UF devolvida pela consulta de CEP para o estado gerenciado.
     *
     * <p>
     * {@code findFirst} e não {@code find}: a sigla é única <b>dentro de um país</b>, mas nada no
     * schema impede que dois países compartilhem uma — e uma exceção de resultado múltiplo aqui
     * derrubaria a consulta de CEP por um dado de referência mal cadastrado.
     */
    @EntityGraph(value = "estado-entity-graph")
    Optional<Estado> findFirstBySiglaEstadoIgnoreCaseAndPaisNomeIgnoreCase( String siglaEstado, String paisNome );

}
