import { expect, test } from '@utils/test-fixtures';
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
  languageUsed,
  statementOfTruthOne,
  claimSentToHighCourt,
  youCannotApplyForWrit
} from '@data/page-data/page-data-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';

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
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${process.env.CHANGE_ID ? `PCS-${process.env.CHANGE_ID}` : 'PCS'}/${process.env.CASE_NUMBER}#Summary`);
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

test.describe('[Enforcement - Writ of Possession]', async () => {
  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [Yes] - Repayment [SOME] @PR @regression',
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
      await performAction('inputErrorValidation', {
        validationReq: yourApplication.errorValidation,
        validationType: yourApplication.errorValidationType.three,
        inputArray: yourApplication.errorValidationField.errorRadioOption,
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.writOfPossession,
        button: yourApplication.continueButton
      });
      await performAction('selectApplicationType', {
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.writOfPossession,
      });
      await performValidation('mainHeader', claimSentToHighCourt.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: claimSentToHighCourt.errorValidation,
        validationType: claimSentToHighCourt.errorValidationType.three,
        inputArray: claimSentToHighCourt.errorValidationField.errorRadioOption,
        question: claimSentToHighCourt.claimTransferredToHighCourtQuestion,
        option: claimSentToHighCourt.yesRadioOption,
        button: claimSentToHighCourt.continueButton
      });
      await performAction('confirmClaimTransferredToHighCourt', {
        question: claimSentToHighCourt.claimTransferredToHighCourtQuestion,
        option: claimSentToHighCourt.yesRadioOption,
      });
      await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: nameAndAddressForEviction.errorValidation,
        validationType: nameAndAddressForEviction.errorValidationType.three,
        inputArray: nameAndAddressForEviction.errorValidationField.errorRadioOption,
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        button: nameAndAddressForEviction.continueButton
      });
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      });
      await performValidation('mainHeader', confirmHCEOHired.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: confirmHCEOHired.errorValidation,
        validationType: confirmHCEOHired.errorValidationType.three,
        inputArray: confirmHCEOHired.errorValidationField.errorRadioOption,
        question: confirmHCEOHired.haveYouHiredHCEOQuestion,
        option: confirmHCEOHired.yesRadioOption,
        button: confirmHCEOHired.continueButton
      });
      await performAction('selectHaveHiredHCEO', {
        question: confirmHCEOHired.haveYouHiredHCEOQuestion,
        option: confirmHCEOHired.yesRadioOption,
      });
      await performValidation('mainHeader', yourHCEO.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: yourHCEO.errorValidation,
        validationType: yourHCEO.errorValidationType.two,
        inputArray: yourHCEO.errorValidationField.errorTextField,
        header: yourHCEO.errors,
        label: yourHCEO.nameOfYourHCEOLabel,
        button: yourHCEO.continueButton
      });
      await performAction('nameYourHCEO', {
        label: yourHCEO.nameOfYourHCEOLabel,
        input: yourHCEO.nameOfYourHCEOInput,
      });
      await performValidation('mainHeader', moneyOwed.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: moneyOwed.errorValidation,
        validationType: moneyOwed.errorValidationType.one,
        inputArray: moneyOwed.errorValidationField.errorMoneyField,
        label: moneyOwed.totalAmountOwedTextLabel,
        button: moneyOwed.continueButton
      });
      await performAction('provideMoneyOwed', {
        label: moneyOwed.totalAmountOwedTextLabel,
        input: moneyOwed.totalAmountOwedTextInput,
        nextPage: legalCosts.mainHeader
      });
      await performValidation('mainHeader', legalCosts.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: legalCosts.errorValidation,
        validationType: legalCosts.errorValidationType.three,
        inputArray: legalCosts.errorValidationField.errorRadioOption,
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        button: legalCosts.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: legalCosts.errorValidation,
        validationType: legalCosts.errorValidationType.five,
        inputArray: legalCosts.errorValidationField.errorMoneyField,
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        option2: legalCosts.noRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabel,
        button: legalCosts.continueButton
      });
      await performAction('provideLegalCosts', {
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabel,
        input: legalCosts.howMuchYouWantToReclaimTextInput,
        nextPage: landRegistryFees.mainHeader
      });
      await performAction('inputErrorValidation', {
        validationReq: landRegistryFees.errorValidation,
        validationType: landRegistryFees.errorValidationType.three,
        inputArray: landRegistryFees.errorValidationField.errorRadioOption,
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
        button: landRegistryFees.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: landRegistryFees.errorValidation,
        validationType: landRegistryFees.errorValidationType.five,
        inputArray: landRegistryFees.errorValidationField.errorMoneyField,
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
        option2: landRegistryFees.noRadioOption,
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
        button: landRegistryFees.continueButton
      });
      await performAction('provideLandRegistryFees', {
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
        label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
        input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
        nextPage: rePayments.mainHeader
      });
      await performValidation('mainHeader', rePayments.mainHeader);
      await performAction('validateAmountToRePayTable', { headerName: rePayments.mainHeader });
      await performAction('inputErrorValidation', {
        validationReq: rePayments.errorValidation,
        validationType: rePayments.errorValidationType.three,
        inputArray: rePayments.errorValidationField.errorRadioOption,
        question: rePayments.rePaymentQuestion,
        option: rePayments.rePaymentRadioOptions.some,
        button: rePayments.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: rePayments.errorValidation,
        validationType: rePayments.errorValidationType.five,
        inputArray: rePayments.errorValidationField.errorMoneyField,
        question: rePayments.rePaymentQuestion,
        option: rePayments.rePaymentRadioOptions.some,
        option2: rePayments.rePaymentRadioOptions.none,
        label: rePayments.enterTheAmountTextLabel,
        button: rePayments.continueButton
      });
      await performAction('provideAmountToRePay', {
        question: rePayments.rePaymentQuestion,
        option: rePayments.rePaymentRadioOptions.some,
        label: rePayments.enterTheAmountTextLabel,
        input: rePayments.enterTheAmountTextInput,
        nextPage: languageUsed.mainHeader
      });
      await performValidation('mainHeader', languageUsed.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: languageUsed.errorValidation,
        validationType: languageUsed.errorValidationType.three,
        inputArray: languageUsed.errorValidationField.errorRadioOption,
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.languageUsedRadioOptions.englishRadioOption,
        button: languageUsed.continueButton
      });
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.languageUsedRadioOptions.englishRadioOption
      });
      await performValidation('mainHeader', statementOfTruthOne.mainHeaderWrit);
    });

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [No] - Repayment [ALL] @PR @regression', async () => {
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
    });
    await performValidation('mainHeader', claimSentToHighCourt.mainHeader);
    await performAction('confirmClaimTransferredToHighCourt', {
      question: claimSentToHighCourt.claimTransferredToHighCourtQuestion,
      option: claimSentToHighCourt.yesRadioOption,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
    });
    await performValidation('mainHeader', confirmHCEOHired.mainHeader);
    await performAction('selectHaveHiredHCEO', {
      question: confirmHCEOHired.haveYouHiredHCEOQuestion,
      option: confirmHCEOHired.noRadioOption,
    });
    await performValidation('mainHeader', theNICEWillChoose.mainHeader);
    await performAction('clickButton', theNICEWillChoose.continueButton);
    await performValidation('mainHeader', moneyOwed.mainHeader);
    await performAction('inputErrorValidation', {
      validationReq: moneyOwed.errorValidation,
      validationType: moneyOwed.errorValidationType.one,
      inputArray: moneyOwed.errorValidationField.errorMoneyField,
      label: moneyOwed.totalAmountOwedTextLabel,
      button: moneyOwed.continueButton
    });
    await performAction('provideMoneyOwed', {
      label: moneyOwed.totalAmountOwedTextLabel,
      input: moneyOwed.totalAmountOwedTextInput,
      nextPage: legalCosts.mainHeader
    });
    await performValidation('mainHeader', legalCosts.mainHeader);
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
      option: languageUsed.languageUsedRadioOptions.welshRadioOption
    });
    await performValidation('mainHeader', statementOfTruthOne.mainHeaderWrit);
  });

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [No] - Repayment [None] ', async () => {
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
    });
    await performValidation('mainHeader', claimSentToHighCourt.mainHeader);
    await performAction('confirmClaimTransferredToHighCourt', {
      question: claimSentToHighCourt.claimTransferredToHighCourtQuestion,
      option: claimSentToHighCourt.yesRadioOption,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
    });
    await performValidation('mainHeader', confirmHCEOHired.mainHeader);
    await performAction('selectHaveHiredHCEO', {
      question: confirmHCEOHired.haveYouHiredHCEOQuestion,
      option: confirmHCEOHired.noRadioOption,
    });
    await performValidation('mainHeader', theNICEWillChoose.mainHeader);
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
      option: languageUsed.languageUsedRadioOptions.englishAndWelshRadioOption
    });
    await performValidation('mainHeader', statementOfTruthOne.mainHeaderWrit);
  });

  test('Writ - Apply for a Writ of Possession - Claim sent to High Court [No] @PR @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.writOfPossession,
    });
    await performValidation('mainHeader', claimSentToHighCourt.mainHeader);
    await performAction('confirmClaimTransferredToHighCourt', {
      question: claimSentToHighCourt.claimTransferredToHighCourtQuestion,
      option: claimSentToHighCourt.noRadioOption,
    });
    await performValidation('mainHeader', youCannotApplyForWrit.mainHeader);
    await performAction('clickButton', youCannotApplyForWrit.continueButton);
    await performValidation('errorMessage', { header: youCannotApplyForWrit.errors, message: youCannotApplyForWrit.errMessage });
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
      });
      await performValidation('mainHeader', claimSentToHighCourt.mainHeader);
      await performAction('confirmClaimTransferredToHighCourt', {
        question: claimSentToHighCourt.claimTransferredToHighCourtQuestion,
        option: claimSentToHighCourt.yesRadioOption,
      });
      await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.noRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      });
      await performValidation('mainHeader', youNeedPermission.mainHeader);
      await performAction('clickButton', youNeedPermission.continueButton);
      await performValidation('errorMessage', { header: youNeedPermission.eventCouldNotBeCreatedErrorMessage, message: youNeedPermission.errMessage });
    });
});
