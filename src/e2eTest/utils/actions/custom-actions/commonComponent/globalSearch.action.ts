import { expect, Locator, Page } from '@playwright/test';
import { performAction, performValidation } from '@utils/controller';
import { actionRecord, IAction } from '@utils/interfaces';
import {
  challengedAccessSuccess,
  challengedCaseDetails,
  globalSearch,
  noResultFound,
  searchResults,
  whyDoYouNeedToAccessThisCase,
  workAccess
} from '@data/page-data-figma';
import { home } from '@data/page-data';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from 'playwright.config';

export class GlobalSearchCaseAction implements IAction {
  async execute(
    page: Page,
    action: string,
    fieldName: string | actionRecord,
    value?: string | actionRecord
  ): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['accessingTheSearch', () => this.accessingTheSearch(page)],
      ['searchByCaseReference', () => this.searchByCaseReference(fieldName as string, page, value as string | undefined)],
      ['invalidCaseReferenceSearch', () => this.invalidCaseReferenceSearch(page)],
      ['changeSearchLink', () => this.changeSearchLink(page)],
      ['handleJudgeBookingPageForGlobalSearch', () => this.handleJudgeBookingPageForGlobalSearch(page)],
      ['submitGlobalSearch', () => this.submitGlobalSearch(page)],
      ['executeSearch', () => this.executeSearch(page)],
      ['validateResults', () => this.validateResults(page)],
      ['validateResultsWithRetry', () => this.validateResultsWithRetry(page)],
      ['validateChallengedAccessLink', () => this.validateChallengedAccessLink(page)],
      ['requestChallengedAccess', () => this.requestChallengedAccess(page, fieldName as actionRecord)]
    ]);

    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) {
      throw new Error(`No action found for '${action}'`);
    }

    await actionToPerform();
  }

  private async accessingTheSearch(page: Page): Promise<void> {
    await performAction('clickButton', home.globalSearchTab);
  }

  private async searchByCaseReference(caseReference: string, page: Page, serviceOption?: string): Promise<void> {
    await performAction('inputText', globalSearch.DigitCaseReferenceLabel, caseReference);
    await performAction('select', globalSearch.servicesLabel, serviceOption ?? globalSearch.servicesDropdownOption2);
    await this.executeSearch(page);
    await performValidation('mainHeader', searchResults.mainHeader);
  }

  private async invalidCaseReferenceSearch(page: Page): Promise<void> {
    await performAction('inputText', globalSearch.DigitCaseReferenceLabel, globalSearch.invalidCaseReferenceInputText);
    await performAction('select', globalSearch.servicesLabel, globalSearch.servicesDropdownOption1);
    await this.executeSearch(page);
    await performValidation('mainHeader', noResultFound.mainHeader);
  }

  private async changeSearchLink(page: Page): Promise<void> {
    await performAction('clickLink', searchResults.changeSearchLink);
    await performValidation('mainHeader', globalSearch.mainHeader);
  }

  private async handleJudgeBookingPageForGlobalSearch(page: Page): Promise<void> {
    await performValidation('mainHeader', workAccess.mainHeader);
    const judgeRadio = page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true });
    await expect(judgeRadio).toBeVisible();
    await judgeRadio.check();
    await performAction('clickButton', workAccess.continueButton);
    await performAction('clickButton', home.globalSearchTab);
  }

  private async submitGlobalSearch(page: Page): Promise<void> {
    await page.locator('button[type="submit"]').click();
  }

  private async executeSearch(page: Page): Promise<void> {
    await this.submitGlobalSearch(page);
  }

  private getCaseReference(): string {
    const caseReference = String(process.env.CASE_NUMBER ?? '').trim();
    if (!caseReference) {
      throw new Error('CASE_NUMBER environment variable is required for global search validation.');
    }

    return caseReference;
  }

  private getNormalizedCaseReference(caseReference = this.getCaseReference()): string {
    return caseReference.replace(/\D/g, '');
  }

  private async validateSearchHeader(page: Page): Promise<void> {
    await expect(page.getByRole('heading', { name: searchResults.mainHeader })).toBeVisible();
  }

  private async validateResults(page: Page): Promise<void> {
    const caseReference = this.getCaseReference();
    const normalizedCaseReference = this.getNormalizedCaseReference(caseReference);
    await this.validateSearchHeader(page);

    const resultRow = await this.findCaseReferenceRowAcrossPages(page, normalizedCaseReference);
    const caseCellText = (await resultRow.locator('td').first().innerText()).trim();
    const normalizedCellText = caseCellText.replace(/\D/g, '');
    const formattedCaseReference = caseReference.replace(/(\d{4})(?=\d)/g, '$1-');

    expect(normalizedCellText).toContain(normalizedCaseReference);

    const caseNameText = caseCellText.replace(caseReference, '').replace(formattedCaseReference, '').trim();
    expect(caseNameText.length).toBeGreaterThan(0);

    await expect(resultRow).toContainText(searchResults.serviceLabel);
    await expect(resultRow).toContainText(searchResults.stateLabel);
    await expect(resultRow).toContainText(searchResults.locationLabel);

    const viewLink = resultRow.getByRole('link', { name: searchResults.viewLinkText });
    await expect(viewLink).toBeVisible();
    await viewLink.click();

    await performAction('clickTab', home.caseSummary);
    await performValidation('mainHeader', home.caseSummary);
  }

  private async validateResultsWithRetry(page: Page): Promise<void> {
    const maxRetries = 6;

    for (let retryCount = 0; retryCount < maxRetries; retryCount++) {
      try {
        await this.validateResults(page);
        return;
      } catch (error: any) {
        const shouldRetry = String(error?.message ?? '').includes('was not found on any paginated search result page');

        if (!shouldRetry || retryCount === maxRetries - 1) {
          throw error;
        }

        await page.reload({ waitUntil: 'domcontentloaded' });
        await this.validateSearchHeader(page);
      }
    }
  }

  private async validateChallengedAccessLink(page: Page): Promise<void> {
    const normalizedCaseReference = this.getNormalizedCaseReference();
    await this.validateSearchHeader(page);

    const resultRow = await this.findCaseReferenceRowAcrossPages(page, normalizedCaseReference);
    const challengedAccessLink = resultRow.getByRole('link', { name: /challenged access/i });

    await expect(challengedAccessLink).toBeVisible();
    await expect(challengedAccessLink).toBeEnabled();
  }

  private async requestChallengedAccess(page: Page, accessReason: actionRecord): Promise<void> {
    if (!accessReason?.option) {
      throw new Error('A challenge access reason must be provided.');
    }

    const normalizedCaseReference = this.getNormalizedCaseReference();
    await this.validateSearchHeader(page);

    const resultRow = await this.findCaseReferenceRowAcrossPages(page, normalizedCaseReference);
    const challengedAccessLink = resultRow.getByRole('link', { name: /challenged access/i });

    await expect(challengedAccessLink).toBeVisible();
    await expect(challengedAccessLink).toBeEnabled();
    await challengedAccessLink.click();

    await expect(
      page.getByRole('heading', {
        name: challengedCaseDetails.caseDetailsSubHeader,
        exact: true
      })
    ).toBeVisible();

    await performAction('clickButton', challengedCaseDetails.requestAccessbutton);

    await expect(
      page.getByText(whyDoYouNeedToAccessThisCase.whyDoYouNeedToAccessThisCaseQuestion, { exact: true })
    ).toBeVisible();

    await performAction('clickRadioButton', {
      question: whyDoYouNeedToAccessThisCase.whyDoYouNeedToAccessThisCaseQuestion,
      option: accessReason.option
    });

    if (accessReason.option === whyDoYouNeedToAccessThisCase.otherReasonRadioOption) {
      const otherReasonTextbox = page
        .getByRole('group', {
          name: whyDoYouNeedToAccessThisCase.whyDoYouNeedToAccessThisCaseQuestion
        })
        .getByRole('textbox');

      await otherReasonTextbox.fill(accessReason.text?.toString() ?? '');
    }

    const challengedAccessRequest = page.waitForResponse(
      (response) =>
        response.url().includes('/api/challenged-access-request') && response.status() === 201
    );

    await performAction('clickButton', whyDoYouNeedToAccessThisCase.submitButton);
    await challengedAccessRequest;

    await expect(
      page.getByRole('heading', {
        name: new RegExp(challengedAccessSuccess.successMessage, 'i')
      })
    ).toBeVisible({ timeout: SHORT_TIMEOUT });

    await page.waitForURL(/challenged-access-request\/success/, { timeout: LONG_TIMEOUT });

    await expect(
      page.getByRole('heading', { name: new RegExp(challengedAccessSuccess.successMessage, 'i') })
    ).toBeVisible({ timeout: SHORT_TIMEOUT });
    await expect(page.getByText(normalizedCaseReference)).toBeVisible();
    await expect(page.getByText(/you can access this case file until midnight tonight/i)).toBeVisible();
    await expect(page.getByText(/your request will be logged for auditing purposes/i)).toBeVisible();
    await expect(page.getByRole('link', { name: challengedAccessSuccess.viewCaseFileLink })).toBeVisible();

    await performAction('clickLink', challengedAccessSuccess.viewCaseFileLink);
    await performValidation('mainHeader', home.caseSummary);
    await performAction('clickTab', home.caseDetails);

    await expect(page.getByText('Claim details', { exact: true }).first()).toBeVisible({
      timeout: LONG_TIMEOUT
    });
  }

  private async findCaseReferenceRowAcrossPages(page: Page, normalizedCaseReference: string): Promise<Locator> {
    const maxPagesToScan = 50;
    const rows = page.locator('tbody tr');

    for (let pageCounter = 0; pageCounter < maxPagesToScan; pageCounter++) {
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
