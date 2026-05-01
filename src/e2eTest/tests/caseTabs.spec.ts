import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor,performAction } from '@utils/controller';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);


  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseTab });

  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Case%20Parties`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Case%20Parties`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
  PageContentValidation.finaliseTest();
});
// Skipping this test case as the feature is not part of Release 1 to save execution time.
// It will be enabled once the feature is included in the execution scope.
test.describe('[Case tabs - England Journey] @nightly', async () => {
  test('Case tabs - Case parties tab test', async () => {
  });

});
