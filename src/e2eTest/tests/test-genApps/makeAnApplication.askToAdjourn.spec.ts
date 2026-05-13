import { chooseAnApplication, askToAdjournTheCourtHearing, isTheCourtHearingInTheNext14Days, doYouNeedHelpPayingTheFee, haveYouAlreadyAppliedForHelpWithFees, haveTheOtherPartiesAgreedToThisApplication, whatOrderDoYouWantTheCourtToMakeAndWhy, doYouWantToUploadDocumentToSupportYourApplication, uploadDocumentsToSupportYourApplication, whichLanguageDidYouUseToCompleteThisService, checkYourAnswersGenApps, areThereAnyReasonsThatThisApplicationShouldNotBeShared } from '@data/page-data-figma/page-data-genApps-figma';
import { createCaseApiData, submitCaseApiData } from '../../data/api-data';


import { initializeExecutor } from '../../utils/controller';
import test, { expect } from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { initializeGenAppsExecutor, performAction, performValidation } from '@utils/controller-genApps';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

const home_url = process.env.TEST_URL;

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeGenAppsExecutor(page);
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadDefault });
  await performAction('fetchPINsAPI');
  await performAction('createUser', 'citizen', ['citizen']);
  await performAction('validateAccessCodeAPI');
  await performAction('navigateToUrl', home_url);
  await performAction('login');
   await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
    // Login and cookie consent are handled globally via storageState in global-setup.config.ts
    await expect(async () => {
      await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
});

test.afterEach(async () => {

});

test.describe('Make an Application - e2e Journey @nightly', async () => {
  test('Select an Application - Ask to Adjourn journey - Court hearing in 14 days[Yes] @regression @smoke', async () => {
    await performAction('chooseAnApplication', {
      question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
      option: chooseAnApplication.adjournTheHearingRadioOption,
    });
    await performValidation('mainHeader', askToAdjournTheCourtHearing.mainHeader);
    await performAction('clickButton', askToAdjournTheCourtHearing.startNowButton);
    await performValidation('mainHeader', isTheCourtHearingInTheNext14Days.mainHeader);
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion,
      option: isTheCourtHearingInTheNext14Days.yesRadioOption,
    });
    await performValidation('mainHeader', doYouNeedHelpPayingTheFee.mainHeader);
    await performAction('doYouNeedHelpPayingFee', {
      question: doYouNeedHelpPayingTheFee.doYouNeedHelpPayingTheFeeQuestion,
      option: doYouNeedHelpPayingTheFee.iNeedHelpPayingTheFeeRadioOption,
    });
    await performValidation('mainHeader', haveYouAlreadyAppliedForHelpWithFees.mainHeader);
    await performAction('confirmYouHaveAppliedForFeeHelp', {
      question: haveYouAlreadyAppliedForHelpWithFees.haveYouAlreadyAppliedForHelpQuestion,
      option: haveYouAlreadyAppliedForHelpWithFees.yesRadioOption,
      label: haveYouAlreadyAppliedForHelpWithFees.hwfReferenceHiddenTextLabel,
      input: haveYouAlreadyAppliedForHelpWithFees.hwfReferenceTextInput,
    });
    await performAction('confirmOtherPartiesAgreed', {
      question: haveTheOtherPartiesAgreedToThisApplication.haveTheOtherPartiesAgreedQuestion,
      option: haveTheOtherPartiesAgreedToThisApplication.yesRadioOption,
    });
    await performValidation('mainHeader', whatOrderDoYouWantTheCourtToMakeAndWhy.mainHeader);
    await performAction('confirmOrderDoYouWant', {
      label: whatOrderDoYouWantTheCourtToMakeAndWhy.explainWhatYouWantTextLabel,
      input: whatOrderDoYouWantTheCourtToMakeAndWhy.whatYouWantTheCourtToDoTextInput,
    });
    await performValidation('mainHeader', doYouWantToUploadDocumentToSupportYourApplication.mainHeader);
    await performAction('clickRadioButton', doYouWantToUploadDocumentToSupportYourApplication.yesRadioOption);
    await performAction('clickButton', doYouWantToUploadDocumentToSupportYourApplication.continueButton);
    await performValidation('mainHeader', uploadDocumentsToSupportYourApplication.mainHeader);
    await performAction('clickButton', uploadDocumentsToSupportYourApplication.continueButton);
    await performValidation('mainHeader', whichLanguageDidYouUseToCompleteThisService.mainHeader);
    await performAction('selectLanguageUsedToComplete', {
      question: whichLanguageDidYouUseToCompleteThisService.whichLanguageDidYouUseQuestion,
      option: whichLanguageDidYouUseToCompleteThisService.englishRadioOption,
    });
    await performValidation('mainHeader', checkYourAnswersGenApps.mainHeader);
    await performAction('retrieveCYATableData');
    await performAction('validateCYA');
    await performAction('reviewCYA', 'journey1');
    await performAction('reviewAndUpdateCYA', {
      changeOption: isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion,
      journey: 'journey2',
    });
    await performAction('retrieveCYATableData');
    await performAction('validateCYA');
    await performAction('selectStatementOfTruth', {
      question: checkYourAnswersGenApps.statementOfTruthQuestion,
      option: checkYourAnswersGenApps.iBelieveTheFactsHiddenCheckbox,
      label: checkYourAnswersGenApps.yourFullNameTextLabel,
      input: checkYourAnswersGenApps.yourFullNameTextInput,
    });
  });
});
