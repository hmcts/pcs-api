// src/e2eTest/tests/searchCase.spec.ts
import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import { getSessionVariable } from '../utils/controller';
import {
  initializeExecutor,
  performAction,
  performActions,
  performValidation
} from '@utils/controller';
import { caseData } from '@data/case.data';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await parentSuite('Create Case');
  await performAction('login', 'exuiUser');
});

test.afterEach(async () => {
  await performAction('click', 'Sign out');
});

async function createCaseWithAddress(){

  await performAction('click', 'Create case');

  await performActions(
    'Case option selection',
    ['select', 'Jurisdiction', caseData.jurisdiction],
    ['select', 'Case type', caseData.caseType],
    ['select', 'Event', caseData.event]
  );

  await performAction('click', 'Start');

  await performActions(
    'Find Address based on postcode',
    ['fill', 'Enter a UK postcode', caseData.postcode],
    ['click', 'Find address'],
    ['select', 'Select an address', caseData.addressIndex],
    ['fill', 'Address Line 2', caseData.addressLine2],
    ['fill', 'Address Line 3', caseData.addressLine3],
    ['fill', 'County', caseData.county]
  );

  await performAction('click', 'Continue');
  await performAction('fill', "Applicant's forename", caseData.applicantFirstName);
  await performAction('click', 'Submit');

  await performValidation('bannerAlert', {message: 'Case #.* has been created.'});
}
async function searchCase(caseNumber: string) {
  await performActions(
    'Search for case',
    ['select', 'Jurisdiction', caseData.jurisdiction],
    ['select', 'Case type', caseData.caseType],
    ['fill', 'case number', caseNumber],
    ['click', 'Apply']
  );
}

test.describe('Search case by case number @Master @nightly', () => {
  test('Search for case via caselist', async ({}) => {
    await createCaseWithAddress();
    let caseNumber = await getSessionVariable<string>('caseNumber');
    if (!caseNumber) {
      throw new Error('Case number not found in session variable');
    } else {
      searchCase(caseNumber);
      await performValidation('Search Case', 'case number', {caseNumber});
    }
  });
  test('Search for case via find case', async ({}) => {
    await createCaseWithAddress();
    let caseNumber = await getSessionVariable<string>('caseNumber');
    if (!caseNumber) {
      throw new Error('Case number not found in session variable');
    }
    await performAction('click', 'Find case');
    await searchCase(caseNumber);
    await performValidation('Search Case', 'case number', {caseNumber});
  });
});
