import { test } from '@playwright/test';
import config from "../config";

test('has title @PR @nightly', async ({ page }) => {
  await page.goto(config.manageCasesBaseURL);
});
