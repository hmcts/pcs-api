import { Page, test} from '@playwright/test';
import {initializeExecutor, performAction, performValidation} from '@utils/controller';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {provideMoreDetailsOfClaim} from '@data/page-data/provideMoreDetailsOfClaim.page.data';
import {user} from '@data/user-data/permanent.user.data';
import {home} from '@data/page-data/home.page.data';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await page.getByRole('button', { name: 'Accept additional cookies' }).click();
  await page.getByRole('button', { name: 'Hide this cookie message' }).click();
  await performAction('login', user.claimantSolicitor);
  await page.getByRole('button', { name: 'Accept analytics cookies' }).click();
  await page.waitForTimeout(2000);
  await performAction('clickTab', home.createCaseTab);
  await performAction('selectJurisdictionCaseTypeEvent');
  await performAction('housingPossessionClaim');
});

test.describe('[Create Case - England] @Master @nightly', async () => {
  test('App snapshot compare- negative scenario', async ({page}) => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performValidation('compareWithSnapshot','claimType');
    await performAction('clickRadioButton', 'No');
    await performValidation('compareWithSnapshot','claimTypeYes');
  });
  test('App snapshot compare- positive scenario', async ({page}) => {
    await performAction('selectAddress', {
      postcode: addressDetails.walesCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    await performAction('selectClaimantType', claimantType.registeredCommunityLandlord);
    await performValidation('compareWithSnapshot','claimType');
    await performAction('clickRadioButton', 'Yes');
    await performValidation('compareWithSnapshot','claimTypeYes');
  });
});
