import { test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { home } from '@data/page-data';
import { Page, BrowserContext } from '@playwright/test';
import { users } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';

async function clearBrowserSession(page: Page, context: BrowserContext): Promise<void> {
  await context.clearCookies();
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch {
      // Ignore if storage is not accessible
    }
  });
}

const userEmail = users.find(
  user => user.user === 'Creator'
)?.email;


const userPassword = users.find(
  user => user.user === 'Creator'
)?.password;


test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPIDynamicUsers', { data: createCaseApiData.createCasePayload, email: userEmail, password: userPassword });
  await performAction('submitCaseAPIDynamicUsers', { data: submitCaseApiData.submitCasePayloadCaseSummary, email: userEmail, password: userPassword });
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('[Case tabs Access - England Journey] @nightly', async () => {
users.forEach(({ user, email, password, tabAccess }) => {
  test(`Case tabs Access - Check for update access for user "${user}" @MAC @regression`, async ({ page, context }) => {

    if (user === 'Defendant Solicitor') {
      await performAction('getCaseAPIDynamic', { req: 'Link Solicitor', email: email, password: password });
    } else if (user === 'Claimant Solicitor') {
      await performAction('createCaseAPIDynamicUsers', { data: createCaseApiData.createCasePayload, email: email, password: password });
      await performAction('submitCaseAPIDynamicUsers', { data: submitCaseApiData.submitCasePayloadDefault, email: email, password: password });
    }
    await clearBrowserSession(page, context);
    await dismissCookieBanner(page, 'additional');
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}`);
    await performAction('login', { email: email, password: password });
    await dismissCookieBanner(page, 'analytics');
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    await performValidation('mainHeader', home.caseSummary);
    await performAction('validateTabAccess', { user: user, tabs: tabAccess });
  });

})
});