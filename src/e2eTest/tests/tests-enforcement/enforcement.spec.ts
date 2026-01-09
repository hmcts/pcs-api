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
  enterDefendantsDOB
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

test.describe('[Enforcement - Warrant of Possession]', async () => {
  test('Apply for a Warrant of Possession - risk to Bailiff [Yes] @PR @regression',
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
        option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
        button: yourApplication.continueButton
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
      await performAction('defendantsDOB', {
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
        input: moneyOwed.totalAmountOwedTextInput
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
        input: legalCosts.howMuchYouWantToReclaimTextInput
      });
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
        input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput
      });
      await performValidation('mainHeader', rePayments.mainHeader);
      await performAction('validateAmountToRePayTable');
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
        input: rePayments.enterTheAmountTextInput
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
      await performAction('selectLanguageUsed', { question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.languageUsedRadioOptions.englishRadioOption });
    });

  test('Apply for a Warrant of Possession - risk to Bailiff [No]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performAction('validateWritOrWarrantFeeAmount', {
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
    await performValidation('mainHeader', peopleWillBeEvicted.mainHeader);
    await performAction('selectPeopleWhoWillBeEvicted', {
      question: peopleWillBeEvicted.evictEveryOneQuestion,
      option: peopleWillBeEvicted.noRadioOption,
    });
    await performValidation('mainHeader', peopleYouWantToEvict.mainHeader);
    await performAction('inputErrorValidation', {
      validationReq: peopleYouWantToEvict.errorValidation,
      validationType: peopleYouWantToEvict.errorValidationType.four,
      inputArray: peopleYouWantToEvict.errorValidationField.errorCheckBoxOption,
      label: peopleYouWantToEvict.whoDoYouWantToEvictQuestion,
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
      input: moneyOwed.totalAmountOwedTextInput
    });
    await performValidation('mainHeader', legalCosts.mainHeader);
    await performAction('provideLegalCosts', {
      question: legalCosts.reclaimLegalCostsQuestion,
      option: legalCosts.noRadioOption,
      label: legalCosts.howMuchYouWantToReclaimTextLabel,
      input: legalCosts.howMuchYouWantToReclaimTextInput
    });
    await performValidation('mainHeader', landRegistryFees.mainHeader);
    await performAction('provideLandRegistryFees', {
      question: landRegistryFees.landRegistryFeeQuestion,
      option: landRegistryFees.noRadioOption,
      label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
      input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput
    });
    await performValidation('mainHeader', rePayments.mainHeader);
    await performAction('validateAmountToRePayTable');
    await performAction('provideAmountToRePay', {
      question: rePayments.rePaymentQuestion,
      option: rePayments.rePaymentRadioOptions.none,
      label: rePayments.enterTheAmountTextLabel,
      input: rePayments.enterTheAmountTextInput
    });
    await performValidation('mainHeader', languageUsed.mainHeader);
    await performAction('selectLanguageUsed', { question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.languageUsedRadioOptions.englishRadioOption });
  });

  test('Apply for a Warrant of Possession - risk to Bailiff [Not sure]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
    await performAction('validateWritOrWarrantFeeAmount', {
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
      input: moneyOwed.totalAmountOwedTextInput
    });
    await performValidation('mainHeader', legalCosts.mainHeader);
    await performAction('provideLegalCosts', {
      question: legalCosts.reclaimLegalCostsQuestion,
      option: legalCosts.yesRadioOption,
      label: legalCosts.howMuchYouWantToReclaimTextLabel,
      input: legalCosts.howMuchYouWantToReclaimTextInput
    });
    await performValidation('mainHeader', landRegistryFees.mainHeader);
    await performAction('provideLandRegistryFees', {
      question: landRegistryFees.landRegistryFeeQuestion,
      option: landRegistryFees.noRadioOption,
      label: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextLabel,
      input: landRegistryFees.howMuchYouSpendOnLandRegistryFeeTextInput
    });
    await performValidation('mainHeader', rePayments.mainHeader);
    await performAction('validateAmountToRePayTable');
    await performAction('provideAmountToRePay', {
      question: rePayments.rePaymentQuestion,
      option: rePayments.rePaymentRadioOptions.all,
      label: rePayments.enterTheAmountTextLabel,
      input: rePayments.enterTheAmountTextInput
    });
    await performValidation('mainHeader', languageUsed.mainHeader);
    await performAction('selectLanguageUsed', { question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.languageUsedRadioOptions.englishRadioOption });
  });

  test('Apply for a Warrant of Possession [General application journey] - risk to Bailiff [Yes]', {
    annotation: {
      type: 'issue',
      description: 'General application journey is a placeholder for now,this test will be fully etched out when this is ready to be developed - https://tools.hmcts.net/jira/browse/HDPI-2237 ',
    },
  },
    async () => {
      await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
      await performAction('clickButton', caseSummary.go);
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
    });
});
