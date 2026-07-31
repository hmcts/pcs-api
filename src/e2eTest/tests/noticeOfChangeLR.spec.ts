import { createCaseApiData, submitCaseApiData } from '@data/api-data';


import { initializeExecutor , performAction } from '@utils/controller';
import test from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { home } from '@data/page-data';


test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('clickTab', home.noticeOfChangeTab);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Make an Application - LR - e2e Journey @nightly', async () => {
  test('Notice of change - LR @regression @smoke', async () => {
    await performAction('noticeOfChange', { caseRefNo: caseInfo.id } );
    await performAction('clientDetails', { firstName: 'Peter' , lastName: 'Parker' });

  });
});
