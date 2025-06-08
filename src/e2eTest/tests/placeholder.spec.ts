import {test} from '@playwright/test';
import Config from "../config/config";

test('has title @functional @PR @nightly', async ({ page }) => {
  console.log('Manage Cases Base URL:', Config.manageCasesBaseURL);
  await page.goto(Config.manageCasesBaseURL);
});
