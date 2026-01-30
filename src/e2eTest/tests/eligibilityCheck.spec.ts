import { test } from '@playwright/test';
import {
  addressCheckYourAnswers,
  addressDetails,
  borderPostcode,
  canNotUseOnlineService,
  housingPossessionClaim,
  propertyIneligible,
  userIneligible,
  home
} from '@data/page-data';
import{
  claimantType,
  claimType,
} from '@data/page-data-figma';
import {
  initializeExecutor,
  performAction,
  performValidation
} from '@utils/controller';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
  PageContentValidation.finaliseTest();
});

test.describe('[Eligibility Check - Create Case]', async () => {
  test('Cross border - Verify postcode eligibility check redirection and content for England and Wales @pr1', async () => {
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
      "text": borderPostcode.isProtpertyLocatedInEnglandOrWalesQuestion,
      "elementType": "inlineText"
    });
    await performAction('selectBorderPostcode', borderPostcode.countryOptions.england);
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
  });

  test('Cross border - Verify postcode page for England and Scotland content @regression', async () => {
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
    await performValidation('text', {"text": borderPostcode.continueButton, "elementType": "button"})
    await performValidation('text', {"text": borderPostcode.cancel, "elementType": "button"})
  });

  test('Cross border England - Verify postcode eligibility check', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
  });

  test('Cross border England - Verify postcode not assigned to court - Can not use this service page @PR @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandWalesNoCourtCrossBorderPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performAction('selectBorderPostcode', borderPostcode.countryOptions.england);
    await performValidation('mainHeader', propertyIneligible.mainHeader);
  });

  test('Wales - Verify non cross border postcode eligibility check for Wales', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
  });

  test('England - Verify postcode not assigned to court - Can not use this service page', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandNoCourtAssignedPostcodeTextInput,
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

  test('England - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1only @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.mortgageLenderRadioOption);
    await performAction('clickButton', userIneligible.continue);
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performValidation('text', {
      "text": userIneligible.exitBackHintText,
      "elementType": "inlineText"
    });
    await performAction('clickButton', userIneligible.cancel);
  });

  test('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1only', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.privateLandlordRadioOption);
    await performValidation('text', {"text": userIneligible.thisServiceIsCurrentlyOnlyAvailableParagraph, "elementType": "paragraph"})
    await performValidation('text', {"text": userIneligible.formN5Wales, "elementType": "paragraph"})
    await performValidation('text', {"text": userIneligible.propertyPossessionsFullListLink, "elementType": "paragraph"})
    await performAction('clickButton', userIneligible.continue);
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performValidation('text', {
      "text": userIneligible.exitBackHintText,
      "elementType": "inlineText"
    });
    await performAction('clickButton', userIneligible.cancel);
  });

  test('Wales - Unsuccessful case creation journey due to claim type not in scope of Release1 @R1only @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.wales.communityLandlord);
    await performAction('selectClaimType', claimType.yesRadioOption);
    await performValidation('text', {"text": userIneligible.formN5Wales, "elementType": "paragraph"})
    await performValidation('text', {"text": userIneligible.propertyPossessionsFullListLink, "elementType": "paragraph"})
    await performAction('clickButton', userIneligible.continue);
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performValidation('text', {
      "text": userIneligible.exitBackHintText,
      "elementType": "inlineText"
    });
    await performAction('clickButton', userIneligible.cancel);
  });

  test('England - Unsuccessful case creation journey due to claim type not in scope of Release1 @R1only @regression', async () => {
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performAction('extractCaseIdFromAlert');
    await performAction('provideMoreDetailsOfClaim');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performAction('selectClaimType', claimType.yesRadioOption);
    await performAction('clickButton', userIneligible.continue);
    await performValidation('errorMessage', {
      header: userIneligible.eventNotCreated, message: userIneligible.unableToProceed
    });
    await performValidation('errorMessage', {
      header: userIneligible.errors, message: userIneligible.notEligibleForOnlineService
    });
    await performValidation('text', {
      "text": userIneligible.exitBackHintText,
      "elementType": "inlineText"
    });
    await performAction('clickButton', userIneligible.cancel);
  });
})

