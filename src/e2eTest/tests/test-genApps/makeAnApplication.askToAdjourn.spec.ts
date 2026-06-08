import { createCaseApiData, submitCaseApiData } from '@data/api-data';


import { initializeExecutor } from '@utils/controller';
import test, { expect } from '@playwright/test';
import { FieldsStore } from '@utils/actions/custom-actions/custom-actions-genApps/recordAnsweredFields.action';
import { initializeGenAppsExecutor, performAction, performValidation } from '@utils/controller-genApps';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { user } from '@data/user-data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  askToAdjournTheCourtHearing, haveTheyAlreadyAppliedForHelpWithFees, helpPayingTheFee,
  chooseAnApplication,
  isTheCourtHearingInTheNext14Days,
  selectParty, whatOrderDoYouWantTheCourtToMakeAndWhy, hasTheDefendantAskedTheOtherPartiesAgreedToThisApplication,
  areThereAnyReasonsThatThisApplicationShouldNotBeShared,
  doYouWantToUploadDocumentsToSupportDefendantsApplication, whichLanguageDidYouUseToCompleteThisService,
  statementOfTruth, uploadDocumentsToSupportDefendantsApplication, checkYourAnswersGenApps
} from '@data/page-data-figma/page-data-genApps-figma';
import { defendantDetails } from '@utils/actions/custom-actions/custom-actions-genApps/genApps.action';
import {confirmGenApps} from "@data/page-data-figma/page-data-genApps-figma/confirmGenApps.page.data";

test.use({ storageState: undefined });

test.beforeEach(async ({ page, context }) => {
  await context.clearCookies();
  initializeExecutor(page);
  initializeGenAppsExecutor(page);
  defendantDetails.length = 0;
  FieldsStore.clear();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('getCaseAPI');
  await performAction('getDefendantDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayload.addAnotherDefendant,
    payLoad: submitCaseApiData.submitCasePayload
  });
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  // await page.evaluate(() => {
  //   try {
  //     localStorage.clear();
  //     sessionStorage.clear();
  //   } catch (e) {
  //     // Ignore if storage is not accessible
  //   }
  // });

  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.defendantSolicitor);
  await dismissCookieBanner(page, 'analytics');
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});

test.describe('Make an Application - e2e Journey @nightly', async () => {
  test('Select an Application - Ask to Adjourn journey - Court hearing in 14 days[Yes] @regression @PR @smoke', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.makeAnApplication);
    await performAction('clickButton', caseSummary.go);
    await performAction('chooseAnApplication', {
      question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
      option: chooseAnApplication.adjournTheHearingRadioOption,
    });
    await performValidation('mainHeader', askToAdjournTheCourtHearing.mainHeader);
    await performAction('clickButton', askToAdjournTheCourtHearing.continueButton);
    await performValidation('mainHeader', selectParty.mainHeader);
    await performAction('selectApplicant', {
      question: selectParty.partyMakingApplicationQuestion,
      option: defendantDetails[0],
    });
    await performAction('confirmIfCourtHearingInNext14Days', {
      question: isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion,
      option: isTheCourtHearingInTheNext14Days.yesRadioOption,
    });
    await performValidation('mainHeader', helpPayingTheFee.mainHeader);
    await performAction('doYouNeedHelpPayingFee', {
      question: helpPayingTheFee.doYouNeedHelpPayingTheFeeQuestion,
      option: helpPayingTheFee.yesRadioOption,
    });
    await performAction('confirmYouHaveAppliedForFeeHelp', {
      question: haveTheyAlreadyAppliedForHelpWithFees.haveYouAlreadyAppliedForHelpQuestion,
      option: haveTheyAlreadyAppliedForHelpWithFees.yesRadioOption,
      label: haveTheyAlreadyAppliedForHelpWithFees.hwfReferenceHiddenTextLabel,
      input: haveTheyAlreadyAppliedForHelpWithFees.hwfReferenceTextInput,
    });
    await performValidation('mainHeader',hasTheDefendantAskedTheOtherPartiesAgreedToThisApplication.mainHeader);
    await performAction('confirmOtherPartiesAgreed', {
      question: hasTheDefendantAskedTheOtherPartiesAgreedToThisApplication.haveTheOtherPartiesAgreedQuestion,
      option: hasTheDefendantAskedTheOtherPartiesAgreedToThisApplication.yesRadioOption,
    });
    await performValidation('mainHeader', whatOrderDoYouWantTheCourtToMakeAndWhy.mainHeader);
    await performAction('confirmOrderDoYouWant', {
      label: whatOrderDoYouWantTheCourtToMakeAndWhy.explainWhatYouWantTextLabel,
      input: whatOrderDoYouWantTheCourtToMakeAndWhy.whatYouWantTheCourtToDoTextInput,
    });
    await performAction('confirmDocumentToUpload', {
      question: doYouWantToUploadDocumentsToSupportDefendantsApplication.doYouWantToUploadDocumentQuestion,
      option: doYouWantToUploadDocumentsToSupportDefendantsApplication.yesRadioOption,
    });
    await performValidation('mainHeader', uploadDocumentsToSupportDefendantsApplication.mainHeader);
    await performAction('uploadFilesGenApps', {
      documents: [
        {type: uploadDocumentsToSupportDefendantsApplication.witnessStatementDropDownInput, fileName: 'genApps.xlsx'},
      ]
    });
        await performAction('selectLanguageUsedToComplete', {
      question: whichLanguageDidYouUseToCompleteThisService.whichLanguageDidYouUseQuestion,
      option: whichLanguageDidYouUseToCompleteThisService.englishRadioOption,
    });
    await performValidation('mainHeader', statementOfTruth.mainHeader);
    await performAction('selectStatementOfTruth', {
      question: statementOfTruth.completedByTheDefendantsLegalParagraph,
      option: statementOfTruth.theDefendantBelievesCheckBox,
      label1: statementOfTruth.fullNameTextLabel,
      input1: statementOfTruth.fullNameTextInput,
      label2: statementOfTruth.nameOfFirmTextLabel,
      input2: statementOfTruth.nameOfFirmTextInput,
      label3: statementOfTruth.positionOrOfficeHeldTextLabel,
      input3: statementOfTruth.positionOrOfficeHeldTextInput,
    });
    await performValidation('mainHeader', checkYourAnswersGenApps.mainHeader);
    await performAction('retrieveCYATableData', { name: 'check your answers table' });
    await performAction('validateCYA');
    await performAction('clickButton', checkYourAnswersGenApps.submitButton);
    await performAction('clickButton', confirmGenApps.closeAndReturnToCaseDetailsButton);
    await performValidation('bannerAlert', 'Case #.* has been updated with event: Make an application');
  });


