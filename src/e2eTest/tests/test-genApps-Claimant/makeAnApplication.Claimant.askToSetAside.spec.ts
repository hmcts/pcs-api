import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { initializeExecutor } from '@utils/controller';
import test, { expect } from '@playwright/test';
import { initializeGenAppsExecutor, performAction, performValidation } from '@utils/controller-genApps';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { caseInfo } from '@utils/actions/custom-actions';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import {
  chooseAnApplication,
  haveYouAskedTheOtherPartiesAgreedToThisApplication,
  whatOrderDoYouWantTheCourtToMakeAndWhy,
  doYouWantToUploadDocumentsToSupportYourApplication,
  uploadDocumentsToSupportYourApplication,
  whichLanguageDidYouUseToCompleteThisService, checkYourAnswersClaimantGenApps, statementOfTruthClaimant,
  serviceRequestGenApps, askTheCourtToSetAsideTheOrder,
} from "@data/page-data-figma/page-data-genApps-claimant-figma";

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeGenAppsExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadCaseFileView });
  await performAction('updatePaymentAPI');
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('navigateToSummaryPage');
});

test.afterEach(async () => {
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
  PageContentValidation.finaliseTest();

});


test.describe('Claimant - Make an Application - e2e Journey @nightly @regression', async () => {
  test('Claimant - Select an Application - Ask to Set aside', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.makeAnApplication);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', chooseAnApplication.mainHeader);
    await performAction('chooseAnApplication', {
      question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
      option: chooseAnApplication.setAsideRadioOption,
    });
    await performValidation('mainHeader', askTheCourtToSetAsideTheOrder.mainHeader);
    await performAction('clickButton', askTheCourtToSetAsideTheOrder.continueButton);
    await performValidation('mainHeader', haveYouAskedTheOtherPartiesAgreedToThisApplication.mainHeader);
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
        {type: uploadDocumentsToSupportYourApplication.tenancyAgreementDropDownInput, fileName: 'genApps.docx'},
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
    await performAction('retrieveCYATableData', {name: 'check your answers table'});
    await performAction('validateCYA');
    await performAction('clickButton', checkYourAnswersClaimantGenApps.submitButton);
    await performAction('payClaimFeeGenApps', {clickLink: true});
    await performAction('clickPayNowLinkGenApps');
    await performAction('selectPaymentOptions', {
      amountLabel: serviceRequestGenApps.amountToPayLabel,
      payByOption: serviceRequestGenApps.payByAccountRadioOption,
      expectedAmount: serviceRequestGenApps.amount126,
      pbaLabel: serviceRequestGenApps.selectPBALabel,
      pbaValue: serviceRequestGenApps.pbaIndex1,
      referenceLabel: serviceRequestGenApps.pbaReferenceLabel,
      referenceText: serviceRequestGenApps.pbaReferenceInputText,
      button: serviceRequestGenApps.confirmPaymentButton,
    });
    await performValidation('mainHeader', serviceRequestGenApps.paymentSuccessMainHeader);
  });
  });

