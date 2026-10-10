export interface RuntimeEnvironment {
  production: boolean;
  apiBaseUrl: string;
  firebase: { apiKey: string; authDomain: string; projectId: string; appId: string };
  authEmulatorUrl?: string;
}
export type RuntimeEnvironmentOverrides = Partial<Omit<RuntimeEnvironment, 'firebase'>> & {
  firebase?: Partial<RuntimeEnvironment['firebase']>;
};
export function buildEnvironment(
  defaults: RuntimeEnvironment,
  overrides: RuntimeEnvironmentOverrides = {},
): RuntimeEnvironment {
  return { ...defaults, ...overrides, firebase: { ...defaults.firebase, ...overrides.firebase } };
}
