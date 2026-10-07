export interface RuntimeEnvironment {
  production: boolean;
  apiBaseUrl: string;
  oidc: {
    issuer: string;
    clientId: string;
    redirectUri: string;
    postLogoutRedirectUri: string;
    scope: string;
  };
}

export type RuntimeEnvironmentOverrides = Partial<
  Omit<RuntimeEnvironment, 'oidc'> & {
    oidc: Partial<RuntimeEnvironment['oidc']>;
  }
>;

export function buildEnvironment(
  defaults: RuntimeEnvironment,
  runtimeOverrides: RuntimeEnvironmentOverrides = {},
): RuntimeEnvironment {
  return {
    ...defaults,
    ...runtimeOverrides,
    oidc: {
      ...defaults.oidc,
      ...runtimeOverrides.oidc,
    },
  };
}
