import { expect, Locator, Page, Response as PlaywrightResponse, test } from '@playwright/test';
import type { AuthService } from '../src/app/core/auth/auth.service';

// Real Firebase test project: public Web config and synthetic, per-run accounts come from the
// environment prepared by FirebaseEndToEndIntegrationTest. Admin credentials never reach this process.
// Google popup and reset e-mail delivery are verified by a human: see
// specs/003-firebase-multi-tenant/configuracao-homologacao-firebase.md.
const api = process.env['T029_API_URL']!;
const firebase = {
  apiKey: process.env['T029_FIREBASE_API_KEY']!,
  authDomain: process.env['T029_FIREBASE_AUTH_DOMAIN']!,
  projectId: process.env['T029_FIREBASE_PROJECT_ID']!,
  appId: process.env['T029_FIREBASE_APP_ID']!,
};
const aliceEmail = process.env['T029_ALICE_EMAIL']!;
const bobEmail = process.env['T029_BOB_EMAIL']!;
const unknownEmail = process.env['T029_UNKNOWN_EMAIL']!;
const sameId = '00000000-0000-0000-0000-000000000123';

type Runtime = typeof globalThis & {
  ng: {
    getComponent(element: Element): {
      auth: AuthService;
      navigation: { navigate(commands: string[]): Promise<boolean> };
    };
  };
  t029Token?: { started: boolean; release(): void };
};

async function navigateInApp(page: Page, path: string): Promise<void> {
  await page.evaluate((path) => {
    const root = (globalThis as Runtime).ng.getComponent(document.querySelector('app-root')!);
    return root.navigation.navigate([path]);
  }, path);
}

async function delayCurrentNativeToken(page: Page): Promise<void> {
  await page.evaluate(() => {
    const runtime = globalThis as Runtime;
    const auth = runtime.ng.getComponent(document.querySelector('app-root')!).auth;
    const user = auth.authenticatedUser()!;
    const original = user.getIdToken;
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));
    const observation = { started: false, release };
    runtime.t029Token = observation;
    user.getIdToken = async function () {
      // Call the actual Firebase user method; only the first completion is delayed.
      user.getIdToken = original;
      const token = await original.call(user);
      observation.started = true;
      await gate;
      return token;
    };
  });
}

async function releaseNativeToken(page: Page): Promise<void> {
  await page.evaluate(() => (globalThis as Runtime).t029Token!.release());
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(
    ({ api, firebase }) => {
      Object.assign(globalThis, {
        // An empty authEmulatorUrl keeps the app on the real Firebase project.
        __env: { production: false, apiBaseUrl: api, authEmulatorUrl: '', firebase },
      });
    },
    { api, firebase },
  );
});

// The synthetic password must never be an argument of a Playwright action: locator.fill() prints its
// value in the call log of a failure. Evaluate arguments are not logged.
async function setSecret(field: Locator, value: string): Promise<void> {
  await field.evaluate((element, secret) => {
    const input = element as HTMLInputElement;
    input.value = secret;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }, value);
}

// Quota is an explicit failure (never retried and never mistaken for a denial); only the Firebase
// error code is read, never the body.
async function firebaseOutcome(
  response: PlaywrightResponse,
): Promise<{ status: number; code: string }> {
  let code = '';
  try {
    const body = await response.json();
    code = String(body?.error?.message ?? '').split(/[ :]/)[0];
  } catch {
    code = '';
  }
  if (
    response.status() === 429 ||
    /RESOURCE_EXHAUSTED|QUOTA_EXCEEDED|TOO_MANY_ATTEMPTS/.test(code)
  ) {
    throw new Error(`QUOTA_EXHAUSTED signInWithPassword (HTTP ${response.status()}); sem retry`);
  }
  return { status: response.status(), code };
}

function signInResponse(page: Page): Promise<PlaywrightResponse> {
  return page.waitForResponse(
    (response) => response.url().includes('accounts:signInWithPassword'),
    {
      timeout: 15_000,
    },
  );
}

