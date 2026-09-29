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
  chooseAnApplication,
  haveYouAskedTheOtherPartiesAgreedToThisApplication,
  whatOrderDoYouWantTheCourtToMakeAndWhy,
  doYouWantToUploadDocumentsToSupportYourApplication,
  uploadDocumentsToSupportYourApplication,
  whichLanguageDidYouUseToCompleteThisService, checkYourAnswersClaimantGenApps, statementOfTruthClaimant,
  serviceRequestGenApps, askTheCourtToMakeAnOrder, areThereAnyReasonsThatThisApplicationShouldNotBeShared,
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
  test('Claimant - Select an Application - Ask to Set aside', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.makeAnApplication);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', chooseAnApplication.mainHeader);
    await performAction('chooseAnApplication', {
      question: chooseAnApplication.whatDoYouWantToApplyForQuestion,
      option: chooseAnApplication.somethingElseRadioOption,
    });
    await performValidation('mainHeader', askTheCourtToMakeAnOrder.mainHeader);
    await performAction('clickButton', askTheCourtToMakeAnOrder.continueButton);
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
        {type: uploadDocumentsToSupportYourApplication.inspectionOrReportDropDownInput, fileName: 'genApps.docx'},
      ]
    });
    await performAction('selectLanguageUsedToComplete', {
      question: whichLanguageDidYouUseToCompleteThisService.whichLanguageDidYouUseQuestion,
      option: whichLanguageDidYouUseToCompleteThisService.englishAndWelshRadioOption,
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

