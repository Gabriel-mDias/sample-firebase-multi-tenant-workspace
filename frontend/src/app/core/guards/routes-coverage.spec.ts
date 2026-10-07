// Este spec resolve `loadChildren` de verdade, e isso arrasta pacotes do Angular compilados
// parcialmente (`@angular/common`), que exigem o compilador JIT como alternativa. Os demais specs
// do projeto o recebem de graça pelo TestBed; este, que não monta TestBed nenhum, precisa pedir.
import '@angular/compiler';
import { Route, Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { routes as APP_ROUTES } from '../../app.routes';
import { roleGuard } from './role.guard';

/**
 * Varredura de rotas — espelho, no frontend, do `CoberturaPreAuthorizeTest` do backend.
 *
 * Percorre **todas** as rotas alcançáveis a partir de `app.routes.ts` e falha, nomeando o caminho
 * completo, se alguma rota guardada por `roleGuard` não declarar `data.roles` como array não vazio.
 * Como o `roleGuard` falha **fechado**, uma rota guardada sem ação declarada já é
 * negada em runtime — esta varredura existe para pegar o erro em CI, antes de chegar lá.
 *
 * <h2>Por que a lista de rotas não é escrita à mão</h2>
 * Este arquivo já enumerou as tabelas de rotas uma a uma, por importação direta. O
 * resultado previsível aconteceu: `SOLICITACAO_ROUTES`, a feature mais nova, **nunca entrou na lista** —
 * a rede montada exatamente para pegar o esquecimento foi vítima do mesmo esquecimento, e passou
 * verde ignorando as cinco rotas dela. Uma varredura que precisa ser lembrada não é uma varredura.
 *
 * <p>Hoje a origem é `app.routes.ts`, e cada `loadChildren` é resolvido de verdade. Uma feature
 * nova é coberta no instante em que é registrada na raiz — que é o passo sem o qual ela não existe
 * para o roteador. O `expect` de quantidade mínima ao fim é o que impede a degeneração silenciosa:
 * se a resolução falhar e a varredura passar a não visitar nada, ela falha em vez de passar vazia.
 */
interface RotaVisitada {
  caminho: string;
  route: Route;
}

async function coletar(routes: Routes, prefixo: string): Promise<RotaVisitada[]> {
  const visitadas: RotaVisitada[] = [];

  for (const route of routes) {
    const trecho = route.path ?? '';
    const caminho = [prefixo, trecho].filter((p) => p.length > 0).join('/') || '(raiz)';

    visitadas.push({ caminho, route });

    if (route.children) {
      visitadas.push(...(await coletar(route.children, caminho === '(raiz)' ? '' : caminho)));
    }

    if (route.loadChildren) {
      const filhas = await (route.loadChildren as () => Promise<Routes>)();
      visitadas.push(...(await coletar(filhas, caminho === '(raiz)' ? '' : caminho)));
    }
  }

  return visitadas;
}

const TODAS = await coletar(APP_ROUTES, '');
const GUARDADAS = TODAS.filter((v) => (v.route.canActivate ?? []).includes(roleGuard));

describe('cobertura de rotas — data.roles', () => {
  it('deveAlcancarRotasGuardadas_quandoVarrerTodasAsFeatures', () => {
    // O piso é deliberadamente folgado — existe para acusar uma varredura que parou de enxergar
    // (uma feature inteira ignorada, um `loadChildren` que deixou de resolver), não para ser
    // atualizado a cada rota nova. Suba-o quando o projeto crescer o bastante para que o número
    // atual pare de significar alguma coisa.
    expect(GUARDADAS).toHaveLength(8);
  });

  for (const { caminho, route } of GUARDADAS) {
    it(`"${caminho}" guardada por roleGuard declara data.roles não vazio`, () => {
      const roles = route.data?.['roles'];
      const isNonEmptyArray = Array.isArray(roles) && roles.length > 0;

      // O caminho já vai no título do `it` acima — é o que aparece no relatório de falha.
      expect(isNonEmptyArray).toBe(true);
    });
  }
});
