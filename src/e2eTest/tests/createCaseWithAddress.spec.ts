import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import {caseData} from "../utils/data/case.data";
import {initializeExecutor, performAction, performValidation} from '../utils/test-executor';

test.beforeEach(async ({ page },testInfo) => {

  initializeExecutor(page);
  await parentSuite('Create Case');
  await performAction('login', 'newIdamUser');

});

test.describe('Create Case with Address @Master @nightly', async () => {

  test('should create a case successfully with Dropdown Address Flow', async () => {
    await performAction('click','Create case');
    await performAction('select','Jurisdiction', caseData.jurisdiction);
    await performAction('select', 'Case type', caseData.caseType);
    await performAction('select', 'Event', caseData.event);
    await performAction('click','Start');

    await performAction('fill','Enter a UK postcode', caseData.postcode);
    await performAction('click','Find address');
    await performAction('select','Select an address', caseData.addressIndex);
    await performAction('fill', 'Address Line 2', caseData.addressLine2);
    await performAction('fill', 'Address Line 3', caseData.addressLine3);
    await performAction('fill', 'County', caseData.county);
    await performAction('click', 'Continue');

    await performAction('fill', "Applicant's forename", caseData.applicantFirstName);

    await performAction('click', 'Submit');

    await performValidation("alert","alert-message",{pattern: "Case #.* has been created."});
    }
  );

  test('should create a case successfully with Manual Address Flow', async () => {
    await performAction('click','Create case');
    await performAction('select','Jurisdiction', caseData.jurisdiction);
    await performAction('select', 'Case type', caseData.caseType);
    await performAction('select', 'Event', caseData.event);
    await performAction('click','Start');

    await performAction('click', "I can't enter a UK postcode");
    await performAction('fill', 'Building and Street', caseData.manualBuildingAndStreet);
    await performAction('fill', 'Address Line 2', caseData.addressLine2);
    await performAction('fill', 'Address Line 3', caseData.addressLine3);
    await performAction('fill', 'Town or City', caseData.manualTownOrCity);
    await performAction('fill', 'County', caseData.county);
    await performAction('fill', 'Postcode/Zipcode', caseData.manualPostcode);
    await performAction('fill', 'Country', caseData.manualCountry);

    await performAction('click','Continue');

    await performAction('fill', "Applicant's forename", caseData.applicantFirstName);

    await performAction('click', 'Submit');

    await performValidation("alert","alert-message",{pattern: "Case #.* has been created."});

  });

  test.afterEach(async () => {

  });
});
