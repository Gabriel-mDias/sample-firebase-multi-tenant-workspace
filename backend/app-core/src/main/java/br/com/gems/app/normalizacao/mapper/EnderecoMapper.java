package br.com.gems.app.normalizacao.mapper;

import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.mapstruct.GemsMappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GemsMappingConfig.class)
public interface EnderecoMapper {

    EnderecoDTO toDTO( Endereco endereco );

    // A identidade pertence à entidade gerenciada carregada pelo service.
    @Mapping(target = "id", ignore = true)
    // A criação é preenchida pelo Hibernate e não pode vir do payload.
    @Mapping(target = "dataCriacao", ignore = true)
    // O soft delete pertence ao ciclo de persistência, não ao payload de edição.
    @Mapping(target = "dataExclusao", ignore = true)
    // O país é resolvido pelo repository para manter uma referência gerenciada.
    @Mapping(target = "pais", ignore = true)
    // O estado é resolvido pelo repository para manter uma referência gerenciada.
    @Mapping(target = "estado", ignore = true)
    void atualizar( EnderecoDTO dto, @MappingTarget Endereco endereco );

}