async function submitCredentials(page: Page, email: string): Promise<void> {
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await setSecret(page.getByLabel('Senha', { exact: true }), process.env['T029_PASSWORD']!);
  const response = signInResponse(page);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  const outcome = await firebaseOutcome(await response);
  if (outcome.status !== 200) {
    throw new Error(`Login real recusado (HTTP ${outcome.status} ${outcome.code})`);
  }
  await expect(page.getByRole('heading', { name: 'Escolha sua empresa' })).toBeVisible();
}

async function passwordLogin(page: Page, returnTo = '/demo-registro/list'): Promise<void> {
  await page.goto(`/login?returnTo=${encodeURIComponent(returnTo)}`);
  await submitCredentials(page, aliceEmail);
}

// Bob belongs to Alpha and Beta; a second real password identity replaces the former Google popup.
async function bobLogin(page: Page, navigate = true): Promise<void> {
  if (navigate) await page.goto('/login');
  await submitCredentials(page, bobEmail);
}

for (const width of [320, 768, 1440]) {
  test(`login keyboard labels focus and no horizontal overflow at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Bem-vindo de volta.' })).toBeVisible();
    await expect(page.getByLabel('E-mail', { exact: true })).toHaveAttribute(
      'autocomplete',
      'username',
    );
    await expect(page.getByLabel('Senha', { exact: true })).toHaveAttribute(
      'autocomplete',
      'current-password',
    );
    await page.getByLabel('E-mail', { exact: true }).focus();
    await page.keyboard.press('Tab');
    await expect(page.getByLabel('Senha', { exact: true })).toBeFocused();
    const dimensions = await page.evaluate(() => ({
      width: document.documentElement.clientWidth,
      scroll: document.documentElement.scrollWidth,
    }));
    expect(dimensions.scroll).toBeLessThanOrEqual(dimensions.width + 1);
    const focus = await page.getByLabel('Senha', { exact: true }).evaluate((input) => {
      const style = getComputedStyle(input);
      return { outline: style.outlineStyle, shadow: style.boxShadow };
    });
    expect(focus.outline !== 'none' || focus.shadow !== 'none').toBe(true);
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await expect(page.locator('.login-error[role="alert"]')).toContainText('Confira os campos');
    await expect(page).toHaveURL(/\/login/);
  });
}

test('invalid credentials announce a generic error with native Firebase request', async ({
  page,
}) => {
  await page.goto('/login');
  // The address belongs to this run's namespace but is never created.
  await page.getByLabel('E-mail', { exact: true }).fill(unknownEmail);
  await page.getByLabel('Senha', { exact: true }).fill('invalid-test-credential');
  const response = signInResponse(page);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  const answer = await response;
  expect(answer.url().startsWith('https://identitytoolkit.googleapis.com/')).toBe(true);
  // The denial must be the provider's credential rejection, not quota, throttling or a bad key.
  const outcome = await firebaseOutcome(answer);
  expect(outcome.status).toBe(400);
  expect(outcome.code).toMatch(/^(INVALID_LOGIN_CREDENTIALS|EMAIL_NOT_FOUND|INVALID_PASSWORD)$/);
  await expect(page.locator('.login-error[role="alert"]')).toHaveText(
    'Não foi possível entrar. Confira seu acesso ou tente novamente.',
  );
});

// The reset request would send real e-mail to a synthetic address, so it is not automated; the
// generic confirmation is verified by a human (configuracao-homologacao-firebase.md, section 8).

test('password login loads real context and restricted fields remain hidden', async ({ page }) => {
  await passwordLogin(page, `/demo-registro/view/${sameId}`);
  const returnPath = await page.evaluate(() => {
    const runtime = globalThis as typeof globalThis & {
      ng: { getComponent(element: Element): { auth: { returnPath: string } } };
    };
    return runtime.ng.getComponent(document.querySelector('app-root')!).auth.returnPath;
  });
  console.log(`T029 BROWSER INTERNAL_RETURN_BEFORE_CONTEXT ${returnPath}`);
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/demo-registro/view/${sameId}$`));
  await expect(page.getByRole('heading', { name: 'Visualizar registro' })).toBeVisible();
  await expect(page.getByLabel('Nome', { exact: true })).toHaveValue('Alpha');
  await expect(page.getByText('Valor restrito', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Trocar empresa' }).click();
  await expect(page.getByRole('button', { name: 'Beta', exact: true })).toHaveCount(0);
});

