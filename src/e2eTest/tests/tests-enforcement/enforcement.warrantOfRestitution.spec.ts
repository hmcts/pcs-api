import { expect, test } from '@utils/test-fixtures';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import { caseSummary } from '@data/page-data';
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
  statementOfTruthTwo,
  checkYourAnswers
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

test.describe('[Enforcement - Warrant of Restitution]', async () => {
  test('Warrant - Apply for a Warrant of Restitution @enforcement @PR @regression',
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
        nextPage: peopleWillBeEvicted.mainHeaderWarrantOfRestitution
      });
    });
});
