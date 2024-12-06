import { test } from '@playwright/test';
import config from "../config";

test('has title @smoke', async ({ page }) => {
  await page.goto(config.manageCasesBaseURL);
});
