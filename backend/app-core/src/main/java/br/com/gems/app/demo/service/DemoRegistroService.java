package br.com.gems.app.demo.service;

import br.com.gems.app.demo.repository.DemoRegistroRepository;
import br.com.gems.app.model.dto.DemoRegistroDTO;
import br.com.gems.app.model.entity.demo.DemoRegistro;
import br.com.gems.app.model.params.DemoRegistroFilterParams;
import br.com.gems.app.model.response.DemoRegistroResponseDTO;
import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras do exemplo mínimo e proteção de campo nos três caminhos. */
@Service
@RequiredArgsConstructor
public class DemoRegistroService {

    private static final String ID_NAO_INFORMADO = "O id não foi informado!";
    private final DemoRegistroRepository repository;
    private final UsuarioAutenticadoService usuario;

    @Transactional(readOnly = true)
    public DemoRegistroDTO findById( String id ) {
        if ( ObjectUtil.isNullOrEmpty( id ) ) {
            throw new BusinessException( ID_NAO_INFORMADO );
        }
        return findById( UUID.fromString( id ) );
    }

    @Transactional(readOnly = true)
    public DemoRegistroDTO findById( UUID id ) {
        return toDTO( buscar( id ) );
    }

    @Transactional(readOnly = true)
    public Page<DemoRegistroResponseDTO> search( DemoRegistroFilterParams filter, Pageable pageable ) {
        return repository.search( filter, pageable, usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete( UUID id ) {
        repository.delete( buscar( id ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public DemoRegistroDTO insert( DemoRegistroDTO dto ) {
        validate( dto, true );
        var entity = DemoRegistro.builder().nome( dto.nome() ).descricao( dto.descricao() ).build();
        if ( usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO ) ) {
            entity.setValorRestrito( dto.valorRestrito() );
        }
        return toDTO( repository.save( entity ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public DemoRegistroDTO save( UUID id, DemoRegistroDTO dto ) {
        validate( dto, false );
        var entity = buscar( id );
        entity.setNome( dto.nome() );
        entity.setDescricao( dto.descricao() );
        if ( usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO ) ) {
            entity.setValorRestrito( dto.valorRestrito() );
        }
        return toDTO( repository.save( entity ) );
    }

    private DemoRegistro buscar( UUID id ) {
        if ( ObjectUtil.isNullOrEmpty( id ) ) {
            throw new BusinessException( ID_NAO_INFORMADO );
        }
        return repository.findById( id ).orElseThrow( () -> new BusinessException( "Registro não encontrado." ) );
    }

    private DemoRegistroDTO toDTO( DemoRegistro entity ) {
        return new DemoRegistroDTO( entity.getId(), entity.getNome(), entity.getDescricao(), usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO ) ? entity.getValorRestrito() : null );
    }

    private void validate( DemoRegistroDTO dto, boolean isInsert ) {
        var errors = new ArrayList<String>();
        if ( ObjectUtil.isNullOrEmpty( dto ) ) {
            errors.add( "Informe o registro." );
        } else {
            validateNome( dto, errors );
            validateDescricao( dto, errors );
            validateIdGeradoPeloServidor( dto, isInsert, errors );
            validateValorRestrito( dto, errors );
        }
        if ( ObjectUtil.isNotNullAndNotEmpty( errors ) ) {
            throw new BusinessException( errors );
        }
    }

    private void validateNome( DemoRegistroDTO dto, ArrayList<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( dto.nome() ) || dto.nome().isBlank() ) {
            errors.add( "Informe o nome." );
        } else if ( dto.nome().length() > 150 ) {
            errors.add( "Nome deve ter no máximo 150 caracteres." );
        }
    }

    private void validateDescricao( DemoRegistroDTO dto, ArrayList<String> errors ) {
        if ( ObjectUtil.isNotNullAndNotEmpty( dto.descricao() ) && dto.descricao().length() > 1000 ) {
            errors.add( "Descrição deve ter no máximo 1000 caracteres." );
        }
    }

    private void validateIdGeradoPeloServidor( DemoRegistroDTO dto, boolean isInsert, ArrayList<String> errors ) {
        if ( isInsert && ObjectUtil.isNotNullAndNotEmpty( dto.id() ) ) {
            errors.add( "ID é gerado pelo servidor." );
        }
    }

    private void validateValorRestrito( DemoRegistroDTO dto, ArrayList<String> errors ) {
        if ( usuario.possuiAcao( AcaoSistema.VISUALIZAR_VALOR_RESTRITO )
                && ObjectUtil.isNotNullAndNotEmpty( dto.valorRestrito() )
                && (dto.valorRestrito().scale() > 2
                || dto.valorRestrito().precision() - dto.valorRestrito().scale() > 17) ) {
            errors.add( "Valor restrito deve ter até 17 dígitos inteiros e 2 decimais." );
        }
    }

}
