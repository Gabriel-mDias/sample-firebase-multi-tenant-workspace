package br.com.gems.app.localidade.service;

import br.com.gems.app.localidade.mapper.LocalidadeMapper;
import br.com.gems.app.localidade.repository.EstadoRepository;
import br.com.gems.app.localidade.repository.PaisRepository;
import br.com.gems.app.model.dto.EstadoDTO;
import br.com.gems.app.model.dto.PaisDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Leitura dos dados de referência que alimentam os selects do wizard de parceiro.
 *
 * <p>
 * Somente leitura: país e estado são carregados por seed Liquibase e não têm manutenção pelo
 * usuário nesta entrega. Por isso não há {@code validate}, {@code insert}, {@code save} nem
 * {@code delete} — a ordem canônica do Princípio IV se aplica ao que existe.
 */
@Service
@RequiredArgsConstructor
public class LocalidadeService {

    private final LocalidadeMapper mapper;
    private final PaisRepository paisRepository;
    private final EstadoRepository estadoRepository;

    public List<PaisDTO> listarPaises() {
        return paisRepository.findAllByOrderByNomeAsc()
                .stream()
                .map( mapper::toPaisDTO )
                .toList();
    }

    public List<EstadoDTO> listarEstadosPorPais( String paisId ) {
        return listarEstadosPorPais( UUID.fromString( paisId ) );
    }

    public List<EstadoDTO> listarEstadosPorPais( UUID paisId ) {
        return estadoRepository.findAllByPaisIdOrderByNomeAsc( paisId )
                .stream()
                .map( mapper::toEstadoDTO )
                .toList();
    }

}
