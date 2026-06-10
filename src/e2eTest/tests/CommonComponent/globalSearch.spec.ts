import { test } from '@utils/test-fixtures';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { globalSearch} from '@data/page-data-figma';
import { dismissCookieBanner } from '@config/cookie-banner';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { user} from '@data/page-data';

test.use({ storageState: undefined });

const setupGlobalSearchUser = async (
  page: any,
  context: any,
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

const runGlobalSearchScenarios = () => {
  test('Global search menu @smoke', async () => {
    await performAction('accessingTheSearch');
    await performValidation('mainHeader', globalSearch.mainHeader);
  });

  test('Valid case reference using Mortage and Landlord Possession Claim Service', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER);
    await performAction('validateResults');
  });

  test('Valid case reference using All Services', async () => {
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
};

[
  { roleName: 'CTSC User', account: user.ctscAdministrator },
  { roleName: 'Judge', account: user.judge }
].forEach(({ roleName, account }) => {
  test.describe(`[Global Search - ${roleName} - @globalSearch @CC @nightly]`, () => {
    test.beforeEach(async ({ page, context }) => {
      await setupGlobalSearchUser(page, context, account);
    });

    runGlobalSearchScenarios();
  });
});
