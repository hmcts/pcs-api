import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {borderPostcode} from '@data/page-data/borderPostcode.page.data';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {canNotUseOnlineService} from '@data/page-data/canNotUseOnlineService.page.data';
import {propertyIneligible} from '@data/page-data/propertyIneligible.page.data';
import {userIneligible} from '@data/page-data/userIneligible.page.data';
import {provideMoreDetailsOfClaim} from '@data/page-data/provideMoreDetailsOfClaim.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {claimType} from '@data/page-data/claimType.page.data';
import {user} from '@data/user-data/permanent.user.data';
import {home} from '@data/page-data/home.page.data';

test.beforeEach(async ({page}, testInfo) => {
  initializeExecutor(page);
  await parentSuite('Eligibility Check');
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  // await performAction('login', user.claimantSolicitor);
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.afterEach(async ({ page }, testInfo) => {
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
});

test.describe.skip('[Eligibility checks for cross and non cross border postcodes] @Master @nightly', async () => {
  test('My failed test', async ({page}) => {
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
  });
  test.skip('Cross border - Verify postcode eligibility check redirection and content for England and Wales', async ({page}) => {
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
    await performAction('selectBorderPostcode', borderPostcode.countryOptions.england);
    await performValidation('bannerAlert', 'Case #.* has been created.');
  });

  test.skip('Cross border - Verify postcode page for England and Scotland content', async () => {
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

  test.skip('Cross border England - Verify postcode eligibility check', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
  });

  test.skip('Cross border England - Verify postcode not assigned to court - Can not use this service page', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandWalesNoCourtCrossBorderPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectBorderPostcode', borderPostcode.countryOptions.england);
    await performValidation('mainHeader', propertyIneligible.mainHeader);
  });

  test.skip('Wales - Verify non cross border postcode eligibility check for Wales', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
  });

  test.skip('England - Verify postcode not assigned to court - Can not use this service page', async () => {
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

  test.skip('England - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    await performAction('selectClaimantType', claimantType.mortgageLender);
    await performAction('clickButton', userIneligible.continue);
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performAction('clickButton', userIneligible.cancel);
  });

  test.skip('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    await performAction('selectClaimantType', claimantType.privateLandlord);
    await performAction('clickButton', userIneligible.continue);
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performAction('clickButton', userIneligible.cancel);
  });

  test.skip('England - Unsuccessful case creation journey due to claim type not in scope of Release1 @R1only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.yes);
    await performAction('clickButton', userIneligible.continue);
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performAction('clickButton', userIneligible.cancel);
  });
})

