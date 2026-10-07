// origem: local; precedencia: local-vence; versao: 2.1.0
package br.com.gems.{app}.{dominio}.service;

import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import br.com.gems.{app}.model.dto.{dominio}.{Entidade}DTO;
import br.com.gems.{app}.model.params.{dominio}.{Entidade}FilterParams;
import br.com.gems.{app}.model.response.{dominio}.{Entidade}ResponseDTO;
import br.com.gems.{app}.{dominio}.mapper.{Entidade}Mapper;
import br.com.gems.{app}.{dominio}.repository.{Entidade}Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

// Forma: rules/backend-java/services.md. Mapper MapStruct como primeiro campo; ordem canônica dos
// métodos; validação acumulada; ObjectUtil no lugar de null/isEmpty; @Transactional do Spring com rollbackFor.
@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    private final {Entidade}Mapper mapper;
    private final {Entidade}Repository repository;

    public {Entidade}DTO findById( String id ) {
        return this.findById( ObjectUtil.isNullOrEmpty( id ) ? null : UUID.fromString( id ) );
    }

    public {Entidade}DTO findById( UUID id ) {
        if ( ObjectUtil.isNullOrEmpty( id ) ) {
            throw new BusinessException( "O id não foi informado!" );
        }
        return repository.findById( id )
                .map( mapper::toDTO )
                .orElseThrow( () -> new BusinessException( "{Entidade} não encontrado(a)!" ) );
    }

    public Page<{Entidade}ResponseDTO> search( {Entidade}FilterParams filterParams, Pageable pageable ) {
        return repository.search( filterParams, pageable );
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete( String id ) {
        this.delete( ObjectUtil.isNullOrEmpty( id ) ? null : UUID.fromString( id ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete( UUID id ) {
        if ( ObjectUtil.isNullOrEmpty( id ) ) {
            throw new BusinessException( "O id não foi informado!" );
        }
        if ( !repository.existsById( id ) ) {
            throw new BusinessException( "{Entidade} não encontrado(a)!" );
        }
        repository.deleteById( id );  // soft delete pelo @SQLDelete da entidade
    }

    @Transactional(rollbackFor = Exception.class)
    public {Entidade}DTO insert( {Entidade}DTO dto ) {
        if ( ObjectUtil.isNullOrEmpty( dto ) ) {
            throw new BusinessException( "Os dados não foram informados!" );
        }
        validate( dto );
        dto.setId( null );
        dto.setDataCriacao( LocalDateTime.now() );
        var entity = repository.save( mapper.toEntity( dto ) );
        return mapper.toDTO( entity );
    }

    @Transactional(rollbackFor = Exception.class)
    public {Entidade}DTO save( UUID id, {Entidade}DTO dto ) {
        if ( ObjectUtil.isNullOrEmpty( id ) ) {
            throw new BusinessException( "O id não foi informado!" );
        }
        validate( dto );
        var entity = repository.findById( id )
                .orElseThrow( () -> new BusinessException( "{Entidade} não encontrado(a)!" ) );
        mapper.atualizar( dto, entity );  // @MappingTarget: id, dataCriacao e dataExclusao ficam como estão
        return mapper.toDTO( repository.save( entity ) );
    }

    private void validate( {Entidade}DTO dto ) {
        var errors = new ArrayList<String>();

        if ( ObjectUtil.isNullOrEmpty( dto.getNome() ) ) {
            errors.add( "O nome é obrigatório." );
        }

        if ( ObjectUtil.isNullOrEmpty( dto.getTipo() ) ) {
            errors.add( "O tipo é obrigatório." );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
            throw new BusinessException( errors );
        }
    }
}
