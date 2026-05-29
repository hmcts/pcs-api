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


// test.beforeEach(async ({ page }) => {
//   initializeExecutor(page);
//   await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
//   await performAction('login', user.hearingCenterAdmin);
// });

test.describe('[Global Search - Mortgage and Landlord Possessions @regression @globalSearch @PR @CC]', () => {
  test('Should find a case by case reference', async () => {
    await performAction('accessingTheSearch');
    await performValidation('mainHeader', globalSearch.mainHeader);
    await performAction('searchByCaseReference', process.env.CASE_NUMBER!);
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find a case by name', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByName', globalSearch.searchPartyNameInputText);
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find a case by first line of address', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByFirstLineOfAddress', globalSearch.firstLineOfAddressInputText);
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find a case by postcode', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByPostcode', globalSearch.searchPostcodeInputText);
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find a case by email address', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByEmailAddress', globalSearch.searchEmailAddressInputText);
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find a case by date of birth', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByDateOfBirth', {
      day: globalSearch.searchDateOfBirthDayInputText,
      month: globalSearch.searchDateOfBirthMonthInputText,
      year: globalSearch.searchDateOfBirthYearInputText
    });
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find cases by service', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByService', globalSearch.servicesDropdownOption2);
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should navigate to the Case Summary page when clicking the case number link', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', process.env.CASE_NUMBER!);
    await performValidation('link', { text: process.env.CASE_NUMBER! });
    await performAction('clickCaseNumberLink');
  });

  test('Should find a case by name and postcode combination', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByNameAndPostcode', {
      name: globalSearch.searchPartyNameInputText,
      postcode: globalSearch.searchPostcodeInputText
    });
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find a case by address and postcode combination', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByAddressAndPostcode', {
      address: globalSearch.firstLineOfAddressInputText,
      postcode: globalSearch.searchPostcodeInputText
    });
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should find a case by name and service combination', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByNameAndService', {
      name: globalSearch.searchPartyNameInputText,
      service: globalSearch.servicesDropdownOption2
    });
    await performValidation('link', { text: process.env.CASE_NUMBER! });
  });

  test('Should display no results found text for an invalid case reference number', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByCaseReference', globalSearch.invalidCaseReferenceInputText);
    await performValidation('text', { text: globalSearch.noResultsFoundText, elementType: 'paragraph' });
  });

  test('Should display no results when a valid name is combined with an invalid postcode', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByInvalidCombination', {
      name: globalSearch.searchPartyNameInputText,
      postcode: globalSearch.invalidPostcodeInputText
    });
    await performValidation('text', { text: globalSearch.noResultsFoundText, elementType: 'paragraph' });
  });

  test('Should display no results for an invalid name @regression @globalSearch', async () => {
    await performAction('accessingTheSearch');
    await performAction('searchByInvalidCombination', {
      name: globalSearch.invalidNameInputText
    });
    await performValidation('text', { text: globalSearch.noResultsFoundText, elementType: 'paragraph' });
  });
});
