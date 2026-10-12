import { Acao } from './acoes';
export interface Tenant {
  alias: string;
  name: string;
}
export interface AuthMe {
  user: { id: string; name: string; email: string };
  tenants: Tenant[];
  globalActions: Acao[];
}
export interface AuthContext {
  tenant: Tenant;
  profiles: { code: string; name: string }[];
  groups: { code: string; name: string }[];
  actions: Acao[];
}
export interface SessionSnapshot {
  readonly generation: number;
  readonly session: number;
  readonly uid: string;
  readonly alias: string | null;
  readonly ready: boolean;
}
export type AuthorizationScope = 'global' | 'tenant';
export interface ProductAuthorizationData {
  roles: readonly Acao[];
  authorizationScope: AuthorizationScope;
  roleMode?: 'any' | 'all';
}
