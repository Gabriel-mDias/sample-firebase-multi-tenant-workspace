import { Route, Routes } from '@angular/router';
import { routes } from '../../app.routes';
import { roleGuard } from './role.guard';
import { belongsToScope } from '../auth/acoes';
async function visit(routes: Routes): Promise<number> {
  let count = 0;
  for (const route of routes) {
    if (route.canActivate?.includes(roleGuard)) {
      count++;
      const data = route.data;
      expect(data?.['roles']?.length).toBeGreaterThan(0);
      expect(['global', 'tenant']).toContain(data?.['authorizationScope']);
      for (const action of data?.['roles'] ?? [])
        expect(belongsToScope(action, data!['authorizationScope'])).toBe(true);
    }
    if (route.children) count += await visit(route.children);
    if (route.loadChildren) {
      const loaded = await (route.loadChildren as () => Promise<{ default: Routes }>)();
      count += await visit(loaded.default);
    }
  }
  return count;
}
describe('Cobertura real de rotas lazy', () => {
  it('deveExigirRolesEscopo_quandoResolveTodasAsFeatures', async () => {
    expect(await visit(routes)).toBe(5);
  });
});
