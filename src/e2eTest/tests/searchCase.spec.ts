// src/e2eTest/tests/searchCase.spec.ts
import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {getSessionVariable} from '../utils/controller';
import configData from "@config/test.config";
import {
  initializeExecutor,
  performAction,
  performActions,
  performValidation
} from '@utils/controller';
import {caseData} from '@data/case.data';

test.beforeEach(async ({page}, testInfo) => {

  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('login', 'exuiUser');

  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
});

/*test.afterEach(async () => {
  await performAction('click', 'Sign out');
});*/

async function createCaseWithAddress() {

  await performAction('click', 'Create case');

  await performActions('Case option selection'
    , ['select', 'Jurisdiction', caseData.jurisdiction]
    , ['select', 'Case type', caseData.caseType]
    , ['select', 'Event', caseData.event]);

  await performAction('click', 'Start');

  await performActions('Find Address based on postcode'
    , ['fill', 'Enter a UK postcode', caseData.postcode]
    , ['click', 'Find address']
    , ['select', 'Select an address', caseData.addressIndex]
    , ['fill', 'Address Line 2', caseData.addressLine2]
    , ['fill', 'Address Line 3', caseData.addressLine3]
    , ['fill', 'County', caseData.county]);

  await performAction('click', 'Continue');

  await performAction('fill', "Applicant's forename", caseData.applicantFirstName);

  await performAction('click', 'Submit');

  await performValidation("bannerAlert", {message: "Case #.* has been created."});
  await performAction('caseNumber', 'case number');

}

async function searchCase(caseNumber: string) {
  await performAction('select', 'Jurisdiction', caseData.jurisdiction),
    await performAction('select', 'Case type', caseData.caseType),
    await performAction('fill', 'Case Number', caseNumber),
    await performAction('click', 'Apply')
}

test.describe('Search case by case number @Master @nightly', () => {
  test('Search for case via caselist', async ({}) => {
    await createCaseWithAddress();
    let caseNumber = await getSessionVariable<string>('caseNumber');
    if (!caseNumber) {
      throw new Error('Case number not found in session variable');
    } else {
      await performAction('click', 'Case list');
      await searchCase(caseNumber);
      await performValidation('text', 'caseNumber', {content: caseNumber});
    }
  });
  test('Search for case via find case', async ({}) => {
    let caseNumber = await getSessionVariable<string>('caseNumber');
    if (!caseNumber) {
      throw new Error('Case number not found in session variable');
    }
    await performAction('click', 'Find case');
    await searchCase(caseNumber);
    await performValidation('text', 'caseNumber', {content: caseNumber});
  });
});
