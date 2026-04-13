import {actionData, actionRecord, IAction} from '@utils/interfaces';
import {Page} from '@playwright/test';
import {performAction, performValidation} from '@utils/controller';
import {caseFlags} from '@data/page-data/caseFlags.page.data';
import {caseSummary} from '@data/page-data/caseSummary.page.data';

export class caseFlagsAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createCaseFlags', () => this.createCaseFlags()],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async createCaseFlags() {
    await performAction('select', caseSummary.nextStepEventList, caseSummary.createFlagsEvent);
    await performAction('clickButton', caseSummary.go);
    await performValidation('mainHeader', caseFlags.mainHeader);
    await performAction('clickRadioButton', {
      question: caseFlags.whereShouldThisFlagBeAddedQuestion,
      option: caseFlags.caseLevelRadioButton
    });
    await performAction('clickButton', caseFlags.continueButton);
  }
}
