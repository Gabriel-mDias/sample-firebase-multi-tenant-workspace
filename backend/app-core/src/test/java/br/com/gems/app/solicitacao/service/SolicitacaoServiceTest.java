package br.com.gems.app.solicitacao.service;

import br.com.gems.app.model.dto.SolicitacaoDTO;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.entity.solicitacao.Solicitacao;
import br.com.gems.app.model.enums.SituacaoSolicitacao;
import br.com.gems.app.parceiro.repository.ParceiroRepository;
import br.com.gems.app.solicitacao.event.SolicitacaoConcluidaEvent;
import br.com.gems.app.solicitacao.mapper.SolicitacaoMapper;
import br.com.gems.app.solicitacao.repository.EtapaSolicitacaoRepository;
import br.com.gems.app.solicitacao.repository.SolicitacaoRepository;
import br.com.gems.exception.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * As regras da máquina de estados, atacadas diretamente.
 *
 * <p>
 * Nenhum destes casos aparece num teste de controller: o {@code @PreAuthorize} já barraria quem
 * não pode chamar, e passaria quem pode — inclusive para uma transição inválida. É aqui que a regra
 * mora, e é aqui que ela precisa falhar quando afrouxada.
 */
class SolicitacaoServiceTest {

    private SolicitacaoMapper mapper;
    private SolicitacaoRepository repository;
    private EtapaSolicitacaoRepository etapaRepository;
    private ParceiroRepository parceiroRepository;
    private ApplicationEventPublisher eventPublisher;
    private SolicitacaoService service;

    private final UUID id = UUID.randomUUID();
    private final UUID parceiroId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        mapper = mock( SolicitacaoMapper.class );
        repository = mock( SolicitacaoRepository.class );
        etapaRepository = mock( EtapaSolicitacaoRepository.class );
        parceiroRepository = mock( ParceiroRepository.class );
        eventPublisher = mock( ApplicationEventPublisher.class );
        service = new SolicitacaoService( mapper, repository, etapaRepository, parceiroRepository, eventPublisher );

