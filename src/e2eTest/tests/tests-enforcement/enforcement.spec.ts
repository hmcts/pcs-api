import { test } from "@playwright/test";
import { caseList } from "@data/page-data/caseList.page.data";
import { user } from "@data/user-data/permanent.user.data";
import { caseSummary } from "@data/page-data/caseSummary.page.data";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { initializeEnforcementExecutor, performAction, performValidation } from "@utils/controller-enforcement";
import { caseNumber } from "@utils/actions/custom-actions/createCase.action";
import { initializeExecutor } from "@utils/controller";
import { caseNotFoundAfterFilter } from "@utils/actions/custom-actions/searchCase.action";
import { nameAndAddressForEviction } from "@data/page-data/page-data-enforcement/nameAndAddressForEviction.page.data";
import { everyoneLivingAtTheProperty } from "@data/page-data/page-data-enforcement/everyoneLivingAtTheProperty.page.data";
import { riskPosedByEveryoneAtProperty } from "@data/page-data/page-data-enforcement/riskPosedByEveryoneAtProperty.page.data";
import { vulnerableAdultsAndChildren } from "@data/page-data/page-data-enforcement/vulnerableAdultsAndChildren.page.data";
import { evictionCouldBeDelayed } from "@data/page-data/page-data-enforcement/evictionCouldBeDelayed.page.data";
import { violentOrAggressiveBehaviour } from "@data/page-data/page-data-enforcement/violentOrAggressiveBehaviour.page.data";
import { firearmPossession } from "@data/page-data/page-data-enforcement/firearmPossession.page.data";
import { criminalOrAntisocialBehaviour } from "@data/page-data/page-data-enforcement/criminalOrAntisocialBehaviour.page.data";
import { verbalOrWrittenThreats } from "@data/page-data/page-data-enforcement/verbalOrWrittenThreats.page.data";
import { groupProtestsEviction } from "@data/page-data/page-data-enforcement/groupProtestsEviction.page.data";
import { policeOrSocialServiceVisit } from "@data/page-data/page-data-enforcement/policeOrSocialServiceVisit.page.data";
import { animalsAtTheProperty } from "@data/page-data/page-data-enforcement/animalsAtTheProperty";

test.beforeEach(async ({page}) => {
  initializeExecutor(page);
  initializeEnforcementExecutor(page);
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await performAction('login', user.claimantSolicitor);
  await performAction('filterCaseFromCaseList', caseList.stateAwaitingSubmission);
  await performAction('noCasesFoundAfterSearch')
  //Below three lines will be merged into a single action as part of improvement
  await performAction("selectFirstCaseFromTheFilter", caseNotFoundAfterFilter);
  await performAction('createNewCase', caseNotFoundAfterFilter);
  await performAction('searchMyCaseFromFindCase', {caseNumber: caseNumber, criteria: caseNotFoundAfterFilter});
});

test.describe('[Enforcement - Warrant of Possession] @Master @nightly', async () => {
  test('Apply for a Warrant of Possession - risk to Bailiff [Yes]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yes
    });
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.yes
    });
    await performValidation('mainHeader', riskPosedByEveryoneAtProperty.mainHeader);
    await performAction('selectRiskPosedByEveryoneAtProperty', {
      riskTypes: [riskPosedByEveryoneAtProperty.violentOrAggressiveBehaviour, riskPosedByEveryoneAtProperty.historyOfFirearmPossession,
      riskPosedByEveryoneAtProperty.criminalOrAntisocialBehaviour, riskPosedByEveryoneAtProperty.verbalOrWrittenThreats,
      riskPosedByEveryoneAtProperty.protestGroup, riskPosedByEveryoneAtProperty.policeOrSocialService, riskPosedByEveryoneAtProperty.aggressiveAnimals]
    });
    await performAction('provideDetailsViolentOrAggressiveBehaviour', {
      label: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressive,
      input: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressiveInput
    });
    await performAction('provideDetailsFireArmPossession', {
      label: firearmPossession.whatIsTheirHistoryOfFirearmPossession,
      input: firearmPossession.whatIsTheirHistoryOfFirearmPossessionInput
    });
    await performAction('provideDetailsCriminalOrAntisocialBehavior', {
      label: criminalOrAntisocialBehaviour.whatIsTheirHistoryOfCriminalAntisocialBehaviour,
      input: criminalOrAntisocialBehaviour.whatIsTheirHistoryOfCriminalAntisocialBehaviourInput
    });
    await performAction('provideDetailsVerbalOrWrittenThreats', {
      label: verbalOrWrittenThreats.verbalOrWrittenThreatsMade,
      input: verbalOrWrittenThreats.verbalOrWrittenThreatsMadeInput
    });
    await performAction('provideDetailsGroupProtestsEviction', {
      label: groupProtestsEviction.whichGroupMember,
      input: groupProtestsEviction.whichGroupMemberInput
    });
    await performAction('provideDetailsPoliceOrSocialServiceVisits', {
      label: policeOrSocialServiceVisit.whyDidThePoliceOrSSVisitTheProperty,
      input: policeOrSocialServiceVisit.whyDidThePoliceOrSSVisitThePropertyInput
    });
    await performAction('provideDetailsAnimalsAtTheProperty', {
      label: animalsAtTheProperty.whatKindOfAnimalDoTheyHave,
      input: animalsAtTheProperty.whatKindOfAnimalDoTheyHaveInput
    });
    await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
    await performAction('clickButton', vulnerableAdultsAndChildren.continue);
  });

  test('Apply for a Warrant of Possession - risk to Bailiff [No]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yes
    });
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.no
    });
    await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
    await performAction('clickButton', vulnerableAdultsAndChildren.continue);
  });

  test('Apply for a Warrant of Possession - risk to Bailiff [Not sure]', async () => {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.enforceTheOrderEvent);
    await performAction('clickButtonAndVerifyPageNavigation', caseSummary.go, yourApplication.mainHeader);
    await performAction('selectApplicationType', {
      question: yourApplication.typeOfApplicationQuestion,
      option: yourApplication.typeOfApplicationOptions.warrantOfPossession
    });
    await performValidation('mainHeader', nameAndAddressForEviction.mainHeader);
    await performAction('selectNameAndAddressForEviction', {
      question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
      option: nameAndAddressForEviction.yes
    });
    await performValidation('mainHeader', everyoneLivingAtTheProperty.mainHeader);
    await performAction('selectEveryoneLivingAtTheProperty', {
      question: everyoneLivingAtTheProperty.riskToBailiffQuestion,
      option: everyoneLivingAtTheProperty.notSure
    });
    await performValidation('mainHeader', evictionCouldBeDelayed.mainHeader);
    await performAction('clickButton', evictionCouldBeDelayed.continue);
    await performValidation('mainHeader', vulnerableAdultsAndChildren.mainHeader);
    await performAction('clickButton', vulnerableAdultsAndChildren.continue);
  });
});
