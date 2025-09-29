import { Page } from '@playwright/test';
import {createCase} from '@data/page-data/createCase.page.data';
import { performAction } from '../../controller';
import { IAction } from '../../interfaces/action.interface';

export class searchCaseActions implements IAction {
  async execute(page: Page, action: string, caseData: string): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['searchCaseById', () => this.searchCase(caseData)],
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  async searchCase(caseNumber: string): Promise<void> {
    await performAction('select', 'Jurisdiction', createCase.possessionsJurisdiction);
    await performAction('select', 'Case type', createCase.caseType.civilPossessions);
    await performAction('inputText', 'Case Number', caseNumber);
    await performAction('clickButton', 'Apply');
  }
}
