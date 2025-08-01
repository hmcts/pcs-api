import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performActions,
  performValidation
} from '@utils/controller';
import {caseData} from "@data/case.data";
import configData from "@config/test.config";


test.beforeEach(async ({ page }, testInfo) => {

  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl',configData.manageCasesBaseURL);
  await performAction('login', 'exuiUser');

  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
});

test.describe.skip('[Create Case Flow With Address]  @Master @nightly', async () => {

  test('Dropdown Address Selection Flow - should create case sucessfully', async () => {
    await performAction('click','Create case');
    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseData.jurisdiction]
      ,['select', 'Case type', caseData.caseType]
      ,['select', 'Event', caseData.event]);
    await performAction('click','Start');
    await performActions('Find Address based on postcode'
      ,['inputText', 'Enter a UK postcode', caseData.postcode]
      ,['click', 'Find address']
      ,['select', 'Select an address', caseData.addressIndex]
      ,['inputText', 'Address Line 2', caseData.addressLine2]
      ,['inputText', 'Address Line 3', caseData.addressLine3]
      ,['inputText', 'County', caseData.county]);
    await performAction('click', 'Continue');
    await performAction('inputText', "Applicant's forename", caseData.applicantFirstName);
    await performAction('click', 'Submit');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performValidation('formLabelValue', "Applicant's forename", {value:'AutomationTestUser'});
    await performAction('clickTab', 'Property Details');
    await performValidation('formLabelValue', 'Building and Street');
    await performValidation('formLabelValue', 'Address Line 2');
    await performValidation('formLabelValue', 'Town or City');
    await performValidation('formLabelValue', 'Postcode/Zipcode');
    await performValidation('formLabelValue', 'Country');
    }
  );

  test('Manual Address Input Flow - should create case sucessfully', async () => {
    await performAction('click', 'Create case');
    await performActions('Case option selection'
      ,['select', 'Jurisdiction', caseData.jurisdiction]
      ,['select', 'Case type', caseData.caseType]
      ,['select', 'Event', caseData.event]);
    await performAction('click', 'Start');
    await performActions('Enter Address Manually'
      ,['click', "I can't enter a UK postcode"]
      ,['inputText', 'Building and Street', caseData.manualBuildingAndStreet]
      ,['inputText', 'Address Line 2', caseData.addressLine2]
      ,['inputText', 'Address Line 3', caseData.addressLine3]
      ,['inputText', 'Town or City', caseData.manualTownOrCity]
      ,['inputText', 'County', caseData.county]
      ,['inputText', 'Postcode/Zipcode', caseData.manualPostcode]
      ,['inputText', 'Country', caseData.manualCountry]);
    await performAction('click','Continue');
    await performAction('inputText', "Applicant's forename", caseData.applicantFirstName);
    await performAction('click', 'Submit');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performValidation('formLabelValue', "Applicant's forename", {value:'AutomationTestUser'});
    await performAction('clickTab', 'Property Details');
    await performValidation('formLabelValue', 'Building and Street', {value:caseData.manualBuildingAndStreet});
    await performValidation('formLabelValue', 'Address Line 2', {value:caseData.addressLine2});
    await performValidation('formLabelValue', 'Town or City', {value:caseData.manualTownOrCity});
    await performValidation('formLabelValue', 'Postcode/Zipcode', {value:caseData.manualPostcode});
    await performValidation('formLabelValue', 'Country', {value:caseData.manualCountry});
  });
});
