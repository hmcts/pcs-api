import { test } from '@utils/test-fixtures';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { globalSearch } from '@data/page-data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Global Search]', () => {
  test('Should load the global search page with correct header @PR', async () => {
  await performAction('navigateToUrl', 'navigateToGlobalSearch');
  await performValidation('mainHeader', globalSearch.mainHeader);
});

test('Should find a case by 16-digit case reference @PR', async () => {
  await performAction('navigateToUrl', 'navigateToGlobalSearch');
  await performAction('searchByCaseReference', caseInfo.fid);
  await performValidation('text', {
    text: caseInfo.fid,
    elementType: 'paragraph'
  });
});
});