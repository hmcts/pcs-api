import { expect, test } from '@playwright/test';
import { LoginPage } from '../loginPage';
import Config from '../../config';

test('Authentication tests @functional @PR @nightly', async ({ page }) => {
  await page.goto(Config.manageCasesBaseURL);
  await expect(page.locator('h1')).toHaveText('RSE Idam Simulator Login Form');
});
test('login with valid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto(Config.manageCasesBaseURL);
    await loginPage.login('caseworker@pcs.com', 'pass123');
    await expect(page.locator('a.hmcts-header__link')).toHaveText('Manage cases');
    await expect(page.locator('a.hmcts-header__link')).toBeVisible();

  });
test('property search @PR @nightly', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto(Config.manageCasesBaseURL);
  await loginPage.login("caseworker@pcs.com", 'pass123');
  await page.goto(`${Config.manageCasesBaseURL}/cases/case-filter`);

});

