import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {caseApiData} from '@data/api-data/case.api.data';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {createCase} from "@data/page-data/createCase.page.data";
import {caseInfo} from '@utils/actions/custom-actions/createCase.action';

test.beforeEach(async ({page}, testInfo) => {
    initializeExecutor(page);
    await parentSuite('Search Case');
    await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
    await testInfo.attach('Page URL', {
      body: page.url(),
      contentType: 'text/plain',
    });
    await performAction('createUserAndLogin', 'claimant', ['caseworker-pcs', 'caseworker']);
    createCaseWithAddress();
});

async function createCaseWithAddress() {
  await performAction('createCase', {
    data: caseApiData.createCasePayload,
  });
}

async function searchCase(caseNumber: string) {
  await performAction('select', 'Jurisdiction', createCase.possessionsJurisdiction);
  await performAction('select', 'Case type', createCase.caseType.civilPossessions);
  await performAction('inputText', 'Case Number', caseNumber);
  await performAction('clickButton', 'Apply');
}

//Skipping these tests until create case journey is fully developed because tests may fail each time when payload changes for create case API
test.describe.skip('[Search case by case number] @PR @Master @nightly', () => {
  test('Search for case via caselist', async ({}) => {
    await searchCase(caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
  test('Search for case via find case', async ({}) => {
    await searchCase(caseInfo.id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: caseInfo.fid}
    );
  });
});
