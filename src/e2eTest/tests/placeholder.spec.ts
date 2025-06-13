import { test } from '@playwright/test';
import ConfigData from "@data/config.data";

test('has title @PR @nightly', async ({ page }) => {
  await page.goto(ConfigData.manageCasesBaseURL);
});
