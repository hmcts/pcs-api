import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import { caseSummary } from '@data/page-data';
import {
  evidenceUpload,
  provideEvidence,
  warrantOfRestitutionAnyoneAtPropertyRiskIntro,
} from '@data/page-data/page-data-enforcement';
import { createCaseApiData, enforceWarrantApiData, submitCaseApiData } from '@data/api-data';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { additionalInformation, enforcementApplication, evictionRisksPosed, livingInTheProperty, peopleYouWantToEvict, propertyAccessDetails, shareEvidenceWithJudge, vulnerableAdultsChildren } from '@data/page-data-figma/page-data-enforcement-figma';
import { EnforcementCommonUtils } from '@utils/actions/element-actions/enforcementUtils.action';
import { explainHowDefendantsReturned } from '@data/page-data-figma/page-data-enforcement-figma/explainHowDefendantsReturned.page.data';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';

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
    if (testInfo.title.includes('@allYES')) {
      await performAction('enforceCaseAPI', { data: enforceWarrantApiData.enforceCasePayloadYesJourney });
    } else if (testInfo.title.includes('@allNO')) {
      await performAction('enforceCaseAPI', { data: enforceWarrantApiData.enforceCasePayloadNoJourney });
    }
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
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  }
    PageContentValidation.finaliseTest();
});
// Skipping this test case as the feature is not part of Release 1 to save execution time.
test.describe.skip('[Enforcement - Warrant of Restitution]', async () => {
  test('Warrant - Apply for a Warrant of Restitution - Warrant with all YES selection - no update on prepopulated data ,upload more than one evidence @allYES @enforcement @PR',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: enforcementApplication.warrantOfRestitutionRadioOptionDynamic,
        type: enforcementApplication.summaryWritOrWarrantLink,
        label1: enforcementApplication.warrantFeeValidationLabelHidden,
        text1: enforcementApplication.warrantFeeValidationTextHidden,
        label2: enforcementApplication.writFeeValidationLabelHidden,
        text2: enforcementApplication.writFeeValidationTextHidden
      });
      await performAction('validateGetQuoteFromBailiffLink', {
        type: enforcementApplication.summaryWritOrWarrantLink,
        link: enforcementApplication.quoteFromBailiffLinkHidden,
        newPage: enforcementApplication.hceoPageTitleHidden
      });
      await performAction('expandSummary', enforcementApplication.summarySaveApplicationLink);
      await performAction('errorValidationYourApplicationPage', enforcementApplication.errorValidation);
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfRestitutionRadioOptionDynamic,
        nextPage: peopleYouWantToEvict.mainHeaderWarrantOfRestitutionDynamic
      });
      await performAction('reTryOnCallBackError', peopleYouWantToEvict.continueButton, shareEvidenceWithJudge.mainHeader);
      await performAction('reTryOnCallBackError', shareEvidenceWithJudge.continueButton, explainHowDefendantsReturned.mainHeader);
      await performAction('errorValidationHowDefendantsEnteredPage', explainHowDefendantsReturned.errorValidation);
      await performAction('provideHowDefendantReturnToProperty', {
        label: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextLabel,
        input: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextInput,
        nextPage: provideEvidence.mainHeader
      });
      await performAction('errorValidationExplainHowDefendantsEnteredPage', evidenceUpload.errorValidation);
      await performAction('uploadEvidenceThatDefendantsAreAtProperty', {
        documents: [
          { type: evidenceUpload.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
          { type: evidenceUpload.photoGraphicEvidenceDropDownInput, fileName: 'photographicEvidence.pdf', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
          { type: evidenceUpload.otherDocumentDropDownInput, fileName: 'otherDocument.pdf', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
          { type: evidenceUpload.policeReportDropDownInput, fileName: 'tenancyLicence.docx', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
        ],
        nextPage: warrantOfRestitutionAnyoneAtPropertyRiskIntro.mainHeader
      });
      await performAction('reTryOnCallBackError', warrantOfRestitutionAnyoneAtPropertyRiskIntro.continueButton, livingInTheProperty.mainHeader);
      await performAction('validatePrePopulatedData', {
        testPage: livingInTheProperty.mainHeader,
        inputData:
          [
            { type: 'radio', inputRadioQuestion: livingInTheProperty.riskToBailiffQuestion, expectedAnswer: enforceWarrantApiData.enforceCasePayloadYesJourney.warrantAnyRiskToBailiff },
          ]
      });
      await performAction('reTryOnCallBackError', livingInTheProperty.continueButton, evictionRisksPosed.mainHeader);
      await performAction('validatePrePopulatedData', {
        testPage: evictionRisksPosed.mainHeader,
        inputData:
          [
            {
              type: 'checkBox', inputCheckBoxQuestion: evictionRisksPosed.kindOfRiskQuestion, expectedAnswer: EnforcementCommonUtils.mapRiskPosedPayLoadWithUI(enforceWarrantApiData.enforceCasePayloadYesJourney.warrantEnforcementRiskCategories)
            },
          ]
      });
      await performAction('reTryOnCallBackError', evictionRisksPosed.continueButton, vulnerableAdultsChildren.mainHeader);
      await performAction('validatePrePopulatedData', {
        testPage: vulnerableAdultsChildren.mainHeader,
        inputData:
          [
            {
              type: 'radio', inputRadioQuestion: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion, expectedAnswer: enforceWarrantApiData.enforceCasePayloadYesJourney.vulnerablePeoplePresent
            },
            {
              type: 'radio', inputRadioQuestion: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion, expectedAnswer: EnforcementCommonUtils.formatPayLoadData(enforceWarrantApiData.enforceCasePayloadYesJourney.vulnerableAdultsChildren.vulnerableCategory)
            },
            {
              type: 'inputText', inputTextLabel: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel, expectedAnswer: enforceWarrantApiData.enforceCasePayloadYesJourney.vulnerableAdultsChildren.vulnerableReasonText
            }
          ]
      });
      await performAction('reTryOnCallBackError', vulnerableAdultsChildren.continueButton, propertyAccessDetails.mainHeader);
      await performAction('validatePrePopulatedData', {
        testPage: propertyAccessDetails.mainHeader,
        inputData:
          [
            {
              type: 'radio', inputRadioQuestion: propertyAccessDetails.accessToThePropertyQuestion, expectedAnswer: enforceWarrantApiData.enforceCasePayloadYesJourney.warrantIsDifficultToAccessProperty
            },
            {
              type: 'inputText', inputTextLabel: propertyAccessDetails.whyItsDifficultToAccessToThePropertyTextLabelHidden, expectedAnswer: enforceWarrantApiData.enforceCasePayloadYesJourney.warrantClarificationOnAccessDifficultyText
            }
          ]
      });
      await performAction('reTryOnCallBackError', propertyAccessDetails.continueButton, additionalInformation.mainHeader);

    });

  test('Warrant - Apply for a Warrant of Restitution - Warrant with all NO selection - no update on prepopulated data  @allNO @enforcement @PR',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: enforcementApplication.warrantOfRestitutionRadioOptionDynamic,
        type: enforcementApplication.summaryWritOrWarrantLink,
        label1: enforcementApplication.warrantFeeValidationLabelHidden,
        text1: enforcementApplication.warrantFeeValidationTextHidden,
        label2: enforcementApplication.writFeeValidationLabelHidden,
        text2: enforcementApplication.writFeeValidationTextHidden
      });
      await performAction('validateGetQuoteFromBailiffLink', {
        type: enforcementApplication.summaryWritOrWarrantLink,
        link: enforcementApplication.quoteFromBailiffLinkHidden,
        newPage: enforcementApplication.hceoPageTitleHidden
      });
      await performAction('expandSummary', enforcementApplication.summarySaveApplicationLink);
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfRestitutionRadioOptionDynamic,
        nextPage: peopleYouWantToEvict.mainHeaderWarrantOfRestitutionDynamic
      });
      await performAction('reTryOnCallBackError', peopleYouWantToEvict.continueButton, shareEvidenceWithJudge.mainHeader);
      await performAction('reTryOnCallBackError', shareEvidenceWithJudge.continueButton, explainHowDefendantsReturned.mainHeader);
      await performAction('provideHowDefendantReturnToProperty', {
        label: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextLabel,
        input: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextInput,
        nextPage: provideEvidence.mainHeader
      });
      await performAction('uploadEvidenceThatDefendantsAreAtProperty', {
        documents: [
          { type: evidenceUpload.witnessStatementDropDownInput, fileName: 'witnessStatement.pdf', description: evidenceUpload.shortDescriptionHiddenTextInput, docType: evidenceUpload.typeOfDocumentHiddenTextLabel, label: evidenceUpload.shortDescriptionHiddenTextLabel },
        ],
        nextPage: warrantOfRestitutionAnyoneAtPropertyRiskIntro.mainHeader
      });
      await performAction('reTryOnCallBackError', warrantOfRestitutionAnyoneAtPropertyRiskIntro.continueButton, livingInTheProperty.mainHeader);
      await performAction('validatePrePopulatedData', {
        testPage: livingInTheProperty.mainHeader,
        inputData:
          [
            { type: 'radio', inputRadioQuestion: livingInTheProperty.riskToBailiffQuestion, expectedAnswer: enforceWarrantApiData.enforceCasePayloadNoJourney.warrantAnyRiskToBailiff },
          ]
      });
      await performAction('reTryOnCallBackError', livingInTheProperty.continueButton, vulnerableAdultsChildren.mainHeader);
      await performAction('validatePrePopulatedData', {
        testPage: vulnerableAdultsChildren.mainHeader,
        inputData:
          [
            {
              type: 'radio', inputRadioQuestion: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion, expectedAnswer: enforceWarrantApiData.enforceCasePayloadNoJourney.vulnerablePeoplePresent
            },
          ]
      });
      await performAction('reTryOnCallBackError', vulnerableAdultsChildren.continueButton, propertyAccessDetails.mainHeader);
      await performAction('validatePrePopulatedData', {
        testPage: propertyAccessDetails.mainHeader,
        inputData:
          [
            { type: 'radio', inputRadioQuestion: propertyAccessDetails.accessToThePropertyQuestion, expectedAnswer: enforceWarrantApiData.enforceCasePayloadNoJourney.warrantIsDifficultToAccessProperty },
          ]
      });
      await performAction('reTryOnCallBackError', propertyAccessDetails.continueButton, additionalInformation.mainHeader);
    });

});