import { expect, test } from '@playwright/test';
import { LoginPage } from '../loginPage';
import Config from '../../config';

test('create case @PR @nightly', async ({ page }) => {
  const loginPage = new LoginPage(page);
  await loginPage.goto(Config.manageCasesBaseURL);
  await loginPage.login("caseworker@pcs.com", 'pass123');
  await page.click('a:has-text("Create case")');
  await page.waitForTimeout(5000);
  await page.click('button:has-text("Start")');
    // Locate the label by 'for' attribute
    const label = await page.locator('label[for="applicantForename"] >> span.form-label').textContent();

    if (label?.trim() === "Applicant's first name") {
     // Fill the input with id "applicantForename"
     await page.fill('#applicantForename', 'sameena test');
    } else {
     throw new Error('Label text did not match');
    }
    await page.click('button:has-text("Submit")');
    const alertLocator = page.locator('div.alert-message');
    await alertLocator.waitFor({ state: 'visible' });

    const message = await alertLocator.textContent();
    console.log('Alert message:', message);

    // Extract the case number(s) after "Case #"
    // Matches digits and dashes (e.g., "13", "13-14", "13-14-15", etc.)
    const match = message?.match(/Case #([\d-]+) has been created\./);

    if (match) {
      const caseNumbers = match[1]; // e.g. "13-14-15"
      console.log('Case numbers found:', caseNumbers);
    } else {
      throw new Error('Case numbers not found in alert message.');
    }

    // Optionally, assert the message pattern (you can skip if just extracting)
    await expect(alertLocator).toHaveText(/Case #([\d-]+) has been created\./);
    await page.waitForTimeout(10000);

});
