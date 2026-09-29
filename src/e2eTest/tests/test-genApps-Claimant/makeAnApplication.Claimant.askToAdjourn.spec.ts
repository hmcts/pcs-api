import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test, { expect } from '@playwright/test';
import { initializeGenAppsExecutor, performAction, performValidation } from '@utils/controller-genApps';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  askToAdjournTheCourtHearing,
  chooseAnApplication,
  isTheCourtHearingInTheNext14Days,
  haveYouAskedTheOtherPartiesAgreedToThisApplication,
  whatOrderDoYouWantTheCourtToMakeAndWhy,
  doYouWantToUploadDocumentsToSupportYourApplication,
  uploadDocumentsToSupportYourApplication,
  whichLanguageDidYouUseToCompleteThisService, checkYourAnswersClaimantGenApps, statementOfTruthClaimant,
  serviceRequestGenApps, paymentDetails, areThereAnyReasonsThatThisApplicationShouldNotBeShared,
} from "@data/page-data-figma/page-data-genApps-claimant-figma";
import {home} from "@data/page-data";



test.use({ storageState: undefined })

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeGenAppsExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('updatePaymentAPI');
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.claimantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
  await page.waitForLoadState();
  await page.locator('.spinner-container').waitFor({ state: 'detached' });
  await performValidation('mainHeader', home.caseSummary);
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});


