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
  languageUsed
} from '@data/page-data/page-data-enforcement';
import { createCaseApiData, submitCaseApiData } from '@data/api-data';
import { defendantDetails } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { LONG_TIMEOUT } from 'playwright.config';

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
    timeout: LONG_TIMEOUT + LONG_TIMEOUT,
  });
});

test.describe('[Enforcement - Warrant of Possession] @regression', async () => {
  test('Apply for a Warrant of Possession - risk to Bailiff [Yes] @PR', {
    annotation: {
      type: 'issue',
      description: `Fee validation in Your Application page will handle dynamic fee validation upon completion of the following - 'https://tools.hmcts.net/jira/browse/HDPI-3386'`,
    },
  },
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
        option: peopleWillBeEvicted.yesRadioOption,
      })
      await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
      await performAction('selectEveryoneLivingAtTheProperty', {
        question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
        option: everyoneLivingAtTheProperty.yesRadioOption,
      });
      await performValidation('mainHeader', riskPosedByEveryoneAtProperty.mainHeader);
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
      await performAction('provideLegalCosts', {
        question: legalCosts.reclaimLegalCostsQuestion,
        option: legalCosts.yesRadioOption,
        label: legalCosts.howMuchYouWantToReclaimTextLabel,
        input: legalCosts.howMuchYouWantToReclaimTextInput
      });
      await performValidation('mainHeader', landRegistryFees.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: landRegistryFees.errorValidation,
        validationType: landRegistryFees.errorValidationType.five,
        inputArray: landRegistryFees.errorValidationField.errorMoneyField,
        question: landRegistryFees.landRegistryFeeQuestion,
        option: landRegistryFees.yesRadioOption,
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
      await performAction('clickButton', rePayments.continueButton);
      await performValidation('mainHeader', languageUsed.mainHeader);
      await performAction('inputErrorValidation', {
        validationReq: languageUsed.errorValidation,
        validationType: languageUsed.errorValidationType.three,
        inputArray: languageUsed.errorValidationField.errorRadioOption,
        question: languageUsed.whichLanguageUsedQuestion,
        option: languageUsed.languageUsedRadioOptions.englishRadioOption,
        label: languageUsed.whichLanguageUsedQuestion,
        button: languageUsed.continueButton
      });
      await performAction('selectLanguageUsed', { question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.languageUsedRadioOptions.englishRadioOption });
    });

  test('Apply for a Warrant of Possession - risk to Bailiff [No]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
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
      defendants: defendantDetails,
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
    await performAction('clickButton', rePayments.continueButton);
    await performValidation('mainHeader', languageUsed.mainHeader);
    await performAction('selectLanguageUsed', { question: languageUsed.whichLanguageUsedQuestion, option: languageUsed.languageUsedRadioOptions.englishRadioOption });
  });

  test('Apply for a Warrant of Possession - risk to Bailiff [Not sure]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButton', caseSummary.go);
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
      defendants: defendantDetails[0],
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
