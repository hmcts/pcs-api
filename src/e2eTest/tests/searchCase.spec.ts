import { test } from '@fixtures/authenticated-context.fixture';
import { caseApiData } from '@data/api-data';
import { signInOrCreateAnAccount } from '@data/page-data';
import { user } from '@data/user-data';
import { caseInfo } from '@utils/actions/custom-actions';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';

test.beforeEach(async ({authenticatedPage}) => {
  initializeExecutor(authenticatedPage);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('createCase', {data: caseApiData.createCasePayload});
});

//Skipping these tests until create case journey is fully developed because tests may fail each time when payload changes for create case API
test.describe.skip('[Search Case] @regression', () => {
  test('Search for case via caselist', async ({}) => {
    await performAction('searchCaseFromCaseList', caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
  test('Search for case via find case', async ({}) => {
    await performAction('searchCaseFromCaseList', caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
});
