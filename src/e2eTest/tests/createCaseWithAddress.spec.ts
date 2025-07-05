
import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import ConfigData from "@data/config.data";
import {caseData} from "@data/case.data";
import {assertAlertMessageMatches, initAssertionHelper, loginHelper} from '@helpers/index';
import { deleteAccount }from 'helpers/idam-helpers/idam.helper';
import {initActionHelper, performAction,setStepFunction} from "helpers";
import { attachTestMetadata } from '@helpers/testMetaData.helper';


let email: string;

test.beforeEach(async ({ page }) => {
  initActionHelper(page);
  initAssertionHelper(page);
  setStepFunction(test.step.bind(test));
  await parentSuite('Create Case');
  await page.goto(ConfigData.manageCasesBaseURL);
  // @ts-ignore
  email = await loginHelper.login(page);
});

test.describe('Create Case with Address @Master @nightly', async () => {

  test('should create a case successfully with Dropdown Address Flow', async ({page},testInfo) => {
    await attachTestMetadata(testInfo, page, email);

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

    await assertAlertMessageMatches(/^Case #\d{4}-\d{4}-\d{4}-\d{4} has been created\.$/);
    }
  );
  test('should create a case successfully with Manual Address Flow', async ({page},testInfo) => {

    await attachTestMetadata(testInfo, page, email);

    await performAction('click','Create case');
    await performAction('select','Jurisdiction', caseData.jurisdiction);
    await performAction('select', 'Case type', caseData.caseType);
    await performAction('select', 'Event', caseData.event);
    await performAction('click','Start');

    // Manual address entry
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

    await assertAlertMessageMatches(/^Case #\d{4}-\d{4}-\d{4}-\d{4} has been created\.$/);

  });

  test.afterEach(async () => {
    if (email) {
      try {
        await deleteAccount(email);
      } catch (err) {
        console.warn(`Teardown failed for user ${email}:`, err);
      }
    }
  });

});
