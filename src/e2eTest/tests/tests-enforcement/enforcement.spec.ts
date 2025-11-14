import { test } from '@playwright/test';
import { initializeEnforcementExecutor, performAction, performValidation } from '@utils/controller-enforcement';
import { caseNumber, caseNotFoundAfterFilter } from '@utils/actions/custom-actions';
import { initializeExecutor } from '@utils/controller';
import { caseList, user, caseSummary, signInOrCreateAnAccount } from '@data/page-data';
import {
  nameAndAddressForEviction, violentOrAggressiveBehaviour, firearmPossession, yourApplication, animalsAtTheProperty,
  criminalOrAntisocialBehaviour, evictionCouldBeDelayed, vulnerableAdultsAndChildren, policeOrSocialServiceVisit,
  accessToTheProperty, riskPosedByEveryoneAtProperty, everyoneLivingAtTheProperty, verbalOrWrittenThreats, groupProtestsEviction, anythingElseHelpWithEviction,
  peopleWillBeEvicted,
  youNeedPermission
} from '@data/page-data/page-data-enforcement';

test.beforeEach(async ({ page }) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAdditionalCookiesButton,
    hide: signInOrCreateAnAccount.hideThisCookieMessageButton,
  });
  await performAction('login', user.claimantSolicitor);
  await performAction('handleCookieConsent', {
    accept: signInOrCreateAnAccount.acceptAnalyticsCookiesButton,
  });
  await performAction('filterCaseFromCaseList', caseList.stateAwaitingSubmission);
  await performAction('noCasesFoundAfterSearch');
  //Below three lines will be merged into a single action as part of improvement
  await performAction('selectFirstCaseFromTheFilter', caseNotFoundAfterFilter);
  await performAction('createNewCase', caseNotFoundAfterFilter);
  await performAction('searchMyCaseFromFindCase', { caseNumber: caseNumber, criteria: caseNotFoundAfterFilter });
});

test.describe('[Enforcement - Warrant of Possession] @regression', async () => {
  test('Apply for a Warrant of Possession - risk to Bailiff [Yes] @PR', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
    });
    await performValidation('mainHeader', peopleWillBeEvicted.mainHeader);
    await performAction('selectPeopleWhoWillBeEvicted', {
      question: peopleWillBeEvicted.evictEveryOneQuestion,
      option: peopleWillBeEvicted.yesRadioOption,
    })
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.yes,
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
  });

  test('Apply for a Warrant of Possession - risk to Bailiff [No]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
    });
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.no,
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
  });

  test('Apply for a Warrant of Possession - risk to Bailiff [Not sure]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yesRadioOption,
    });
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.notSure,
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
  });

   test('Apply for a Warrant of Possession [General application journey] - risk to Bailiff [Yes]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession,
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.noRadioOption,
    });
    await performValidation('mainHeader', youNeedPermission.mainHeader);
  });
});
