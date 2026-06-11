import { test } from '@utils/test-fixtures';
import { expect, Page, BrowserContext } from '@playwright/test';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { globalSearch} from '@data/page-data-figma';
import { dismissCookieBanner } from '@config/cookie-banner';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { user} from '@data/page-data';

test.use({ storageState: undefined });

const setupGlobalSearchUser = async (
  page: Page,
  context: BrowserContext,
  loggedInUser: typeof user.ctscAdministrator
) => {
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
  await performAction('login', loggedInUser);

  if (loggedInUser.email === user.judge.email) {
    await performAction('handleJudgeBookingPage');
  }

  await dismissCookieBanner(page, 'analytics');
};

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

const runFieldSearch = async (label: string, value: string) => {
  await performAction('accessingTheSearch');
  await performAction('inputText', label, value);
  await performAction('select', globalSearch.servicesLabel, globalSearch.servicesDropdownOption2);
  await performAction('executeSearch');
  await performAction('validateResultsWithRetry');
};

const runGlobalSearchScenarios = () => {
  test('Global search menu @smoke', async () => {
    await performAction('accessingTheSearch');
    await performValidation('mainHeader', globalSearch.mainHeader);
  });

  test('Valid case reference using Mortgage and Landlord Possession Claim Service', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER, globalSearch.servicesDropdownOption2);
    await performAction('validateResults');
  });

  test('Valid case reference using All Services', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER, globalSearch.servicesDropdownOption1);
    await performAction('validateResults');
  });

  test('Search by postcode', async () => {
    await runFieldSearch(globalSearch.postCodeLabel, globalSearch.postcodeInputText);
  });

  test('Search by email address', async () => {
    await runFieldSearch(globalSearch.emailAddressLabel, globalSearch.emailAddressInputText);
  });

  test('first line of address', async () => {
    await runFieldSearch(globalSearch.firstLineOfAddressLabel, globalSearch.firstLineOfAddressInputText);
  });

  test('Search by party name', async () => {
    await runFieldSearch(globalSearch.nameLabel, globalSearch.nameInputText);
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
};

[
  { roleName: 'CTSC User', account: user.ctscAdministrator },
  { roleName: 'Judge User', account: user.judge }
].forEach(({ roleName, account }) => {
  test.describe(`[Global Search - ${roleName} - @nightly @CC @caseFlags]`, () => {
    test.beforeEach(async ({ page, context }) => {
      await setupGlobalSearchUser(page, context, account);
    });

    runGlobalSearchScenarios();
  });
});
