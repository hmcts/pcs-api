import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import { caseSummary } from '@data/page-data';
import {
  yourHCEO,
  theNICEWillChoose,
  youCannotApplyForWrit,
  checkYourAnswers
} from '@data/page-data/page-data-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { changeNameAddress, confirmHCEOfficer, enforcementApplication, landRegistryFees, languageUsed, legalCosts, moneyOwed, nameAndAddressForEviction, repayments, statementOfTruth } from '@data/page-data-figma/page-data-enforcement-figma';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  defendantDetails.length = 0;
  moneyMap.clear();
  fieldsMap.clear();

  if (testInfo.title.includes('@noDefendants')) {
    await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayloadNoDefendants() });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayloadNoDefendants().defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayloadNoDefendants().addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayloadNoDefendants()
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
    await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload() });
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitCaseApiData.submitCasePayload().defendant1.nameKnown,
      additionalDefendants: submitCaseApiData.submitCasePayload().addAnotherDefendant,
      payLoad: submitCaseApiData.submitCasePayload()
    });
  }
  await performAction('updatePaymentAPI');
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
test.describe('[Enforcement - Writ of Possession]', async () => {
  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [Yes] - Repayment [SOME] @enforcement',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('errorValidationYourApplicationPage', enforcementApplication.errorValidation);
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.writOfPossessionRadioOption,
        question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
        question2: enforcementApplication.genAppSuccessfulQuestionHidden,
        option1: enforcementApplication.yesRadioOptionHidden,
        nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('errorValidationNameAndAddressForEvictionPage', nameAndAddressForEviction.errorValidation);
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload().defendant1.nameKnown,
        nextPage: confirmHCEOfficer.mainHeader
      });
      await performAction('errorValidationConfirmHCEOHiredPage', confirmHCEOfficer.errorValidation);
      await performAction('selectHaveHiredHCEO', {
        question: confirmHCEOfficer.haveYouHiredHCEOQuestion,
        option: confirmHCEOfficer.yesRadioOption,
        nextPage: yourHCEO.mainHeader
      });
      await performValidation('mainHeader', yourHCEO.mainHeader);
      await performAction('errorValidationYourHCEOPage', yourHCEO.errorValidation);
      await performAction('nameYourHCEO', {
        label: yourHCEO.nameOfYourHCEOTextLabel,
        input: yourHCEO.nameOfYourHCEOTextInput,
        nextPage: languageUsed.mainHeader
      });
      await performAction('errorValidationLanguageUsedPage', languageUsed.errorValidation);
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.englishRadioOption,
        nextPage: statementOfTruth.mainHeader
      });
      await performAction('errorValidationSOTWritPage', statementOfTruth.errorValidation);
      await performAction('selectStatementOfTruthWrit', {
        question: statementOfTruth.completedByParagraph,
        option: statementOfTruth.claimantRadioOption,
        option1: statementOfTruth.iCertifyCheckboxDynamic,
        label: statementOfTruth.fullNameHiddenTextLabel,
        input: statementOfTruth.fullNameHiddenTextInput,
        label1: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
        input1: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
        nextPage: checkYourAnswers.mainHeader,
        iBelieveCheckBox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
      });
    });

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [No] - Repayment [ALL] @enforcement', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', enforcementApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: enforcementApplication.writOfPossessionRadioOption,
      type: enforcementApplication.summaryWritOrWarrantLink,
      label1: enforcementApplication.warrantFeeValidationLabelHidden,
      text1: enforcementApplication.warrantFeeValidationTextHidden,
      label2: enforcementApplication.writFeeValidationLabelHidden,
      text2: enforcementApplication.writFeeValidationTextHidden
    });
    await performAction('selectApplicationType', {
      question: enforcementApplication.typeOfApplicationQuestion,
      option: enforcementApplication.writOfPossessionRadioOption,
      question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
      question2: enforcementApplication.genAppSuccessfulQuestionHidden,
      option1: enforcementApplication.yesRadioOptionHidden,
      nextPage: nameAndAddressForEviction.mainHeader
    });
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload().defendant1.nameKnown,
      nextPage: confirmHCEOfficer.mainHeader
    });
    await performAction('selectHaveHiredHCEO', {
      question: confirmHCEOfficer.haveYouHiredHCEOQuestion,
      option: confirmHCEOfficer.noRadioOption,
      nextPage: theNICEWillChoose.mainHeader
    });
    await performAction('clickButton', theNICEWillChoose.continueButton);
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.welshRadioOption,
      nextPage: statementOfTruth.mainHeader
    });
    await performAction('selectStatementOfTruthWrit', {
      question: statementOfTruth.completedByParagraph,
      option: statementOfTruth.claimantLegalRepresentativeRadioOption,
      option1: statementOfTruth.iCertifyCheckboxDynamic,
      label: statementOfTruth.fullNameHiddenTextLabel,
      input: statementOfTruth.fullNameHiddenTextInput,
      label1: statementOfTruth.nameOfFirmHiddenTextLabel,
      input1: statementOfTruth.nameOfFirmHiddenTextInput,
      label2: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
      input2: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
      nextPage: checkYourAnswers.mainHeader,
      claimantBelieveCheckBox: statementOfTruth.theClaimantBelievesHiddenCheckBox
    });
  });

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [No] - Repayment [None] @enforcement', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', enforcementApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: enforcementApplication.writOfPossessionRadioOption,
      type: enforcementApplication.summaryWritOrWarrantLink,
      label1: enforcementApplication.warrantFeeValidationLabelHidden,
      text1: enforcementApplication.warrantFeeValidationTextHidden,
      label2: enforcementApplication.writFeeValidationLabelHidden,
      text2: enforcementApplication.writFeeValidationTextHidden
    });
    await performAction('selectApplicationType', {
      question: enforcementApplication.typeOfApplicationQuestion,
      option: enforcementApplication.writOfPossessionRadioOption,
      question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
      question2: enforcementApplication.genAppSuccessfulQuestionHidden,
      option1: enforcementApplication.yesRadioOptionHidden,
      nextPage: nameAndAddressForEviction.mainHeader
    });
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload().defendant1.nameKnown,
      nextPage: confirmHCEOfficer.mainHeader
    });
    await performAction('selectHaveHiredHCEO', {
      question: confirmHCEOfficer.haveYouHiredHCEOQuestion,
      option: confirmHCEOfficer.noRadioOption,
      nextPage: theNICEWillChoose.mainHeader
    });
    await performAction('clickButton', theNICEWillChoose.continueButton);
    await performValidation('mainHeader', languageUsed.mainHeader);
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.englishAndWelshRadioOption,
      nextPage: statementOfTruth.mainHeader
    });
    await performAction('selectStatementOfTruthWrit', {
      question: statementOfTruth.completedByParagraph,
      option: statementOfTruth.claimantRadioOption,
      option1: statementOfTruth.iCertifyCheckboxDynamic,
      label: statementOfTruth.fullNameHiddenTextLabel,
      input: statementOfTruth.fullNameHiddenTextInput,
      label1: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
      input1: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
      nextPage: checkYourAnswers.mainHeader,
      iBelieveCheckBox: statementOfTruth.iBelieveTheFactsHiddenCheckbox,
    });
  });

  test('Writ - Apply for a Writ of Possession - Claim sent to High Court [No] @enforcement', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', enforcementApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: enforcementApplication.typeOfApplicationQuestion,
      option: enforcementApplication.writOfPossessionRadioOption,
      question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
      question2: enforcementApplication.genAppSuccessfulQuestionHidden,
      option1: enforcementApplication.noRadioOptionHidden,
      nextPage: youCannotApplyForWrit.mainHeader
    });
    await performAction('confirmClaimTransferredToHighCourt', {
      question: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
      option: enforcementApplication.noRadioOptionHidden,
      nextPage: youCannotApplyForWrit.mainHeader
    });
    await performAction('clickButton', youCannotApplyForWrit.continueButton);
    await performValidation('errorMessage', { header: youCannotApplyForWrit.errorMessageHeader, message: youCannotApplyForWrit.errMessage });
  });

  test('Writ - Apply for a Writ of Possession [General application journey]', {
    annotation: {
      type: 'issue',
      description: 'General application journey is a placeholder for now,this test will be fully etched out when this is ready to be developed',
    },
  },
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', enforcementApplication.mainHeader);
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.writOfPossessionRadioOption,
        question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
        question2: enforcementApplication.genAppSuccessfulQuestionHidden,
        option1: enforcementApplication.yesRadioOptionHidden,
        nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.noRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload().defendant1.nameKnown,
        nextPage: changeNameAddress.mainHeader
      });
      await performValidation('mainHeader', changeNameAddress.mainHeader);
      await performAction('clickButton', changeNameAddress.continueButton);
      await performValidation('errorMessage', { header: changeNameAddress.eventCouldNotBeCreatedErrorMessageHeader, message: changeNameAddress.errMessageDynamic });
    });
});
