package br.com.gems.app.normalizacao.service;

import br.com.gems.app.localidade.repository.EstadoRepository;
import br.com.gems.app.localidade.repository.PaisRepository;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.app.normalizacao.mapper.EnderecoMapper;
import br.com.gems.app.normalizacao.repository.EnderecoRepository;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Serviço de apoio dos endereços normalizados.
 *
 * <p>
 * <b>Não trava país.</b> Uma regra do tipo "a sede precisa estar em território brasileiro" é
 * específica do cadastro que a exige e permanece no serviço dele; empurrá-la para cá faria todo
 * cadastro que reaproveita {@code Endereco} herdar uma restrição que não é dele — um parceiro
 * externo pode morar fora do Brasil.
 *
 * <p>
 * O que o serviço faz é <b>usar</b> o país para decidir a obrigatoriedade de
 * CEP e de estado. Essa condicional não cabe no banco: uma constraint não sabe
 * condicionar-se ao valor de outra coluna. Com {@code ID_ESTADO} e {@code CD_CEP} agora anuláveis, é
 * este serviço — e só ele — que impede um endereço brasileiro incompleto.
 *
 * <p>
 * Como em {@link ContatoService}, {@link #validar} acumula na lista do chamador e nunca lança.
 */
@Service
@RequiredArgsConstructor
public class EnderecoService {

    private static final String PAIS_COM_UF_E_CEP = "Brasil";

    private final EnderecoMapper mapper;
    private final EnderecoRepository repository;
    private final EstadoRepository estadoRepository;
    private final PaisRepository paisRepository;

    public EnderecoDTO toDTO( Endereco endereco ) {
        return mapper.toDTO( endereco );
    }

    public void delete( Endereco endereco ) {
        if ( ObjectUtil.isNullOrEmpty( endereco ) ) {
            return;
        }
        repository.delete( endereco );
    }

    /** Cria e persiste, devolvendo instância <b>gerenciada</b> — ver {@link ContatoService#insert}. */
    public Endereco insert( EnderecoDTO dto ) {
        return save( new Endereco(), dto );
    }

    /**
     * Aplica o payload sobre a instância recebida. {@code pais} e {@code estado} são resolvidos pelos
     * repositórios, e não copiados do DTO, para que o que se persiste seja sempre instância
     * gerenciada.
     *
     * <p>
     * Chamado apenas depois de {@link #validar}, então a busca do país não pode falhar. Já o
     * estado é resolvido de forma <b>opcional</b>: endereço fora do Brasil legitimamente não tem um,
     * e um {@code orElseThrow()} incondicional aqui quebraria todo cadastro estrangeiro.
     */
    public Endereco save( Endereco gerenciado, EnderecoDTO dto ) {
        mapper.atualizar( dto, gerenciado );
        gerenciado.setPais( buscarPais( dto ).orElseThrow() );
        gerenciado.setEstado( buscarEstado( dto ).orElse( null ) );
        return repository.save( gerenciado );
    }

    public Optional<Estado> buscarEstado( EnderecoDTO dto ) {
        if ( ObjectUtil.isNullOrEmpty( dto )
                || ObjectUtil.isNullOrEmpty( dto.getEstado() )
                || ObjectUtil.isNullOrEmpty( dto.getEstado().getId() ) ) {
            return Optional.empty();
        }
        return estadoRepository.findById( dto.getEstado().getId() );
    }

    public Optional<Pais> buscarPais( EnderecoDTO dto ) {
        if ( ObjectUtil.isNullOrEmpty( dto )
                || ObjectUtil.isNullOrEmpty( dto.getPais() )
                || ObjectUtil.isNullOrEmpty( dto.getPais().getId() ) ) {
            return Optional.empty();
        }
        return paisRepository.findById( dto.getPais().getId() );
    }

    public void validar( EnderecoDTO endereco, List<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( endereco ) ) {
            errors.add( "O endereço é obrigatório." );
            return;
        }

        var pais = validarPais( endereco, errors );

        if ( ObjectUtil.isNullOrEmpty( endereco.getCidade() ) ) {
            errors.add( "A cidade é obrigatória." );
        }

        if ( ObjectUtil.isNullOrEmpty( endereco.getBairro() ) ) {
            errors.add( "O bairro é obrigatório." );
        }

        if ( ObjectUtil.isNullOrEmpty( endereco.getLogradouro() ) ) {
            errors.add( "O logradouro é obrigatório." );
        }

        validarCep( endereco, pais, errors );
        validarEstado( endereco, pais, errors );
    }

    private Optional<Pais> validarPais( EnderecoDTO endereco, List<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( endereco.getPais() )
                || ObjectUtil.isNullOrEmpty( endereco.getPais().getId() ) ) {
            errors.add( "O país é obrigatório." );
            return Optional.empty();
        }

        var pais = buscarPais( endereco );

        if ( pais.isEmpty() ) {
            errors.add( "O país informado não existe." );
        }

        return pais;
    }

    private void validarCep( EnderecoDTO endereco, Optional<Pais> pais, List<String> errors ) {
        if ( exigeUfECep( pais ) && ObjectUtil.isNullOrEmpty( endereco.getCep() ) ) {
            errors.add( "O CEP é obrigatório." );
        }
    }

    private void validarEstado( EnderecoDTO endereco, Optional<Pais> pais, List<String> errors ) {
        var estadoInformado = ObjectUtil.isNotNullAndNotEmpty( endereco.getEstado() )
                && ObjectUtil.isNotNullAndNotEmpty( endereco.getEstado().getId() );

        if ( !estadoInformado ) {
            if ( exigeUfECep( pais ) ) {
                errors.add( "O estado é obrigatório." );
            }
            return;
        }

        var estado = buscarEstado( endereco );

        if ( estado.isEmpty() ) {
            errors.add( "O estado informado não existe." );
            return;
        }

        // Checagem impossível antes da 009: o país era derivado do próprio estado, então nunca havia
        // o que divergir. Agora que são informados separadamente, um pode contradizer o outro.
        if ( pais.isPresent()
                && ObjectUtil.isNotNullAndNotEmpty( estado.get().getPais() )
                && !pais.get().getId().equals( estado.get().getPais().getId() ) ) {
            errors.add( "O estado informado não pertence ao país selecionado." );
        }
    }

    /**
     * UF e CEP são exigências brasileiras, não universais — mas <b>relaxar a regra exige prova</b>.
     * Sem um país resolvido que comprovadamente não é o Brasil (ausente, inexistente ou ainda não
     * validado), a exigência permanece de pé.
     *
     * <p>
     * Isso preserva a validação em rodada única: um formulário vazio acusa país, CEP e estado de
     * uma vez, em vez de exigir que o operador escolha o país, submeta, e só então descubra os
     * outros dois.
     */
    private boolean exigeUfECep( Optional<Pais> pais ) {
        return pais.isEmpty() || PAIS_COM_UF_E_CEP.equalsIgnoreCase( pais.get().getNome() );
    }

}
