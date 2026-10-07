package br.com.gems.app.localidade.service;

import br.com.gems.app.localidade.mapper.LocalidadeMapper;
import br.com.gems.app.localidade.repository.EstadoRepository;
import br.com.gems.app.localidade.repository.PaisRepository;
import br.com.gems.app.model.dto.EstadoDTO;
import br.com.gems.app.model.dto.PaisDTO;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalidadeServiceTest {

    @Mock
    private LocalidadeMapper mapper;

    @Mock
    private PaisRepository paisRepository;

    @Mock
    private EstadoRepository estadoRepository;

    @InjectMocks
    private LocalidadeService service;

    @Test
    void deveMapearPaises_quandoListagemForSolicitada() {
        var pais = Pais.builder().nome( "Brasil" ).build();
        var dto = new PaisDTO( UUID.randomUUID(), "Brasil" );
        when( paisRepository.findAllByOrderByNomeAsc() ).thenReturn( List.of( pais ) );
        when( mapper.toPaisDTO( pais ) ).thenReturn( dto );

        var resultado = service.listarPaises();

        assertThat( resultado ).containsExactly( dto );
        verify( mapper ).toPaisDTO( pais );
    }

    @Test
    void deveMapearEstadoSemPais_quandoListagemForSolicitada() {
        var paisId = UUID.randomUUID();
        var estado = Estado.builder().nome( "Espírito Santo" ).build();
        var dto = new EstadoDTO( UUID.randomUUID(), null, "Espírito Santo", "ES" );
        when( estadoRepository.findAllByPaisIdOrderByNomeAsc( paisId ) ).thenReturn( List.of( estado ) );
        when( mapper.toEstadoDTO( estado ) ).thenReturn( dto );

        var resultado = service.listarEstadosPorPais( paisId );

        assertThat( resultado ).singleElement().extracting( EstadoDTO::getPais ).isNull();
        verify( mapper ).toEstadoDTO( estado );
    }

}
