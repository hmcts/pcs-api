import { expect, Locator, Page } from '@playwright/test';
import { performAction, performValidation } from '../../../controller';
import { actionRecord, IAction } from '@utils/interfaces';
import { globalSearch, noResultFound, searchResults, workAccess } from '@data/page-data-figma';
import { caseList, home } from '@data/page-data';

export class GlobalSearchCaseAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['accessingTheSearch', () => this.accessingTheSearch(page)],
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string, page)],
      ['invalidCaseReferenceSearch', () => this.invalidCaseReferenceSearch(page)],
      ['changeSearchLink', () => this.changeSearchLink(page)],
      ['handleJudgeBookingPage', () => this.handleJudgeBookingPage(page)],
      ['validateResults', () => this.validateResults(page)],
      ['validateResultsWithRetry', () => this.validateResultsWithRetry(page)]
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
  
  private async invalidCaseReferenceSearch(page: Page): Promise<void> {
    await performAction('inputText', globalSearch.DigitCaseReferenceLabel, globalSearch.invalidCaseReferenceInputText);
    await performAction('select', globalSearch.servicesLabel, globalSearch.servicesDropdownOption1);
    await performAction('clickButton', globalSearch.searchButton);
    await page.locator('button[type="submit"]').click();
    await performValidation('mainHeader', noResultFound.mainHeader);
  }

  private async changeSearchLink(page: Page): Promise<void> {
    await performAction('clickLink', searchResults.changeSearchLink);
    await performValidation('mainHeader', globalSearch.mainHeader);
  }

  private async handleJudgeBookingPage(page: Page): Promise<void> {
    await performValidation('mainHeader', workAccess.mainHeader);
    await expect(page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true })).toBeVisible();
    await page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true }).check();
    await page.getByRole('button', { name: workAccess.continueButton, exact: true }).click();
    await performValidation('mainHeader', caseList.mainHeader);
  }

  private async validateResults(page: Page): Promise<void> {
    const caseReference = String(process.env.CASE_NUMBER ?? '');
    const normalizedCaseReference = caseReference.replace(/\D/g, '');
    await expect(page.getByRole('heading', { name: searchResults.mainHeader })).toBeVisible();

    const resultRow = await this.findCaseReferenceRowAcrossPages(page, normalizedCaseReference);
    const caseCell = resultRow.locator('td').first();
    const caseCellText = (await caseCell.innerText()).trim();
    const normalizedCellText = caseCellText.replace(/\D/g, '');
    const formattedCaseReference = caseReference.replace(/(\d{4})(?=\d)/g, '$1-');
    expect(normalizedCellText).toContain(normalizedCaseReference);
    const caseNameText = caseCellText.replace(caseReference, '').replace(formattedCaseReference, '').trim();
    expect(caseNameText.length).toBeGreaterThan(0); 
    await expect(resultRow).toContainText(searchResults.serviceLabel);  
    await expect(resultRow).toContainText(searchResults.stateLabel);
    await expect (resultRow).toContainText(searchResults.locationLabel);
    await expect(resultRow.getByRole('link', { name: searchResults.viewLinkText })).toBeVisible();
    await resultRow.getByRole('link', { name: searchResults.viewLinkText }).click();
    await performAction('clickTab', home.caseSummary);
    await performValidation('mainHeader', home.caseSummary);
  }

  private async validateResultsWithRetry(page: Page): Promise<void> {
    const attempts = 6;
    const waitMs = 5000;

    for (let attempt = 1; attempt <= attempts; attempt++) {
      try {
        await this.validateResults(page);
        return;
      } catch (error: any) {
        const shouldRetry = String(error?.message ?? '').includes('was not found on any paginated search result page');
        if (!shouldRetry || attempt === attempts) {
          throw error;
        }

        await page.waitForTimeout(waitMs);
        await page.reload({ waitUntil: 'domcontentloaded' });
        await expect(page.getByRole('heading', { name: searchResults.mainHeader })).toBeVisible();
      }
    }
  }

  private async findCaseReferenceRowAcrossPages(page: Page, normalizedCaseReference: string): Promise<Locator> {
    const maxPagesToScan = 50;
    const rows = page.locator('tbody tr');

    for (let pageIndex = 0; pageIndex < maxPagesToScan; pageIndex++) {
      const rowCount = await rows.count();
      for (let rowIndex = 0; rowIndex < rowCount; rowIndex++) {
        const row = rows.nth(rowIndex);
        const firstCellText = (await row.locator('td').first().innerText()).trim();
        const normalizedRowCaseReference = firstCellText.replace(/\D/g, '');
        if (normalizedRowCaseReference.includes(normalizedCaseReference)) {
          return row;
        }
      }

      const nextPageLink = page.getByRole('link', { name: globalSearch.nextLink, exact: true });
      if ((await nextPageLink.count()) === 0 || !(await nextPageLink.isVisible())) {
        break;
      }

      const currentPageMarker = page.locator('.hmcts-pagination__item--current').first();
      const previousMarkerText = (await currentPageMarker.count()) > 0 ? (await currentPageMarker.innerText()).trim() : '';
      await nextPageLink.click();
      if (previousMarkerText) {
        await expect(currentPageMarker).not.toHaveText(previousMarkerText, { timeout: 10000 });
      }
      await expect(rows.first()).toBeVisible({ timeout: 10000 });
    }

    throw new Error(`Case reference '${normalizedCaseReference}' was not found on any paginated search result page.`);
  }
}