test('Select an Application - Ask to Adjourn journey - Court hearing 14 days[No]', async () => {
  await performAction('select', caseSummary.nextStepEventList, caseSummary.makeAnApplication);
  await performAction('clickButton', caseSummary.go);
  await performAction('chooseAnApplication', {
    question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
    option: chooseAnApplication.adjournTheHearingRadioOption,
  });
  await performValidation('mainHeader', askToAdjournTheCourtHearing.mainHeader);
  await performAction('clickButton', askToAdjournTheCourtHearing.continueButton);
  await performValidation('mainHeader', selectParty.mainHeader);
  await performAction('selectApplicant', {
    question: selectParty.partyMakingApplicationQuestion,
    option: defendantDetails[0],
  });
  await performAction('confirmIfCourtHearingInNext14Days', {
    question: isTheCourtHearingInTheNext14Days.isTheCourtHearingInTheNext14DaysQuestion,
    option: isTheCourtHearingInTheNext14Days.noRadioOption,
  });
  await performValidation('mainHeader',hasTheDefendantAskedTheOtherPartiesAgreedToThisApplication.mainHeader);
  await performAction('confirmOtherPartiesAgreed', {
    question: hasTheDefendantAskedTheOtherPartiesAgreedToThisApplication.haveTheOtherPartiesAgreedQuestion,
    option: hasTheDefendantAskedTheOtherPartiesAgreedToThisApplication.noRadioOption,
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
    question: doYouWantToUploadDocumentsToSupportDefendantsApplication.doYouWantToUploadDocumentQuestion,
    option: doYouWantToUploadDocumentsToSupportDefendantsApplication.noRadioOption,
  });
  await performAction('selectLanguageUsedToComplete', {
    question: whichLanguageDidYouUseToCompleteThisService.whichLanguageDidYouUseQuestion,
    option: whichLanguageDidYouUseToCompleteThisService.welshRadioOption,
  });
  await performValidation('mainHeader', statementOfTruth.mainHeader);
  await performAction('selectStatementOfTruth', {
    question: statementOfTruth.completedByTheDefendantsLegalParagraph,
    option: statementOfTruth.theDefendantBelievesCheckBox,
    label1: statementOfTruth.fullNameTextLabel,
    input1: statementOfTruth.fullNameTextInput,
    label2: statementOfTruth.nameOfFirmTextLabel,
    input2: statementOfTruth.nameOfFirmTextInput,
    label3: statementOfTruth.positionOrOfficeHeldTextLabel,
    input3: statementOfTruth.positionOrOfficeHeldTextInput,
  });
  await performValidation('mainHeader', checkYourAnswersGenApps.mainHeader);
  await performAction('retrieveCYATableData', { name: 'check your answers table' });
  await performAction('validateCYA');
  await performAction('clickButton', checkYourAnswersGenApps.submitButton);
  await performAction('clickButton', confirmGenApps.closeAndReturnToCaseDetailsButton);
  await performValidation('bannerAlert', 'Case #.* has been updated with event: Make an application');
});
});
