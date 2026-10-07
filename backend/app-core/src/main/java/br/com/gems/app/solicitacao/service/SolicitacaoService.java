package br.com.gems.app.solicitacao.service;

import br.com.gems.app.model.dto.SolicitacaoDTO;
import br.com.gems.app.model.entity.solicitacao.Solicitacao;
import br.com.gems.app.model.enums.SituacaoSolicitacao;
import br.com.gems.app.model.params.SolicitacaoFilterParams;
import br.com.gems.app.model.response.SolicitacaoResponseDTO;
import br.com.gems.app.parceiro.repository.ParceiroRepository;
import br.com.gems.app.solicitacao.event.SolicitacaoConcluidaEvent;
import br.com.gems.app.solicitacao.mapper.SolicitacaoMapper;
import br.com.gems.app.solicitacao.repository.EtapaSolicitacaoRepository;
import br.com.gems.app.solicitacao.repository.SolicitacaoRepository;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Regras de negócio da solicitação — o domínio de fluxo.
 *
 * <p>
 * Ordem canônica dos métodos (Princípio IV): {@code findById > search > delete > insert > save >
 * validate}. As transições vêm depois de {@code save}, agrupadas, porque são um bloco coeso: cada
 * uma é chamada por um endpoint de ação próprio, com a sua ação de {@code AcaoSistema}.
 *
 * <p>
 * <b>A situação nunca é lida do DTO de entrada.</b> {@link #insert} a define como
 * {@code RASCUNHO}; {@link #save} a preserva; e só os métodos de transição a alteram, sempre
 * passando por {@link #transitar}, que é o único ponto que consulta a máquina de estados.
 */
@Service
@RequiredArgsConstructor
public class SolicitacaoService {

    private static final String NAO_ENCONTRADA = "Solicitação não encontrada.";

    private final SolicitacaoMapper mapper;
    private final SolicitacaoRepository repository;
    private final EtapaSolicitacaoRepository etapaRepository;
    private final ParceiroRepository parceiroRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SolicitacaoDTO findById( String id ) {
        return findById( UUID.fromString( id ) );
    }

    public SolicitacaoDTO findById( UUID id ) {
        return toDTO( buscarAtiva( id ) );
    }

    public Page<SolicitacaoResponseDTO> search( SolicitacaoFilterParams filterParams, Pageable pageable ) {
        return repository.search( filterParams, pageable );
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete( String id ) {
        delete( UUID.fromString( id ) );
    }

    /**
     * Só rascunho e solicitação cancelada podem ser excluídos.
     *
     * <p>
     * Excluir uma solicitação concluída apagaria o fato que alimentou o contador do parceiro, e o
     * evento já foi publicado — não há como desfazê-lo. A recusa aqui é o que mantém as duas coisas
     * coerentes sem precisar de evento de compensação.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete( UUID id ) {
        var solicitacao = buscarAtiva( id );

        if ( solicitacao.getSituacao() == SituacaoSolicitacao.EM_ANDAMENTO
                || solicitacao.getSituacao() == SituacaoSolicitacao.CONCLUIDA ) {
            throw new BusinessException(
                    "Só é possível excluir solicitação em rascunho ou cancelada." );
        }

        // Dono primeiro no delete: as etapas saem depois, porque a exclusão é lógica e não há
        // violação de chave estrangeira em nenhuma ordem — a sequência é semântica.
        repository.delete( solicitacao );
        etapaRepository.findBySolicitacaoIdOrderByOrdemAsc( id ).forEach( etapaRepository::delete );
    }

    @Transactional(rollbackFor = Exception.class)
    public SolicitacaoDTO insert( SolicitacaoDTO dto ) {
        validate( dto, true );

        var solicitacao = Solicitacao.builder()
                .parceiro( parceiroRepository.findById( dto.getParceiroId() ).orElseThrow() )
                // Definida aqui, e em nenhum outro lugar: toda solicitação nasce rascunho.
                .situacao( SituacaoSolicitacao.RASCUNHO )
                .build();

        aplicar( dto, solicitacao );

        return toDTO( repository.save( solicitacao ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public SolicitacaoDTO save( String id, SolicitacaoDTO dto ) {
        return save( UUID.fromString( id ), dto );
    }

    /**
     * Edita os campos de cadastro. <b>Não toca na situação</b>: {@code dto.getSituacao()} é ignorado
     * de propósito, e é por isso que não existe caminho para alguém concluir uma solicitação
     * mandando um PUT.
     */
    @Transactional(rollbackFor = Exception.class)
    public SolicitacaoDTO save( UUID id, SolicitacaoDTO dto ) {
        var solicitacao = buscarAtiva( id );

        dto.setId( id );
        validate( dto, false );

        if ( solicitacao.getSituacao().isTerminal() ) {
            throw new BusinessException( "Solicitação concluída ou cancelada não pode ser alterada." );
        }

        aplicar( dto, solicitacao );

        return toDTO( repository.save( solicitacao ) );
    }

    // ---- Transições ------------------------------------------------------------------------
    // Uma por endpoint, cada uma com a sua ação. Todas passam por transitar().

    @Transactional(rollbackFor = Exception.class)
    public SolicitacaoDTO abrir( UUID id ) {
        return toDTO( transitar( id, SituacaoSolicitacao.ABERTA ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public SolicitacaoDTO iniciar( UUID id ) {
        return toDTO( transitar( id, SituacaoSolicitacao.EM_ANDAMENTO ) );
    }

    /**
     * Conclui e <b>publica o evento</b>.
     *
     * <p>
     * O contador do parceiro não é atualizado aqui: chamar o {@code ParceiroService} seria
     * escrever no dado de outro domínio, que o Princípio I proíbe. Este service avisa; quem reage é
     * um listener que vive junto do domínio que reage.
     */
    @Transactional(rollbackFor = Exception.class)
    public SolicitacaoDTO concluir( UUID id ) {
        var solicitacao = buscarAtiva( id );

        if ( etapaRepository.existsBySolicitacaoIdAndConcluidaFalse( id ) ) {
            throw new BusinessException( "Há etapas pendentes: conclua todas antes de encerrar a solicitação." );
        }

        var concluida = transitar( solicitacao, SituacaoSolicitacao.CONCLUIDA );
        concluida.setDataConclusao( LocalDateTime.now() );
        var salva = repository.save( concluida );

        eventPublisher.publishEvent( new SolicitacaoConcluidaEvent(
                salva.getId(), salva.getParceiro().getId(), salva.getDataConclusao() ) );

        return toDTO( salva );
    }

    @Transactional(rollbackFor = Exception.class)
    public SolicitacaoDTO cancelar( UUID id, String motivo ) {
        if ( ObjectUtil.isNullOrEmpty( motivo ) ) {
            // Cancelar sem motivo é perda de informação: ninguém consegue responder depois por que
            // aquela solicitação morreu.
            throw new BusinessException( "O motivo do cancelamento é obrigatório." );
        }

        var cancelada = transitar( id, SituacaoSolicitacao.CANCELADA );
        cancelada.setMotivoCancelamento( motivo );

        return toDTO( repository.save( cancelada ) );
    }

    private Solicitacao buscarAtiva( UUID id ) {
        return repository.findById( id )
                .orElseThrow( () -> new BusinessException( NAO_ENCONTRADA ) );
    }

    private Solicitacao transitar( UUID id, SituacaoSolicitacao destino ) {
        var solicitacao = transitar( buscarAtiva( id ), destino );
        return repository.save( solicitacao );
    }

    /**
     * O <b>único</b> ponto que altera a situação.
     *
     * <p>
     * A regra de quais transições existem vive no enum, não aqui: acrescentar uma transição é uma
     * linha lá, e este método não muda.
     */
    private Solicitacao transitar( Solicitacao solicitacao, SituacaoSolicitacao destino ) {
        var origem = solicitacao.getSituacao();

        if ( !origem.permiteTransicaoPara( destino ) ) {
            throw new BusinessException(
                    "Não é possível passar de %s para %s.".formatted(
                            origem.getDescricao(), destino.getDescricao() ) );
        }

        solicitacao.setSituacao( destino );
        return solicitacao;
    }

    /** Copia o que é editável. A situação não está aqui, e essa ausência é o desenho. */
    private void aplicar( SolicitacaoDTO dto, Solicitacao solicitacao ) {
        solicitacao.setTitulo( dto.getTitulo().trim() );
        solicitacao.setDescricao( dto.getDescricao() );
    }

    private SolicitacaoDTO toDTO( Solicitacao solicitacao ) {
        return mapper.toDTO( solicitacao );
    }

    /** Validação acumulada (Princípio IV): reúne tudo e lança uma vez. */
    private void validate( SolicitacaoDTO dto, boolean isInsert ) {
        var errors = new ArrayList<String>();

        if ( ObjectUtil.isNullOrEmpty( dto.getTitulo() ) ) {
            errors.add( "O título da solicitação é obrigatório." );
        }

        if ( isInsert ) {
            if ( ObjectUtil.isNullOrEmpty( dto.getParceiroId() ) ) {
                errors.add( "O parceiro é obrigatório." );
            } else if ( parceiroRepository.findById( dto.getParceiroId() ).isEmpty() ) {
                errors.add( "Parceiro inexistente ou excluído." );
            }
        }

        if ( !errors.isEmpty() ) {
            throw new BusinessException( errors );
        }
    }

}
