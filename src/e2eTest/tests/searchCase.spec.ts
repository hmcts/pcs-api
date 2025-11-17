import {test} from '@playwright/test';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import {caseInfo} from '@utils/actions/custom-actions';
import {caseApiData} from '@data/api-data';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  // User is already authenticated via globalSetup
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-create/PCS/${process.env.CHANGE_ID ? `PCS-${process.env.CHANGE_ID}` : 'PCS'}/createPossessionClaim/createPossessionClaimstartTheService`);
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
