package br.com.gems.app.parceiro.listener;

import br.com.gems.app.parceiro.repository.ParceiroRepository;
import br.com.gems.app.solicitacao.event.SolicitacaoConcluidaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A outra ponta da fronteira do Princípio I.
 *
 * <p>
 * O domínio {@code solicitacao} publicou que algo aconteceu; quem decide o que fazer com isso é
 * <b>este</b> domínio, aqui, junto do dado que ele altera. O {@code SolicitacaoService} não conhece
 * o {@code ParceiroService}, e o contrário também vale.
 *
 * <h2>Por que este listener vive em {@code app-core}, e não em {@code app-events}</h2>
 * Ele precisa do {@link ParceiroRepository}. Se morasse em {@code app-events}, aquele módulo passaria
 * a depender de {@code app-core}, e o reator recusaria o ciclo. A restrição não é convenção — ela não
 * compila. {@code app-events} guarda só os POJOs.
 *
 * <h2>{@code BEFORE_COMMIT}, e não {@code AFTER_COMMIT}</h2>
 * O contador precisa subir <b>dentro da mesma transação</b> que concluiu a solicitação. Com
 * {@code AFTER_COMMIT} haveria uma janela em que a solicitação está concluída e o contador não subiu,
 * e uma falha nessa janela deixaria os dois permanentemente divergentes — sem nada que reconciliasse.
 *
 * <p>
 * ⚠️ A entrega é <b>em memória</b>: não há registro persistido de publicações. Uma queda do
 * processo entre a publicação e este método perde o evento. É aceitável aqui porque o contador é
 * recalculável a partir das solicitações; não seria, se o efeito fosse irreversível.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitacaoConcluidaListener {

    private final ParceiroRepository parceiroRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoConcluirSolicitacao( SolicitacaoConcluidaEvent evento ) {
        parceiroRepository.findById( evento.parceiroId() ).ifPresentOrElse(
                parceiro -> {
                    parceiro.setSolicitacoesConcluidas( parceiro.getSolicitacoesConcluidas() + 1 );
                    parceiroRepository.save( parceiro );
                },
                // Parceiro excluído logicamente entre a abertura e a conclusão. Não é erro: o
                // contador dele deixou de importar. Registrar é o que permite descobrir depois que
                // isso acontece com frequência, se acontecer.
                () -> log.warn( "Solicitação {} concluída para parceiro {} que já não está ativo; "
                        + "contador não atualizado.",
                        evento.solicitacaoId(), evento.parceiroId() ) );
    }

}
