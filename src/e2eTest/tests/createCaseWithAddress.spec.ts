import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performActions,
  performValidation
} from '@utils/controller';
import {caseData} from "@data/case.data";


test.beforeEach(async ({ page }) => {

  initializeExecutor(page);
  await parentSuite('Create Case');
  await performAction('login', 'caseworker');
});

test.describe('Create Case with Address @Master @nightly', async () => {

  test('should create a case successfully with Dropdown Address Flow', async () => {
    await performAction('click','Create case');

    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseData.jurisdiction]
      ,['select', 'Case type', caseData.caseType]
      ,['select', 'Event', caseData.event]);

    await performAction('click','Start');

    await performActions('Find Address based on postcode'
      ,['fill', 'Enter a UK postcode', caseData.postcode]
      ,['click', 'Find address']
      ,['select', 'Select an address', caseData.addressIndex]
      ,['fill', 'Address Line 2', caseData.addressLine2]
      ,['fill', 'Address Line 3', caseData.addressLine3]
      ,['fill', 'County', caseData.county]);

    await performAction('click', 'Continue');

    await performAction('fill', "Applicant's forename", caseData.applicantFirstName);

    await performAction('click', 'Submit');

    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    }
  );

  test('should create a case successfully with Manual Address Flow', async () => {
    await performAction('click', 'Create case');

    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseData.jurisdiction]
      ,['select', 'Case type', caseData.caseType]
      ,['select', 'Event', caseData.event]);

    await performAction('click', 'Start');

    await performActions('Enter Address Manually'
      ,['click', "I can't enter a UK postcode"]
      ,['fill', 'Building and Street', caseData.manualBuildingAndStreet]
      ,['fill', 'Address Line 2', caseData.addressLine2]
      ,['fill', 'Address Line 3', caseData.addressLine3]
      ,['fill', 'Town or City', caseData.manualTownOrCity]
      ,['fill', 'County', caseData.county]
      ,['fill', 'Postcode/Zipcode', caseData.manualPostcode]
      ,['fill', 'Country', caseData.manualCountry]);

    await performAction('click','Continue');

    await performAction('fill', "Applicant's forename", caseData.applicantFirstName);

    await performAction('click', 'Submit');

    await performValidation("bannerAlert", {message: "Case #.* has been created."});

  });

  test.afterEach(async () => {
    await performAction('click', 'Sign out');
    //await performAction('logout');//one parameter
  });
});
