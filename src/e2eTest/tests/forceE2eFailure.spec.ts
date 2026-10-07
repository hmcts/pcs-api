import { test, expect } from '@playwright/test';

// TEMPORARY: deliberately failing test to verify the Jenkins pipeline aborts the build
// after an E2E failure on master, instead of continuing to Publish Helm chart / prod deploy.
// Remove this file once verified (see follow-up PR).
test('canary - deliberately fails to verify pipeline abort @healthCheck', async () => {
  expect(true).toBe(false);
});
