import { test } from '@playwright/test';
import { parentSuite } from 'allure-js-commons';
import {
  initializeExecutor,
  performAction,
  performValidation, performValidations
} from '@utils/controller';
import configData from '@config/test.config';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {legislativeCountry} from '@data/page-data/legislativeCountry.page.data';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  await parentSuite('Case Creation');
  await performAction('navigateToUrl', configData.manageCasesBaseURL);
  await performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker']);
  await testInfo.attach('Page URL', {
    body: page.url(),
    contentType: 'text/plain',
  });
  await performAction('clickButton', 'Create case');
  await performAction('selectJurisdictionCaseTypeEvent');
});

test.describe('[Create Case Flow With Address and Claimant Type]  @Master @nightly', async () => {
  test('England - Successful case creation', async () => {
    await performAction('selectAddress', {postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex});
    await performAction('selectLegislativeCountry', legislativeCountry.england);
    await performAction('selectClaimantType', claimantType.registeredProviderForSocialHousing);
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickTab', 'Property Details');
    await performValidations(
      'address info not null',
      ['formLabelValue', 'Building and Street'],
      ['formLabelValue', 'Town or City'],
      ['formLabelValue', 'Postcode/Zipcode'],
      ['formLabelValue', 'Country']
    );
  });

  test('Wales - Successful case creation', async () => {
    await performAction('enterTestAddressManually');
    await performAction('selectLegislativeCountry', legislativeCountry.wales);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performAction('clickButton', 'Save and continue');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('clickTab', 'Property Details');
    await performValidations('address information entered',
      ['formLabelValue', 'Building and Street', addressDetails.buildingAndStreet],
      ['formLabelValue', 'Address Line 2', addressDetails.addressLine2],
      ['formLabelValue', 'Town or City', addressDetails.townOrCity],
      ['formLabelValue', 'Postcode/Zipcode', addressDetails.postcode],
      ['formLabelValue', 'Country', addressDetails.country]);
  });

  test('England - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1', async () => {
    await performAction('selectAddress', {postcode: addressDetails.englandPostcode,
      addressIndex: addressDetails.addressIndex});
    await performAction('selectLegislativeCountry', legislativeCountry.england);
    await performAction('selectClaimantType', claimantType.mortgageLender);
    await performValidation('mainHeader', 'You\'re not eligible for this online service');
    await performAction('clickButton', 'Close and return to case list');
  });

  test('Wales - Unsuccessful case creation journey due to claimant type not in scope of Release1 @R1', async () => {
    await performAction('selectAddress', {postcode: addressDetails.walesPostcode,
      addressIndex: addressDetails.addressIndex});
    await performAction('selectLegislativeCountry', legislativeCountry.wales);
    await performAction('selectClaimantType', claimantType.privateLandlord);
    await performValidation('mainHeader', 'You\'re not eligible for this online service');
    await performAction('clickButton', 'Close and return to case list');
  });
});

