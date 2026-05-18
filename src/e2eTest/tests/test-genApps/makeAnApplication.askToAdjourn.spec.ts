import { createCaseApiData, submitCaseApiData } from '../../data/api-data';


import { initializeExecutor } from '../../utils/controller';
import test, { expect } from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { initializeGenAppsExecutor, performAction, performValidation } from '@utils/controller-genApps';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';

const home_url = process.env.TEST_URL;

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeGenAppsExecutor(page);
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('getCaseAPI');
  await performAction('linkSolicitorAPI');
   await performAction('reloginAndFindTheCase', user.defendantSolicitor);
   await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    // Login and cookie consent are handled globally via storageState in global-setup.config.ts
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
});

test.afterEach(async () => {

});

test.describe('Make an Application - e2e Journey @nightly', async () => {
  test('Select an Application - Ask to Adjourn journey - Court hearing in 14 days[Yes] @regression @smoke', async () => {
    console.log('testing');
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
  });
    
});