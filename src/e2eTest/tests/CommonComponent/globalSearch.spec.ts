import { test } from '@utils/test-fixtures';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { globalSearch } from '@data/page-data-figma';
import { user } from '@data/user-data';

test.use({ storageState: { cookies: [], origins: [] } });

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('login', user.hearingCenterAdmin);
});

test.describe('[Global Search - Hearing Center Admin]', () => {
  test('Should find a case by case reference @regression @globalSearch', async () => {
    await performAction('navigateToGlobalSearch');
    await performValidation('mainHeader', globalSearch.mainHeader);
    await performAction('searchByCaseReference', globalSearch.caseReferenceInputText);
  });

  test('Should find a case by name @regression @globalSearch', async () => {
    await performAction('navigateToGlobalSearch');
    await performAction('searchByName', globalSearch.searchPartyNameInputText);
  });

  test('Should find a case by first line of address @regression @globalSearch', async () => {
    await performAction('navigateToGlobalSearch');
    await performAction('searchByFirstLineOfAddress', globalSearch.firstLineOfAddressInputText);
  });

  test('Should find a case by postcode @regression @globalSearch', async () => {
    await performAction('navigateToGlobalSearch');
    await performAction('searchByPostcode', globalSearch.searchPostcodeInputText);
  });

  test('Should find a case by email address @regression @globalSearch', async () => {
    await performAction('navigateToGlobalSearch');
    await performAction('searchByEmailAddress', globalSearch.searchEmailAddressInputText);
  });

  test('Should find a case by date of birth @regression @globalSearch', async () => {
    await performAction('navigateToGlobalSearch');
    await performAction('searchByDateOfBirth', {
      day: globalSearch.searchDateOfBirthDayInputText,
      month: globalSearch.searchDateOfBirthMonthInputText,
      year: globalSearch.searchDateOfBirthYearInputText
    });
  });

  test('Should find cases by service @regression @globalSearch', async () => {
    await performAction('navigateToGlobalSearch');
    await performAction('searchByService', globalSearch.servicesDropdownOption2);
  });
});
