import { Page } from "@playwright/test";
import { performAction, performValidation } from "@utils/controller-enforcement";
import { IAction, actionData, actionRecord } from "@utils/interfaces/action.interface";
import { yourApplication } from "@data/page-data/page-data-enforcement/yourApplication.page.data";
import { enforcementTestCaseNumber } from "../searchCase.action";
import { nameAndAddressForEviction } from "@data/page-data/page-data-enforcement/nameAndAddressForEviction.page.data";

export class EnforcementAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, data?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['selectApplicationType', () => this.selectApplicationType(fieldName as actionRecord)],
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
}
