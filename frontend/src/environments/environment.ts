import { buildEnvironment, RuntimeEnvironmentOverrides } from './environment.model';
const runtime =
  (globalThis as typeof globalThis & { __env?: RuntimeEnvironmentOverrides }).__env ?? {};
export const environment = buildEnvironment(
  {
    production: false,
    apiBaseUrl: 'http://localhost:8080/api',
    firebase: {
      apiKey: 'demo-key',
      authDomain: 'localhost',
      projectId: 'demo-gems',
      appId: 'demo-gems-web',
    },
    authEmulatorUrl: 'http://localhost:9099',
  },
  runtime,
);
