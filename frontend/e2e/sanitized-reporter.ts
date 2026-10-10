import type { Reporter, TestCase, TestResult } from '@playwright/test/reporter';

/** Do not serialize request bodies, headers, storage, remote errors or password fields. */
export default class SanitizedReporter implements Reporter {
  onTestEnd(test: TestCase, result: TestResult): void {
    console.log(`T029 BROWSER ${result.status.toUpperCase()} ${test.title}`);
    for (const error of result.errors) {
      const lines = (error.message ?? '').split('\n');
      console.log(
        lines
          .filter((line) => !/Bearer|eyJ|password|postData|localId|oobCode/i.test(line))
          .slice(0, 12)
          .join('\n'),
      );
    }
  }
}
