import { Page } from "@playwright/test";
import { performAction, performValidation } from "@utils/controller-enforcement";
import { IAction, actionData, actionRecord } from "@utils/interfaces/action.interface";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { enforcementTestCaseNumber } from "../searchCase.action";
import { nameAndAddressForEviction } from "@data/page-data/page-data-enforcement/nameAndAddressForEviction.page.data";
import { riskPosedByEveryoneAtProperty } from "@data/page-data/page-data-enforcement/riskPosedByEveryoneAtProperty.page.data";
import { everyoneLivingAtTheProperty } from "@data/page-data/page-data-enforcement/everyoneLivingAtTheProperty.page.data";
import { violentOrAggressiveBehaviour } from "@data/page-data/page-data-enforcement/violentOrAggressiveBehaviour.page.data";
import { firearmPossession } from "@data/page-data/page-data-enforcement/firearmPossession.page.data";
import { criminalOrAntisocialBehaviour } from "@data/page-data/page-data-enforcement/criminalOrAntisocialBehaviour.page.data";
import { verbalOrWrittenThreats } from "@data/page-data/page-data-enforcement/verbalOrWrittenThreats.page.data";
import { groupProtestsEviction } from "@data/page-data/page-data-enforcement/groupProtestsEviction.page.data";
import { policeOrSocialServiceVisit } from "@data/page-data/page-data-enforcement/policeOrSocialServiceVisit.page.data";
import { animalsAtTheProperty } from "@data/page-data/page-data-enforcement/animalsAtTheProperty";

export class EnforcementAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectApplicationType', () => this.selectApplicationType(fieldName as actionRecord)],
      ['selectNameAndAddressForEviction', () => this.selectNameAndAddressForEviction(fieldName as actionRecord)],
      ['selectEveryoneLivingAtTheProperty', () => this.selectEveryoneLivingAtTheProperty(fieldName as actionRecord)],
      ['selectRiskPosedByEveryoneAtProperty', () => this.selectRiskPosedByEveryoneAtProperty(fieldName as actionRecord)],
      ['provideDetailsViolentOrAggressiveBehaviour', () => this.provideDetailsViolentOrAggressiveBehaviour(fieldName as actionRecord)],
      ['provideDetailsFireArmPossession', () => this.provideDetailsFireArmPossession(fieldName as actionRecord)],
      ['provideDetailsCriminalOrAntisocialBehavior', () => this.provideDetailsCriminalOrAntisocialBehavior(fieldName as actionRecord)],
      ['provideDetailsVerbalOrWrittenThreats', () => this.provideDetailsVerbalOrWrittenThreats(fieldName as actionRecord)],
      ['provideDetailsGroupProtestsEviction', () => this.provideDetailsGroupProtestsEviction(fieldName as actionRecord)],
      ['provideDetailsPoliceOrSocialServiceVisits', () => this.provideDetailsPoliceOrSocialServiceVisits(fieldName as actionRecord)],
      ['provideDetailsAnimalsAtTheProperty', () => this.provideDetailsAnimalsAtTheProperty(fieldName as actionRecord)],
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

  private async selectEveryoneLivingAtTheProperty(riskToBailiff: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('clickRadioButton', { question: riskToBailiff.question, option: riskToBailiff.option });
    await performAction('clickButton', everyoneLivingAtTheProperty.continue);
  }

  private async selectRiskPosedByEveryoneAtProperty(riskCategory: actionRecord) {
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('check', riskCategory.riskTypes);
    await performAction('clickButton', riskPosedByEveryoneAtProperty.continue);
  }

  private async provideDetailsViolentOrAggressiveBehaviour(violentAggressiveBehaviour: actionRecord) {
    await performValidation('mainHeader', violentOrAggressiveBehaviour.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', violentAggressiveBehaviour.label, violentAggressiveBehaviour.input);
    await performAction('clickButton', violentOrAggressiveBehaviour.continue);
  }

  private async provideDetailsFireArmPossession(firearm: actionRecord) {
    await performValidation('mainHeader', firearmPossession.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', firearm.label, firearm.input);
    await performAction('clickButton', firearmPossession.continue);
  }

  private async provideDetailsCriminalOrAntisocialBehavior(criminalAntisocialBehaviour: actionRecord) {
    await performValidation('mainHeader', criminalOrAntisocialBehaviour.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', criminalAntisocialBehaviour.label, criminalAntisocialBehaviour.input);
    await performAction('clickButton', criminalOrAntisocialBehaviour.continue);
  }

  private async provideDetailsVerbalOrWrittenThreats(verbalWritten: actionRecord) {
    await performValidation('mainHeader', verbalOrWrittenThreats.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', verbalWritten.label, verbalWritten.input);
    await performAction('clickButton', verbalOrWrittenThreats.continue);
  }

  private async provideDetailsGroupProtestsEviction(protestGroup: actionRecord) {
    await performValidation('mainHeader', groupProtestsEviction.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', protestGroup.label, protestGroup.input);
    await performAction('clickButton', groupProtestsEviction.continue);
  }

   private async provideDetailsPoliceOrSocialServiceVisits(policeOrSSVisit: actionRecord) {
    await performValidation('mainHeader', policeOrSocialServiceVisit.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', policeOrSSVisit.label, policeOrSSVisit.input);
    await performAction('clickButton', policeOrSocialServiceVisit.continue);
  }

   private async provideDetailsAnimalsAtTheProperty(theAnimalsAtTheProperty: actionRecord) {
    await performValidation('mainHeader', animalsAtTheProperty.mainHeader);
    await performValidation('text', { elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber });
    await performAction('inputText', theAnimalsAtTheProperty.label, theAnimalsAtTheProperty.input);
    await performAction('clickButton', animalsAtTheProperty.continue);
  }
}
