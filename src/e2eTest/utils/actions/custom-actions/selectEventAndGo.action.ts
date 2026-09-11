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

    await this.select.execute(page, 'select', caseSummary.nextStepEventList, eventType);

    for (let attempt = 1; attempt <= actionRetries; attempt++) {
      await this.clickButton.execute(page, 'clickButton', caseSummary.go, '');

      const left = await page
        .locator('h1', { hasText: home.caseSummary })
        .first()
        .waitFor({ state: 'detached', timeout: waitForPageRedirectionTimeout })
        .then(() => true)
        .catch(() => false);
      if (left) {
        // DIAGNOSTIC ONLY — not for merge.
        if (attempt > 1) {
          console.log(`[attemptLog] "${eventType}" launched on attempt ${attempt} of ${actionRetries}`);
        }
        return;
      }
    }
    const heading = await page.locator('h1').first().innerText().catch(() => '<no heading>');
    const errorSummary = await page
      .locator('.govuk-error-summary, .error-summary, #error-summary-title, .alert-message')
      .first().innerText().catch(() => '');
    throw new Error(`Event "${eventType}" never launched after ${actionRetries} attempts `
      + `— page shows "${heading}"${nextPage ? `, wanted "${nextPage}"` : ''}`
      + `${errorSummary ? `; error: ${errorSummary.replace(/\s+/g, ' ').slice(0, 300)}` : ''}`);
  }
}
