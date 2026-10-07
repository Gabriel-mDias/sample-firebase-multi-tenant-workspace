package br.com.gems.app.normalizacao.service;

import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.normalizacao.mapper.ContatoMapper;
import br.com.gems.app.normalizacao.repository.ContatoRepository;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de apoio dos dados de contato normalizados.
 *
 * <p>
 * Não é um domínio de negócio par — é dado compartilhado, da mesma natureza de {@code localidade}
 * —, por isso é injetado diretamente por quem o compõe ({@code ParceiroService},
 * {@code UsuarioService}) em vez de conversar por evento. A gravação precisa ser síncrona e na
 * mesma transação do agregado-raiz, o que um evento não entrega.
 *
 * <p>
 * <b>{@link #validar} acumula e nunca lança.</b> Ele recebe a lista de erros do chamador e só
 * acrescenta mensagens; quem dispara a {@code BusinessException} é sempre o {@code validate} do
 * agregado-raiz. Fosse diferente, a validação viraria fail-fast na prática — o operador corrigiria o
 * e-mail, submeteria, e só então descobriria o erro de endereço.
 */
@Service
@RequiredArgsConstructor
public class ContatoService {

    private final ContatoMapper mapper;
    private final ContatoRepository repository;

    public ContatoDTO toDTO( Contato contato ) {
        return mapper.toDTO( contato );
    }

    /** Exclusão lógica: o {@code @SQLDelete} da entidade converte isto num UPDATE de DT_EXCLUSAO. */
    public void delete( Contato contato ) {
        if ( ObjectUtil.isNullOrEmpty( contato ) ) {
            return;
        }
        repository.delete( contato );
    }

    /**
     * Cria e persiste, devolvendo instância <b>gerenciada</b>.
     *
     * <p>
     * Devolver uma instância gerenciada é o contrato: quem compõe o agregado tem chave estrangeira
     * {@code nullable = false} e, sem {@code CascadeType.PERSIST}, gravar o dono com um contato
     * transiente estouraria {@code TransientPropertyValueException}.
     */
    public Contato insert( ContatoDTO dto ) {
        return save( new Contato(), dto );
    }

    /** Aplica o payload sobre a instância recebida, preservando a identidade dela. */
    public Contato save( Contato gerenciado, ContatoDTO dto ) {
        mapper.atualizar( dto, gerenciado );
        return repository.save( gerenciado );
    }

    public void validar( ContatoDTO contato, List<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( contato ) ) {
            errors.add( "Os dados de contato são obrigatórios." );
            return;
        }

        if ( ObjectUtil.isNullOrEmpty( contato.getEmail() ) ) {
            errors.add( "O e-mail é obrigatório." );
        }

        if ( ObjectUtil.isNullOrEmpty( contato.getTelefone() ) ) {
            errors.add( "O telefone é obrigatório." );
        }
    }

}
