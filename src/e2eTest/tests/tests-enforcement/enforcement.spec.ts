import { expect, test } from '@playwright/test';
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
  statementOfTruthTwo
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

test.describe('[Enforcement - Warrant of Possession]', async () => {
  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [Yes] @PR @regression',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', yourApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: yourApplication.typeOfApplicationOptions.warrantOfPossession,
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
        option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
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
      await performValidation('mainHeader', confirmDefendantsDOB.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: confirmDefendantsDOB.errorValidation,
        validationType: confirmDefendantsDOB.errorValidationType.three,
        inputArray: confirmDefendantsDOB.errorValidationField.errorRadioOption,
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.yesRadioOption,
        button: confirmDefendantsDOB.continueButton
      });
      await performAction('confirmDefendantsDOB', {
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.yesRadioOption,
      });
      await performValidation('mainHeader', enterDefendantsDOB.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: enterDefendantsDOB.errorValidation,
        validationType: enterDefendantsDOB.errorValidationType.two,
        inputArray: enterDefendantsDOB.errorValidationField.errorTextField,
        header: enterDefendantsDOB.errors,
        label: enterDefendantsDOB.defendantsDOBTextLabel,
        button: enterDefendantsDOB.continueButton
      });
      await performAction('enterDefendantsDOB', {
        label: enterDefendantsDOB.defendantsDOBTextLabel,
        input: defendantDetails,
      });
      await performValidation('mainHeader', peopleWillBeEvicted.mainHeader);
      await performAction('selectPeopleWhoWillBeEvicted', {
        question: peopleWillBeEvicted.evictEveryOneQuestion,
        option: peopleWillBeEvicted.yesRadioOption,
      });
      await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
      await performAction('selectEveryoneLivingAtTheProperty', {
        question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
        option: everyoneLivingAtTheProperty.yesRadioOption,
      });
      await performValidation('mainHeader', riskPosedByEveryoneAtProperty.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: riskPosedByEveryoneAtProperty.errorValidation,
        validationType: riskPosedByEveryoneAtProperty.errorValidationType.four,
        inputArray: riskPosedByEveryoneAtProperty.errorValidationField.errorCheckBoxOption,
        label: riskPosedByEveryoneAtProperty.kindOfRiskQuestion,
        checkBox: riskPosedByEveryoneAtProperty.violentOrAggressiveBehaviourCheckbox,
        button: riskPosedByEveryoneAtProperty.continueButton
      });
      await performAction('selectRiskPosedByEveryoneAtProperty', {
        riskTypes: [
          riskPosedByEveryoneAtProperty.violentOrAggressiveBehaviourCheckbox,
          riskPosedByEveryoneAtProperty.historyOfFirearmPossessionCheckbox,
          riskPosedByEveryoneAtProperty.criminalOrAntisocialBehaviourCheckbox,
          riskPosedByEveryoneAtProperty.verbalOrWrittenThreatsCheckbox,
          riskPosedByEveryoneAtProperty.protestGroupCheckbox,
          riskPosedByEveryoneAtProperty.policeOrSocialServiceCheckbox,
          riskPosedByEveryoneAtProperty.aggressiveAnimalsCheckbox,
        ],
      });
      await performValidation('mainHeader', violentOrAggressiveBehaviour.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: violentOrAggressiveBehaviour.errorValidation,
        validationType: violentOrAggressiveBehaviour.errorValidationType.two,
        inputArray: violentOrAggressiveBehaviour.errorValidationField.errorTextField,
        header: violentOrAggressiveBehaviour.errors,
        label: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressive,
        button: violentOrAggressiveBehaviour.continueButton
      });
      await performAction('provideDetailsViolentOrAggressiveBehaviour', {
        label: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressive,
        input: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressiveInput,
      });
      await performAction('provideDetailsFireArmPossession', {
        label: firearmPossession.whatIsTheirHistoryOfFirearmPossession,
        input: firearmPossession.whatIsTheirHistoryOfFirearmPossessionInput,
      });
      await performAction('provideDetailsCriminalOrAntisocialBehavior', {
        label: criminalOrAntisocialBehaviour.whatIsTheirHistoryOfCriminalAntisocialBehaviour,
        input: criminalOrAntisocialBehaviour.whatIsTheirHistoryOfCriminalAntisocialBehaviourInput,
      });
      await performAction('provideDetailsVerbalOrWrittenThreats', {
        label: verbalOrWrittenThreats.verbalOrWrittenThreatsMade,
        input: verbalOrWrittenThreats.verbalOrWrittenThreatsMadeInput,
      });
      await performAction('provideDetailsGroupProtestsEviction', {
        label: groupProtestsEviction.whichGroupMember,
        input: groupProtestsEviction.whichGroupMemberInput,
      });
      await performAction('provideDetailsPoliceOrSocialServiceVisits', {
        label: policeOrSocialServiceVisit.whyDidThePoliceOrSSVisitTheProperty,
        input: policeOrSocialServiceVisit.whyDidThePoliceOrSSVisitThePropertyInput,
      });
      await performAction('provideDetailsAnimalsAtTheProperty', {
        label: animalsAtTheProperty.whatKindOfAnimalDoTheyHave,
        input: animalsAtTheProperty.whatKindOfAnimalDoTheyHaveInput,
      });
      await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: vulnerableAdultsAndChildren.errorValidation,
        validationType: vulnerableAdultsAndChildren.errorValidationType.three,
        inputArray: vulnerableAdultsAndChildren.errorValidationField.errorRadioOption1,
        question: vulnerableAdultsAndChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsAndChildren.yesRadioOption,
        button: vulnerableAdultsAndChildren.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: vulnerableAdultsAndChildren.errorValidation,
        validationType: vulnerableAdultsAndChildren.errorValidationType.three,
        inputArray: vulnerableAdultsAndChildren.errorValidationField.errorRadioOption2,
        question: vulnerableAdultsAndChildren.confirmVulnerablePeopleQuestion,
        option: vulnerableAdultsAndChildren.vulnerableAdultsRadioOption,
        button: vulnerableAdultsAndChildren.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: vulnerableAdultsAndChildren.errorValidation,
        validationType: vulnerableAdultsAndChildren.errorValidationType.two,
        inputArray: vulnerableAdultsAndChildren.errorValidationField.errorTextField,
        header: vulnerableAdultsAndChildren.errors,
        label: vulnerableAdultsAndChildren.howAreTheyVulnerableTextLabel,
        button: vulnerableAdultsAndChildren.continueButton
      });
      await performAction('selectVulnerablePeopleInTheProperty', {
        question: vulnerableAdultsAndChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsAndChildren.yesRadioOption,
        confirm: vulnerableAdultsAndChildren.confirmVulnerablePeopleQuestion,
        peopleOption: vulnerableAdultsAndChildren.vulnerableAdultsRadioOption,
        label: vulnerableAdultsAndChildren.howAreTheyVulnerableTextLabel,
        input: vulnerableAdultsAndChildren.howAreTheyVulnerableTextInput
      });
      await performValidation('mainHeader', accessToTheProperty.mainHeader);
      await performAction('accessToProperty', {
        question: accessToTheProperty.accessToThePropertyQuestion,
        option: accessToTheProperty.yesRadioOption,
        label: accessToTheProperty.whyItsDifficultToAccessToThePropertyTextLabel,
        input: accessToTheProperty.whyItsDifficultToAccessToThePropertyTextInput,
      });
      await performValidation('mainHeader', anythingElseHelpWithEviction.mainHeader);
      await performAction('provideDetailsAnythingElseHelpWithEviction', {
        question: anythingElseHelpWithEviction.anythingElseQuestion,
        option: anythingElseHelpWithEviction.yesRadioOption,
        label: anythingElseHelpWithEviction.tellUsAnythingElseTextLabel,
        input: anythingElseHelpWithEviction.tellUsAnythingElseTextInput
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
      await performValidation('mainHeader', suspendedOrder.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: suspendedOrder.errorValidation,
        validationType: suspendedOrder.errorValidationType.three,
        inputArray: suspendedOrder.errorValidationField.errorRadioOption,
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.yesRadioOption,
        button: suspendedOrder.continueButton
      });
      await performAction('confirmSuspendedOrder', {
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.yesRadioOption
      });
      await performValidation('mainHeader', statementOfTruthOne.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: statementOfTruthOne.errorValidation,
        validationType: statementOfTruthOne.errorValidationType.four,
        inputArray: statementOfTruthOne.errorValidationField.errorCheckBoxOption,
        label: statementOfTruthOne.checkBoxGenericErrorLabel,
        checkBox: statementOfTruthOne.iCertifyCheckbox,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: statementOfTruthOne.errorValidation,
        validationType: statementOfTruthOne.errorValidationType.three,
        inputArray: statementOfTruthOne.errorValidationField.errorRadioOption,
        question: statementOfTruthOne.completedByLabel,
        option: statementOfTruthOne.claimantRadioOption,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: statementOfTruthOne.errorValidation,
        validationType: statementOfTruthOne.errorValidationType.four,
        inputArray: statementOfTruthOne.errorValidationField.errorCheckBoxOption,
        label: statementOfTruthOne.checkBoxGenericErrorLabel,
        checkBox: statementOfTruthOne.iBelieveTheFactsHiddenCheckbox,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: statementOfTruthOne.errorValidation,
        validationType: statementOfTruthOne.errorValidationType.two,
        inputArray: statementOfTruthOne.errorValidationField.errorTextField1,
        header: statementOfTruthOne.errors,
        label: statementOfTruthOne.fullNameHiddenTextLabel,
        button: statementOfTruthOne.continueButton
      });
      await performAction('inputErrorValidation', {
        validationReq: statementOfTruthOne.errorValidation,
        validationType: statementOfTruthOne.errorValidationType.two,
        inputArray: statementOfTruthOne.errorValidationField.errorTextField2,
        header: statementOfTruthOne.errors,
        label: statementOfTruthOne.positionOrOfficeHeldHiddenTextLabel,
        button: statementOfTruthOne.continueButton
      });
      await performAction('validateAmountToRePayTable', { headerName: statementOfTruthOne.mainHeader });
      await performAction('selectStatementOfTruthOne', {
        selectCheckbox: statementOfTruthOne.iCertifyCheckbox,
        completedBy: statementOfTruthOne.claimantRadioOption,
        iBelieveCheckbox: statementOfTruthOne.iBelieveTheFactsHiddenCheckbox,
        fullNameTextInput: statementOfTruthOne.fullNameHiddenTextInput,
        positionOrOfficeTextInput: statementOfTruthOne.positionOrOfficeHeldHiddenTextInput
      });
    });

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [No] @PR @regression', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', yourApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: yourApplication.typeOfApplicationOptions.warrantOfPossession,
      type: yourApplication.summaryWritOrWarrant,
      label1: yourApplication.warrantFeeValidationLabel,
      text1: yourApplication.warrantFeeValidationText,
      label2: yourApplication.writFeeValidationLabel,
      text2: yourApplication.writFeeValidationText
    });
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
    });
    await performValidation('mainHeader', confirmDefendantsDOB.mainHeader);
    await performAction('confirmDefendantsDOB', {
      question: confirmDefendantsDOB.defendantsDOBQuestion,
      option: confirmDefendantsDOB.noRadioOption,
    });
    await performValidation('mainHeader', peopleWillBeEvicted.mainHeader);
    await performAction('selectPeopleWhoWillBeEvicted', {
      question: peopleWillBeEvicted.evictEveryOneQuestion,
      option: peopleWillBeEvicted.noRadioOption,
    });
    await performValidation('mainHeader', peopleYouWantToEvict.mainHeader);
    await performAction('inputErrorValidation', {
      validationReq: peopleYouWantToEvict.errorValidation,
      validationType: peopleYouWantToEvict.errorValidationType.six,
      inputArray: peopleYouWantToEvict.errorValidationField.errorCheckBoxOption,
      label: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
      header: peopleYouWantToEvict.errors,
      checkBox: defendantDetails[0],
      button: peopleYouWantToEvict.continueButton
    });
    await performAction('selectPeopleYouWantToEvict', {
      question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
      option: defendantDetails,
    });
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.noRadioOption,
    });
    await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
    await performAction('selectVulnerablePeopleInTheProperty', {
      question: vulnerableAdultsAndChildren.IsAnyOneLivingAtThePropertyQuestion,
      option: vulnerableAdultsAndChildren.noRadioOption,
      confirm: vulnerableAdultsAndChildren.confirmVulnerablePeopleQuestion,
      peopleOption: vulnerableAdultsAndChildren.vulnerableAdultsRadioOption,
      label: vulnerableAdultsAndChildren.howAreTheyVulnerableTextLabel,
      input: vulnerableAdultsAndChildren.howAreTheyVulnerableTextInput
    });
    await performValidation('mainHeader', accessToTheProperty.mainHeader);
    await performAction('accessToProperty', {
      question: accessToTheProperty.accessToThePropertyQuestion,
      option: accessToTheProperty.noRadioOption,
    });
    await performValidation('mainHeader', anythingElseHelpWithEviction.mainHeader);
    await performAction('provideDetailsAnythingElseHelpWithEviction', {
      question: anythingElseHelpWithEviction.anythingElseQuestion,
      option: anythingElseHelpWithEviction.noRadioOption,
      label: anythingElseHelpWithEviction.tellUsAnythingElseTextLabel,
      input: anythingElseHelpWithEviction.tellUsAnythingElseTextInput
    });
    await performValidation('mainHeader', moneyOwed.mainHeader);
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
      nextPage: rePayments.mainHeader,
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
    await performAction('selectLanguageUsed', {
      question: languageUsed.whichLanguageUsedQuestion,
      option: languageUsed.languageUsedRadioOptions.englishRadioOption
    });
    await performValidation('mainHeader', suspendedOrder.mainHeader);
    await performAction('confirmSuspendedOrder', {
      question: suspendedOrder.suspendedOrderQuestion,
      option: suspendedOrder.noRadioOption
    });
    await performValidation('mainHeader', statementOfTruthTwo.mainHeader);
    await performAction('inputErrorValidation', {
      validationReq: statementOfTruthTwo.errorValidation,
      validationType: statementOfTruthTwo.errorValidationType.four,
      inputArray: statementOfTruthTwo.errorValidationField.errorCheckBoxOption,
      label: statementOfTruthTwo.checkBoxGenericErrorLabel,
      //header: statementOfTruthTwo.errors,
      checkBox: statementOfTruthTwo.iCertifyCheckbox,
      button: statementOfTruthTwo.continueButton
    });
    await performAction('inputErrorValidation', {
      validationReq: statementOfTruthTwo.errorValidation,
      validationType: statementOfTruthTwo.errorValidationType.three,
      inputArray: statementOfTruthTwo.errorValidationField.errorRadioOption,
      question: statementOfTruthTwo.completedByLabel,
      option: statementOfTruthTwo.claimantLegalRepresentativeRadioOption,
      button: statementOfTruthTwo.continueButton
    });
    await performAction('inputErrorValidation', {
      validationReq: statementOfTruthTwo.errorValidation,
      validationType: statementOfTruthTwo.errorValidationType.four,
      inputArray: statementOfTruthTwo.errorValidationField.errorCheckBoxOption,
      label: statementOfTruthTwo.checkBoxGenericErrorLabel,
      //header: statementOfTruthTwo.errors,
      checkBox: statementOfTruthTwo.signThisStatementHiddenCheckbox,
      button: statementOfTruthTwo.continueButton
    });
    await performAction('inputErrorValidation', {
      validationReq: statementOfTruthTwo.errorValidation,
      validationType: statementOfTruthTwo.errorValidationType.two,
      inputArray: statementOfTruthTwo.errorValidationField.errorTextField1,
      header: statementOfTruthTwo.errors,
      label: statementOfTruthTwo.fullNameHiddenTextLabel,
      button: statementOfTruthTwo.continueButton
    });
    await performAction('inputErrorValidation', {
      validationReq: statementOfTruthTwo.errorValidation,
      validationType: statementOfTruthTwo.errorValidationType.two,
      inputArray: statementOfTruthTwo.errorValidationField.errorTextField2,
      header: statementOfTruthTwo.errors,
      label: statementOfTruthTwo.nameOfFirmHiddenTextLabel,
      button: statementOfTruthTwo.continueButton
    });
    await performAction('inputErrorValidation', {
      validationReq: statementOfTruthTwo.errorValidation,
      validationType: statementOfTruthTwo.errorValidationType.two,
      inputArray: statementOfTruthTwo.errorValidationField.errorTextField3,
      header: statementOfTruthTwo.errors,
      label: statementOfTruthTwo.positionOrOfficeHeldHiddenTextLabel,
      button: statementOfTruthTwo.continueButton
    });
    await performAction('validateAmountToRePayTable', { headerName: statementOfTruthTwo.mainHeader });
    await performAction('selectStatementOfTruthTwo', {
      selectCheckbox: statementOfTruthTwo.iCertifyCheckbox,
      completedBy: statementOfTruthTwo.claimantLegalRepresentativeRadioOption,
      signThisStatementCheckbox: statementOfTruthTwo.signThisStatementHiddenCheckbox,
      fullNameTextInput: statementOfTruthTwo.fullNameHiddenTextInput,
      nameOfFirmTextInput: statementOfTruthTwo.nameOfFirmHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruthTwo.positionOrOfficeHeldHiddenTextInput
    });
  });

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [Not sure]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', yourApplication.mainHeader);
    await performAction('validateWritOrWarrantFeeAmount', {
      journey: yourApplication.typeOfApplicationOptions.warrantOfPossession,
      type: yourApplication.summaryWritOrWarrant,
      label1: yourApplication.warrantFeeValidationLabel,
      text1: yourApplication.warrantFeeValidationText,
      label2: yourApplication.writFeeValidationLabel,
      text2: yourApplication.writFeeValidationText
    });
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
      defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
    });
    await performValidation('mainHeader', confirmDefendantsDOB.mainHeader);
    await performAction('confirmDefendantsDOB', {
      question: confirmDefendantsDOB.defendantsDOBQuestion,
      option: confirmDefendantsDOB.noRadioOption,
    });
    await performValidation('mainHeader', peopleWillBeEvicted.mainHeader);
    await performAction('selectPeopleWhoWillBeEvicted', {
      question: peopleWillBeEvicted.evictEveryOneQuestion,
      option: peopleWillBeEvicted.noRadioOption,
    });
    await performAction('selectPeopleYouWantToEvict', {
      question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
      option: defendantDetails[0]
    });
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.notSureRadioOption,
    });
    await performValidation('mainHeader', evictionCouldBeDelayed.mainHeader);
    await performAction('clickButton', evictionCouldBeDelayed.continue);
    await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
    await performAction('selectVulnerablePeopleInTheProperty', {
      question: vulnerableAdultsAndChildren.IsAnyOneLivingAtThePropertyQuestion,
      option: vulnerableAdultsAndChildren.notSureRadioOption,
      confirm: vulnerableAdultsAndChildren.confirmVulnerablePeopleQuestion,
      peopleOption: vulnerableAdultsAndChildren.vulnerableAdultsRadioOption,
      label: vulnerableAdultsAndChildren.howAreTheyVulnerableTextLabel,
      input: vulnerableAdultsAndChildren.howAreTheyVulnerableTextInput
    });
    await performValidation('mainHeader', accessToTheProperty.mainHeader);
    await performAction('accessToProperty', {
      question: accessToTheProperty.accessToThePropertyQuestion,
      option: accessToTheProperty.yesRadioOption,
      label: accessToTheProperty.whyItsDifficultToAccessToThePropertyTextLabel,
      input: accessToTheProperty.whyItsDifficultToAccessToThePropertyTextInput,
    });
    await performValidation('mainHeader', anythingElseHelpWithEviction.mainHeader);
    await performAction('provideDetailsAnythingElseHelpWithEviction', {
      question: anythingElseHelpWithEviction.anythingElseQuestion,
      option: anythingElseHelpWithEviction.yesRadioOption,
      label: anythingElseHelpWithEviction.tellUsAnythingElseTextLabel,
      input: anythingElseHelpWithEviction.tellUsAnythingElseTextInput,
    });
    await performValidation('mainHeader', moneyOwed.mainHeader);
    await performAction('provideMoneyOwed', {
      label: moneyOwed.totalAmountOwedTextLabel,
      input: moneyOwed.totalAmountOwedTextInput,
      nextPage: legalCosts.mainHeader
    });
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
      nextPage: rePayments.mainHeader,
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
      option: languageUsed.languageUsedRadioOptions.englishRadioOption
    });
    await performValidation('mainHeader', suspendedOrder.mainHeader);
    await performAction('confirmSuspendedOrder', {
      question: suspendedOrder.suspendedOrderQuestion,
      option: suspendedOrder.yesRadioOption
    });
    await performValidation('mainHeader', statementOfTruthOne.mainHeader);
    await performAction('validateAmountToRePayTable', { headerName: statementOfTruthOne.mainHeader });
    await performAction('selectStatementOfTruthOne', {
      selectCheckbox: statementOfTruthOne.iCertifyCheckbox,
      completedBy: statementOfTruthOne.claimantLegalRepresentativeRadioOption,
      signThisStatementCheckbox: statementOfTruthOne.signThisStatementHiddenCheckbox,
      fullNameTextInput: statementOfTruthOne.fullNameHiddenTextInput,
      nameOfFirmTextInput: statementOfTruthOne.nameOfFirmHiddenTextInput,
      positionOrOfficeTextInput: statementOfTruthOne.positionOrOfficeHeldHiddenTextInput
    });
  });

  test('Warrant - Apply for a Warrant of Possession [General application journey] - risk to Bailiff [Yes]', {
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
        option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
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

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [No] no defendants added @noDefendants @PR @regression',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', yourApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: yourApplication.typeOfApplicationOptions.warrantOfPossession,
        type: yourApplication.summaryWritOrWarrant,
        label1: yourApplication.warrantFeeValidationLabel,
        text1: yourApplication.warrantFeeValidationText,
        label2: yourApplication.writFeeValidationLabel,
        text2: yourApplication.writFeeValidationText
      });
      await performAction('selectApplicationType', {
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
      });
      await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      });
      await performValidation('mainHeader', confirmDefendantsDOB.mainHeader);
      await performAction('confirmDefendantsDOB', {
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.noRadioOption,
      });
      await performValidation('mainHeader', peopleWillBeEvicted.mainHeader);
      await performAction('selectPeopleWhoWillBeEvicted', {
        question: peopleWillBeEvicted.evictEveryOneQuestion,
        option: peopleWillBeEvicted.noRadioOption,
      });
      await performValidation('mainHeader', peopleYouWantToEvict.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: peopleYouWantToEvict.errorValidation,
        validationType: peopleYouWantToEvict.errorValidationType.six,
        inputArray: peopleYouWantToEvict.errorValidationField.errorCheckBoxOption,
        label: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
        header: peopleYouWantToEvict.errors,
        checkBox: defendantDetails[0],
        button: peopleYouWantToEvict.continueButton
      });
      await performAction('selectPeopleYouWantToEvict', {
        question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
        option: defendantDetails,
      });
      await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
      await performAction('selectEveryoneLivingAtTheProperty', {
        question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
        option: everyoneLivingAtTheProperty.noRadioOption,
      });
      await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
      await performAction('selectVulnerablePeopleInTheProperty', {
        question: vulnerableAdultsAndChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsAndChildren.noRadioOption,
        confirm: vulnerableAdultsAndChildren.confirmVulnerablePeopleQuestion,
        peopleOption: vulnerableAdultsAndChildren.vulnerableAdultsRadioOption,
        label: vulnerableAdultsAndChildren.howAreTheyVulnerableTextLabel,
        input: vulnerableAdultsAndChildren.howAreTheyVulnerableTextInput
      });
      await performValidation('mainHeader', accessToTheProperty.mainHeader);
      await performAction('accessToProperty', {
        question: accessToTheProperty.accessToThePropertyQuestion,
        option: accessToTheProperty.noRadioOption,
      });
      await performValidation('mainHeader', anythingElseHelpWithEviction.mainHeader);
      await performAction('provideDetailsAnythingElseHelpWithEviction', {
        question: anythingElseHelpWithEviction.anythingElseQuestion,
        option: anythingElseHelpWithEviction.noRadioOption,
        label: anythingElseHelpWithEviction.tellUsAnythingElseTextLabel,
        input: anythingElseHelpWithEviction.tellUsAnythingElseTextInput
      });
      await performValidation('mainHeader', moneyOwed.mainHeader);
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
        nextPage: rePayments.mainHeader,
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
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.languageUsedRadioOptions.englishRadioOption
      });
      await performValidation('mainHeader', suspendedOrder.mainHeader);
      await performAction('confirmSuspendedOrder', {
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.noRadioOption
      });
      await performValidation('mainHeader', statementOfTruthTwo.mainHeader);
      await performAction('validateAmountToRePayTable', { headerName: statementOfTruthTwo.mainHeader });
      await performAction('selectStatementOfTruthTwo', {
        selectCheckbox: statementOfTruthTwo.iCertifyCheckbox,
        completedBy: statementOfTruthTwo.claimantLegalRepresentativeRadioOption,
        signThisStatementCheckbox: statementOfTruthTwo.signThisStatementHiddenCheckbox,
        fullNameTextInput: statementOfTruthTwo.fullNameHiddenTextInput,
        nameOfFirmTextInput: statementOfTruthTwo.nameOfFirmHiddenTextInput,
        positionOrOfficeTextInput: statementOfTruthTwo.positionOrOfficeHeldHiddenTextInput
      });
    });

  test('Warrant - Apply for a Warrant of Possession - risk to Bailiff [No]- only main defendants name known @onlyMain @PR @regression',
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
      await performValidation('mainHeader', yourApplication.mainHeader);
      await performAction('validateWritOrWarrantFeeAmount', {
        journey: yourApplication.typeOfApplicationOptions.warrantOfPossession,
        type: yourApplication.summaryWritOrWarrant,
        label1: yourApplication.warrantFeeValidationLabel,
        text1: yourApplication.warrantFeeValidationText,
        label2: yourApplication.writFeeValidationLabel,
        text2: yourApplication.writFeeValidationText
      });
      await performAction('selectApplicationType', {
        question: yourApplication.typeOfApplicationQuestion,
        option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
      });
      await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
      await performAction('selectNameAndAddressForEviction', {
        question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
        option: nameAndAddressForEviction.yesRadioOption,
        defendant1NameKnown: submitCaseApiData.submitCasePayload.defendant1.nameKnown,
      });
      await performValidation('mainHeader', confirmDefendantsDOB.mainHeader);
      await performAction('confirmDefendantsDOB', {
        question: confirmDefendantsDOB.defendantsDOBQuestion,
        option: confirmDefendantsDOB.noRadioOption,
      });
      await performValidation('mainHeader', peopleWillBeEvicted.mainHeader);
      await performAction('selectPeopleWhoWillBeEvicted', {
        question: peopleWillBeEvicted.evictEveryOneQuestion,
        option: peopleWillBeEvicted.noRadioOption,
      });
      await performValidation('mainHeader', peopleYouWantToEvict.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: peopleYouWantToEvict.errorValidation,
        validationType: peopleYouWantToEvict.errorValidationType.six,
        inputArray: peopleYouWantToEvict.errorValidationField.errorCheckBoxOption,
        label: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
        header: peopleYouWantToEvict.errors,
        checkBox: defendantDetails[0],
        button: peopleYouWantToEvict.continueButton
      });
      await performAction('selectPeopleYouWantToEvict', {
        question: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
        option: defendantDetails[0],
      });
      await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
      await performAction('selectEveryoneLivingAtTheProperty', {
        question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
        option: everyoneLivingAtTheProperty.noRadioOption,
      });
      await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
      await performAction('selectVulnerablePeopleInTheProperty', {
        question: vulnerableAdultsAndChildren.IsAnyOneLivingAtThePropertyQuestion,
        option: vulnerableAdultsAndChildren.noRadioOption,
        confirm: vulnerableAdultsAndChildren.confirmVulnerablePeopleQuestion,
        peopleOption: vulnerableAdultsAndChildren.vulnerableAdultsRadioOption,
        label: vulnerableAdultsAndChildren.howAreTheyVulnerableTextLabel,
        input: vulnerableAdultsAndChildren.howAreTheyVulnerableTextInput
      });
      await performValidation('mainHeader', accessToTheProperty.mainHeader);
      await performAction('accessToProperty', {
        question: accessToTheProperty.accessToThePropertyQuestion,
        option: accessToTheProperty.noRadioOption,
      });
      await performValidation('mainHeader', anythingElseHelpWithEviction.mainHeader);
      await performAction('provideDetailsAnythingElseHelpWithEviction', {
        question: anythingElseHelpWithEviction.anythingElseQuestion,
        option: anythingElseHelpWithEviction.noRadioOption,
        label: anythingElseHelpWithEviction.tellUsAnythingElseTextLabel,
        input: anythingElseHelpWithEviction.tellUsAnythingElseTextInput
      });
      await performValidation('mainHeader', moneyOwed.mainHeader);
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
        nextPage: rePayments.mainHeader,
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
      await performAction('selectLanguageUsed', {
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.languageUsedRadioOptions.englishRadioOption
      });
      await performValidation('mainHeader', suspendedOrder.mainHeader);
      await performAction('confirmSuspendedOrder', {
        question: suspendedOrder.suspendedOrderQuestion,
        option: suspendedOrder.noRadioOption
      });
      await performValidation('mainHeader', statementOfTruthTwo.mainHeader);
      await performAction('validateAmountToRePayTable', { headerName: statementOfTruthTwo.mainHeader });
      await performAction('selectStatementOfTruthTwo', {
        selectCheckbox: statementOfTruthTwo.iCertifyCheckbox,
        completedBy: statementOfTruthTwo.claimantLegalRepresentativeRadioOption,
        signThisStatementCheckbox: statementOfTruthTwo.signThisStatementHiddenCheckbox,
        fullNameTextInput: statementOfTruthTwo.fullNameHiddenTextInput,
        nameOfFirmTextInput: statementOfTruthTwo.nameOfFirmHiddenTextInput,
        positionOrOfficeTextInput: statementOfTruthTwo.positionOrOfficeHeldHiddenTextInput
      });
    });
});