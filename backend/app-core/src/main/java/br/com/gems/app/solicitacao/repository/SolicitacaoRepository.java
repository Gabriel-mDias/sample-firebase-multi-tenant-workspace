package br.com.gems.app.solicitacao.repository;

import br.com.gems.app.model.entity.solicitacao.Solicitacao;
import br.com.gems.app.model.params.SolicitacaoFilterParams;
import br.com.gems.app.model.response.SolicitacaoResponseDTO;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import br.com.gems.utils.ObjectUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID>, BaseCustomJpaRepository<Solicitacao> {

    @Override
    @EntityGraph(value = "solicitacao-entity-graph")
    Optional<Solicitacao> findById( UUID id );

    /** Usado pelo delete do parceiro para recusar a exclusão de quem tem solicitação viva. */
    boolean existsByParceiroId( UUID parceiroId );

    default Page<SolicitacaoResponseDTO> search( SolicitacaoFilterParams filterParams, Pageable pageable ) {
        return new PageImpl<>( this.searchQuery( filterParams, pageable ), pageable, this.countQuery( filterParams ) );
    }

    private Long countQuery( SolicitacaoFilterParams filterParams ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append( " SELECT count(s.id) " );
        hql.append( " FROM Solicitacao s " );
        hql.append( " JOIN s.parceiro pa " );
        appendFilters( filterParams, hql, params );
        return this.executeCountHql( hql, params );
    }

    private List<SolicitacaoResponseDTO> searchQuery( SolicitacaoFilterParams filterParams, Pageable pageable ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append( " SELECT new br.com.gems.app.model.response.SolicitacaoResponseDTO( " );
        hql.append( "       s.id, " );
        hql.append( "       s.titulo, " );
        hql.append( "       s.situacao, " );
        hql.append( "       pa.id, " );
        hql.append( "       pa.nome, " );
        hql.append( "       s.dataCriacao, " );
        hql.append( "       s.dataConclusao " );
        hql.append( " ) " );
        hql.append( " FROM Solicitacao s " );
        hql.append( " JOIN s.parceiro pa " );

        appendFilters( filterParams, hql, params );
        return this.executeHql( hql, params, resolveSort( pageable ), SolicitacaoResponseDTO.class );
    }

    /** Tradução de propriedade do DTO para alias HQL. Ver a nota longa em {@code ParceiroRepository}. */
    private Pageable resolveSort( Pageable pageable ) {
        var orders = new ArrayList<Sort.Order>();

        for ( Sort.Order order : pageable.getSort() ) {
            var hqlProperty = switch ( order.getProperty() ) {
                case "titulo" -> "s.titulo";
                case "situacao" -> "s.situacao";
                case "parceiroNome" -> "pa.nome";
                case "dataCriacao" -> "s.dataCriacao";
                case "dataConclusao" -> "s.dataConclusao";
                default -> null;
            };

            if ( hqlProperty != null ) {
                orders.add( new Sort.Order( order.getDirection(), hqlProperty ) );
            }
        }

        var sort = orders.isEmpty() ? Sort.by( Sort.Direction.DESC, "s.dataCriacao" ) : Sort.by( orders );
        return PageRequest.of( pageable.getPageNumber(), pageable.getPageSize(), sort );
    }

    private void appendFilters( SolicitacaoFilterParams filterParams, StringBuilder hql,
            HashMap<String, Object> params ) {
        hql.append( " WHERE 1=1 " );

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getTitulo() ) ) {
            hql.append( " AND LOWER(s.titulo) LIKE :titulo " );
            params.put( "titulo", "%" + filterParams.getTitulo().toLowerCase() + "%" );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getSituacao() ) ) {
            hql.append( " AND s.situacao = :situacao " );
            params.put( "situacao", filterParams.getSituacao() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getParceiroId() ) ) {
            hql.append( " AND pa.id = :parceiroId " );
            params.put( "parceiroId", filterParams.getParceiroId() );
        }
    }

}
