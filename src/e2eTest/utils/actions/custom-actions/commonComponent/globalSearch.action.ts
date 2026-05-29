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
      ['clickCaseNumberLink', () => this.clickCaseNumberLink(page)], 
      ['submitSearch', () => this.submitSearch(page)]
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
    await this.submitSearch(page);
  }
  
  private async clickCaseNumberLink(page: Page): Promise<void> {
    const caseNumber = process.env.CASE_NUMBER;
    if (!caseNumber) throw new Error('CASE_NUMBER environment variable is not set');
    const resultRow = page.getByRole('row').filter({ hasText: caseNumber }).first();
    await expect(resultRow).toBeVisible();

    const viewLink = resultRow.getByRole('link', { name: /^View$/i }).first();
    if (await viewLink.count()) {
      await expect(viewLink).toBeVisible();
      await viewLink.click();
    } else {
      const caseNumberLink = resultRow.getByRole('link', { name: caseNumber, exact: true }).first();
      await expect(caseNumberLink).toBeVisible();
      await caseNumberLink.click();
    }
    await expect(page).toHaveURL(new RegExp(caseNumber));
  }

  private async submitSearch(page: Page): Promise<void> {
    const searchButton = page.getByRole('button', { name: globalSearch.searchButton, exact: true }).first();
    await page.locator('.spinner-container').waitFor({ state: 'detached' }).catch(() => undefined);
    await searchButton.waitFor({ state: 'visible' });
    await searchButton.scrollIntoViewIfNeeded().catch(() => undefined);
    await searchButton.click();
    await page.waitForLoadState();
    await page.locator('.spinner-container').waitFor({ state: 'detached' }).catch(() => undefined);
  }
}