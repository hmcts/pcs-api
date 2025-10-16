import {Page,test} from '@playwright/test';
import {initializeExecutor, performAction, performValidation, performValidations} from '@utils/controller';
import {addressDetails} from '@data/page-data/addressDetails.page.data';
import {claimantType} from '@data/page-data/claimantType.page.data';
import {claimType} from '@data/page-data/claimType.page.data';
import {claimantName} from '@data/page-data/claimantName.page.data';
import {contactPreferences} from '@data/page-data/contactPreferences.page.data';
import {defendantDetails} from '@data/page-data/defendantDetails.page.data';
import {tenancyLicenceDetails} from '@data/page-data/tenancyLicenceDetails.page.data';
import {groundsForPossession} from '@data/page-data/groundsForPossession.page.data';
import {rentArrearsPossessionGrounds} from '@data/page-data/rentArrearsPossessionGrounds.page.data';
import {preActionProtocol} from '@data/page-data/preActionProtocol.page.data';
import {mediationAndSettlement} from '@data/page-data/mediationAndSettlement.page.data';
import {noticeOfYourIntention} from '@data/page-data/noticeOfYourIntention.page.data';
import {noticeDetails} from '@data/page-data/noticeDetails.page.data';
import {rentDetails} from '@data/page-data/rentDetails.page.data';
import {dailyRentAmount} from '@data/page-data/dailyRentAmount.page.data';
import {provideMoreDetailsOfClaim} from '@data/page-data/provideMoreDetailsOfClaim.page.data';
import {whatAreYourGroundsForPossession} from '@data/page-data/whatAreYourGroundsForPossession.page.data';
import {reasonsForPossession} from '@data/page-data/reasonsForPossession.page.data';
import {moneyJudgment} from '@data/page-data/moneyJudgment.page.data';
import {claimantCircumstances} from '@data/page-data/claimantCircumstances.page.data';
import {applications} from '@data/page-data/applications.page.data';
import {completeYourClaim} from '@data/page-data/completeYourClaim.page.data';
import {user} from '@data/user-data/permanent.user.data';
import {reasonsForRequestingASuspensionOrder} from '@data/page-data/reasonsForRequestingASuspensionOrder.page.data';
import {checkYourAnswers} from '@data/page-data/checkYourAnswers.page.data';
import {propertyDetails} from '@data/page-data/propertyDetails.page.data';
import {languageUsed} from '@data/page-data/languageUsed.page.data';
import {defendantCircumstances} from '@data/page-data/defendantCircumstances.page.data';
import {claimingCosts} from '@data/page-data/claimingCosts.page.data';
import {uploadAdditionalDocs} from '@data/page-data/uploadAdditionalDocs.page.data';
import {statementOfTruth} from '@data/page-data/statementOfTruth.page.data';
import {home} from '@data/page-data/home.page.data';
import {additionalReasonsForPossession} from '@data/page-data/additionalReasonsForPossession.page.data';
import {underlesseeOrMortgageeEntitledToClaim} from '@data/page-data/underlesseeOrMortgageeEntitledToClaim.page.data';
import {alternativesToPossession} from '@data/page-data/alternativesToPossession.page.data';
import {housingAct} from '@data/page-data/housingAct.page.data';
import {reasonsForRequestingADemotionOrder} from '@data/page-data/reasonsForRequestingADemotionOrder.page.data';
import {statementOfExpressTerms} from '@data/page-data/statementOfExpressTerms.page.data';
import {wantToUploadDocuments} from '@data/page-data/wantToUploadDocuments.page.data';
import {reasonsForRequestingASuspensionAndDemotionOrder} from '@data/page-data/reasonsForRequestingASuspensionAndDemotionOrder.page.data';

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('login', user.claimantSolicitor);
  await performAction('clickButton', 'Accept analytics cookies');
  await page.locator('#cookie-banner').waitFor({ state: 'hidden' });
  //await performValidation('compareWithSnapshot','caseList');//Observation: list of case displayed is dynamic, so lot of content will change
  await performAction('clickTab', home.createCaseTab);
  //await performValidation('compareWithSnapshot','createCase');//Observation:PR number is dynamic
  await performAction('selectJurisdictionCaseTypeEvent')
  await performValidation('compareWithSnapshot','makeAClaim');
  await performAction('housingPossessionClaim');
});

