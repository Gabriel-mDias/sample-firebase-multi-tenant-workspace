package br.com.gems.app.parceiro.repository;

import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.params.ParceiroFilterParams;
import br.com.gems.app.model.response.ParceiroResponseDTO;
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

/**
 * O repository de referência. A ordem dos métodos é fixa:
 * {@code search > countQuery > searchQuery > resolveSort > appendFilters}.
 */
@Repository
public interface ParceiroRepository extends JpaRepository<Parceiro, UUID>, BaseCustomJpaRepository<Parceiro> {

    @Override
    @EntityGraph(value = "parceiro-entity-graph")
    Optional<Parceiro> findById( UUID id );

    /**
     * Unicidade do nome. O {@code @SQLRestriction("DT_EXCLUSAO IS NULL")} da entidade já limita a
     * checagem aos parceiros ativos, então um nome liberado por exclusão lógica pode ser reutilizado.
     */
    boolean existsByNomeIgnoreCase( String nome );

    /** Mesma regra da sobrecarga acima, ignorando o próprio registro — usada na edição. */
    boolean existsByNomeIgnoreCaseAndIdNot( String nome, UUID id );

    default Page<ParceiroResponseDTO> search( ParceiroFilterParams filterParams, Pageable pageable ) {
        return new PageImpl<>( this.searchQuery( filterParams, pageable ), pageable, this.countQuery( filterParams ) );
    }

    private Long countQuery( ParceiroFilterParams filterParams ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append( " SELECT count(pa.id) " );
        hql.append( " FROM Parceiro pa " );
        hql.append( " JOIN pa.pessoa pe " );
        hql.append( " JOIN pa.contato ct " );
        hql.append( " JOIN pa.endereco en " );
        hql.append( " LEFT JOIN en.estado es " );
        appendFilters( filterParams, hql, params );
        return this.executeCountHql( hql, params );
    }

    private List<ParceiroResponseDTO> searchQuery( ParceiroFilterParams filterParams, Pageable pageable ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        // Projeção por CONSTRUTOR. A ordem dos argumentos é o contrato com o construtor do
        // ParceiroResponseDTO: trocar um pelo outro compila e falha em runtime.
        hql.append( " SELECT new br.com.gems.app.model.response.ParceiroResponseDTO( " );
        hql.append( "       pa.id, " );
        hql.append( "       pa.nome, " );
        hql.append( "       pe.cpf, " );
        hql.append( "       ct.email, " );
        hql.append( "       es.siglaEstado, " );
        hql.append( "       pa.comissao, " );
        hql.append( "       pa.solicitacoesConcluidas " );
        hql.append( " ) " );
        // Os aliases do FROM/JOIN MUST ser os mesmos do SELECT e do appendFilters. Divergência de
        // alias só falha em runtime — nenhum compilador a pega.
        hql.append( " FROM Parceiro pa " );
        hql.append( " JOIN pa.pessoa pe " );
        hql.append( " JOIN pa.contato ct " );
        hql.append( " JOIN pa.endereco en " );
        hql.append( " LEFT JOIN en.estado es " );

        appendFilters( filterParams, hql, params );
        return this.executeHql( hql, params, resolveSort( pageable ), ParceiroResponseDTO.class );
    }

    /**
     * NOTA sobre o {@code executeHql} do gems-jpa: ele JÁ aplica o {@code Sort} do {@code Pageable},
     * mas de forma <b>literal</b> — pega cada {@code Sort.Order#getProperty()} e concatena direto em
     * {@code "ORDER BY <property> <DIRECTION>"}, validando só com uma regex anti-injeção.
     *
     * <p>
     * <b>Ele não traduz</b> nomes de propriedade do DTO de projeção para os aliases HQL desta
     * query. Se o {@code Pageable} chegar com {@code "cpf"} ou {@code "estadoSigla"} sem tradução, o
     * HQL gerado referencia identificadores que não existem no escopo do FROM/JOIN e a query quebra
     * em runtime. Por isso a tradução acontece aqui, antes de delegar: propriedade reconhecida vira
     * o path qualificado pelo alias certo, e propriedade desconhecida é ignorada, caindo no padrão.
     */
    private Pageable resolveSort( Pageable pageable ) {
        var orders = new ArrayList<Sort.Order>();

        for ( Sort.Order order : pageable.getSort() ) {
            var hqlProperty = switch ( order.getProperty() ) {
                case "nome" -> "pa.nome";
                case "cpf" -> "pe.cpf";
                case "email" -> "ct.email";
                case "estadoSigla" -> "es.siglaEstado";
                case "solicitacoesConcluidas" -> "pa.solicitacoesConcluidas";
                default -> null;
            };

            if ( hqlProperty != null ) {
                orders.add( new Sort.Order( order.getDirection(), hqlProperty ) );
            }
        }

        var sort = orders.isEmpty() ? Sort.by( Sort.Direction.ASC, "pa.nome" ) : Sort.by( orders );
        return PageRequest.of( pageable.getPageNumber(), pageable.getPageSize(), sort );
    }

    private void appendFilters( ParceiroFilterParams filterParams, StringBuilder hql, HashMap<String, Object> params ) {
        hql.append( " WHERE 1=1 " );

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getNome() ) ) {
            hql.append( " AND LOWER(pa.nome) LIKE :nome " );
            params.put( "nome", "%" + filterParams.getNome().toLowerCase() + "%" );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getCpf() ) ) {
            hql.append( " AND pe.cpf = :cpf " );
            params.put( "cpf", filterParams.getCpf() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getEstadoId() ) ) {
            hql.append( " AND es.id = :estadoId " );
            params.put( "estadoId", filterParams.getEstadoId() );
        }
    }

}
