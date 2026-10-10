import { defineConfig } from '@playwright/test';

// The suite runs against the real Firebase test project with per-run synthetic accounts: the Java
// test passes the API URL, the public Web config and the account names. Never run it standalone.
const required = ['T029_API_URL', 'T029_PASSWORD', 'T029_FIREBASE_API_KEY', 'T029_FIREBASE_APP_ID'];
if (required.some((name) => !process.env[name])) {
  throw new Error(
    'Run through FirebaseEndToEndIntegrationTest with its owned disposable resources.',
  );
}

export default defineConfig({
  testDir: './e2e',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  // Serialized on purpose: real accounts and Firebase quotas must not be exercised in parallel.
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['./e2e/sanitized-reporter.ts']],
  outputDir: './.angular/t029-playwright',
  use: {
    baseURL: 'http://localhost:4301',
    browserName: 'chromium',
    headless: true,
    trace: 'off',
    screenshot: 'off',
    video: 'off',
  },
  webServer: {
    command: 'npx ng serve --host localhost --port 4301',
    url: 'http://localhost:4301/login',
    reuseExistingServer: false,
    timeout: 180_000,
    stdout: 'ignore',
    stderr: 'ignore',
  },
});
