import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {borderPostcode} from '@data/page-data/borderPostcode.page.data';
import configData from '@config/test.config';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {legislativeCountry} from '@data/page-data/legislativeCountry.page.data';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await parentSuite('Eligibility Check');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker']);
  await performAction('clickButton', 'Create case');
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.describe.skip('Eligibility checks for cross and non cross border postcodes @nightly', async () => {
  //Skipping these tests until the postcode data insertion is handled in AAT via automation
  test('Verify cross border postcode eligibility check redirection and content for England and Wales', async ({page}) => {
    await performAction('selectAddress', {
      postcode: borderPostcode.englandWalesPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', borderPostcode.mainHeader);
    await performAction('selectCountryRadioButton', borderPostcode.countryOptions.england);
    await performValidation('mainHeader', legislativeCountry.mainHeader);
    await page.goBack()
    await page.waitForLoadState()
    await performAction('selectCountryRadioButton', borderPostcode.countryOptions.wales);
    await performValidation('mainHeader', borderPostcode.mainHeader);
  });

  test('Verify cross border postcode page for England and Scotland content', async () => {
    await performAction('selectAddress', {
      postcode: borderPostcode.englandScotlandPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', borderPostcode.mainHeader);
    await performValidation('text', {
      "text": borderPostcode.englandScotlandParagraphContent,
      "elementType": "paragraph"
    });
    await performValidation('text', {
      "text": borderPostcode.englandScotlandInlineContent,
      "elementType": "inlineText"
    });
    await performValidation('text', {"text": borderPostcode.continue, "elementType": "button"})
    await performValidation('text', {"text": borderPostcode.cancel, "elementType": "link"})
  });

  test('Verify non cross border postcode eligibility check for England', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', legislativeCountry.mainHeader);
  });

  test('Verify non cross border postcode eligibility check for Wales', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', legislativeCountry.mainHeader);
  });
})
