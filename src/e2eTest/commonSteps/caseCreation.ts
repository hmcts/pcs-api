import {Page} from "@playwright/test";
import * as actions from "@helpers/actions.helper";
import {caseData} from "@data/case.data";

export async function startCaseCreation(page: Page) {
  await actions.clickLink(page, 'Create case');
  await actions.selectDropdown(page, 'Jurisdiction', caseData.jurisdiction);
  await actions.selectDropdown(page, 'Case type', caseData.caseType);
  await actions.selectDropdown(page, 'Event', caseData.event);
  await actions.clickButton(page, 'Start');
}

export async function selectAddress(page: Page, useManualEntry: boolean): Promise<void> {
  if (useManualEntry) {
    // Manual address entry
    await actions.clickLinkByText(page, "I can't enter a UK postcode", 'a.manual-link');

    await actions.fillInput(page, 'Building and Street', caseData.manualBuildingAndStreet);
    await actions.fillInput(page, 'Address Line 2', caseData.addressLine2);
    await actions.fillInput(page, 'Address Line 3', caseData.addressLine3);
    await actions.fillInput(page, 'Town or City', caseData.manualTownOrCity);
    await actions.fillInput(page, 'County', caseData.county);
    await actions.fillInput(page, 'Postcode/Zipcode', caseData.manualPostcode);
    await actions.fillInput(page, 'Country', caseData.manualCountry);
  } else {
    // UK postcode search + dropdown selection
    await actions.fillInput(page, 'Enter a UK postcode', caseData.postcode);
    await actions.clickButton(page, 'Find address');
    await actions.selectDropdown(page, 'Select an address', caseData.addressIndex);

    await actions.fillInput(page, 'Address Line 2', caseData.addressLine2);
    await actions.fillInput(page, 'Address Line 3', caseData.addressLine3);
    await actions.fillInput(page, 'County', caseData.county);
  }

  await actions.clickButton(page, 'Continue');
}

export async function enterApplicantDetails(page: Page) {
  await actions.fillInput(page, "Applicant's first name", caseData.applicantFirstName);
}

export async function submitCaseCreation(page: Page) {

  await actions.clickButton(page, 'Submit');
  await actions.expectAlertTextMatches(page, /^Case #\d{4}-\d{4}-\d{4}-\d{4} has been created\.$/
  );
}


