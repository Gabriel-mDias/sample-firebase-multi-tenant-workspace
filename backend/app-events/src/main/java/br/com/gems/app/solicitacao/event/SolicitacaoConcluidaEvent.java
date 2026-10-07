package br.com.gems.app.solicitacao.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publicado quando uma solicitação transita para {@code CONCLUIDA}.
 *
 * <p>
 * É a demonstração da fronteira do Princípio I: o domínio {@code solicitacao} <b>não</b> chama o
 * {@code ParceiroService} para atualizar o contador. Ele avisa que algo aconteceu, e quem reage é um
 * listener que vive junto do domínio que reage — em {@code app-core}, não aqui.
 *
 * <p>
 * <b>Por que este módulo só tem POJOs.</b> Um listener que chame regra de negócio dependeria de
 * {@code app-core}, e o reator recusaria o ciclo {@code events -> core}. A restrição não é
 * convenção: ela não compila.
 *
 * <p>
 * ⚠️ <b>Entrega em memória.</b> Não há registro persistido de publicações. Se a aplicação cair
 * entre a publicação e a execução do listener, este evento se perde e o contador fica defasado —
 * aceitável porque o número é recalculável. Um evento cuja consequência não seja recalculável exige
 * persistência de publicações como decisão consciente, não por omissão.
 *
 * @param solicitacaoId
 *            a solicitação concluída
 * @param parceiroId
 *            o parceiro dono dela, para que o listener não precise consultar de volta
 * @param concluidaEm
 *            o instante da conclusão
 */
public record SolicitacaoConcluidaEvent(
        UUID solicitacaoId,
        UUID parceiroId,
        LocalDateTime concluidaEm) {
}