test('external return is rejected after password login and context validation', async ({
  page,
}) => {
  await passwordLogin(page, '//untrusted.example');
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await expect(page).toHaveURL(/\/demo-registro\/list$/);
});

test('password login of a multi-company user switches same primary key Alpha to Beta without leakage', async ({
  page,
}) => {
  await bobLogin(page);
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Trocar empresa' }).click();
  await page.getByRole('button', { name: 'Beta', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'Beta', exact: true })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toHaveCount(0);
  await page.getByTitle('Encerrar sessão', { exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('button', { name: 'Trocar empresa' })).toHaveCount(0);
});

test('logout while a real context response is delayed prevents late navigation', async ({
  page,
}) => {
  await passwordLogin(page);
  let release!: () => void;
  const gate = new Promise<void>((resolve) => (release = resolve));
  let arrived!: () => void;
  const started = new Promise<void>((resolve) => (arrived = resolve));
  await page.route(`${api}/auth/context`, async (route) => {
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    arrived();
    await gate;
    await route.fulfill({ response }).catch(() => {});
  });
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await started;
  try {
    await page.getByTitle('Encerrar sessão', { exact: true }).click();
    await expect(page).toHaveURL(/\/login$/);
  } finally {
    release();
  }
  await page.waitForTimeout(300);
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('heading', { name: 'Bem-vindo de volta.' })).toBeVisible();
});

test('actual role guard denies business route before tenant context', async ({ page }) => {
  await passwordLogin(page);
  await navigateInApp(page, '/demo-registro/list');
  await expect(page).toHaveURL(/\/acesso-negado$/);
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toHaveCount(0);
});

test('late real Alpha response cannot overwrite ready Beta context or navigate back', async ({
  page,
}) => {
  await bobLogin(page);
  let release!: () => void;
  const gate = new Promise<void>((resolve) => (release = resolve));
  let arrived!: () => void;
  const started = new Promise<void>((resolve) => (arrived = resolve));
  await page.route(`${api}/auth/context`, async (route) => {
    if (route.request().headers()['x-tenant-alias'] !== 'alpha') return route.continue();
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    arrived();
    await gate;
    await route.fulfill({ response }).catch(() => {});
  });
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await started;
  try {
    await page.evaluate(() =>
      (globalThis as Runtime).ng
        .getComponent(document.querySelector('app-root')!)
        .auth.selectTenant('beta'),
    );
    await navigateInApp(page, '/demo-registro/list');
    await expect(page.getByRole('cell', { name: 'Beta', exact: true })).toBeVisible();
  } finally {
    release();
  }
  await page.waitForTimeout(300);
  await expect(page).toHaveURL(/\/demo-registro\/list$/);
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toHaveCount(0);
  expect(
    await page.evaluate(
      () =>
        (globalThis as Runtime).ng
          .getComponent(document.querySelector('app-root')!)
          .auth.currentTenant()?.alias,
    ),
  ).toBe('beta');
});

test('new native UID ignores delayed context from previous UID', async ({ page }) => {
  await passwordLogin(page);
  const oldUid = await page.evaluate(
    () =>
      (globalThis as Runtime).ng
        .getComponent(document.querySelector('app-root')!)
        .auth.authenticatedUser()!.uid,
  );
  let release!: () => void;
  const gate = new Promise<void>((resolve) => (release = resolve));
  let arrived!: () => void;
  const started = new Promise<void>((resolve) => (arrived = resolve));
  let first = true;
  await page.route(`${api}/auth/context`, async (route) => {
    if (!first) return route.continue();
    first = false;
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    arrived();
    await gate;
    await route.fulfill({ response }).catch(() => {});
  });
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await started;
  try {
    await page.getByTitle('Encerrar sessão', { exact: true }).click();
    await expect(page).toHaveURL(/\/login$/);
    await bobLogin(page, false);
    await page.getByRole('button', { name: 'Beta', exact: true }).click();
    await expect(page.getByRole('cell', { name: 'Beta', exact: true })).toBeVisible();
  } finally {
    release();
  }
  await page.waitForTimeout(300);
  const current = await page.evaluate(() => {
    const auth = (globalThis as Runtime).ng.getComponent(document.querySelector('app-root')!).auth;
    return { uid: auth.authenticatedUser()!.uid, alias: auth.currentTenant()?.alias };
  });
  expect(current.uid).not.toBe(oldUid);
  expect(current.alias).toBe('beta');
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toHaveCount(0);
});

