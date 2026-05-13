import { Page } from '@playwright/test';
import { performAction } from '../../controller';
import { actionRecord, IAction } from '@utils/interfaces';
import { globalSearch, home } from '@data/page-data';
import { waitForPageRedirectionTimeout } from 'playwright.config';

export class GlobalSearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['navigateToGlobalSearch', () => this.navigateToGlobalSearch(page)],
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string)],
      ['searchByName', () => this.searchByName(fieldName as string)]
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async navigateToGlobalSearch(page: Page): Promise<void> {
    await performAction('clickButton', home.globalSearchTab);
    await page.waitForTimeout(waitForPageRedirectionTimeout);
  }

  private async searchByCaseReference(caseReference: string): Promise<void> {
    await performAction('inputText', globalSearch.caseReferenceLabel, caseReference);
    await performAction('clickButton', globalSearch.search);
  }

  private async searchByName(name: string): Promise<void> {
    await performAction('inputText', globalSearch.nameOfAPartyLabel, name);
    await performAction('clickButton', globalSearch.search);
  }
}