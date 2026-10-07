package br.com.gems.app.parceiro.service;

import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.params.ParceiroFilterParams;
import br.com.gems.app.model.response.ParceiroResponseDTO;
import br.com.gems.app.normalizacao.service.ContatoService;
import br.com.gems.app.normalizacao.service.EnderecoService;
import br.com.gems.app.normalizacao.service.PessoaService;
import br.com.gems.app.parceiro.mapper.ParceiroMapper;
import br.com.gems.app.parceiro.repository.ParceiroRepository;
import br.com.gems.app.security.AcaoSistema;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Regras de negócio do cadastro de parceiros — <b>o service de referência deste projeto</b>.
 *
 * <p>
 * Ordem canônica dos métodos (Princípio IV): {@code findById > search > delete > insert > save >
 * validate}. Os auxiliares privados ficam entre {@code save} e {@code validate}, que é sempre o
 * último método da classe.
 *
 * <p>
 * <b>Nada é gravado por cascade.</b> Pessoa, contato e endereço são dados normalizados de apoio,
 * com services próprios. Em troca da conveniência do cascade, a ordem das operações é
 * responsabilidade explícita deste serviço — ver os comentários em {@link #insert} e {@link #delete}.
 *
 * <p>
 * É também onde vive a <b>autorização de campo</b>: a comissão só é preenchida para quem tem
 * {@link AcaoSistema#VISUALIZAR_COMISSAO}, e nos <b>três</b> caminhos — leitura, busca e gravação.
 */
@Service
@RequiredArgsConstructor
public class ParceiroService {

    private static final String NAO_ENCONTRADO = "Parceiro não encontrado.";

    private final ParceiroMapper mapper;
    private final ParceiroRepository repository;
    private final PessoaService pessoaService;
    private final ContatoService contatoService;
    private final EnderecoService enderecoService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public ParceiroDTO findById( String id ) {
        return findById( UUID.fromString( id ) );
    }

    public ParceiroDTO findById( UUID id ) {
        return toDTO( buscarAtivo( id ) );
    }

    /**
     * Busca paginada. A comissão é apagada de <b>cada linha</b> quando o chamador não pode vê-la:
     * filtrar só no {@code findById} deixaria o valor vazando pela listagem, que é justamente a tela
     * que mais gente abre.
     */
    public Page<ParceiroResponseDTO> search( ParceiroFilterParams filterParams, Pageable pageable ) {
        var pagina = repository.search( filterParams, pageable );

        if ( !podeVerComissao() ) {
            pagina.getContent().forEach( linha -> linha.setComissao( null ) );
        }

        return pagina;
    }

    /**
     * Exclusão lógica em cascata, explícita.
     *
     * <p>
     * Os deletes viram {@code UPDATE ... SET DT_EXCLUSAO = NOW()} por conta do {@code @SQLDelete}
     * de cada entidade, então não há risco de violação de chave estrangeira em nenhuma ordem — a
     * sequência dono → agregados é semântica. As linhas continuam fisicamente na base; apenas somem
     * das consultas, por conta do {@code @SQLRestriction}.
     *
     * <p>
     * O {@code @EntityGraph} de {@code findById} garante que os agregados já estão inicializados
     * aqui, o que importa com {@code open-in-view: false}.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete( String id ) {
        delete( UUID.fromString( id ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete( UUID id ) {
        var parceiro = buscarAtivo( id );
        var pessoa = parceiro.getPessoa();
        var contato = parceiro.getContato();
        var endereco = parceiro.getEndereco();

        repository.delete( parceiro );
        pessoaService.delete( pessoa );
        contatoService.delete( contato );
        enderecoService.delete( endereco );
    }

    /**
     * Grava parceiro, pessoa, contato e endereço em uma única transação, a partir do payload único
     * do wizard.
     *
     * <p>
     * <b>A ordem é obrigatória.</b> As chaves estrangeiras são {@code nullable = false} e não há
     * {@code CascadeType.PERSIST}: salvar o parceiro com um agregado transiente estouraria
     * {@code TransientPropertyValueException}. Os três precisam estar gerenciados <b>antes</b>.
     */
    @Transactional(rollbackFor = Exception.class)
    public ParceiroDTO insert( ParceiroDTO dto ) {
        validate( dto, true );

        var parceiro = Parceiro.builder()
                .pessoa( pessoaService.insert( dto.getPessoa() ) )
                .contato( contatoService.insert( dto.getContato() ) )
                .endereco( enderecoService.insert( dto.getEndereco() ) )
                .solicitacoesConcluidas( 0 )
                .build();

        aplicar( dto, parceiro );

        return toDTO( repository.save( parceiro ) );
    }

    @Transactional(rollbackFor = Exception.class)
    public ParceiroDTO save( String id, ParceiroDTO dto ) {
        return save( UUID.fromString( id ), dto );
    }

    @Transactional(rollbackFor = Exception.class)
    public ParceiroDTO save( UUID id, ParceiroDTO dto ) {
        var parceiro = buscarAtivo( id );

        // O id da rota é a autoridade: o corpo pode vir sem ele, e a checagem de duplicidade precisa
        // dele para ignorar o próprio registro.
        dto.setId( id );
        validate( dto, false );

        // As instâncias já são gerenciadas; os services aplicam o payload sobre elas, preservando a
        // identidade de cada agregado.
        pessoaService.save( parceiro.getPessoa(), dto.getPessoa() );
        contatoService.save( parceiro.getContato(), dto.getContato() );
        enderecoService.save( parceiro.getEndereco(), dto.getEndereco() );

        aplicar( dto, parceiro );

        return toDTO( repository.save( parceiro ) );
    }

    private Parceiro buscarAtivo( UUID id ) {
        return repository.findById( id )
                .orElseThrow( () -> new BusinessException( NAO_ENCONTRADO ) );
    }

    /**
     * Copia para a entidade o que é do próprio parceiro.
     *
     * <p>
     * <b>A comissão só é escrita por quem pode lê-la.</b> Não é excesso de zelo: sem esta guarda,
     * um payload de quem não vê o campo — e que por isso o recebeu {@code null} na leitura —
     * sobrescreveria o valor existente com {@code null} no primeiro salvamento. A autorização de
     * leitura viraria uma autorização de escrita ao contrário, e ninguém notaria até faltar o número.
     */
    private void aplicar( ParceiroDTO dto, Parceiro parceiro ) {
        parceiro.setNome( dto.getNome().trim() );
        parceiro.setDescricao( dto.getDescricao() );

        if ( podeVerComissao() ) {
            parceiro.setComissao( dto.getComissao() );
        }
    }

    private ParceiroDTO toDTO( Parceiro parceiro ) {
        // STRICT, e não o mapper padrão: o DTO tem caminhos aninhados que competiriam pelo mesmo
        // destino sob a estratégia STANDARD, e o mapeamento errado passaria silencioso.
        var dto = mapper.toDTO( parceiro );

        if ( !podeVerComissao() ) {
            dto.setComissao( null );
        }

        return dto;
    }

    private boolean podeVerComissao() {
        return usuarioAutenticadoService.possuiAcao( AcaoSistema.VISUALIZAR_COMISSAO );
    }

    /** {@code null} no insert; o id da pessoa já gravada na edição. */
    private UUID idDaPessoa( ParceiroDTO dto ) {
        return ObjectUtil.isNullOrEmpty( dto.getPessoa() ) ? null : dto.getPessoa().getId();
    }

    private boolean existeDuplicado( ParceiroDTO dto, boolean isInsert ) {
        var nome = dto.getNome().trim();

        return isInsert
                ? repository.existsByNomeIgnoreCase( nome )
                : repository.existsByNomeIgnoreCaseAndIdNot( nome, dto.getId() );
    }

    /**
     * Validação <b>acumulada</b> (Princípio IV): todas as violações são reunidas e lançadas de uma
     * vez, nunca a primeira encontrada. O usuário corrige tudo de um submit só.
     *
     * <p>
     * Os serviços de normalização acumulam na mesma lista e nunca lançam — este é o único ponto
     * que dispara a {@link BusinessException}.
     */
    private void validate( ParceiroDTO dto, boolean isInsert ) {
        var errors = new ArrayList<String>();

        if ( ObjectUtil.isNullOrEmpty( dto.getNome() ) ) {
            errors.add( "O nome do parceiro é obrigatório." );
        } else if ( existeDuplicado( dto, isInsert ) ) {
            errors.add( "Já existe um parceiro com este nome." );
        }

        if ( ObjectUtil.isNotNullAndNotEmpty( dto.getComissao() )
                && dto.getComissao().signum() < 0 ) {
            errors.add( "A comissão não pode ser negativa." );
        }

        // O id da pessoa é passado para que a checagem de CPF duplicado ignore o próprio registro.
        pessoaService.validar( dto.getPessoa(), idDaPessoa( dto ), errors );
        contatoService.validar( dto.getContato(), errors );
        enderecoService.validar( dto.getEndereco(), errors );

        if ( !errors.isEmpty() ) {
            throw new BusinessException( errors );
        }
    }

}