        when( mapper.toDTO( any( Solicitacao.class ) ) ).thenAnswer( invocation -> {
            var entity = invocation.getArgument( 0, Solicitacao.class );
            var dto = new SolicitacaoDTO();
            dto.setId( entity.getId() );
            dto.setTitulo( entity.getTitulo() );
            dto.setDescricao( entity.getDescricao() );
            dto.setSituacao( entity.getSituacao() );
            dto.setMotivoCancelamento( entity.getMotivoCancelamento() );
            dto.setDataConclusao( entity.getDataConclusao() );
            if ( entity.getParceiro() != null ) {
                dto.setParceiroId( entity.getParceiro().getId() );
                dto.setParceiroNome( entity.getParceiro().getNome() );
            }
            return dto;
        } );
        when( repository.save( any( Solicitacao.class ) ) ).thenAnswer( i -> i.getArgument( 0 ) );
    }

    @Nested
    @DisplayName("A situação só avança pelas transições declaradas")
    class Transicoes {

        @Test
        @DisplayName("rascunho abre")
        void deveComprovarRascunhoAbre_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.RASCUNHO );

            var resultado = service.abrir( id );

            assertThat( resultado.getSituacao() ).isEqualTo( SituacaoSolicitacao.ABERTA );
        }

        @Test
        @DisplayName("rascunho NÃO conclui direto — pular a execução é o defeito que a máquina impede")
        void deveComprovarRascunhoNaoConcluiDireto_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.RASCUNHO );

            assertThatThrownBy( () -> service.concluir( id ) )
                    .isInstanceOf( BusinessException.class )
                    .hasMessageContaining( "Rascunho" );
        }

        @Test
        @DisplayName("solicitação concluída não volta atrás")
        void deveComprovarConcluidaEhTerminal_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.CONCLUIDA );

            assertThatThrownBy( () -> service.iniciar( id ) )
                    .isInstanceOf( BusinessException.class );
        }

        @Test
        @DisplayName("solicitação cancelada não é reaberta")
        void deveComprovarCanceladaEhTerminal_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.CANCELADA );

            assertThatThrownBy( () -> service.abrir( id ) )
                    .isInstanceOf( BusinessException.class );
        }
    }

    @Nested
    @DisplayName("A conclusão")
    class Conclusao {

        @Test
        @DisplayName("publica o evento com o parceiro, para que o listener não precise consultar de volta")
        void deveComprovarPublicaEvento_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.EM_ANDAMENTO );
            when( etapaRepository.existsBySolicitacaoIdAndConcluidaFalse( id ) ).thenReturn( false );

            service.concluir( id );

            var captor = ArgumentCaptor.forClass( SolicitacaoConcluidaEvent.class );
            verify( eventPublisher ).publishEvent( captor.capture() );
            assertThat( captor.getValue().parceiroId() ).isEqualTo( parceiroId );
            assertThat( captor.getValue().concluidaEm() ).isNotNull();
        }

        @Test
        @DisplayName("é recusada com etapa pendente, e o evento NÃO é publicado")
        void deveComprovarRecusaComEtapaPendente_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.EM_ANDAMENTO );
            when( etapaRepository.existsBySolicitacaoIdAndConcluidaFalse( id ) ).thenReturn( true );

            assertThatThrownBy( () -> service.concluir( id ) )
                    .isInstanceOf( BusinessException.class )
                    .hasMessageContaining( "etapas pendentes" );

            // Um evento publicado numa conclusão recusada incrementaria o contador do parceiro por
            // uma solicitação que nunca fechou.
            verify( eventPublisher, never() ).publishEvent( any( SolicitacaoConcluidaEvent.class ) );
        }
    }

    @Nested
    @DisplayName("O cancelamento")
    class Cancelamento {

        @Test
        @DisplayName("exige motivo")
        void deveComprovarExigeMotivo_quandoExecutaOCenario() {
            assertThatThrownBy( () -> service.cancelar( id, "  " ) )
                    .isInstanceOf( BusinessException.class )
                    .hasMessageContaining( "motivo" );
        }

        @Test
        @DisplayName("grava o motivo informado")
        void deveComprovarGravaMotivo_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.ABERTA );

            var resultado = service.cancelar( id, "cliente desistiu" );

            assertThat( resultado.getSituacao() ).isEqualTo( SituacaoSolicitacao.CANCELADA );
            assertThat( resultado.getMotivoCancelamento() ).isEqualTo( "cliente desistiu" );
        }
    }

    @Nested
    @DisplayName("A situação nunca vem do corpo")
    class SituacaoNaoVemDoCorpo {

        @Test
        @DisplayName("um PUT que declare CONCLUIDA não altera a situação")
        void deveComprovarPutIgnoraSituacao_quandoExecutaOCenario() {
            dado( SituacaoSolicitacao.RASCUNHO );

            var dto = new br.com.gems.app.model.dto.SolicitacaoDTO();
            dto.setTitulo( "qualquer" );
            dto.setSituacao( SituacaoSolicitacao.CONCLUIDA );

            var resultado = service.save( id, dto );

            // Se este teste falhar, a autorização por transição virou decoração: quem tem
            // ALTERAR_SOLICITACAO passou a poder concluir.
            assertThat( resultado.getSituacao() ).isEqualTo( SituacaoSolicitacao.RASCUNHO );
        }
    }

    private void dado( SituacaoSolicitacao situacao ) {
        var parceiro = Parceiro.builder().id( parceiroId ).nome( "Parceiro" ).build();
        var solicitacao = Solicitacao.builder()
                .id( id )
                .titulo( "Solicitação de teste" )
                .situacao( situacao )
                .parceiro( parceiro )
                .build();

        when( repository.findById( id ) ).thenReturn( Optional.of( solicitacao ) );
    }
}