test('switch during actual token await prevents Alpha payload from reaching Beta', async ({
  page,
}) => {
  await bobLogin(page);
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toBeVisible();
  await navigateInApp(page, `/demo-registro/form/${sameId}`);
  await expect(page.getByLabel('Nome', { exact: true })).toHaveValue('Alpha');
  await delayCurrentNativeToken(page);
  let writes = 0;
  page.on('request', (request) => {
    if (request.method() === 'PUT' && request.url() === `${api}/demo-registros/${sameId}`) writes++;
  });
  await page.getByLabel('Nome', { exact: true }).fill('Alpha payload never sent');
  await page.getByRole('button', { name: 'Salvar', exact: true }).click();
  await expect
    .poll(() => page.evaluate(() => (globalThis as Runtime).t029Token?.started))
    .toBe(true);
  try {
    await page.getByRole('button', { name: 'Trocar empresa' }).click();
    await page.getByRole('button', { name: 'Beta', exact: true }).click();
    await expect(page.getByRole('cell', { name: 'Beta', exact: true })).toBeVisible();
  } finally {
    await releaseNativeToken(page);
  }
  await page.waitForTimeout(300);
  expect(writes).toBe(0);
  await expect(
    page.getByRole('cell', { name: 'Alpha payload never sent', exact: true }),
  ).toHaveCount(0);
});

test('logout during actual native token await prevents old business request and late state', async ({
  page,
}) => {
  await passwordLogin(page);
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toBeVisible();
  await delayCurrentNativeToken(page);
  let reads = 0;
  page.on('request', (request) => {
    if (request.url() === `${api}/demo-registros/${sameId}`) reads++;
  });
  await navigateInApp(page, `/demo-registro/view/${sameId}`);
  await expect
    .poll(() => page.evaluate(() => (globalThis as Runtime).t029Token?.started))
    .toBe(true);
  try {
    await page.getByTitle('Encerrar sessão', { exact: true }).click();
    await expect(page).toHaveURL(/\/login$/);
  } finally {
    await releaseNativeToken(page);
  }
  await page.waitForTimeout(300);
  expect(reads).toBe(0);
  await expect(page).toHaveURL(/\/login$/);
});

test('failed Beta context leaves no active Alpha data or actions', async ({ page }) => {
  await bobLogin(page);
  await page.getByRole('button', { name: 'Alpha', exact: true }).click();
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toBeVisible();
  await page.route(`${api}/auth/context`, async (route) => {
    expect(route.request().headers()['x-tenant-alias']).toBe('beta');
    // Controlled transport failure; backend authorization itself is covered by HTTP cases.
    await route.fulfill({
      status: 503,
      contentType: 'application/json',
      body: '{"code":"PROVIDER_UNAVAILABLE"}',
    });
  });
  await page.getByRole('button', { name: 'Trocar empresa' }).click();
  await page.getByRole('button', { name: 'Beta', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Não foi possível acessar esta empresa');
  const state = await page.evaluate(() => {
    const auth = (globalThis as Runtime).ng.getComponent(document.querySelector('app-root')!).auth;
    return { alias: auth.currentTenant()?.alias ?? null, actions: auth.effectiveActions() };
  });
  expect(state).toEqual({ alias: null, actions: [] });
  await expect(page.getByRole('cell', { name: 'Alpha', exact: true })).toHaveCount(0);
});
