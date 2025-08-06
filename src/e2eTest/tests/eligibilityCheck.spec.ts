import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performActions, performValidation} from '@utils/controller';
import {borderPostcode} from "@data/page-data/borderPostcode.page.data";
import configData from "@config/test.config";
import {caseOptions} from "@data/page-data/createCase.page.data";
import {addressDetails} from "@data/page-data/addressDetails.page.data";
import {applicantDetails} from "@data/page-data/ApplicantDetails.page.data";

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await parentSuite('Eligibility Check');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('login', 'exuiUser');
  await performAction('clickButton', 'Create case');
  await selectJurisdictionCaseTypeEvent()
});

async function selectJurisdictionCaseTypeEvent() {
  await performActions('Case option selection'
    , ['select', 'Jurisdiction', caseOptions.jurisdiction.posessions]
    , ['select', 'Case type', caseOptions.caseType.civilPosessions]
    , ['select', 'Event', caseOptions.event.makeAPosessionClaim]);
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
  await performAction('clickButton', borderPostcode.continueBtn);
}

test.describe('[Eligibility Checks for Cross border postcodes]  @Master @nightly', async () => {
  test('verify cross border postcode eligibility check redirection and content for england&wales countries, ', async ({page}) => {
      await inputAddressDetails(borderPostcode.englandWalesPostcode)
      await performValidation('text', {
        "text": borderPostcode.borderPostcodeHeader,
        "elementType": "heading"
      });
      await selectCountryRadioBtn(borderPostcode.countryOptions.england);
      await performValidation('text', {
        "text": applicantDetails.header,
        "elementType": "heading"
      });
      await page.goBack()
      await page.waitForLoadState()
      await selectCountryRadioBtn(borderPostcode.countryOptions.wales);
      await performValidation('text', {
        "text": borderPostcode.borderPostcodeHeader,
        "elementType": "heading"
      });
    }
  );

  test('verify content for cross border postcode page when postcode is in England&Wales', async () => {
      await inputAddressDetails(borderPostcode.englandWalesPostcode)
      await performValidation('text', {
        "text": borderPostcode.borderPostcodeHeader,
        "elementType": "heading"
      });
      await performValidation('text', {
        "text": borderPostcode.englandWalesParagraphContent,
        "elementType": "paragraph"
      });
      await performValidation('text', {
        "text": borderPostcode.englandWalesInlineContent,
        "elementType": "inlineText"
      });
      await performValidation('text', {"text": borderPostcode.continueBtn, "elementType": "button"})
      await performValidation('text', {"text": borderPostcode.cancelLink, "elementType": "link"})
    }
  );


  test('verify cross border postcode eligibility check redirection and content for england&scotland countries', async ({page}) => {
      await inputAddressDetails(borderPostcode.englandScotlandPostcode)
      await performValidation('text', {
        "text": borderPostcode.borderPostcodeHeader,
        "elementType": "heading"
      });
      await selectCountryRadioBtn(borderPostcode.countryOptions.england);
      await performValidation('text', {
        "text": applicantDetails.header,
        "elementType": "heading"
      });
      await page.goBack()
      await page.waitForLoadState()
      await selectCountryRadioBtn(borderPostcode.countryOptions.scotland);
      await performValidation('text', {
        "text": borderPostcode.borderPostcodeHeader,
        "elementType": "heading"
      });
    }
  );

  test('verify content for cross border postcode page when postcode is in England&Scotland', async () => {
    await inputAddressDetails(borderPostcode.englandScotlandPostcode)
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

