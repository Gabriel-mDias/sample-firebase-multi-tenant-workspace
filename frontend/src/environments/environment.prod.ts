import { buildEnvironment, RuntimeEnvironmentOverrides } from './environment.model';
const runtime =
  (globalThis as typeof globalThis & { __env?: RuntimeEnvironmentOverrides }).__env ?? {};
export const environment = buildEnvironment(
  {
    production: true,
    apiBaseUrl: '/api',
    firebase: { apiKey: '', authDomain: '', projectId: '', appId: '' },
  },
  runtime,
);
