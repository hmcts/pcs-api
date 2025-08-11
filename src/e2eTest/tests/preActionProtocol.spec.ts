import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performActions, performValidation} from '@utils/controller';
import configData from '@config/test.config';
import {createCase} from '@data/page-data/createCase.page.data';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {applicantDetails} from '@data/page-data/applicantDetails.page.data';
import {caseData} from "@data/case.data";
import {groundsForPossession} from '@data/page-data/groundsForPossession.page.data';
import {preActionProtocol} from '@data/page-data/preActionProtocol.page.data';
import {mediationAndSettlement} from '@data/page-data/mediationAndSettlement.page.data';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await parentSuite('Pre Action Protocol');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker']);
  await performAction('clickButton', 'Create case');
  await selectJurisdictionCaseTypeEvent()
});

async function selectJurisdictionCaseTypeEvent() {
  await performActions('Case option selection'
    , ['select', 'Jurisdiction', createCase.caseOption.jurisdiction.possessions]
    , ['select', 'Case type', createCase.caseOption.caseType.civilPossessions]
    , ['select', 'Event', createCase.caseOption.event.makeAPossessionClaim]);
  await performAction('clickButton', 'Start');
}

async function inputAddressDetails(postcode: string) {
  await performActions('Enter Address Manually'
    , ['inputText', 'Enter a UK postcode', postcode]
    , ['clickButton', 'Find address']
    , ['select', 'Select an address', addressDetails.propertyAddressSection.addressIndex]
    , ['inputText', 'Address Line 2', addressDetails.propertyAddressSection.addressLine2]
    , ['inputText', 'Address Line 3', addressDetails.propertyAddressSection.addressLine3]
    , ['inputText', 'County', addressDetails.propertyAddressSection.englandCounty]);
  await performAction('clickButton', 'Continue');
}
test.describe('[Pre Action Protocol page] @nightly', () => {
  test('Verify that the Pre-action Protocol page displays required pre-requisite information and user navigates to mediation and settlement page', async ({page}) => {
    await inputAddressDetails(caseData.postcode);
    await performAction('inputText', "Applicant's forename", applicantDetails.applicantFirstName);
    await performAction('clickButton', 'Continue');
    await performValidation('text', {
      text: groundsForPossession.mainHeader,
      elementType: 'heading'
    });

    await performAction('clickRadioButton', groundsForPossession.groundsForPossessionsOptions.yes);
    await performAction('clickButton', 'Continue');
    await performValidation('text', {
      text: preActionProtocol.mainHeader,
      elementType: 'heading'
    });
    await page.waitForLoadState()
    await performAction('clickRadioButton', preActionProtocol.preActionProtocolOptions.yes);
    await performAction('clickButton', 'Continue');
    await performValidation('text', {
      text: mediationAndSettlement.mainHeader,
      elementType: 'heading'
    });
  });
});
