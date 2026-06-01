import { expect, Page } from '@playwright/test';
import { performAction, performValidation } from '../../../controller';
import { actionRecord, IAction } from '@utils/interfaces';
import { globalSearch,searchResults,noResultFound } from '@data/page-data-figma';
import { home } from '@data/page-data';

export class GlobalSearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['accessingTheSearch', () => this.accessingTheSearch(page)],
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string, page)],
      ['invalidCaseReferenceSearch', () => this.invalidCaseReferenceSearch(fieldName as string, page)],
      ['changeSearchLink', () => this.changeSearchLink(fieldName as string, page)],
      ['validateResults', () => this.validateResults(page)]
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

  private async changeSearchLink(changeSearch: string, page: Page): Promise<void> {
    await performAction('clickLink', searchResults.changeSearchLink);
    await performValidation('mainHeader', globalSearch.mainHeader);
  }

  private async validateResults(page: Page): Promise<void> {
    const caseReference = String(process.env.CASE_NUMBER ?? '');
    const resultRow = page.locator('tr').filter({ hasText: caseReference });
    const caseCell = resultRow.locator('td').first();
    await expect(page.getByRole('heading', { name: searchResults.mainHeader })).toBeVisible();
    await expect(resultRow).toContainText(caseReference);
    await expect(caseCell).toContainText(caseReference);
    const caseCellText = (await caseCell.innerText()).trim();
    const caseNameText = caseCellText.replace(caseReference, '').trim();
    expect(caseNameText.length).toBeGreaterThan(0); 
    await expect(resultRow).toContainText(searchResults.serviceLabel);  
    await expect(resultRow).toContainText(searchResults.stateLabel);
    await expect (resultRow).toContainText(searchResults.locationLabel);
    await expect(resultRow.getByRole('link', { name: searchResults.viewLinkText })).toBeVisible();
    await resultRow.getByRole('link', { name: searchResults.viewLinkText }).click();
    await expect(page).toHaveURL(/#Summary$/);
    await expect(page.locator('.mat-tab-label-active .mat-tab-label-content', { hasText: /^Summary$/ })).toBeVisible();
  }
}