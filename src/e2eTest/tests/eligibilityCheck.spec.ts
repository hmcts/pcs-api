import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import { initializeExecutor, performAction, performValidation } from '@utils/controller';
import { borderPostcode } from '@data/page-data/borderPostcode.page.data';
import configData from '@config/test.config';
import { addressDetails } from '@data/page-data/addressDetails.page.data';
import { canNotUseOnlineService } from '@data/page-data/canNotUseOnlineService.page.data';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await parentSuite('Eligibility Check');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('createUserAndLogin', 'claimant', ['caseworker-pcs', 'caseworker']);
  await performAction('clickButton', 'Create case');
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.describe('Eligibility checks for cross and non cross border postcodes @Master @nightly', async () => {
  test('Verify cross border postcode eligibility check redirection and content for England and Wales', async ({page}) => {
    await performAction('selectAddress', {
      postcode: borderPostcode.englandWalesPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', borderPostcode.mainHeader);
    await performValidation('text', {
      "text": borderPostcode.englandWalesParagraphContent,
      "elementType": "paragraph"
    });
    await performValidation('text', {
      "text": borderPostcode.englandWalesInlineContent,
      "elementType": "inlineText"
    });
    await performAction('selectCountryRadioButton', borderPostcode.countryOptions.england);
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await page.goBack();
    await page.waitForLoadState();
    await page.goBack();
    await page.waitForLoadState();
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
    await performValidation('text', {"text": borderPostcode.submit, "elementType": "button"})
    await performValidation('text', {"text": borderPostcode.cancel, "elementType": "link"})
  });

  test('Verify non cross border postcode eligibility check for England', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
  });

  test('Verify non cross border postcode eligibility check for Wales', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
  });

  test('Verify postcode not assigned to court - Can not use this service page - All countries', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandNoCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', canNotUseOnlineService.mainHeader);
    await performValidation('text', {"text": canNotUseOnlineService.basedOnPostcodeContent, "elementType": "paragraph"})
    await performValidation('text', {"text": canNotUseOnlineService.PCOLLink, "elementType": "link"})
    await performValidation('text', {"text": canNotUseOnlineService.claimsInWalesContent, "elementType": "listItem"})
    await performValidation('text', {"text": canNotUseOnlineService.claimsInEnglandContent, "elementType": "listItem"})
    await performValidation('text', {"text": canNotUseOnlineService.claimsInScotlandLink, "elementType": "link"})
    await performValidation('text', {"text": canNotUseOnlineService.claimsInNorthernIrelandLink, "elementType": "link"})
    await performValidation('text', {
      "text": canNotUseOnlineService.propertyPossessionsFullListLink,
      "elementType": "link"
    })
  });
  //This test case is failing currently due to scenario leading to different page, so test need to be corrected as
  // part of house keeping task
  test.skip('Verify postcode not assigned to court - Can not use this service page - cross border England', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandWalesNoCourtCrossBorderPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectCountryRadioButton', borderPostcode.countryOptions.england);
    await performValidation('mainHeader', canNotUseOnlineService.mainHeader);
    await performValidation('text', {"text": canNotUseOnlineService.basedOnPostcodeContent, "elementType": "paragraph"})
    await performValidation('text', {"text": canNotUseOnlineService.PCOLLink, "elementType": "link"})
    await performValidation('text', {"text": canNotUseOnlineService.forOtherTypesOfClaims, "elementType": "listItem"})
    await performValidation('text', {
      "text": canNotUseOnlineService.propertyPossessionsFullListLink,
      "elementType": "link"
    })
  });
})

