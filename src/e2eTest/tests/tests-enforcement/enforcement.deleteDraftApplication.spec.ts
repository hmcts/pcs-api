import { test } from '@utils/test-fixtures';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { enforcementApplication } from '@data/page-data-figma/page-data-enforcement-figma/enforcementApplication.page.data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView() });
  await performAction('updatePaymentAPI');
  await performAction(
    'navigateToUrl',
    `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`
  );
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('[Enforcement - Delete Draft Application]', () => {
  test('delete draft application',
   async () => {
         await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
         await performAction('clickButton', caseSummary.go);
         await performValidation('mainHeader', enforcementApplication.mainHeader);
         console.log('Enforcement application submitted successfully');
  });
});
