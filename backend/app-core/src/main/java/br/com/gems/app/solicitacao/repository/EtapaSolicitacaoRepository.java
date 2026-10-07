package br.com.gems.app.solicitacao.repository;

import br.com.gems.app.model.entity.solicitacao.EtapaSolicitacao;
import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EtapaSolicitacaoRepository
        extends
            JpaRepository<EtapaSolicitacao, UUID>,
            BaseCustomJpaRepository<EtapaSolicitacao> {

    List<EtapaSolicitacao> findBySolicitacaoIdOrderByOrdemAsc( UUID solicitacaoId );

    boolean existsBySolicitacaoIdAndConcluidaFalse( UUID solicitacaoId );

}
