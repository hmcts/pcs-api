import { expect, test } from '@playwright/test';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import {
  caseSummary,
  signInOrCreateAnAccount,
  user
} from '@data/page-data';
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
  rePayments
} from '@data/page-data/page-data-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';

test.beforeEach(async ({ page }, testInfo) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
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
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/PCS-${process.env.CHANGE_ID}/${process.env.CASE_NUMBER}#Summary`);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
    hide: signInOrCreateAnAccount.hideThisCookieMessageButton,
  });
  await performAction('login', user.claimantSolicitor);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAnalyticsCookiesButton,
  });
  await expect(async () => {
    await page.waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`);
  }).toPass({
    timeout: VERY_LONG_TIMEOUT,
  });
});

test.afterEach(async () => {
  moneyMap.clear();
  fieldsMap.clear();
  if (caseInfo.id) {
    await performAction('deleteCaseRole', '[CREATOR]');
  }
});

test.describe('[Enforcement - Writ of Possession]', async () => {
  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [Yes] @PR @regression',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', yourApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
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
      await performValidation('mainHeader', moneyOwed.mainHeaderWrit);
      await performAction('clickButton', moneyOwed.continueButton);
      await performValidation('mainHeader', legalCosts.mainHeaderWrit);
      await performAction('clickButton', legalCosts.continueButton);
      await performValidation('mainHeader', landRegistryFees.mainHeader);
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
        nextPage: rePayments.mainHeaderWrit
      });
    });

  test('Writ - Apply for a Writ of Possession - Have you hired HCEO [No] @PR @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.writOfPossession,
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
    await performValidation('mainHeader', moneyOwed.mainHeaderWrit);
    await performAction('clickButton', moneyOwed.continueButton);
    await performValidation('mainHeader', legalCosts.mainHeaderWrit);
    await performAction('clickButton', legalCosts.continueButton);
    await performValidation('mainHeader', landRegistryFees.mainHeader);
    await performAction('provideLandRegistryFees', {
      question: landRegistryFees.landRegistryFeeQuestion,
      option: landRegistryFees.noRadioOption,
      label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
      input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput,
      nextPage: rePayments.mainHeaderWrit
    });
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
      await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.noRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      });
      await performValidation('mainHeader', youNeedPermission.mainHeader);
      await performAction('clickButton', youNeedPermission.continueButton);
      await performValidation('errorMessage', { header: youNeedPermission.errors, message: youNeedPermission.errMessage });
    });
});
