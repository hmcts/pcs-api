import { expect, test } from '@playwright/test';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import {
  caseSummary,
  signInOrCreateAnAccount,
  user
} from '@data/page-data';
import {
  accessToTheProperty,
  animalsAtTheProperty,
  anythingElseHelpWithEviction,
  criminalOrAntisocialBehaviour,
  everyoneLivingAtTheProperty,
  evictionCouldBeDelayed,
  firearmPossession,
  groupProtestsEviction,
  nameAndAddressForEviction,
  policeOrSocialServiceVisit,
  riskPosedByEveryoneAtProperty,
  verbalOrWrittenThreats,
  violentOrAggressiveBehaviour,
  vulnerableAdultsAndChildren,
  yourApplication,
  moneyOwed,
  legalCosts,
  landRegistryFees,
  rePayments,
  peopleWillBeEvicted,
  youNeedPermission,
  languageUsed,
  peopleYouWantToEvict,
  confirmDefendantsDOB,
  enterDefendantsDOB,
  suspendedOrder,
  statementOfTruthOne,
  statementOfTruthTwo
} from '@data/page-data/page-data-enforcement';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { defendantDetails } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitCaseApiData.submitCasePayload });
  await performAction('getDefendantDetails', {
    defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
    additionalDefendants: submitCaseApiData.submitCasePayload.addAnotherDefendant,
  });
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

test.describe('[Enforcement - Writ of Possession]', async () => {
  test('Apply for a Writ of Possession @PR @regression',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
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
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      });
      
    });

});
