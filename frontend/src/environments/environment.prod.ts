import { buildEnvironment, RuntimeEnvironmentOverrides } from './environment.model';

const runtimeEnv =
  (
    globalThis as typeof globalThis & {
      __env?: RuntimeEnvironmentOverrides;
    }
  ).__env ?? {};

export const environment = buildEnvironment(
  {
    production: true,
    apiBaseUrl: 'https://api.exemplo.com',
    oidc: {
      issuer: 'https://auth.exemplo.com/realms/app-realm',
      clientId: 'app-frontend',
      redirectUri: `${window.location.origin}/callback`,
      postLogoutRedirectUri: window.location.origin,
      scope: 'openid profile email',
    },
  },
  runtimeEnv,
);