test.describe('[Create Case - England] @functional', async () => {
  test('England Journey - Visual Validations', async ({page:page}) => {
    //await performValidation('compareWithSnapshot','selectAddress');//Observation:Need to add one more compare inside selectAddress
    await performAction('selectAddress', {
      postcode: addressDetails.englandCourtAssignedPostcode,
      addressIndex: addressDetails.addressIndex
    });
    //await performValidation('compareWithSnapshot','provideMoreDetails');
    await performValidation('bannerAlert', 'Case #.* has been created.');
    await performAction('extractCaseIdFromAlert');
    await performAction('clickButtonAndVerifyPageNavigation', provideMoreDetailsOfClaim.continue, claimantType.mainHeader);
    //await performValidation('compareWithSnapshot','claimantType');
    await performAction('selectClaimantType', claimantType.england.registeredProviderForSocialHousing);
    await performValidation('compareWithSnapshot', 'claimType', {selectorsToMask: [claimType.selector.caseNumber]});
    await performAction('selectClaimType', claimType.no);
    //await performValidation('compareWithSnapshot','claimantName');
    //await performAction('selectClaimantName', claimantName.no);//Observation: Text box will appear when option 'No' is selected
    //await performValidation('compareWithSnapshot','claimantNameNo');
    await performAction('selectClaimantName', claimantName.yes);
    //await performValidation('compareWithSnapshot','contactPreferences');//Observation: email and address are dynamic
    // await performAction('selectContactPreferences', {
    //   notifications: contactPreferences.yes,
    //   correspondenceAddress: contactPreferences.yes,
    //   phoneNumber: contactPreferences.no
    // });
    // await performAction('defendantDetails', {
    //   name: defendantDetails.yes,
    //   correspondenceAddress: defendantDetails.yes,
    //   email: defendantDetails.yes,
    //   correspondenceAddressSame: defendantDetails.no
    // });
    // await performValidation('mainHeader', tenancyLicenceDetails.mainHeader);
    // await performAction('selectTenancyOrLicenceDetails', {
    //   tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy,
    //   day: tenancyLicenceDetails.day,
    //   month: tenancyLicenceDetails.month,
    //   year: tenancyLicenceDetails.year,
    //   files: ['tenancyLicence.docx', 'tenancyLicence.png']
    // });
    // await performValidation('mainHeader', groundsForPossession.mainHeader);
    // await performAction('selectGroundsForPossession',{groundsRadioInput: groundsForPossession.yes});
    // await performAction('selectRentArrearsPossessionGround', {
    //   rentArrears: [rentArrearsPossessionGrounds.rentArrears, rentArrearsPossessionGrounds.seriousRentArrears, rentArrearsPossessionGrounds.persistentDelayInPayingRent],
    //   otherGrounds: rentArrearsPossessionGrounds.yes
    // });
    // await performAction('selectYourPossessionGrounds',{
    //   mandatory: [whatAreYourGroundsForPossession.mandatory.holidayLet,whatAreYourGroundsForPossession.mandatory.ownerOccupier],
    //   discretionary: [whatAreYourGroundsForPossession.discretionary.domesticViolence14A,whatAreYourGroundsForPossession.discretionary.rentArrears],
    // });
    // await performValidation('mainHeader', preActionProtocol.mainHeader);
    // await performAction('selectPreActionProtocol', preActionProtocol.yes);
    // await performValidation('mainHeader', mediationAndSettlement.mainHeader);
    // await performAction('selectMediationAndSettlement', {
    //   attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,
    //   settlementWithDefendantsOption: mediationAndSettlement.no,
    // });
    // await performValidation('mainHeader', noticeOfYourIntention.mainHeader);
    // await performValidation('text', {"text": noticeOfYourIntention.guidanceOnPosessionNoticePeriodsLink, "elementType": "paragraphLink"})
    // await performValidation('text', {"text": noticeOfYourIntention.servedNoticeInteractiveText, "elementType": "inlineText"});
    // await performAction('selectNoticeOfYourIntention', noticeOfYourIntention.yes);
    // await performValidation('mainHeader', noticeDetails.mainHeader);
    // await performAction('selectNoticeDetails', {
    //   howDidYouServeNotice: noticeDetails.byFirstClassPost,
    //   day: '16', month: '07', year: '1985', files: 'NoticeDetails.pdf'});
    // await performValidation('mainHeader', rentDetails.mainHeader);
    // await performAction('provideRentDetails', {rentFrequencyOption:'weekly', rentAmount:'800'});
    // await performValidation('mainHeader', dailyRentAmount.mainHeader);
    // await performAction('selectDailyRentAmount', {
    //   calculateRentAmount: '£114.29',
    //   unpaidRentInteractiveOption: dailyRentAmount.no,
    //   unpaidRentAmountPerDay: '20'
    // });
    // await performValidation('mainHeader', moneyJudgment.mainHeader);
    // await performAction('selectMoneyJudgment', moneyJudgment.yes);
    // await performValidation('mainHeader', claimantCircumstances.mainHeader);
    // await performAction('selectClaimantCircumstances', {
    //   circumstanceOption: claimantCircumstances.yes,
    //   claimantInput: claimantCircumstances.claimantCircumstanceInfoInputData
    // });
    // await performValidation('mainHeader', defendantCircumstances.mainHeader);
    // await performAction('selectDefendantCircumstances', defendantCircumstances.yes);
    // await performValidation('mainHeader', alternativesToPossession.mainHeader);
    // await performAction('selectAlternativesToPossession');
    // await performValidation('mainHeader', claimingCosts.mainHeader);
    // await performAction('selectClaimingCosts', claimingCosts.yes);
    // await performValidation('mainHeader', additionalReasonsForPossession.mainHeader);
    // await performAction('selectAdditionalReasonsForPossession', additionalReasonsForPossession.yes);
    // await performValidation('mainHeader', underlesseeOrMortgageeEntitledToClaim.mainHeader);
    // await performAction('clickButton', underlesseeOrMortgageeEntitledToClaim.continue);
    // await performAction('wantToUploadDocuments', {
    //   question: wantToUploadDocuments.uploadAnyAdditionalDocumentsLabel,
    //   option: wantToUploadDocuments.yes
    // });
    // await performAction('uploadAdditionalDocs', {
    //   documents: [{
    //     type: uploadAdditionalDocs.tenancyAgreementOption,
    //     fileName: 'tenancy.pdf',
    //     description: uploadAdditionalDocs.shortDescriptionInput
    //   }]
    // });
    // await performAction('selectApplications', applications.yes);
    // await performAction('selectLanguageUsed', {question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.english});
    // await performAction('completingYourClaim', completeYourClaim.submitAndClaimNow);
    // await performAction('clickButton', statementOfTruth.continue);
    // await performAction('clickButton', checkYourAnswers.saveAndContinue);
    // await performValidation('bannerAlert', 'Case #.* has been updated with event: Make a claim');
    // await performValidations(
    //   'address info not null',
    //   ['formLabelValue', propertyDetails.buildingAndStreetLabel],
    //   ['formLabelValue', propertyDetails.townOrCityLabel],
    //   ['formLabelValue', propertyDetails.postcodeZipcodeLabel],
    //   ['formLabelValue', propertyDetails.countryLabel],
    // )
  });
});
