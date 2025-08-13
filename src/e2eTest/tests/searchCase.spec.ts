import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import configData from "@config/test.config";
import caseDataWithAddress from '@data/api-data/case.api.data.json';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import {createCase} from "@data/page-data/createCase.page.data";
import {getCaseInfo} from '@utils/actions/custom-actions/createCase.action';

test.beforeEach(async ({page}) => {
    initializeExecutor(page);
    await parentSuite('Case Creation');
    await performAction('navigateToUrl', configData.manageCasesBaseURL);
    await performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker']);
    createCaseWithAddress();
});

async function createCaseWithAddress() {
  await performAction('createCase', {
    data: caseDataWithAddress.data,
  });
}

async function searchCase(caseNumber: string) {
  await performAction('select', 'Jurisdiction', createCase.possessionsJurisdiction);
  await performAction('select', 'Case type', createCase.caseType.civilPossessions);
  await performAction('inputText', 'Case Number', caseNumber);
  await performAction('clickButton', 'Apply');
}

test.describe.skip('Search case by case number @PR @Master @nightly', () => {
  test('Search for case via caselist', async ({}) => {
    await performAction('clickButton', 'Case list');
    await searchCase(getCaseInfo().id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: getCaseInfo().fid}
    );
  });
  test('Search for case via find case', async ({}) => {
    await performAction('clickButton', 'Find case');
    await searchCase(getCaseInfo().id);
    await performValidation(
      'visibility',
      'caseNumber',
      {visible: getCaseInfo().fid}
    );
  });
});

