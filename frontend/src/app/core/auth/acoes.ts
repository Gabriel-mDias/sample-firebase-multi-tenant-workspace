import { actionCatalog } from './authorization.actions';
export { Acoes } from './authorization.actions';
export type { Acao } from './authorization.actions';
export function belongsToScope(action: string, scope: 'global' | 'tenant'): boolean {
  return (actionCatalog[scope] as readonly string[]).includes(action);
}
