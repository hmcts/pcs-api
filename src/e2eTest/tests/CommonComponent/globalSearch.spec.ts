import { test } from '@utils/test-fixtures';
import { Page, BrowserContext } from '@playwright/test';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { globalSearch} from '@data/page-data-figma';
import { dismissCookieBanner } from '@config/cookie-banner';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { user} from '@data/page-data';

test.use({ storageState: undefined });

let globalSearchTestData = {
  name: '',
  addressLine1: '',
  postcode: '',
  email: ''
};

const setupGlobalSearchUser = async (
  page: Page,
  context: BrowserContext,
  loggedInUser: typeof user.ctscAdministrator
) => {
  await context.clearCookies();
  initializeExecutor(page);

  const suffix = Date.now();
  const { propertyAddress } = createCaseApiData.createCasePayload;
  const { submitCasePayload } = submitCaseApiData;

  globalSearchTestData = {
    name: `${submitCasePayload.claimantName} ${suffix}`,
    addressLine1: `${propertyAddress.AddressLine1} ${suffix}`,
    postcode: propertyAddress.PostCode,
    email: submitCasePayload.claimantContactEmail.replace('@', `+${suffix}@`)
  };

  await performAction('createCaseAPI', {
    data: {
      ...createCaseApiData.createCasePayload,
      propertyAddress: {
        ...propertyAddress,
        AddressLine1: globalSearchTestData.addressLine1
      }
    }
  });
  await performAction('submitCaseAPI', {
    data: {
      ...submitCasePayload,
      claimantName: globalSearchTestData.name,
      claimantContactEmail: globalSearchTestData.email,
      formattedClaimantContactAddress: `${globalSearchTestData.addressLine1}<br>${propertyAddress.PostTown}<br>${propertyAddress.PostCode}`
    }
  });
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
    await runFieldSearch(globalSearch.postCodeLabel, globalSearchTestData.postcode);
  });

  test('Search by email address', async () => {
    await runFieldSearch(globalSearch.emailAddressLabel, globalSearchTestData.email);
  });

  test('first line of address', async () => {
    await runFieldSearch(globalSearch.firstLineOfAddressLabel, globalSearchTestData.addressLine1);
  });

  test('Search by party name', async () => {
    await runFieldSearch(globalSearch.nameLabel, globalSearchTestData.name);
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
  test.describe(`[Common Component Global Search] - ${roleName} - @nightly @CC @caseFlags`, () => {
    test.beforeEach(async ({ page, context }) => {
      await setupGlobalSearchUser(page, context, account);
    });

    runGlobalSearchScenarios();
  });
});
