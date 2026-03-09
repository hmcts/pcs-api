import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import { caseSummary } from '@data/page-data';
import {
  yourApplication,
  evidenceUpload,
  checkYourAnswers,
  explainHowDefendantsReturned,
  shareEvidenceWithJudge,
  provideEvidence,
} from '@data/page-data/page-data-enforcement';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  defendantDetails.length = 0;
  moneyMap.clear();
  fieldsMap.clear();
  if (testInfo.title.includes('@noDefendants')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadNoDefendants.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadNoDefendants.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadNoDefendants
    });
  } else if (testInfo.title.includes('@onlyMain')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadOnlyMain });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadOnlyMain.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadOnlyMain.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadOnlyMain
    });
  } else {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayload.addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayload
    });
  }
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  // Login and cookie consent are handled globally via storageState in global-setup.config.ts
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  defendantDetails.length = 0;
  moneyMap.clear();
  fieldsMap.clear();
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Enforcement - Warrant of Restitution]', async () => {
  test('Warrant - Apply for a Warrant of Restitution - upload more than one evidence @enforcement @PR',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', yourApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: yourApplication.typeOfApplicationOptions.warrantOfRestitution,
        type: yourApplication.summaryWritOrWarrant,
        label1: yourApplication.warrantFeeValidationLabel,
        text1: yourApplication.warrantFeeValidationText,
        label2: yourApplication.writFeeValidationLabel,
        text2: yourApplication.writFeeValidationText
      });
      await performAction('validateGetQuoteFromBailiffLink', {
        type: yourApplication.summaryWritOrWarrant,
        link: yourApplication.quoteFromBailiffLink,
        newPage: yourApplication.hceoPageTitle
      });
      await performAction('expandSummary', yourApplication.summarySaveApplication);
      await performAction('inputErrorValidation', {
        validationReq: yourApplication.errorValidation,
        validationType: yourApplication.errorValidationType.three,
        inputArray: yourApplication.errorValidationField.errorRadioOption,
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
        button: yourApplication.continueButton
      });
      await performAction('selectApplicationType', {
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.warrantOfRestitution,
        nextPage: shareEvidenceWithJudge.mainHeader
      });
      await performAction('reTryOnCallBackError', shareEvidenceWithJudge.continueButton, explainHowDefendantsReturned.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: explainHowDefendantsReturned.errorValidation,
        validationType: explainHowDefendantsReturned.errorValidationType.two,
        inputArray: explainHowDefendantsReturned.errorValidationField.errorTextField,
        header: explainHowDefendantsReturned.eventCouldNotBeCreatedErrorMessage,
        label: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextLabel,
        button: explainHowDefendantsReturned.continueButton
      });
      await performAction('provideHowDefendantReturnToProperty', {
        label: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextLabel,
        input: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextInput,
        nextPage: provideEvidence.mainHeader
      });
      await performAction('inputErrorValidation', {
        validationReq: evidenceUpload.errorValidation,
        validationType: evidenceUpload.errorValidationType.seven,
        inputArray: evidenceUpload.errorValidationField.errorAddDocument,
        button: evidenceUpload.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: evidenceUpload.errorValidation,
        validationType: evidenceUpload.errorValidationType.eight,
        inputArray: evidenceUpload.errorValidationField.errorDropDown,
        docType: evidenceUpload.typeOfDocumentHiddenTextLabel,
        type: evidenceUpload.witnessStatementDropDownInput,
        button: evidenceUpload.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: evidenceUpload.errorValidation,
        validationType: evidenceUpload.errorValidationType.nine,
        inputArray: evidenceUpload.errorValidationField.errorUpload,
        docType: evidenceUpload.typeOfDocumentHiddenTextLabel,
        type: evidenceUpload.witnessStatementDropDownInput,
        label: evidenceUpload.documentUploadHiddenTextLabel,
        button: evidenceUpload.continueButton
      });

      await performAction('inputErrorValidation', {
        validationReq: evidenceUpload.errorValidation,
        validationType: evidenceUpload.errorValidationType.two,
        inputArray: evidenceUpload.errorValidationField.errorTextField,
        header: evidenceUpload.thereIsAProblemErrorMessageHeader,
        label: evidenceUpload.shortDescriptionHiddenTextLabel,
        button: evidenceUpload.continueButton,
        buttonRemove: evidenceUpload.removeButton
      });
      await performAction('uploadEvidenceThatDefendantsAreAtProperty', {
        documents: [
          { type: evidenceUpload.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
          { type: evidenceUpload.photoGraphicEvidenceDropDownInput, fileName: 'photographicEvidence.pdf', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
          { type: evidenceUpload.otherDocumentDropDownInput, fileName: 'otherDocument.pdf', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
          { type: evidenceUpload.policeReportDropDownInput, fileName: 'tenancyLicence.docx', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
        ],
        nextPage: checkYourAnswers.mainHeader
      })
    });
});