test.describe('Claimant - Make an Application - e2e Journey @nightly', async () => {
  test('Claimant- Select an Application - Ask to Adjourn journey - Court hearing in 14 days[Yes] @regression @smoke', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.makeAnApplication);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', chooseAnApplication.mainHeader);
    await performAction('chooseAnApplication', {
      question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
      option: chooseAnApplication.adjournTheHearingRadioOption,
    });
    await performValidation('mainHeader', askToAdjournTheCourtHearing.mainHeader);
    await performAction('clickButton', askToAdjournTheCourtHearing.continueButton);
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion,
      option: isTheCourtHearingInTheNext14Days.yesRadioOption,
    });
    await performValidation('mainHeader',haveYouAskedTheOtherPartiesAgreedToThisApplication.mainHeader);
    await performAction('confirmOtherPartiesAgreed', {
      question: haveYouAskedTheOtherPartiesAgreedToThisApplication.haveTheOtherPartiesAgreedQuestion,
      option: haveYouAskedTheOtherPartiesAgreedToThisApplication.yesRadioOption,
    });
    await performValidation('mainHeader', whatOrderDoYouWantTheCourtToMakeAndWhy.mainHeader);
    await performAction('confirmOrderDoYouWant', {
      label: whatOrderDoYouWantTheCourtToMakeAndWhy.explainWhatYouWantTextLabel,
      input: whatOrderDoYouWantTheCourtToMakeAndWhy.whatYouWantTheCourtToDoTextInput,
    });
    await performAction('confirmDocumentToUpload', {
      question: doYouWantToUploadDocumentsToSupportYourApplication.doYouWantToUploadDocumentQuestion,
      option: doYouWantToUploadDocumentsToSupportYourApplication.yesRadioOption,
    });
    await performValidation('mainHeader', uploadDocumentsToSupportYourApplication.mainHeader);
    await performAction('uploadFilesGenApps', {
      documents: [
        {type: uploadDocumentsToSupportYourApplication.witnessStatementDropDownInput, fileName: 'genApps.xlsx'},
      ]
    });
    await performAction('selectLanguageUsedToComplete', {
      question: whichLanguageDidYouUseToCompleteThisService.whichLanguageDidYouUseQuestion,
      option: whichLanguageDidYouUseToCompleteThisService.englishRadioOption,
    });
    await performValidation('mainHeader', statementOfTruthClaimant.mainHeader);
    await performAction('selectGenAppsClaimantStatementOfTruth', {
      completedBy: statementOfTruthClaimant.claimantRadioOption,
      iBelieveCheckbox: statementOfTruthClaimant.BelievesHiddenCheckBox,
      fullNameTextInput: statementOfTruthClaimant.fullNameHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruthClaimant.positionOrOfficeHeldHiddenTextInput
    });
    await performValidation('mainHeader', checkYourAnswersClaimantGenApps.mainHeader);
    await performAction('retrieveCYATableData', { name: 'check your answers table' });
    await performAction('validateCYA');
    await performAction('clickButton', checkYourAnswersClaimantGenApps.submitButton);
    await performAction('payClaimFeeGenApps', {clickLink: true});
    await performAction('clickPayNowLinkGenApps');
    await performAction('selectPaymentOptions', {
      amountLabel: serviceRequestGenApps.amountToPayLabel,
      payByOption: serviceRequestGenApps.payByCardRadioOption,
      button: serviceRequestGenApps.continueButton
    });
    await performValidation('mainHeader', paymentDetails.mainHeader);
    await performAction('inputPaymentDetails', {
      question: paymentDetails.mainHeader,
      cardNumber: paymentDetails.cardNumberTextInput,
      month: paymentDetails.monthTextInput,
      year: paymentDetails.yearTextInput,
      nameOnCard: paymentDetails.nameOnCardTextInput,
      cardSecurityCode: paymentDetails.cardSecurityCodeTextInput,
      addressLine1: paymentDetails.addressLine1TextInput,
      townOrCity: paymentDetails.townOrCityTextInput,
      postcode: paymentDetails.postcodeTextInput,
      email: paymentDetails.emailTextInput,
    });
    await performAction('confirmPaymentGenApps');
    await performValidation('mainHeader', serviceRequestGenApps.paymentSuccessMainHeader);
  });

  test('Claimant- Select an Application - Ask to Adjourn journey - Court hearing in 14 days[No]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.makeAnApplication);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', chooseAnApplication.mainHeader);
    await performAction('chooseAnApplication', {
      question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
      option: chooseAnApplication.adjournTheHearingRadioOption,
    });
    await performValidation('mainHeader', askToAdjournTheCourtHearing.mainHeader);
    await performAction('clickButton', askToAdjournTheCourtHearing.continueButton);
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion,
      option: isTheCourtHearingInTheNext14Days.noRadioOption,
    });
    await performValidation('mainHeader',haveYouAskedTheOtherPartiesAgreedToThisApplication.mainHeader);
    await performAction('confirmOtherPartiesAgreed', {
      question: haveYouAskedTheOtherPartiesAgreedToThisApplication.haveTheOtherPartiesAgreedQuestion,
      option: haveYouAskedTheOtherPartiesAgreedToThisApplication.noRadioOption,
    });
    await performValidation('mainHeader', areThereAnyReasonsThatThisApplicationShouldNotBeShared.mainHeader);
    await performAction('reasonsApplicationShouldNotBeShared', {
      question: areThereAnyReasonsThatThisApplicationShouldNotBeShared.areThereAnyReasonQuestion,
      option: areThereAnyReasonsThatThisApplicationShouldNotBeShared.yesRadioOption,
      label: areThereAnyReasonsThatThisApplicationShouldNotBeShared.provideReasonHiddenTextLabel,
      input: areThereAnyReasonsThatThisApplicationShouldNotBeShared.provideReasonTextInput,
    });
    await performValidation('mainHeader', whatOrderDoYouWantTheCourtToMakeAndWhy.mainHeader);
    await performAction('confirmOrderDoYouWant', {
      label: whatOrderDoYouWantTheCourtToMakeAndWhy.explainWhatYouWantTextLabel,
      input: whatOrderDoYouWantTheCourtToMakeAndWhy.whatYouWantTheCourtToDoTextInput,
    });
    await performAction('confirmDocumentToUpload', {
      question: doYouWantToUploadDocumentsToSupportYourApplication.doYouWantToUploadDocumentQuestion,
      option: doYouWantToUploadDocumentsToSupportYourApplication.noRadioOption,
    });
    await performAction('selectLanguageUsedToComplete', {
      question: whichLanguageDidYouUseToCompleteThisService.whichLanguageDidYouUseQuestion,
      option: whichLanguageDidYouUseToCompleteThisService.welshRadioOption,
    });
    await performAction('selectGenAppsClaimantStatementOfTruth', {
      completedBy: statementOfTruthClaimant.claimantLegalRepresentativeRadioOption,
      signThisStatementCheckbox: statementOfTruthClaimant.claimantBelievesHiddenCheckBox,
      fullNameTextInput: statementOfTruthClaimant.fullNameHiddenTextInput,
      nameOfFirmTextInput: statementOfTruthClaimant.nameOfFirmHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruthClaimant.positionOrOfficeHeldHiddenTextInput
    });
    await performValidation('mainHeader', checkYourAnswersClaimantGenApps.mainHeader);
    await performAction('retrieveCYATableData', { name: 'check your answers table' });
    await performAction('validateCYA');
    await performAction('clickButton', checkYourAnswersClaimantGenApps.submitButton);
    await performAction('verifyApplicationSubmitted');
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make an application');
  });
});

