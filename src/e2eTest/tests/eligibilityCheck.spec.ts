import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performActions, performValidation} from '@utils/controller';
import {borderPostcode} from "@data/page-data/borderPostcode.page.data";
import configData from "@config/test.config";
import {createCase} from "@data/page-data/createCase.page.data";
import {addressDetails} from "@data/page-data/addressDetails.page.data";
import {borderPostcode} from '@data/page-data/borderPostcode.page.data';
import configData from '@config/test.config';
import {createCase} from '@data/page-data/createCase.page.data';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import { applicantDetails } from '@data/page-data/applicantDetails.page.data';

test.beforeEach(async ({page}) => {
    initializeExecutor(page);
    await parentSuite('Eligibility Check');
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

async function selectCountryRadioBtn(country: string) {
  await performAction('clickRadioButton', country);
  await performAction('clickButton', borderPostcode.continue);
}

test.describe.skip('[Eligibility Checks for Cross border postcodes] @nightly', async () => {
  //Skipping these tests until the postcode data insertion is handled in AAT via automation
  test('Verify cross border postcode eligibility check redirection and content for England and Wales', async ({page}) => {
      await inputAddressDetails(borderPostcode.englandWalesPostcode)
      await performValidation('text', {
        'text': borderPostcode.mainHeader,
        'elementType': 'heading'
      });
      await selectCountryRadioBtn(borderPostcode.countryOptions.england);
      await performValidation('text', {
        'text': applicantDetails.header,
        'elementType': 'heading'
      });
      await page.goBack()
      await page.waitForLoadState()
      await selectCountryRadioBtn(borderPostcode.countryOptions.wales);
      await performValidation('text', {
        'text': borderPostcode.mainHeader,
        'elementType': 'heading'
      });
    }
  );


    test('verify cross border postcode page redirection for england and scotland', async () => {
            await inputAddressDetails(borderPostcode.enlandScotlandPostcode)
            await performValidation('text', {
                "text": borderPostcode.borderPostcodeHeader,
                "elementType": "heading"
            });
        }
    );

    test('verify cross border postcode page for england and scotland content', async () => {
        await inputAddressDetails(borderPostcode.enlandScotlandPostcode)
        await performValidation('text', {
            "text": borderPostcode.borderPostcodeHeader,
            "elementType": "heading"
        });
        await performValidation('text', {
            "text": borderPostcode.englandScotlandParagraphContent,
            "elementType": "paragraph"
        });
        await performValidation('text', {
            "text": borderPostcode.englandScotlandInlineContent,
            "elementType": "inlineText"
        });
        await performValidation('text', {"text": borderPostcode.continueBtn, "elementType": "button"})
        await performValidation('text', {"text": borderPostcode.cancelLink, "elementType": "link"})
    });
})

