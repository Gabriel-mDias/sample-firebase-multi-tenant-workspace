package br.com.gems.app.solicitacao.mapper;

import br.com.gems.app.model.dto.EtapaSolicitacaoDTO;
import br.com.gems.app.model.dto.SolicitacaoDTO;
import br.com.gems.app.model.entity.solicitacao.EtapaSolicitacao;
import br.com.gems.app.model.entity.solicitacao.Solicitacao;
import br.com.gems.mapstruct.GemsMappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GemsMappingConfig.class)
public interface SolicitacaoMapper {

    @Mapping(target = "parceiroId", source = "parceiro.id")
    @Mapping(target = "parceiroNome", source = "parceiro.nome")
    SolicitacaoDTO toDTO( Solicitacao solicitacao );

    EtapaSolicitacaoDTO toDTO( EtapaSolicitacao etapa );

}
