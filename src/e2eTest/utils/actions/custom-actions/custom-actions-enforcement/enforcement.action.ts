import { Page } from "@playwright/test";
import { performAction, performValidation } from "@utils/controller-enforcement";
import { IAction, actionData, actionRecord } from "@utils/interfaces/action.interface";
import { yourApplication, nameAndAddressForEviction, everyoneLivingAtTheProperty, evictionCouldBeDelayed, vulnerableAdultsAndChildren,
         violentOrAggressiveBehaviour, firearmPossession, criminalOrAntisocialBehaviour, riskPosedByEveryoneAtProperty} from "@data/page-data/page-data-enforcement";
import { enforcementTestCaseNumber } from "../searchCase.action";

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
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async selectApplicationType(applicationType: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber});
    await performAction('clickRadioButton', {question: applicationType.question, option: applicationType.option});
    await performAction('clickButton', yourApplication.continue);
  }

  private async selectNameAndAddressForEviction(nameAndAddress: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber});
    /* The below radio button will be referenced to its corresponding question when this name and address page is worked upon.
    Currently it is a placeholder */
    await performAction('clickRadioButton', nameAndAddress.option);
    await performAction('clickButton', nameAndAddressForEviction.continue);
  }

  private async selectEveryoneLivingAtTheProperty(riskToBailiff: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber});
    await performAction('clickRadioButton', {question: riskToBailiff.question, option: riskToBailiff.option});
    await performAction('clickButton', everyoneLivingAtTheProperty.continue);
  }

  private async selectRiskPosedByEveryoneAtProperty(riskCategory: actionRecord) {
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber});
    await performAction('check', riskCategory.riskTypes);
    await performAction('clickButton', riskPosedByEveryoneAtProperty.continue);
  }

  private async provideDetailsViolentOrAggressiveBehaviour(violentAggressiveBehaviour: actionRecord) {
    await performValidation('mainHeader', violentOrAggressiveBehaviour.mainHeader);
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber});
    await performAction('inputText', violentAggressiveBehaviour.label, violentAggressiveBehaviour.input);
    await performAction('clickButton', violentOrAggressiveBehaviour.continue);
  }

  private async provideDetailsFireArmPossession(firearm: actionRecord) {
    await performValidation('mainHeader', firearmPossession.mainHeader);
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber});
    await performAction('inputText', firearm.label, firearm.input);
    await performAction('clickButton', firearmPossession.continue);
  }

  private async provideDetailsCriminalOrAntisocialBehavior(criminalAntisocialBehaviour: actionRecord) {
    await performValidation('mainHeader', criminalOrAntisocialBehaviour.mainHeader);
    await performValidation('text', {elementType: 'paragraph', text: 'Case number: ' + enforcementTestCaseNumber});
    await performAction('inputText', criminalAntisocialBehaviour.label, criminalAntisocialBehaviour.input);
    await performAction('clickButton', criminalOrAntisocialBehaviour.continue);
  }
}
