// origem: local; precedencia: local-vence; versao: 2.1.0
package br.com.gems.{app}.{dominio}.repository;

import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import br.com.gems.utils.ObjectUtil;
import br.com.gems.{app}.model.entity.{schema}.{Entidade}Entity;
import br.com.gems.{app}.model.params.{dominio}.{Entidade}FilterParams;
import br.com.gems.{app}.model.response.{dominio}.{Entidade}ResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

// Forma: rules/backend-java/repositories.md. Tétrade obrigatória: search (default, público) + countQuery +
// searchQuery + appendFilters (privados). JOINs idênticos em countQuery e searchQuery. Sem SQL nativo.
@Repository
public interface {Entidade}Repository
        extends
            JpaRepository<{Entidade}Entity, UUID>,
            BaseCustomJpaRepository<{Entidade}Entity> {

    default Page<{Entidade}ResponseDTO> search( {Entidade}FilterParams filterParams, Pageable pageable ) {
        return new PageImpl<>( this.searchQuery( filterParams, pageable ), pageable, this.countQuery( filterParams ) );
    }

    private Long countQuery( {Entidade}FilterParams filterParams ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append( " SELECT count(e.id) " );
        hql.append( " FROM {Entidade}Entity e " );
        hql.append( " JOIN e.{relacionado} r " );
        appendFilters( filterParams, hql, params );

        return this.executeCountHql( hql, params );
    }

    private List<{Entidade}ResponseDTO> searchQuery( {Entidade}FilterParams filterParams, Pageable pageable ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append( " SELECT new br.com.gems.{app}.model.response.{dominio}.{Entidade}ResponseDTO( " );
        hql.append( "       e.id, " );
        hql.append( "       e.nome, " );
        hql.append( "       e.tipo, " );
        hql.append( "       r.nome, " );
        hql.append( "       e.dataCriacao " );
        hql.append( " ) " );
        hql.append( " FROM {Entidade}Entity e " );
        hql.append( " JOIN e.{relacionado} r " );
        appendFilters( filterParams, hql, params );

        return this.executeHql( hql, params, pageable, {Entidade}ResponseDTO.class );
    }

    private void appendFilters( {Entidade}FilterParams filterParams, StringBuilder hql,
            HashMap<String, Object> params ) {
        hql.append( " WHERE 1=1 " );

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getNome() ) ) {
            hql.append( " AND LOWER(e.nome) LIKE :nome " );
            params.put( "nome", "%" + filterParams.getNome().toLowerCase() + "%" );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getTipo() ) ) {
            hql.append( " AND e.tipo = :tipo " );
            params.put( "tipo", filterParams.getTipo() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getDataMin() ) ) {
            hql.append( " AND e.dataCriacao >= :dataMin " );
            params.put( "dataMin", filterParams.getDataMin().atStartOfDay() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getDataMax() ) ) {
            hql.append( " AND e.dataCriacao < :dataMax " );
            params.put( "dataMax", filterParams.getDataMax().plusDays( 1 ).atStartOfDay() );
        }
    }
}
