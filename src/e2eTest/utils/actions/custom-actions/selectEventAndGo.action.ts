import { Page } from '@playwright/test';
import { IAction, actionRecord } from '@utils/interfaces/action.interface';
import { SelectAction } from '@utils/actions/element-actions/select.action';
import { ClickButtonAction } from '@utils/actions/element-actions/clickButton.action';
import { caseSummary } from '@data/page-data/caseSummary.page.data';
import { home } from '@data/page-data/home.page.data';
import { actionRetries, waitForPageRedirectionTimeout } from 'playwright.config';

/**
 * Selects a next-step event, clicks Go, and confirms the event actually launched.
 *
 * Deliberately imports **no** controller. Each action family imports its own
 * (`@utils/controller`, `-enforcement`, `-genApps`, `-caseManagement`), so an action that
 * imported one could not be registered in the others' registries without a circular dependency.
 * Driving SelectAction and ClickButtonAction directly avoids that, so this can be registered
 * everywhere and the remaining unverified select-then-Go sites can adopt it.
 */
export class SelectEventAndGoAction implements IAction {
  private readonly select = new SelectAction();
  private readonly clickButton = new ClickButtonAction();

  async execute(page: Page, action: string, event: actionRecord): Promise<void> {
    const eventType = String(event.eventType);
    const nextPage = event.nextPage as string | undefined;
    const dropdown = page.locator('select#next-step, select[id$="event-trigger-select"]').first();

    await this.select.execute(page, 'select', caseSummary.nextStepEventList, eventType);

    // Record what each Go click sends. When this fails, the selection is correct and CCD reports no
    // error, so the open question is whether the click reaches CCD at all — a request that is never
    // made and one that is made and refused look identical from the page.
    const triggerCalls: string[] = [];
    const onResponse = (response: { url: () => string; status: () => number }) => {
      const url = response.url();
      if (url.includes('trigger') || url.includes('event-trigger')) {
        triggerCalls.push(`${response.status()} ${url.replace(/^https?:\/\/[^/]+/, '')}`);
      }
    };
    page.on('response', onResponse);

    try {
    for (let attempt = 1; attempt <= actionRetries; attempt++) {
      if (attempt > 1) {
        // Report the selected option's *label*, not inputValue(). CCD binds objects to the option
        // values, so inputValue() returns Angular's index serialisation ("1: Object") for a
        // perfectly correct selection — which reads like a defect and is not one.
        const selected = await dropdown
          .evaluate((el: HTMLSelectElement) => el.selectedOptions[0]?.textContent?.trim() ?? '<none>')
          .catch(() => '<unreadable>');
        console.warn(`[selectEventAndGo] attempt ${attempt} for "${eventType}": dropdown has `
          + `"${selected}" selected and Go has not moved the page`);
      }
      await this.clickButton.execute(page, 'clickButton', caseSummary.go, '');

      const left = await page
        .locator('h1', { hasText: home.caseSummary })
        .first()
        .waitFor({ state: 'detached', timeout: waitForPageRedirectionTimeout })
        .then(() => true)
        .catch(() => false);
      if (left) {
        return;
      }
    }
    const heading = await page.locator('h1').first().innerText().catch(() => '<no heading>');
    const errorSummary = await page
      .locator('.govuk-error-summary, .error-summary, #error-summary-title, .alert-message')
      .first().innerText().catch(() => '');
    throw new Error(`Event "${eventType}" never launched after ${actionRetries} attempts `
      + `— page shows "${heading}"${nextPage ? `, wanted "${nextPage}"` : ''}`
      + `; event-trigger calls: ${triggerCalls.length ? triggerCalls.join('; ') : '<none>'}`
      + `${errorSummary ? `; error: ${errorSummary.replace(/\s+/g, ' ').slice(0, 300)}` : ''}`);
    } finally {
      page.off('response', onResponse);
    }
  }
}
