import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {caseApiData} from '@data/api-data/case.api.data';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {caseInfo} from '@utils/actions/custom-actions/createCase.action';
import {user} from '@data/user-data/permanent.user.data';

test.beforeEach(async ({page}, testInfo) => {
    initializeExecutor(page);
    await parentSuite('Search Case');
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await testInfo.attach('Page URL', {
      body: page.url(),
      contentType: 'text/plain',
    });
    await performAction('login', user.claimantSolicitor);
  await performAction('createCase', {data: caseApiData.createCasePayload});
});

//Skipping these tests until create case journey is fully developed because tests may fail each time when payload changes for create case API
test.describe.skip('[Search case by case number] @PR @Master @nightly', () => {
  test('Search for case via caselist', async ({}) => {
    await performAction('searchCaseById', caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
  test('Search for case via find case', async ({}) => {
    await performAction('searchCaseById', caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
});
