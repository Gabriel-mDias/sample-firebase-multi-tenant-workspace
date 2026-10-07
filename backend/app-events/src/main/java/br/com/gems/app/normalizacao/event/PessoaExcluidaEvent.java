package br.com.gems.app.normalizacao.event;

import java.util.UUID;

/**
 * Publicado quando uma pessoa é excluída <b>logicamente</b>.
 *
 * <p>
 * <b>Ninguém escuta este evento hoje</b>, e isso é deliberado: ele é o ponto de extensão para
 * o domínio que precisar reagir à saída de uma pessoa — arrastar arquivos pessoais, encerrar
 * vínculos derivados, notificar. Publicar sem consumidor mantém a exclusão de {@code Pessoa}
 * ignorante de quem depende dela, que é o ponto inteiro de um evento de domínio.
 *
 * <p>
 * Como a mesma {@code Pessoa} pode ser referenciada por mais de um cadastro, excluí-la vale
 * para <b>todos</b> — o dono do dado é a pessoa, não o cadastro que a alcançou primeiro.
 */
public record PessoaExcluidaEvent(UUID pessoaId) {
}
