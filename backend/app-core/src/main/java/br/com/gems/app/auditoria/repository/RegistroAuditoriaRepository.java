package br.com.gems.app.auditoria.repository;

import br.com.gems.app.model.entity.auditoria.RegistroAuditoria;
import br.com.gems.app.model.params.AuditoriaFilterParams;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Consulta da trilha. <b>Somente leitura</b> — quem escreve é o interceptador.
 */
@Repository
public interface RegistroAuditoriaRepository
        extends
            JpaRepository<RegistroAuditoria, UUID>,
            BaseCustomJpaRepository<RegistroAuditoria> {

    /**
     * O histórico de um registro — a consulta do requisito, e a única que a tela usa.
     *
     * <p>
     * <b>O {@code @EntityGraph} não é otimização, é correção.</b> {@code camposAlterados} é uma
     * coleção LAZY, {@code open-in-view} é {@code false}, e a conversão para DTO acontece depois de a
     * transação do repositório ter fechado. Sem o grafo, a conversão estoura
     * {@code LazyInitializationException} — em produção, não no teste unitário, onde o repositório é
     * mock. É o defeito que já devolveu 500 no histórico de um agregado e no do parceiro
     * competente na 014, e {@code AuditoriaHistoricoIntegrationTest} existe, deliberadamente NÃO
     * transacional, para que ele não aconteça uma terceira vez.
     *
     * <p>
     * <b>Sobre paginação com coleção buscada:</b> o Hibernate resolve o recorte em memória e
     * avisa. É aceito aqui e só aqui — o histórico de <i>um</i> registro é pequeno por natureza, e a
     * alternativa (duas consultas) tiraria o grafo do caminho e com ele a rede que o teste arma. A
     * busca ampla, que pode devolver muita linha, usa o outro caminho.
     */
    @EntityGraph(value = "registro-auditoria-entity-graph")
    Page<RegistroAuditoria> findByEntidadeAndRegistroAfetadoOrderByDataOperacaoDesc(
            String entidade, UUID registroAfetado, Pageable pageable );

    /**
     * O histórico de um <b>cadastro</b>, e não de uma linha: a raiz mais os agregados dela.
     *
     * <p>
     * <b>Por que existe.</b> A trilha é gravada por linha de tabela, e a tela pergunta por
     * cadastro. As duas coisas quase nunca coincidem neste projeto: {@code Cliente} tem um único
     * {@code @Column} — o identificador —, e telefone, e-mail, nome e endereço moram em
     * {@code Contato}, {@code Pessoa} e {@code Endereco}, cada um com a sua linha. Perguntar só pela
     * raiz devolvia o cadastro inicial e mais nada, por mais que o operador editasse.
     *
     * <p>
     * <b>Por que filtra só por identificador.</b> {@code registroAfetado} é um UUID gerado por
     * linha, único entre todas as tabelas; acrescentar o nome da entidade ao filtro não tornaria a
     * consulta mais seletiva, apenas exigiria pares (entidade, id) que uma consulta derivada não
     * expressa. O nome continua vindo em cada registro, e é o que a tela mostra como origem.
     *
     * <p>
     * O {@code @EntityGraph} está aqui pela mesma razão da consulta acima — é correção, não
     * otimização.
     */
    @EntityGraph(value = "registro-auditoria-entity-graph")
    Page<RegistroAuditoria> findByRegistroAfetadoInOrderByDataOperacaoDesc(
            List<UUID> registrosAfetados, Pageable pageable );

    default Page<RegistroAuditoria> search( AuditoriaFilterParams filterParams, Pageable pageable ) {
        return new PageImpl<>( this.searchQuery( filterParams, pageable ), pageable,
                this.countQuery( filterParams ) );
    }

    private Long countQuery( AuditoriaFilterParams filterParams ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append( " SELECT count(r.id) " );
        hql.append( " FROM RegistroAuditoria r " );
        appendFilters( filterParams, hql, params );

        return this.executeCountHql( hql, params );
    }

    /**
     * Devolve a <b>entidade</b>, e não uma projeção por construtor como nos demais repositórios do
     * projeto. O motivo é o formato do destino: {@code RegistroAuditoriaResponseDTO} carrega uma
     * <b>coleção aninhada</b> de campos alterados, e projeção por construtor em HQL não monta
     * coleção. A montagem acontece no serviço, com uma segunda consulta — que também é o que evita
     * juntar {@code JOIN FETCH} com paginação numa busca que pode devolver muita linha.
     */
    private List<RegistroAuditoria> searchQuery( AuditoriaFilterParams filterParams, Pageable pageable ) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append( " SELECT r " );
        hql.append( " FROM RegistroAuditoria r " );
        appendFilters( filterParams, hql, params );

        return this.executeHql( hql, params, resolveSort( pageable ), RegistroAuditoria.class );
    }

    /**
     * Traduz a propriedade do DTO para o alias do HQL. O {@code executeHql} do {@code gems-jpa}
     * concatena a propriedade do {@code Sort} <b>literalmente</b> no {@code ORDER BY}, validando só
     * com uma regex anti-injeção — ele não sabe que {@code dataOperacao} é {@code r.dataOperacao}
     * aqui. Propriedade não reconhecida é ignorada em silêncio e cai no padrão.
     */
    private Pageable resolveSort( Pageable pageable ) {
        var orders = new ArrayList<Sort.Order>();

        for ( Sort.Order order : pageable.getSort() ) {
            var hqlProperty = switch ( order.getProperty() ) {
                case "dataOperacao" -> "r.dataOperacao";
                case "entidade" -> "r.entidade";
                case "autorNome" -> "r.autorNome";
                case "operacao" -> "r.operacao";
                default -> null;
            };

            if ( hqlProperty != null ) {
                orders.add( new Sort.Order( order.getDirection(), hqlProperty ) );
            }
        }

        var sort = orders.isEmpty()
                ? Sort.by( Sort.Direction.DESC, "r.dataOperacao" )
                : Sort.by( orders );

        return PageRequest.of( pageable.getPageNumber(), pageable.getPageSize(), sort );
    }

    private void appendFilters( AuditoriaFilterParams filterParams, StringBuilder hql,
            HashMap<String, Object> params ) {
        hql.append( " WHERE 1=1 " );

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getEntidade() ) ) {
            hql.append( " AND r.entidade = :entidade " );
            params.put( "entidade", filterParams.getEntidade() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getRegistroAfetado() ) ) {
            hql.append( " AND r.registroAfetado = :registroAfetado " );
            params.put( "registroAfetado", filterParams.getRegistroAfetado() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getOperacao() ) ) {
            hql.append( " AND r.operacao = :operacao " );
            params.put( "operacao",
                    br.com.gems.app.model.enums.OperacaoAuditoria.valueOf( filterParams.getOperacao() ) );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getAutorId() ) ) {
            hql.append( " AND r.autorId = :autorId " );
            params.put( "autorId", filterParams.getAutorId() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getDataInicio() ) ) {
            hql.append( " AND r.dataOperacao >= :dataInicio " );
            params.put( "dataInicio", filterParams.getDataInicio().atStartOfDay() );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getDataFim() ) ) {
            // INCLUSIVO até o fim do dia. O filtro é LocalDate e a coluna é TIMESTAMP:
            // comparar direto descartaria tudo que aconteceu depois da meia-noite do próprio dia
            // pedido, e uma busca por "hoje" voltaria vazia.
            hql.append( " AND r.dataOperacao <= :dataFim " );
            params.put( "dataFim", LocalDateTime.of( filterParams.getDataFim(), LocalTime.MAX ) );
        }
    }
}
