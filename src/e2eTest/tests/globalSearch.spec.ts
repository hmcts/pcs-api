import { test } from '@utils/test-fixtures';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

//Skipping these tests as per the decision taken on https://tools.hmcts.net/jira/browse/HDPI-3317
test.describe.skip('[Search Case]', () => {
  test('Search for case via case list', async ({}) => {
    await performAction('filterCaseFromCaseList', caseInfo.state);
    await performValidation('text', {
      "text": 'Case number: '+caseInfo.fid,
      "elementType": "paragraph"
    });
  });

  test('Search for case via find case @PR' , async ({}) => {
    await performAction('searchCaseFromFindCase', caseInfo.fid);
    await performValidation('text', {
      "text": 'Case number: '+caseInfo.fid,
      "elementType": "paragraph"
    });
  });
});
