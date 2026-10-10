package br.com.gems.app.demo.repository;

import br.com.gems.app.model.entity.demo.DemoRegistro;
import br.com.gems.app.model.params.DemoRegistroFilterParams;
import br.com.gems.app.model.response.DemoRegistroResponseDTO;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import br.com.gems.utils.ObjectUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRegistroRepository
        extends
            JpaRepository<DemoRegistro, UUID>,
            BaseCustomJpaRepository<DemoRegistro> {
    default Page<DemoRegistroResponseDTO> search( DemoRegistroFilterParams filter, Pageable pageable,
            boolean restricted ) {
        return new PageImpl<>( searchQuery( filter, pageable, restricted ), pageable, countQuery( filter ) );
    }
    private Long countQuery( DemoRegistroFilterParams filter ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();
        hql.append( " SELECT count(e.id) " );
        hql.append( " FROM DemoRegistro e " );
        appendFilters( filter, hql, params );
        return executeCountHql( hql, params );
    }
    private List<DemoRegistroResponseDTO> searchQuery( DemoRegistroFilterParams filter, Pageable pageable,
            boolean restricted ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();
        hql.append( " SELECT new br.com.gems.app.model.response.DemoRegistroResponseDTO( " );
        hql.append( " e.id, e.nome, e.descricao, " );
        hql.append( restricted ? " e.valorRestrito " : " cast(null as BigDecimal) " );
        hql.append( " ) " );
        hql.append( " FROM DemoRegistro e " );
        appendFilters( filter, hql, params );
        return executeHql( hql, params, pageable, DemoRegistroResponseDTO.class );
    }
    private void appendFilters( DemoRegistroFilterParams filter, StringBuilder hql, HashMap<String, Object> params ) {
        hql.append( " WHERE 1=1 " );
        if ( ObjectUtil.isNotNullAndNotEmpty( filter ) && ObjectUtil.isNotNullAndNotEmpty( filter.nome() ) ) {
            hql.append( " AND LOWER(e.nome) LIKE :nome " );
            params.put( "nome", "%" + filter.nome().toLowerCase( Locale.ROOT ) + "%" );
        }
    }
}
