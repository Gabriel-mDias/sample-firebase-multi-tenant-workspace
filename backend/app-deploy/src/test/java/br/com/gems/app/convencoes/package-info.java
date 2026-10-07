/**
 * Gates de conformidade de escrita da rodada 008.
 *
 * <p>
 * Três testes moram aqui, e cada um existe porque o anterior não alcança o que ele mede:
 * </p>
 *
 * <ul>
 * <li><b>{@code ConvencoesArquiteturaTest}</b> (ArchUnit) — o que é visível na estrutura compilada:
 * herança de {@code BaseCustomJpaRepository}, presença e visibilidade da tétrade, extensão de
 * {@code BaseController}, ausência de {@code nativeQuery} e as fronteiras entre camadas.</li>
 * <li><b>{@code ConvencoesFonteTest}</b> (JavaParser) — o que <b>não sobrevive à compilação</b>:
 * ordem de campos e de métodos, conteúdo de corpo de método, e a igualdade dos JOINs entre
 * {@code countQuery} e {@code searchQuery}. O bytecode devolve {@code Set} para campos e interfaces,
 * o Lombok é {@code RetentionPolicy.SOURCE} e corpo de método simplesmente não existe lá.</li>
 * <li><b>{@code ExcecoesEscritasTest}</b> — o inventário de {@code GEMS-EXCEPTION} derivado do
 * código, e as três formas de reprovar: vencida, órfã e regra inexistente.</li>
 * </ul>
 *
 * <p>
 * <b>Por que em {@code app-deploy}.</b> É o único módulo que enxerga o reator inteiro — a mesma
 * razão pela qual {@code CoberturaPreAuthorizeTest} e {@code FrontendActionCatalogGateTest} já moram
 * aqui. Regra de fronteira entre módulos verificada de dentro de um módulo só enxerga metade do
 * grafo.
 * </p>
 *
 * <p>
 * O pacote nasce na fase 2 da rodada (T010) e é povoado na fase 7 (T069, T072, T075), depois de o
 * código estar conformado: gate sobre código não conformado nasce vermelho, e gate que nasce
 * vermelho é gate que alguém desliga.
 * </p>
 */
package br.com.gems.app.convencoes;
