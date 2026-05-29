import { expect, test } from '@utils/test-fixtures';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { globalSearch } from '@data/page-data-figma';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { createCaseApiData } from '@data/api-data/createCase.api.data';
import { submitCaseApiData } from '@data/api-data/submitCase.api.data';

test.use({ storageState: { cookies: [], origins: [] } });

test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch (e) {
      // Ignore if storage is not accessible
    }
  });

  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.ctscAdministrator);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.describe('[Global Search - @globalSearch @PR @CC @nightly]', () => {
  test('Validate global search menu @smoke', async () => {
    await performAction('accessingTheSearch');
    await performValidation('mainHeader', globalSearch.mainHeader);
  });
  
  test('Validate global search functionality by valid case reference', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER!);
  });

test('Validate no results found for an invalid case reference number', async () => {
    await performAction('accessingTheSearch');
    await performAction('invalidCaseReferenceSearch', globalSearch.invalidCaseReferenceInputText);
  });
});