import { Page } from '@playwright/test';
import { performAction } from '../../controller';
import { actionData, actionRecord, IAction } from '@utils/interfaces';
import { caseList, home } from '@data/page-data';
import { waitForPageRedirectionTimeout } from 'playwright.config';

export class SearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, caseData: string): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['searchCaseFromFindCase', () => this.searchCaseFromFindCase(page, fieldName)],
      ['filterCaseFromCaseList', () => this.filterCaseFromCaseList(page, fieldName)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  async searchCaseFromFindCase(page: Page, caseNumber: actionData): Promise<void> {
    await performAction('clickButton', home.findCaseTab);
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('inputText', caseList.caseNumberLabel, caseNumber);
    await performAction('clickButton', caseList.apply);
    await performAction('clickButton', caseNumber);
  }

  private async filterCaseFromCaseList(page: Page, caseState: actionData) {
    await performAction('clickButton', home.caseListTab)
    await performAction('select', caseList.jurisdictionLabel, caseList.possessionsJurisdiction);
    await performAction('select', caseList.caseTypeLabel, caseList.caseType.civilPossessions);
    await performAction('select', caseList.stateLabel, caseState);
    await performAction('clickButton', caseList.apply);
    await page.waitForTimeout(waitForPageRedirectionTimeout);
  }
}
