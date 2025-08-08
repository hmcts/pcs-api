import {test} from '@playwright/test';
import {parentSuite} from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performValidation, performValidations,
} from '@utils/controller';
import configData from '@config/test.config';
import {createCase} from '@data/page-data/createCase.page.data';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {legislativeCountry} from '@data/page-data/legislativeCountry.page.data';

test.beforeEach(async ({page}, testInfo) => {
  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('login', 'exuiUser');

  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
});

test.describe.skip('[Create Case Flow With Address and Claimant Type]  @Master @nightly', async () => {
  test('England - Successful case creation', async () => {
    await performAction('clickButton', 'Create case');
    await performAction('selectCaseOptions', {
      jurisdiction: createCase.caseOption.jurisdiction.possessions
      , caseType: createCase.caseOption.caseType.civilPossessions
      , event: createCase.caseOption.event.makeAPossessionClaim
    });
    await performValidation('mainHeader', addressDetails.mainHeader);
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', addressDetails.errorMessage.errorMessage);
    await performAction('selectAddress', {postcode: addressDetails.propertyAddressSection.englandPostcode,
      addressIndex: addressDetails.propertyAddressSection.addressIndex});
    await performAction('selectLegislativeCountry', {country: legislativeCountry.countryOptions.england});
    await performValidation('mainHeader',  claimantType.mainHeader);
    await performAction('clickButton', 'Continue');
    await performValidation('errorMessage', claimantType.errorMessage.errorMessage);
    await performAction('selectClaimantType', {claimantType : claimantType.claimantTypeOptions.england.registeredProviderForSocialHousing});
    await performAction('clickButton', 'Save and continue');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performAction('clickTab', 'Property Details');
    await performValidations('address info is not null '
      , ['formLabelValue', 'Building and Street']
      , ['formLabelValue', 'Town or City']
      , ['formLabelValue', 'Postcode/Zipcode']
      , ['formLabelValue', 'Country']);
  });

  test('Wales - Successful case creation', async () => {
    await performAction('clickButton', 'Create case');
    await performAction('selectCaseOptions', {
      jurisdiction: createCase.caseOption.jurisdiction.possessions
      , caseType: createCase.caseOption.caseType.civilPossessions
      , event: createCase.caseOption.event.makeAPossessionClaim
    });
    await performAction('enterAddress', {
      buildingAndStreet: addressDetails.propertyAddressSection.buildingAndStreet
      ,addressLine2: addressDetails.propertyAddressSection.addressLine2
      ,addressLine3: addressDetails.propertyAddressSection.addressLine3
      ,townOrCity: addressDetails.propertyAddressSection.townOrCity
      ,walesCounty: addressDetails.propertyAddressSection.walesCounty
      ,postcode: addressDetails.propertyAddressSection.walesPostcode
      ,country: addressDetails.propertyAddressSection.country
    });
    await performAction('selectLegislativeCountry', {country: legislativeCountry.countryOptions.wales});
    await performAction('selectClaimantType', {claimantType : claimantType.claimantTypeOptions.wales.registeredCommunityLandlord});
    await performAction('clickButton', 'Save and continue');
    await performValidation("bannerAlert", {message: "Case #.* has been created."});
    await performAction('clickTab', 'Property Details');
    await performValidations('address info is not null',
      ['formLabelValue', 'Building and Street'],
      ['formLabelValue', 'Address Line 2'],
      ['formLabelValue', 'Town or City'],
      ['formLabelValue', 'Postcode/Zipcode'],
      ['formLabelValue', 'Country']);
  });

  test('England - Unsuccessful case creation journey due to claimant type not in scope of Release1', async () => {
    await performAction('clickButton', 'Create case');
    await performAction('selectCaseOptions', {
      jurisdiction: createCase.caseOption.jurisdiction.possessions
      , caseType: createCase.caseOption.caseType.civilPossessions
      , event: createCase.caseOption.event.makeAPossessionClaim
    });
    await performAction('selectAddress', {postcode: addressDetails.propertyAddressSection.englandPostcode,
      addressIndex: addressDetails.propertyAddressSection.addressIndex});
    await performAction('selectLegislativeCountry', {country: legislativeCountry.countryOptions.england});
    await performAction('selectClaimantType', {claimantType : claimantType.claimantTypeOptions.england.mortgageLender});
    await performValidation('mainHeader', "You're not eligible for this online service");
    await performAction('clickButton', 'Close and return to case list');
  });

  test('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1', async () => {
    await performAction('clickButton', 'Create case');
    await performAction('selectCaseOptions', {
      jurisdiction: createCase.caseOption.jurisdiction.possessions
      , caseType: createCase.caseOption.caseType.civilPossessions
      , event: createCase.caseOption.event.makeAPossessionClaim
    });
    await performAction('selectAddress', {postcode: addressDetails.propertyAddressSection.walesPostcode,
      addressIndex: addressDetails.propertyAddressSection.addressIndex});
    await performAction('selectLegislativeCountry', {country: legislativeCountry.countryOptions.wales});
    await performAction('selectClaimantType', {claimantType : claimantType.claimantTypeOptions.wales.privateLandlord});
    await performValidation('mainHeader', "You're not eligible for this online service");
    await performAction('clickButton', 'Close and return to case list');
  });
});

