import { test } from '@utils/test-fixtures';
import {
  initializeExecutor,
  performAction,
  performValidation} from '@utils/controller';
import {
  addressCheckYourAnswers,
  addressDetails,
  user,
  home
} from '@data/page-data';
import {
  noResultFound,
} from '@data/page-data-figma';
import { caseNumber } from '@utils/actions/custom-actions/createCase.action';
import { dismissCookieBanner } from '@config/cookie-banner';
// This test validates the resume & find case functionality with and without saved options.
// It is not intended to reuse for any of the e2e scenarios, those should still be covered in others specs.
// When a new page is added/flow changes, basic conditions in this test should be updated accordingly to continue the journey.
// Due to frequent issues with relogin and "Find Case" (Elasticsearch), this test is made optional only for the pipeline to maintain a green build.
// However, it must be executed locally, and evidence of the passed results should be provided during PR review in case its failing in pipeline.

// Disable global storageState for this file - these tests need to test sign-out/re-login flow
test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await page.evaluate(() => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch (e) {
      // Ignore if storage is not accessible
    }
  });

  await dismissCookieBanner(page, 'additional');
});

test.afterEach(async () => {
  if (caseNumber) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
});

test.describe('[Group Access Resume Case] @nightly @MAC @CC @groupAccess', async () => {
  test('Users belonging to Same SOLICITOR Org allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
    await performAction('login', user.claimantSolicitorForGATest);
    await performAction('createPartialClaimDetails');
    await performAction('signOut');
    //Login as user2 of Org1
    await performAction('reloginAndFindTheCase', user.claimantSolicitor1ForGATest);
    await performAction('resumePartialClaim');
  });
  test('Users belonging to Same Local Authority Org allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
    await performAction('login', user.localAuthorityOrg1Usr1);
    await performAction('createPartialClaimDetails');
    await performAction('signOut');
    //Login as user2 of Org1
    await performAction('reloginAndFindTheCase', user.localAuthorityOrg1Usr2);
    await performAction('resumePartialClaim');
  });
  test('Users belonging to Same Other - Real estate activities Org allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
    await performAction('login', user.otherRealEstateActivitiesOrg1Usr1);
    await performAction('createPartialClaimDetails');
    await performAction('signOut');
    //Login as user2 of Org1
    await performAction('reloginAndFindTheCase', user.otherRealEstateActivitiesOrg1Usr2);
    await performAction('resumePartialClaim');
  });
  test('Users belonging to Same Other - Property and construction Org allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
    await performAction('login', user.otherPropertyAndConstructionOrg1Usr1);
    await performAction('createPartialClaimDetails');
    await performAction('signOut');
    //Login as user2 of Org1
    await performAction('reloginAndFindTheCase', user.otherPropertyAndConstructionOrg1Usr2);
    await performAction('resumePartialClaim');
  });
  test('Users belonging to Same Other-Not for profit Org allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
    await performAction('login', user.otherNotForProfitOrg1Usr1);
    await performAction('createPartialClaimDetails');
    await performAction('signOut');
    //Login as user2 of Org1
    await performAction('reloginAndFindTheCase', user.otherNotForProfitOrg1Usr2);
    await performAction('resumePartialClaim');
  });
  test('Users belonging to Same Other-charity and voluntary work Org allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //England - Resume with saved options - Assured Tenancy - Rent arrears + other grounds when user selects no to rent arrears question', async () => {
    await performAction('login', user.otherCharityAndVoluntaryWorkOrg1Usr1);
    await performAction('createPartialClaimDetails');
    await performAction('signOut');
    //Login as user2 of Org1
    await performAction('reloginAndFindTheCase', user.otherCharityAndVoluntaryWorkOrg1Usr2);
    await performAction('resumePartialClaim');
  });

  test('Users belonging to Different Org [Solicitor - Solicitor And Solicitor- localAuthority]are Not allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //ResumeCase By different user belonging to different Org -England - Resume without saved options - Secure tenancy - No Rent Arrears @MAC', async () => {
    await performAction('login', user.claimantSolicitorForGATest);
    await performAction('clickTab', home.createCaseTab);
    await performAction('selectJurisdictionCaseTypeEvent');
    await performAction('housingPossessionClaim');
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('signOut');
    //Login as user2 belonging to different org of same type.
    await performAction('reloginAndFindTheCase', user.claimantSolicitorOrg2ForGATest);
    await performValidation('mainHeader', noResultFound.mainHeader);
    await performAction('signOut');
    //Login as user belonging to different type of org.
    await performAction('reloginAndFindTheCase', user.localAuthorityOrg1Usr1);
    await performValidation('mainHeader', noResultFound.mainHeader);
   });
   
  test('Users belonging to Different Org [LocalAuthority - LocalAuthority And LocalAuthority- otherRealEstateActivities]are Not allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //ResumeCase By different user belonging to different Org -England - Resume without saved options - Secure tenancy - No Rent Arrears @MAC', async () => {
    await performAction('login', user.localAuthorityOrg1Usr1);
    await performAction('clickTab', home.createCaseTab);
    await performAction('selectJurisdictionCaseTypeEvent');
    await performAction('housingPossessionClaim');
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('signOut');
    //Login as user2 belonging to different org of same type.
    await performAction('reloginAndFindTheCase', user.localAuthorityOrg2Usr1);
    await performValidation('mainHeader', noResultFound.mainHeader);
    await performAction('signOut');
    //Login as user belonging to different type of org.
    await performAction('reloginAndFindTheCase', user.otherRealEstateActivitiesOrg1Usr1);
    await performValidation('mainHeader', noResultFound.mainHeader);
   });

  test('Users belonging to Different Org [Other-Not for profit - LocalAuthority And Other-Not for profit- Solicitor ]are Not allowed to resume case @nightly @MAC @CC @groupAccess', async () => {
    //ResumeCase By different user belonging to different Org -England - Resume without saved options - Secure tenancy - No Rent Arrears @MAC', async () => {
    await performAction('login', user.otherNotForProfitOrg1Usr1);
    await performAction('clickTab', home.createCaseTab);
    await performAction('selectJurisdictionCaseTypeEvent');
    await performAction('housingPossessionClaim');
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcodeTextInput,
      addressIndex: addressDetails.addressIndex
    });
    await performValidation('mainHeader', addressCheckYourAnswers.mainHeader)
    await performAction('submitAddressCheckYourAnswers');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('signOut');
    //Login as user2 belonging to different org of same type.
    await performAction('reloginAndFindTheCase', user.localAuthorityOrg1Usr1);
    await performValidation('mainHeader', noResultFound.mainHeader);
    await performAction('signOut');
    //Login as user belonging to different type of org.
    await performAction('reloginAndFindTheCase', user.claimantSolicitor1ForGATest);
    await performValidation('mainHeader', noResultFound.mainHeader);
   });
});

