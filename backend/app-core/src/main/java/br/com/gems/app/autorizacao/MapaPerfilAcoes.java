package br.com.gems.app.autorizacao;

import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.security.AcaoSistema;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static br.com.gems.app.security.AcaoSistema.ABRIR_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.ALTERAR_ETAPA_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.ALTERAR_PARCEIRO;
import static br.com.gems.app.security.AcaoSistema.ALTERAR_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.CANCELAR_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.CONCLUIR_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.CONSULTAR_PARCEIRO;
import static br.com.gems.app.security.AcaoSistema.CONSULTAR_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.EXCLUIR_ETAPA_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.EXCLUIR_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.INICIAR_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.INSERIR_ETAPA_SOLICITACAO;
import static br.com.gems.app.security.AcaoSistema.INSERIR_PARCEIRO;
import static br.com.gems.app.security.AcaoSistema.INSERIR_SOLICITACAO;

/**
 * Quais ações cada perfil de atuação possui — a matriz de permissões do sistema, em código.
 *
 * <p>
 * Este é o <b>único</b> lugar que se edita para mudar o que um perfil pode fazer. A fonte é a
 * matriz aprovada pelo product owner, no {@code contracts/} da rodada que a definiu, e o
 * {@code MapaPerfilAcoesTest} confere este mapa contra ela. Divergência entre os dois é defeito,
 * não interpretação.
 *
 * <h2>Por que a matriz é código e não uma tela</h2>
 * Como código, uma mudança de permissão aparece em revisão de diferença, com autor e data. Como tela,
 * seria editável em produção sem rastro — e ninguém saberia responder por que um perfil ganhou uma
 * ação em algum momento do ano passado.
 *
 * <h2>Por que esta classe vive no app-core</h2>
 * Ela precisa enxergar {@link PerfilAtuacao}, que está no {@code app-model}, e {@link AcaoSistema},
 * que está no {@code app-security}. O {@code app-core} é o <b>único</b> módulo do reator que depende
 * dos dois — {@code app-model} e {@code app-security} não se conhecem, e o grafo do Maven impede que
 * passem a se conhecer.
 *
 * <h2>O administrador não está na matriz, e isso é o desenho</h2>
 * O grupo {@code ADMINISTRADOR} mapeia para a composta {@code ADMIN}, que contém <b>todas</b> as
 * ações do enum. Uma ação nova o alcança sozinha, sem ninguém lembrar de concedê-la. Listá-lo aqui
 * criaria uma segunda lista a manter em sincronia com o enum — exatamente o defeito que este desenho
 * evita.
 *
 * @see AcaoSistema
 * @see AutorizacaoBootstrap
 */
public final class MapaPerfilAcoes {

    private MapaPerfilAcoes() {
    }

    /**
     * Administrador: <b>todas</b> as ações, por construção.
     *
     * <p>
     * Não é uma lista mantida à mão — é {@code EnumSet.allOf}. Acrescentar uma constante ao
     * {@link AcaoSistema} já a inclui aqui.
     */
    private static final Set<AcaoSistema> ADMINISTRADOR_ACOES = EnumSet.allOf( AcaoSistema.class );

    /**
     * Operador: opera os dois domínios de ponta a ponta, mas não exclui parceiro nem enxerga o valor
     * da comissão.
     *
     * <p>
     * As duas ausências são deliberadas e de naturezas diferentes: excluir parceiro é ato
     * destrutivo sobre cadastro de referência, e a comissão é informação de gestão — o mesmo motivo
     * pelo qual a trilha de auditoria também não está aqui.
     */
    private static final Set<AcaoSistema> OPERADOR_ACOES = EnumSet.of(
            CONSULTAR_PARCEIRO, INSERIR_PARCEIRO, ALTERAR_PARCEIRO,
            CONSULTAR_SOLICITACAO, INSERIR_SOLICITACAO, ALTERAR_SOLICITACAO, EXCLUIR_SOLICITACAO,
            ABRIR_SOLICITACAO, INICIAR_SOLICITACAO, CONCLUIR_SOLICITACAO, CANCELAR_SOLICITACAO,
            INSERIR_ETAPA_SOLICITACAO, ALTERAR_ETAPA_SOLICITACAO, EXCLUIR_ETAPA_SOLICITACAO );

    /**
     * Consulta: só lê.
     *
     * <p>
     * É o perfil que prova que o modelo funciona: sem nenhuma ação de escrita, cada
     * {@code @PreAuthorize} de mutação o barra sem que exista um único {@code if} no código a
     * respeito dele.
     */
    private static final Set<AcaoSistema> CONSULTA_ACOES = EnumSet.of(
            CONSULTAR_PARCEIRO,
            CONSULTAR_SOLICITACAO );

    private static final Map<PerfilAtuacao, Set<AcaoSistema>> MAPA;

    static {
        Map<PerfilAtuacao, Set<AcaoSistema>> mapa = new EnumMap<>( PerfilAtuacao.class );
        mapa.put( PerfilAtuacao.ADMINISTRADOR, Collections.unmodifiableSet( ADMINISTRADOR_ACOES ) );
        mapa.put( PerfilAtuacao.OPERADOR, Collections.unmodifiableSet( OPERADOR_ACOES ) );
        mapa.put( PerfilAtuacao.CONSULTA, Collections.unmodifiableSet( CONSULTA_ACOES ) );
        MAPA = Collections.unmodifiableMap( mapa );
    }

    /**
     * As ações do perfil. Nunca {@code null}: um perfil sem entrada devolve conjunto vazio, porque
     * falhar fechado é o comportamento correto de uma consulta de permissão.
     */
    public static Set<AcaoSistema> acoesDe( PerfilAtuacao perfil ) {
        return MAPA.getOrDefault( perfil, Collections.emptySet() );
    }

    /** O mapa inteiro, imutável. Usado pelo bootstrap e pelo teste que confere a matriz. */
    public static Map<PerfilAtuacao, Set<AcaoSistema>> mapa() {
        return MAPA;
    }

}
