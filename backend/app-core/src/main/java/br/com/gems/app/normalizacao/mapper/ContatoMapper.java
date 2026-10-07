package br.com.gems.app.normalizacao.mapper;

import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.mapstruct.GemsMappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GemsMappingConfig.class)
public interface ContatoMapper {

    ContatoDTO toDTO( Contato contato );

    // A identidade pertence à entidade gerenciada carregada pelo service.
    @Mapping(target = "id", ignore = true)
    // A criação é preenchida pelo Hibernate e não pode vir do payload.
    @Mapping(target = "dataCriacao", ignore = true)
    // O soft delete pertence ao ciclo de persistência, não ao payload de edição.
    @Mapping(target = "dataExclusao", ignore = true)
    void atualizar( ContatoDTO dto, @MappingTarget Contato contato );

}
