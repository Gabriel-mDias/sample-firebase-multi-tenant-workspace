package br.com.gems.app.auditoria.service;

import br.com.gems.app.auditoria.repository.CampoAlteradoRepository;
import br.com.gems.app.auditoria.repository.RegistroAuditoriaRepository;
import br.com.gems.app.model.dto.CampoAlteradoDTO;
import br.com.gems.app.model.entity.auditoria.CampoAlterado;
import br.com.gems.app.model.entity.auditoria.RegistroAuditoria;
import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.app.model.enums.OperacaoAuditoria;
import br.com.gems.app.model.params.AuditoriaFilterParams;
import br.com.gems.app.model.response.RegistroAuditoriaResponseDTO;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Consulta da trilha de auditoria.
 *
 * <h2>Não há mutação aqui, e isso é o requisito</h2>
 * Nenhum {@code insert}, {@code save} ou {@code delete} — o requisito define a trilha como somente
 * escrita e leitura, e quem escreve é o interceptador de persistência. Um método de gravação neste
 * serviço permitiria <b>forjar</b> trilha, e trilha forjável não é trilha.
 */
@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository repository;
    private final CampoAlteradoRepository campoAlteradoRepository;
    private final EntityManagerFactory entityManagerFactory;

    public AuditoriaService( RegistroAuditoriaRepository repository,
            CampoAlteradoRepository campoAlteradoRepository,
            EntityManagerFactory entityManagerFactory ) {
        this.repository = repository;
        this.campoAlteradoRepository = campoAlteradoRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * O histórico de um <b>cadastro</b>: a linha raiz mais os agregados dela.
     *
     * <h2>Por que não basta perguntar pela raiz</h2>
     * A trilha é gravada por linha de tabela; a tela pergunta por cadastro. Neste projeto as duas
     * coisas quase nunca coincidem. {@code Cliente} tem <b>um</b> {@code @Column} — o identificador —
     * e nada mais: telefone e e-mail moram em {@code Contato}, nome e CPF em {@code Pessoa}, e o
     * resto em {@code Endereco}, cada um com a sua linha e o seu identificador. Como o interceptador
     * pula associações de propósito — são LAZY, e tocá-las no flush dispararia carregamento —,
     * perguntar só pela raiz devolvia o cadastro inicial e mais nada, por mais que o operador
     * editasse. Foi o que a homologação encontrou.
     *
     * <p>
     * A coleção de campos vem pelo {@code @EntityGraph} da consulta — ver a nota no repositório
     * sobre por que isso é correção, e não otimização.
     */
    public Page<RegistroAuditoriaResponseDTO> historico( String entidade, UUID registroAfetado,
            Pageable pageable ) {
        validate( entidade, null );

        return repository
                .findByRegistroAfetadoInOrderByDataOperacaoDesc(
                        alvosDoHistorico( entidade, registroAfetado ), pageable )
                .map( registro -> toDTO( registro, registro.getCamposAlterados() ) );
    }

    /**
     * A busca ampla — "o que fulano alterou na semana passada".
     *
     * <p>
     * Os campos alterados vêm de uma <b>segunda consulta</b>, por lote de identificadores. Não é
     * N+1: é uma ida ao banco para a página inteira.
     */
    public Page<RegistroAuditoriaResponseDTO> search( AuditoriaFilterParams filterParams, Pageable pageable ) {
        validate( filterParams.getEntidade(), filterParams );

        var pagina = repository.search( filterParams, pageable );

        if ( pagina.isEmpty() ) {
            return pagina.map( registro -> toDTO( registro, List.of() ) );
        }

        var camposPorRegistro = camposDe( pagina.getContent() );

        return pagina.map( registro -> toDTO( registro,
                camposPorRegistro.getOrDefault( registro.getId(), List.of() ) ) );
    }

    // ------------------------------------------------------------------ apoio

    /**
     * Os identificadores cujo histórico compõe o do cadastro: a raiz e os agregados auditáveis dela.
     *
     * <h2>Derivado do metamodelo, nunca de uma lista escrita à mão</h2>
     * A tentação é um mapa {@code "Cliente" -> [pessoa, contato, endereco]}. Este projeto já foi
     * mordido duas vezes por rede escrita à mão — a varredura de rotas ignorou uma feature inteira
     * porque ninguém lembrou de acrescentá-la à lista. Aqui a fonte é o próprio mapeamento JPA: toda
     * associação singular cujo alvo é {@link EntidadeAuditavel} entra, e um agregado novo passa a
     * ser coberto no instante em que é mapeado.
     *
     * <p>
     * O filtro por {@code EntidadeAuditavel} já exclui o que não deve entrar: {@code Pais} e
     * {@code Estado} são referenciais e isentos, então {@code Parceiro.paisRepresentado} fica de
     * fora sozinho, sem regra especial.
     *
     * <h2>Um nível, de propósito</h2>
     * Não há travessia recursiva. Ela traria {@code Endereco -> Estado -> Pais} e, num grafo mais
     * fundo, o histórico de um cadastro acabaria alcançando meio banco. Um nível é o que corresponde
     * ao que a tela mostra.
     *
     * <h2>Cadastro excluído devolve só a própria trilha</h2>
     * A consulta da raiz respeita o {@code @SQLRestriction} do soft delete, então um cadastro
     * excluído não resolve agregado. O histórico <b>dele</b> continua vindo — que é o que o requisito
     * exige, e o que a homologação confere ao excluir um parceiro e reabrir o histórico.
     */
    private List<UUID> alvosDoHistorico( String entidade, UUID raiz ) {
        var alvos = new ArrayList<UUID>();
        alvos.add( raiz );

        var tipo = tipoDe( entidade );

        if ( ObjectUtil.isNullOrEmpty( tipo ) ) {
            return alvos;
        }

        var agregados = agregadosAuditaveisDe( tipo );

        if ( agregados.isEmpty() ) {
            return alvos;
        }

        var hql = new StringBuilder();
        hql.append( " SELECT " );
        hql.append( agregados.stream().map( nome -> "e." + nome + ".id" )
                .collect( Collectors.joining( ", " ) ) );
        hql.append( " FROM " ).append( entidade ).append( " e " );
        hql.append( " WHERE e.id = :id " );

        // Tuple e não Object[]: com um único agregado o JPQL devolveria o valor solto, e o laço
        // abaixo quebraria só para as entidades de um agregado só — o pior tipo de defeito.
        var linhas = entityManagerFactory.createEntityManager();

        try {
            var tuplas = linhas.createQuery( hql.toString(), Tuple.class )
                    .setParameter( "id", raiz )
                    .getResultList();

            for ( var tupla : tuplas ) {
                for ( int i = 0; i < agregados.size(); i++ ) {
                    if ( tupla.get( i ) instanceof UUID identificador ) {
                        alvos.add( identificador );
                    }
                }
            }
        } finally {
            linhas.close();
        }

        return alvos;
    }

    /** O tipo Java de uma entidade auditável, pelo nome simples que a trilha grava. */
    private Class<?> tipoDe( String entidade ) {
        return entityManagerFactory.getMetamodel().getEntities().stream()
                .map( EntityType::getJavaType )
                .filter( EntidadeAuditavel.class::isAssignableFrom )
                .filter( tipo -> tipo.getSimpleName().equals( entidade ) )
                .findFirst()
                .orElse( null );
    }

    /** Os nomes das associações singulares auditáveis do tipo, em ordem estável. */
    private List<String> agregadosAuditaveisDe( Class<?> tipo ) {
        return entityManagerFactory.getMetamodel().entity( tipo ).getSingularAttributes().stream()
                .filter( Attribute::isAssociation )
                .filter( atributo -> EntidadeAuditavel.class.isAssignableFrom( atributo.getJavaType() ) )
                .map( Attribute::getName )
                .sorted()
                .toList();
    }

    private Map<UUID, List<CampoAlterado>> camposDe( List<RegistroAuditoria> registros ) {
        var ids = registros.stream().map( RegistroAuditoria::getId ).toList();

        return campoAlteradoRepository.findByRegistroAuditoriaIdIn( ids ).stream()
                .collect( Collectors.groupingBy( campo -> campo.getRegistroAuditoria().getId() ) );
    }

    private RegistroAuditoriaResponseDTO toDTO( RegistroAuditoria registro, List<CampoAlterado> campos ) {
        var dto = new RegistroAuditoriaResponseDTO();

        dto.setId( registro.getId() );
        dto.setEntidade( registro.getEntidade() );
        dto.setRegistroAfetado( registro.getRegistroAfetado() );
        dto.setOperacao( registro.getOperacao().name() );
        dto.setOperacaoDescricao( registro.getOperacao().getDescricao() );
        dto.setDataOperacao( registro.getDataOperacao() );
        dto.setAutorNome( registro.getAutorNome() );
        dto.setCamposAlterados( campos.stream().map( this::toDTO ).toList() );

        return dto;
    }

    private CampoAlteradoDTO toDTO( CampoAlterado campo ) {
        return new CampoAlteradoDTO( campo.getCampo(), campo.getValorAnterior(), campo.getValorNovo(),
                campo.isSigiloso() );
    }

    /**
     * Os nomes de entidade que a trilha pode conter, tirados do <b>metamodelo JPA</b>.
     *
     * <p>
     * Nunca de uma lista escrita à mão: a fronteira é a mesma do interceptador — subclasse de
     * {@link EntidadeAuditavel} —, e derivá-la da mesma fonte é o que impede a validação de recusar
     * uma entidade nova que a trilha já está registrando.
     */
    private Set<String> entidadesConhecidas() {
        return entityManagerFactory.getMetamodel().getEntities().stream()
                .map( EntityType::getJavaType )
                .filter( EntidadeAuditavel.class::isAssignableFrom )
                .map( Class::getSimpleName )
                .collect( Collectors.toSet() );
    }

    /**
     * Validação acumulada, nunca fail-fast (Princípio IV): junta tudo e lança uma vez só.
     *
     * @param entidade
     *            o nome a conferir, ou {@code null} quando não veio no filtro
     * @param filterParams
     *            presente apenas na busca ampla; {@code null} no histórico
     */
    private void validate( String entidade, AuditoriaFilterParams filterParams ) {
        var errors = new ArrayList<String>();

        if ( ObjectUtil.isNotNullAndNotEmpty( entidade ) && !entidadesConhecidas().contains( entidade ) ) {
            // Recusar é melhor que devolver página vazia: página vazia esconde o erro de digitação e
            // faz o operador concluir que o registro nunca foi alterado.
            errors.add( "A entidade '" + entidade + "' não existe ou não é auditada. Entidades válidas: "
                    + entidadesConhecidas().stream().sorted().collect( Collectors.joining( ", " ) ) + "." );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( filterParams ) ) {
            if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getOperacao() )
                    && Arrays.stream( OperacaoAuditoria.values() )
                            .noneMatch( operacao -> operacao.name().equals( filterParams.getOperacao() ) ) ) {
                errors.add( "A operação '" + filterParams.getOperacao() + "' é inválida. Valores aceitos: "
                        + Arrays.stream( OperacaoAuditoria.values() ).map( Enum::name )
                                .collect( Collectors.joining( ", " ) )
                        + "." );
            }

            if ( ObjectUtil.isNotNullAndNotEmpty( filterParams.getDataInicio() )
                    && ObjectUtil.isNotNullAndNotEmpty( filterParams.getDataFim() )
                    && filterParams.getDataInicio().isAfter( filterParams.getDataFim() ) ) {
                errors.add( "A data inicial não pode ser posterior à data final." );
            }
        }

        if ( !errors.isEmpty() ) {
            throw new BusinessException( errors );
        }
    }
}
