import {test} from '@playwright/test';
import {caseApiData} from '@data/api-data/case.api.data';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {caseInfo} from '@utils/actions/custom-actions/createCase.action';
import {user} from '@data/user-data/permanent.user.data';

test.beforeEach(async ({page}) => {
    initializeExecutor(page);
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await performAction('login', user.claimantSolicitor);
  await performAction('createCase', {data: caseApiData.createCasePayload});
});

//Skipping these tests until create case journey is fully developed because tests may fail each time when payload changes for create case API
test.describe.skip('[Search Case] @PR @Master @nightly', () => {
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
