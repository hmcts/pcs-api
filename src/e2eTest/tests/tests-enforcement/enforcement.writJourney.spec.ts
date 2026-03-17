import { expect, test } from '@utils/test-fixtures';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import { caseSummary } from '@data/page-data';
import {
  nameAndAddressForEviction,
  youNeedPermission,
  yourApplication,
  confirmHCEOHired,
  yourHCEO,
  theNICEWillChoose,
  landRegistryFees,
  legalCosts,
  moneyOwed,
  rePayments,
  statementOfTruthOne,
  languageUsed,
  youCannotApplyForWrit,
  checkYourAnswers
} from '@data/page-data/page-data-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
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
  PageContentValidation.finaliseTest();
});

test.describe('[Enforcement - Writ of Possession]', async () => {
  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [Yes] - Repayment [SOME] @enforcement @PR @regression',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', yourApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: yourApplication.typeOfApplicationOptions.writOfPossession,
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
      await performAction('errorValidationYourApplicationPage', yourApplication.errorValidation);
      await performAction('selectApplicationType', {
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.writOfPossession,
        question1: yourApplication.claimTransferredToHighCourtQuestion,
        question2: yourApplication.genAppSuccessfulQuestion,
        option1: yourApplication.yesRadioOption,
        nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('errorValidationNameAndAddressForEvictionPage', nameAndAddressForEviction.errorValidation);
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
        nextPage: confirmHCEOHired.mainHeader
      });
      await performAction('errorValidationConfirmHCEOHiredPage', confirmHCEOHired.errorValidation);
      await performAction('selectHaveHiredHCEO', {
        question: confirmHCEOHired.haveYouHiredHCEOQuestion,
        option: confirmHCEOHired.yesRadioOption,
        nextPage: yourHCEO.mainHeader
      });
      await performValidation('mainHeader', yourHCEO.mainHeader);
      await performAction('errorValidationYourHCEOPage', yourHCEO.errorValidation);
      await performAction('nameYourHCEO', {
        label: yourHCEO.nameOfYourHCEOTextLabel,
        input: yourHCEO.nameOfYourHCEOTextInput,
        nextPage: moneyOwed.mainHeader
      });
      await performAction('errorValidationMoneyOwedPage', moneyOwed.errorValidation);
      await performAction('provideMoneyOwed', {
        label: moneyOwed.totalAmountOwedTextLabel,
        input: moneyOwed.totalAmountOwedTextInput,
        nextPage: legalCosts.mainHeader
      });
      await performAction('errorValidationLegalCostsPage', legalCosts.errorValidation);
      await performAction('provideLegalCosts', {
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabel,
        input: legalCosts.howMuchYouWantToReclaimTextInput,
        nextPage: landRegistryFees.mainHeader
      });
      await performAction('errorValidationLandRegistryFeePage', landRegistryFees.errorValidation);
      await performAction('provideLandRegistryFees', {
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
        input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
        nextPage: rePayments.mainHeader
      });
      await performValidation('mainHeader', rePayments.mainHeader);
      await performAction('validateAmountToRePayTable', { headerName: rePayments.mainHeader });
      await performAction('errorValidationRepaymentsPage', rePayments.errorValidation);
      await performAction('provideAmountToRePay', {
        question: rePayments.rePaymentQuestion,
        option: rePayments.rePaymentRadioOptions.some,
        label: rePayments.enterTheAmountTextLabel,
        input: rePayments.enterTheAmountTextInput,
        nextPage: languageUsed.mainHeader
      });
      await performAction('errorValidationLanguageUsedPage', languageUsed.errorValidation);
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.languageUsedRadioOptions.englishRadioOption,
        nextPage: statementOfTruthOne.mainHeader
      });
      await performAction('errorValidationSOT1Page', statementOfTruthOne.errorValidation);
      await performAction('validateAmountToRePayTable', { headerName: statementOfTruthOne.mainHeader });
      await performAction('selectStatementOfTruthWrit', {
        question: statementOfTruthOne.completedByLabel,
        option: statementOfTruthOne.claimantRadioOption,
        option1: statementOfTruthOne.iBelieveTheFactsHiddenCheckbox,
        label: statementOfTruthOne.fullNameHiddenTextLabel,
        input: statementOfTruthOne.fullNameHiddenTextInput,
        label1: statementOfTruthOne.positionOrOfficeHeldHiddenTextLabel,
        input1: statementOfTruthOne.positionOrOfficeHeldHiddenTextInput,
        label2: statementOfTruthOne.nameOfFirmHiddenTextLabel,
        input2: statementOfTruthOne.nameOfFirmHiddenTextInput,
        nextPage: checkYourAnswers.mainHeader
      });
    });

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [No] - Repayment [ALL] @enforcement @PR @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', yourApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: yourApplication.typeOfApplicationOptions.writOfPossession,
      type: yourApplication.summaryWritOrWarrant,
      label1: yourApplication.warrantFeeValidationLabel,
      text1: yourApplication.warrantFeeValidationText,
      label2: yourApplication.writFeeValidationLabel,
      text2: yourApplication.writFeeValidationText
    });
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.writOfPossession,
      question1: yourApplication.claimTransferredToHighCourtQuestion,
      question2: yourApplication.genAppSuccessfulQuestion,
      option1: yourApplication.yesRadioOption,
      nextPage: nameAndAddressForEviction.mainHeader
    });
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      nextPage: confirmHCEOHired.mainHeader
    });
    await performAction('selectHaveHiredHCEO', {
      question: confirmHCEOHired.haveYouHiredHCEOQuestion,
      option: confirmHCEOHired.noRadioOption,
      nextPage: theNICEWillChoose.mainHeader
    });
    await performAction('clickButton', theNICEWillChoose.continueButton);
    await performValidation('mainHeader', moneyOwed.mainHeader);
    await performAction('errorValidationMoneyOwedPage', moneyOwed.errorValidation);
    await performAction('provideMoneyOwed', {
      label: moneyOwed.totalAmountOwedTextLabel,
      input: moneyOwed.totalAmountOwedTextInput,
      nextPage: legalCosts.mainHeader
    });
    await performAction('provideLegalCosts', {
      question: legalCosts.reclaimLegalCostsQuestion,
      option: legalCosts.noRadioOption,
      label: legalCosts.howMuchYouWantToReclaimTextLabel,
      input: legalCosts.howMuchYouWantToReclaimTextInput,
      nextPage: landRegistryFees.mainHeader
    });
    await performAction('provideLandRegistryFees', {
      question: landRegistryFees.landRegistryFeeQuestion,
      option: landRegistryFees.noRadioOption,
      label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
      input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
      nextPage: rePayments.mainHeader
    });
    await performValidation('mainHeader', rePayments.mainHeader);
    await performAction('validateAmountToRePayTable', { headerName: rePayments.mainHeader });
    await performAction('provideAmountToRePay', {
      question: rePayments.rePaymentQuestion,
      option: rePayments.rePaymentRadioOptions.all,
      label: rePayments.enterTheAmountTextLabel,
      input: rePayments.enterTheAmountTextInput,
      nextPage: languageUsed.mainHeader
    });
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.languageUsedRadioOptions.welshRadioOption,
      nextPage: statementOfTruthOne.mainHeader
    });
    await performAction('errorValidationSOT1Page', statementOfTruthOne.errorValidation);
    await performAction('validateAmountToRePayTable', { headerName: statementOfTruthOne.mainHeader });
    await performAction('selectStatementOfTruthWrit', {
      question: statementOfTruthOne.completedByLabel,
      option: statementOfTruthOne.claimantLegalRepresentativeRadioOption,
      option1: statementOfTruthOne.signThisStatementHiddenCheckbox,
      label: statementOfTruthOne.fullNameHiddenTextLabel,
      input: statementOfTruthOne.fullNameHiddenTextInput,
      label1: statementOfTruthOne.nameOfFirmHiddenTextLabel,
      input1: statementOfTruthOne.nameOfFirmHiddenTextInput,
      label2: statementOfTruthOne.positionOrOfficeHeldHiddenTextLabel,
      input2: statementOfTruthOne.positionOrOfficeHeldHiddenTextInput,
      nextPage: checkYourAnswers.mainHeader
    });
  });

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [No] - Repayment [None] @enforcement ', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', yourApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: yourApplication.typeOfApplicationOptions.writOfPossession,
      type: yourApplication.summaryWritOrWarrant,
      label1: yourApplication.warrantFeeValidationLabel,
      text1: yourApplication.warrantFeeValidationText,
      label2: yourApplication.writFeeValidationLabel,
      text2: yourApplication.writFeeValidationText
    });
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.writOfPossession,
      question1: yourApplication.claimTransferredToHighCourtQuestion,
      question2: yourApplication.genAppSuccessfulQuestion,
      option1: yourApplication.yesRadioOption,
      nextPage: nameAndAddressForEviction.mainHeader
    });
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      nextPage: confirmHCEOHired.mainHeader
    });
    await performAction('selectHaveHiredHCEO', {
      question: confirmHCEOHired.haveYouHiredHCEOQuestion,
      option: confirmHCEOHired.noRadioOption,
      nextPage: theNICEWillChoose.mainHeader
    });
    await performAction('clickButton', theNICEWillChoose.continueButton);
    await performValidation('mainHeader', moneyOwed.mainHeader);
    await performAction('provideMoneyOwed', {
      label: moneyOwed.totalAmountOwedTextLabel,
      input: moneyOwed.totalAmountOwedTextInput,
      nextPage: legalCosts.mainHeader
    });
    await performValidation('mainHeader', legalCosts.mainHeader);
    await performAction('provideLegalCosts', {
      question: legalCosts.reclaimLegalCostsQuestion,
      option: legalCosts.yesRadioOption,
      label: legalCosts.howMuchYouWantToReclaimTextLabel,
      input: legalCosts.howMuchYouWantToReclaimTextInput,
      nextPage: landRegistryFees.mainHeader
    });
    await performAction('provideLandRegistryFees', {
      question: landRegistryFees.landRegistryFeeQuestion,
      option: landRegistryFees.noRadioOption,
      label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
      input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
      nextPage: rePayments.mainHeader
    });
    await performValidation('mainHeader', rePayments.mainHeader);
    await performAction('validateAmountToRePayTable', { headerName: rePayments.mainHeader });
    await performAction('provideAmountToRePay', {
      question: rePayments.rePaymentQuestion,
      option: rePayments.rePaymentRadioOptions.none,
      label: rePayments.enterTheAmountTextLabel,
      input: rePayments.enterTheAmountTextInput,
      nextPage: languageUsed.mainHeader
    });
    await performValidation('mainHeader', languageUsed.mainHeader);
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.languageUsedRadioOptions.englishAndWelshRadioOption,
      nextPage: statementOfTruthOne.mainHeader
    });
    await performAction('validateAmountToRePayTable', { headerName: statementOfTruthOne.mainHeader });
    await performAction('selectStatementOfTruthWrit', {
      question: statementOfTruthOne.completedByLabel,
      option: statementOfTruthOne.claimantRadioOption,
      option1: statementOfTruthOne.iBelieveTheFactsHiddenCheckbox,
      label: statementOfTruthOne.fullNameHiddenTextLabel,
      input: statementOfTruthOne.fullNameHiddenTextInput,
      label1: statementOfTruthOne.positionOrOfficeHeldHiddenTextLabel,
      input1: statementOfTruthOne.positionOrOfficeHeldHiddenTextInput,
      label2: statementOfTruthOne.nameOfFirmHiddenTextLabel,
      input2: statementOfTruthOne.nameOfFirmHiddenTextInput,
      nextPage: checkYourAnswers.mainHeader
    });
  });

  test('Writ - Apply for a Writ of Possession - Claim sent to High Court [No] @enforcement @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.writOfPossession,
      question1: yourApplication.claimTransferredToHighCourtQuestion,
      question2: yourApplication.genAppSuccessfulQuestion,
      option1: yourApplication.noRadioOption,
      nextPage: youCannotApplyForWrit.mainHeader
    });
    await performAction('confirmClaimTransferredToHighCourt', {
      question: yourApplication.claimTransferredToHighCourtQuestion,
      option: yourApplication.noRadioOption,
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
      await performValidation('mainHeader', yourApplication.mainHeader);
      await performAction('selectApplicationType', {
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.writOfPossession,
        question1: yourApplication.claimTransferredToHighCourtQuestion,
        question2: yourApplication.genAppSuccessfulQuestion,
        option1: yourApplication.yesRadioOption,
        nextPage: nameAndAddressForEviction.mainHeader
      });
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.noRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
        nextPage: youNeedPermission.mainHeader
      });
      await performValidation('mainHeader', youNeedPermission.mainHeader);
      await performAction('clickButton', youNeedPermission.continueButton);
      await performValidation('errorMessage', { header: youNeedPermission.eventCouldNotBeCreatedErrorMessage, message: youNeedPermission.errMessage });
    });
});
