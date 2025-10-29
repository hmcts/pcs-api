import { Page } from "@playwright/test";
import { performAction, performValidation } from "@utils/controller-enforcement";
import { IAction, actionData, actionRecord } from "@utils/interfaces/action.interface";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { enforcementTestCaseNumber } from "../searchCase.action";
import { nameAndAddressForEviction } from "@data/page-data/page-data-enforcement/nameAndAddressForEviction.page.data";
import { riskPosedByEveryoneAtProperty } from "@data/page-data/page-data-enforcement/riskPosedByEveryoneAtProperty.page.data";
import { everyoneLivingAtTheProperty } from "@data/page-data/page-data-enforcement/everyoneLivingAtTheProperty.page.data";
import { violentOrAggressiveBehaviour } from "@data/page-data/page-data-enforcement/violentAndAggressiveBehaviour.page.data";
import { historyOfFirearmPossession } from "@data/page-data/page-data-enforcement/historyOfFirearmPossession.page.data";
import { criminalOrAntisocialBehaviour } from "@data/page-data/page-data-enforcement/criminalOrAntisocialBehaviour.page.data";
import { verbalOrWrittenThreats } from "@data/page-data/page-data-enforcement/verbalOrWrittenThreats.page.data";
import { groupProtestsEviction } from "@data/page-data/page-data-enforcement/memberOfGroupProtestsEviction.page.data";

export class EnforcementAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectApplicationType', () => this.selectApplicationType(fieldName as actionRecord)],
      ['selectNameAndAddressForEviction', () => this.selectNameAndAddressForEviction(fieldName as actionRecord)],
      ['selectPoseRiskToBailiff', () => this.selectPoseRiskToBailiff(fieldName as actionRecord)],
      ['selectRiskTypesPosedToBailiff', () => this.selectRiskTypesPosedToBailiff(fieldName as actionRecord)],
      ['provideDetailsViolentOrAggressiveBehaviour', () => this.provideDetailsViolentOrAggressiveBehaviour(fieldName as actionRecord)],
      ['provideDetailsHistoryOfFireArmPossession', () => this.provideDetailsHistoryOfFireArmPossession(fieldName as actionRecord)],
      ['provideDetailsHistoryOfCriminalAntisocialBehavior', () => this.provideDetailsHistoryOfCriminalAntisocialBehavior(fieldName as actionRecord)],
      ['provideDetailsVerbalOrWrittenThreats', () => this.provideDetailsVerbalOrWrittenThreats(fieldName as actionRecord)],
      ['provideDetailsWhichGroup', () => this.provideDetailsWhichGroup(fieldName as actionRecord)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectApplicationType(applicationType: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('clickRadioButton', { question: applicationType.question, option: applicationType.option });
    await performAction('clickButton', yourApplication.continue);
  }

  private async selectNameAndAddressForEviction(nameAndAddress: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    /* The below radio button will be referenced to its corresponding question when this name and address page is worked upon.
    Currently it is a placeholder */
    await performAction('clickRadioButton', nameAndAddress.option);
    await performAction('clickButton', nameAndAddressForEviction.continue);
  }

  private async selectPoseRiskToBailiff(riskToBailiff: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('clickRadioButton', { question: riskToBailiff.question, option: riskToBailiff.option });
    await performAction('clickButton', everyoneLivingAtTheProperty.continue);
  }

  private async selectRiskTypesPosedToBailiff(riskCategory: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    if (Array.isArray(riskCategory.riskTypes)) {
      await performAction('check', riskCategory.riskTypes);
      await performAction('clickButton', riskPosedByEveryoneAtProperty.continue);
      for await (const category of riskCategory.riskTypes) {
        await this.provideRiskCategoryDetails(category);
      }
    } else {
      await performAction('check', riskCategory.riskTypes);
      await performAction('clickButton', riskPosedByEveryoneAtProperty.continue);
      await this.provideRiskCategoryDetails(riskCategory.riskTypes as string);
    }
  }

  private async provideRiskCategoryDetails(risk: string) {
    switch (risk) {
      case riskPosedByEveryoneAtProperty.violentOrAggressiveBehaviour:
        await performAction('provideDetailsViolentOrAggressiveBehaviour', { label: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressive, input: violentOrAggressiveBehaviour.howHaveTheyBeenViolentAndAggressiveInput });
        await performAction('clickButton', violentOrAggressiveBehaviour.continue);
        break;
      case riskPosedByEveryoneAtProperty.historyOfFirearmPossession:
        await performAction('provideDetailsHistoryOfFireArmPossession', { label: historyOfFirearmPossession.whatIsTheirHistoryOfFirearmPossession, input: historyOfFirearmPossession.whatIsTheirHistoryOfFirearmPossessionInput });
        await performAction('clickButton', historyOfFirearmPossession.continue);
        break;
      case riskPosedByEveryoneAtProperty.criminalOrAntisocialBehaviour:
        await performAction('provideDetailsHistoryOfCriminalAntisocialBehavior', { label: criminalOrAntisocialBehaviour.whatIsTheirHistoryOfCriminalAntisocialBehaviour, input: criminalOrAntisocialBehaviour.whatIsTheirHistoryOfCriminalAntisocialBehaviourInput });
        await performAction('clickButton', criminalOrAntisocialBehaviour.continue);
        break;
      case riskPosedByEveryoneAtProperty.verbalOrWrittenThreats:
        await performAction('provideDetailsVerbalOrWrittenThreats', { label: verbalOrWrittenThreats.verbalOrWrittenThreatsMade, input: verbalOrWrittenThreats.verbalOrWrittenThreatsMadeInput });
        await performAction('clickButton', verbalOrWrittenThreats.continue);
        break;
      case riskPosedByEveryoneAtProperty.protestGroup:
        await performAction('provideDetailsWhichGroup', { label: groupProtestsEviction.whichGroupMember, input: groupProtestsEviction.whichGroupMemberInput });
        await performAction('clickButton', groupProtestsEviction.continue);
        break;
      default:
        throw new Error(`The page ${risk} is unknown`);
    }
  };

  private async provideDetailsViolentOrAggressiveBehaviour(violentAggressiveBehaviour: actionRecord) {
    await performValidation('mainHeader', violentOrAggressiveBehaviour.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', violentAggressiveBehaviour.label, violentAggressiveBehaviour.input);
  }

  private async provideDetailsHistoryOfFireArmPossession(firearmPossession: actionRecord) {
    await performValidation('mainHeader', historyOfFirearmPossession.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', firearmPossession.label, firearmPossession.input);
  }

  private async provideDetailsHistoryOfCriminalAntisocialBehavior(criminalAntisocialBehaviour: actionRecord) {
    await performValidation('mainHeader', criminalOrAntisocialBehaviour.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', criminalAntisocialBehaviour.label, criminalAntisocialBehaviour.input);
  }

  private async provideDetailsVerbalOrWrittenThreats(verbalWritten: actionRecord) {
    await performValidation('mainHeader', verbalOrWrittenThreats.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', verbalWritten.label, verbalWritten.input);
  }

  private async provideDetailsWhichGroup(protestGroup: actionRecord) {
    await performValidation('mainHeader', verbalOrWrittenThreats.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', protestGroup.label, protestGroup.input);
  }
}
