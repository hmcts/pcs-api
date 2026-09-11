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

    // DIAGNOSTIC ONLY — not for merge. caseTabs:96 fails with the right event selected and no error
    // summary, so a request never sent and a request sent and refused are indistinguishable.
    // Both events are needed: 'response' alone cannot tell "nothing was sent" from "something was
    // sent and never answered" — verified locally, where a fetch produced 1 request and 0 responses.
    const sent: string[] = [];
    const answered: string[] = [];
    // CCD paths only. A looser /event|cases/ matched LaunchDarkly's telemetry
    // (202 /events/bulk/<client-id>), which drowned the signal in feature-flag noise.
    const match = (url: string) =>
      /\/data\/internal\/cases\/|\/event-trigger|\/cases\/\d+\/(events|event-triggers)/.test(url);
    const onRequest = (r: { url: () => string }) => {
      if (match(r.url())) sent.push(r.url().replace(/^https?:\/\/[^/]+/, '').slice(0, 90));
    };
    const onResponse = (r: { url: () => string; status: () => number }) => {
      if (match(r.url())) answered.push(`${r.status()} ${r.url().replace(/^https?:\/\/[^/]+/, '').slice(0, 90)}`);
    };
    page.on('request', onRequest);
    page.on('response', onResponse);
    try {

    for (let attempt = 1; attempt <= actionRetries; attempt++) {
      const sentBefore = sent.length;
      const answeredBefore = answered.length;
      await this.clickButton.execute(page, 'clickButton', caseSummary.go, '');
      const selected = await page
        .locator('select#next-step, select[id$="event-trigger-select"]').first()
        .evaluate((el: HTMLSelectElement) => el.selectedOptions[0]?.textContent?.trim() ?? '<none>')
        .catch(() => '<unreadable>');
      console.log(`[caseTabs96] attempt ${attempt} "${eventType}": selected="${selected}" `
        + `sent=${sent.length - sentBefore} answered=${answered.length - answeredBefore} `
        + `[${answered.slice(answeredBefore).join(' | ') || sent.slice(sentBefore).join(' | ') || 'NOTHING SENT'}]`);

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
      + `; sent=${sent.length} answered=${answered.length}`
      + `; requests: ${answered.join(' | ') || sent.join(' | ') || 'NOTHING SENT'}`
      + `${errorSummary ? `; error: ${errorSummary.replace(/\s+/g, ' ').slice(0, 300)}` : ''}`);
    } finally {
      page.off('request', onRequest);
      page.off('response', onResponse);
    }
  }
}
