package br.com.gems.app.normalizacao.service;

import br.com.gems.app.model.dto.PessoaDTO;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.normalizacao.mapper.PessoaMapper;
import br.com.gems.app.normalizacao.repository.PessoaRepository;
import br.com.gems.app.normalizacao.event.PessoaExcluidaEvent;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de apoio da identidade civil normalizada.
 *
 * <p>
 * Nasce já pensado para ser reaproveitado por todo domínio que precise de identidade civil —
 * daí viver em {@code normalizacao} e não no domínio do parceiro nem no do usuário.
 *
 * <p>
 * O CPF é a chave natural: {@link #buscarPorCpf} é o que permite reaproveitar uma pessoa já
 * cadastrada em vez de duplicá-la. A unicidade é verificada aqui, no serviço, e não por
 * constraint de banco, porque a regra é "único <b>entre os ativos</b>" — e o
 * {@code @SQLRestriction} da entidade já dá exatamente esse recorte às consultas do repositório.
 */
@Service
@RequiredArgsConstructor
public class PessoaService {

    private static final int TAMANHO_CPF = 11;

    private final PessoaMapper mapper;
    private final PessoaRepository repository;

    private final ApplicationEventPublisher eventPublisher;

    public PessoaDTO toDTO( Pessoa pessoa ) {
        return mapper.toDTO( pessoa );
    }

    /**
     * Busca por CPF entre os registros ativos.
     *
     * <p>
     * O CPF é normalizado para apenas dígitos antes da consulta: a máscara é detalhe de
     * apresentação e não pode influenciar a identidade.
     */
    public Optional<Pessoa> buscarPorCpf( String cpf ) {
        if ( ObjectUtil.isNullOrEmpty( cpf ) ) {
            return Optional.empty();
        }
        return repository.findByCpf( apenasDigitos( cpf ) );
    }

    public void delete( Pessoa pessoa ) {
        if ( ObjectUtil.isNullOrEmpty( pessoa ) ) {
            return;
        }
        repository.delete( pessoa );

        // Arrasta os arquivos pessoais para o mesmo estado. Por evento, e não por
        // chamada direta: comunicação entre domínios de negócio é assim (Princípio I), e este
        // serviço não deveria precisar saber que existe acervo.
        eventPublisher.publishEvent( new PessoaExcluidaEvent( pessoa.getId() ) );
    }

    /** Cria e persiste, devolvendo instância <b>gerenciada</b> — ver {@link ContatoService#insert}. */
    public Pessoa insert( PessoaDTO dto ) {
        return save( new Pessoa(), dto );
    }

    public Pessoa save( Pessoa gerenciada, PessoaDTO dto ) {
        mapper.atualizar( dto, gerenciada );
        gerenciada.setCpf( apenasDigitos( dto.getCpf() ) );
        gerenciada.setDataAproximada( Boolean.TRUE.equals( dto.getDataAproximada() ) );
        return repository.save( gerenciada );
    }

    /**
     * @param idExistente
     *            id da pessoa que está sendo reaproveitada ou editada, para que ela não
     *            acuse duplicidade contra si mesma. {@code null} num cadastro novo.
     */
    public void validar( PessoaDTO pessoa, UUID idExistente, List<String> errors ) {
        validar( pessoa, idExistente, true, errors );
    }

    /**
     * Valida a identidade civil de acordo com o papel que a consome.
     *
     * <p>
     * {@code Parceiro} e {@code Usuario} passam {@code exigeCpf=true}, preservando também a
     * obrigatoriedade da data de nascimento — são identidades civis de pessoas vivas e presentes.
     * Um cadastro que registre pessoa sem documento (histórica, estrangeira, ou de terceiro que
     * nunca se apresentou) passa {@code false}: o CPF e a data deixam de ser exigidos, mas
     * qualquer valor informado continua sendo validado.
     */
    public void validar( PessoaDTO pessoa, UUID idExistente, boolean exigeCpf, List<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( pessoa ) ) {
            errors.add( "Os dados pessoais são obrigatórios." );
            return;
        }

        if ( ObjectUtil.isNullOrEmpty( pessoa.getPrimeiroNome() ) ) {
            errors.add( "O primeiro nome é obrigatório." );
        }

        if ( ObjectUtil.isNullOrEmpty( pessoa.getSobrenome() ) ) {
            errors.add( "O sobrenome é obrigatório." );
        }

        if ( exigeCpf && ObjectUtil.isNullOrEmpty( pessoa.getDataNascimento() ) ) {
            errors.add( "A data de nascimento é obrigatória." );
        } else if ( ObjectUtil.isNotNullAndNotEmpty( pessoa.getDataNascimento() )
                && !pessoa.getDataNascimento().isBefore( LocalDate.now() ) ) {
            errors.add( "A data de nascimento deve estar no passado." );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( pessoa.getDataNascimento() )
                && ObjectUtil.isNotNullAndNotEmpty( pessoa.getDataObito() )
                && pessoa.getDataObito().isBefore( pessoa.getDataNascimento() ) ) {
            errors.add( "A data de óbito não pode ser anterior à data de nascimento." );
        }

        validarCpf( pessoa, idExistente, exigeCpf, errors );
    }

    private String apenasDigitos( String valor ) {
        return ObjectUtil.isNullOrEmpty( valor ) ? valor : valor.replaceAll( "\\D", "" );
    }

    private void validarCpf( PessoaDTO pessoa, UUID idExistente, boolean exigeCpf, List<String> errors ) {
        if ( ObjectUtil.isNullOrEmpty( pessoa.getCpf() ) ) {
            if ( exigeCpf ) {
                errors.add( "O CPF é obrigatório." );
            }
            return;
        }

        var cpf = apenasDigitos( pessoa.getCpf() );

        if ( cpf.length() != TAMANHO_CPF ) {
            errors.add( "O CPF deve conter 11 dígitos." );
            return;
        }

        // Sem idExistente, qualquer CPF já cadastrado é duplicidade. Com ele, a própria pessoa é
        // ignorada — é o caso do reaproveitamento e o da edição.
        var duplicado = ObjectUtil.isNullOrEmpty( idExistente )
                ? repository.existsByCpf( cpf )
                : repository.existsByCpfAndIdNot( cpf, idExistente );

        if ( duplicado ) {
            errors.add( "Já existe uma pessoa cadastrada com este CPF." );
        }
    }

}
