import { test } from '@utils/test-fixtures';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { globalSearch} from '@data/page-data-figma';
import { dismissCookieBanner } from '@config/cookie-banner';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { user} from '@data/page-data';

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
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Global Search - @globalSearch @PR @CC @nightly]', () => {
  test('Global search menu @smoke', async () => {
    await performAction('accessingTheSearch');
    await performValidation('mainHeader', globalSearch.mainHeader);
  });

  test('Valid Case Reference with Mortage and Landlord Possession Claim Service', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER);
    await performAction('validateResults');
  });

  test('Valid Case Reference with All Service', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER);
    await performAction('validateResults');
  });
  
  test('Invalid case reference', async () => {
    await performAction('accessingTheSearch');
    await performAction('invalidCaseReferenceSearch', globalSearch.invalidCaseReferenceInputText);
  });

   test('Change search criteria link', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER);
    await performAction('changeSearchLink', 'changeSearch');
  });
});
