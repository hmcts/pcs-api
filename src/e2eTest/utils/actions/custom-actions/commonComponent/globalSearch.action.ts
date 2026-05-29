import { expect, Page } from '@playwright/test';
import { performAction } from '../../../controller';
import { actionRecord, IAction } from '@utils/interfaces';
import { globalSearch } from '@data/page-data-figma';
import { home } from '@data/page-data';

export class GlobalSearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['accessingTheSearch', () => this.accessingTheSearch(page)],
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string, page)],
      ['invalidCaseReferenceSearch', () => this.invalidCaseReferenceSearch(fieldName as string, page)],
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async accessingTheSearch(page: Page): Promise<void> {
    await performAction('clickButton', home.globalSearchTab);
  }

  private async searchByCaseReference(caseReference: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.caseReferenceLabel, caseReference);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await performAction('clickButton', globalSearch.searchButton);
  }
  
  private async invalidCaseReferenceSearch(caseReference: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.caseReferenceLabel, caseReference);
    await performAction('select', globalSearch.servicesDropdownLabel, globalSearch.servicesDropdownOption2);
    await performAction('clickButton', globalSearch.searchButton);
  }
}