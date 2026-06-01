import { expect, Page } from '@playwright/test';
import { performAction, performValidation } from '../../../controller';
import { actionRecord, IAction } from '@utils/interfaces';
import { globalSearch,searchResults,noResultFound } from '@data/page-data-figma';
import { home,caseSummary } from '@data/page-data';

export class GlobalSearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['accessingTheSearch', () => this.accessingTheSearch(page)],
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string, page)],
      ['invalidCaseReferenceSearch', () => this.invalidCaseReferenceSearch(fieldName as string, page)],
      ['changeSearchCriteria', () => this.changeSearchCriteria(page)],
      ['searchResults', () => this.searchResults(page)]
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async accessingTheSearch(page: Page): Promise<void> {
    await performAction('clickButton', home.globalSearchTab);
  }

  private async searchByCaseReference(caseReference: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.DigitCaseReferenceLabel, caseReference);
    await performAction('select', globalSearch.servicesLabel, globalSearch.servicesDropdownOption2);
    await performAction('clickButton', globalSearch.searchButton);
    await page.locator('button[type="submit"]').click();
    await performValidation('mainHeader', searchResults.mainHeader);
  }
  
  private async invalidCaseReferenceSearch(invalidCaseReference: string, page: Page): Promise<void> {
    await performAction('inputText', globalSearch.DigitCaseReferenceLabel, globalSearch.invalidCaseReferenceInputText);
    await performAction('select', globalSearch.servicesLabel, globalSearch.servicesDropdownOption1);
    await performAction('clickButton', globalSearch.searchButton);
    await page.locator('button[type="submit"]').click();
    await performValidation('mainHeader', noResultFound.mainHeader);
  }

  private async changeSearchCriteria(page: Page): Promise<void> {
    await page.getByRole('link', { name: searchResults.changeSearchLink }).click();
    await expect(page.getByRole('heading', { name: globalSearch.mainHeader })).toBeVisible();
  }

  private async searchResults(page: Page): Promise<void> {
    const caseReference = String(process.env.CASE_NUMBER ?? '');
    const resultRow = page.locator('tr').filter({ hasText: caseReference });
    await expect(page.getByRole('heading', { name: searchResults.mainHeader })).toBeVisible();
    await expect(resultRow).toContainText(caseReference);
    await expect(resultRow).toContainText(searchResults.serviceLabel);  
    await expect(resultRow).toContainText(searchResults.stateLabel);
    await expect (resultRow).toContainText(searchResults.locationLabel);
    await expect(resultRow.getByRole('link', { name: searchResults.viewLinkText })).toBeVisible();
    await resultRow.getByRole('link', { name: searchResults.viewLinkText }).click();
    await expect(page).toHaveURL(/#Summary$/);
    //await expect(page.locator('.mat-tab-label-active .mat-tab-label-content', { hasText: /^Summary$/ })).toBeVisible();
  }
}