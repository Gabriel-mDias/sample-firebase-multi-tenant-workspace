package br.com.gems.app.localidade.mapper;

import br.com.gems.app.model.dto.EstadoDTO;
import br.com.gems.app.model.dto.PaisDTO;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.mapstruct.GemsMappingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GemsMappingConfig.class)
public interface LocalidadeMapper {

    PaisDTO toPaisDTO( Pais pais );

    // O pai é omitido para preservar o contrato enxuto da listagem de estados.
    @Mapping(target = "pais", ignore = true)
    EstadoDTO toEstadoDTO( Estado estado );

}
