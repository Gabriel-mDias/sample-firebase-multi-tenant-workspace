package br.com.gems.app.parceiro.mapper;

import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.normalizacao.mapper.ContatoMapper;
import br.com.gems.app.normalizacao.mapper.EnderecoMapper;
import br.com.gems.app.normalizacao.mapper.PessoaMapper;
import br.com.gems.mapstruct.GemsMappingConfig;
import org.mapstruct.Mapper;

@Mapper(config = GemsMappingConfig.class, uses = {PessoaMapper.class, ContatoMapper.class, EnderecoMapper.class})
public interface ParceiroMapper {

    ParceiroDTO toDTO( Parceiro parceiro );

}
