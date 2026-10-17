/**
 * The `recordWarrantReturn` event (feature 11).
 *
 * One page with show-condition-driven drop downs. `Return` (Final / Interim) reveals either `Outcome`
 * (Final only) or the interim return names; the Final return names are split across two fields, one for
 * the successful outcome and one shared by unsuccessful and cancelled, because they offer different lists.
 *
 * `recordWarrantReturn` is available from "Awaiting scheduling" onwards (`RECORDABLE_STATUSES`), so these
 * scenarios do not need the claimant to have confirmed the eviction first.
 */
import { Page, expect } from '@playwright/test';
import { LONG_TIMEOUT } from 'playwright.config';

export const RECORD_WARRANT_RETURN = 'Record warrant return';

export const warrantReturn = {
  pageLabel: 'Record warrant return',
  typeField: '#warrantReturnType',
  outcomeField: '#warrantReturnOutcome',
  successfulReturnNameField: '#warrantReturnSuccessfulReturnName',
  unsuccessfulReturnNameField: '#warrantReturnUnsuccessfulReturnName',
  interimReturnNameField: '#warrantReturnInterimReturnName',
  additionalDetailsLabel: 'Additional details',
  final: 'Final',
  interim: 'Interim',
  evictionSuccessful: 'Eviction successful',
  evictionUnsuccessful: 'Eviction unsuccessful',
  evictionCancelled: 'Eviction cancelled',
};

/** The drop down a feature step names, by its visible label. */
const FIELD_BY_LABEL: Record<string, string> = {
  Return: warrantReturn.typeField,
  Outcome: warrantReturn.outcomeField,
};

export function returnDropDown(page: Page, label: string) {
  const selector = FIELD_BY_LABEL[label];
  if (!selector) {
    throw new Error(`No warrant return drop down known for "${label}"`);
  }
  return page.locator(selector);
}

/**
 * The `Return name` drop down currently on the page. Which of the three fields is shown depends on the
 * return type and outcome already chosen, and CCD re-evaluates the show conditions asynchronously, so
 * this polls rather than reading visibility once.
 */
export async function visibleReturnNameDropDown(page: Page) {
  const candidates = [
    warrantReturn.successfulReturnNameField,
    warrantReturn.unsuccessfulReturnNameField,
    warrantReturn.interimReturnNameField,
  ];
  const shown = async (): Promise<string | undefined> => {
    for (const selector of candidates) {
      if (await page.locator(selector).isVisible().catch(() => false)) {
        return selector;
      }
    }
    return undefined;
  };
  await expect
    .poll(shown, { timeout: LONG_TIMEOUT, message: 'a "Return name" drop down to be shown' })
    .toBeDefined();
  return page.locator((await shown()) as string);
}

/** Option labels, with CCD's placeholder removed. */
export async function optionsOf(field: ReturnType<Page['locator']>): Promise<string[]> {
  await expect(field).toBeVisible({ timeout: LONG_TIMEOUT });
  const options = await field.locator('option').allTextContents();
  return options.map(option => option.trim()).filter(option => option && !option.startsWith('--'));
}

/**
 * Opens the event if the page is not already showing it. Feature 11's Rule only signs the bailiff in, so a
 * scenario can go straight to "I choose the Final return" without an explicit navigation step.
 */
export async function ensureOnWarrantReturnPage(page: Page): Promise<void> {
  if (await page.locator(warrantReturn.typeField).isVisible().catch(() => false)) {
    return;
  }
  const { openCaseSummary, startEvent } = await import('./enforcement-journeys');
  await openCaseSummary(page);
  await startEvent(RECORD_WARRANT_RETURN);
  await expect(page.locator(warrantReturn.typeField)).toBeVisible({ timeout: LONG_TIMEOUT });
}

export async function chooseReturnType(page: Page, type: string): Promise<void> {
  await ensureOnWarrantReturnPage(page);
  await page.locator(warrantReturn.typeField).selectOption({ label: type });
}

export async function chooseOutcome(page: Page, outcome: string): Promise<void> {
  await ensureOnWarrantReturnPage(page);
  const outcomeField = page.locator(warrantReturn.outcomeField);
  await expect(outcomeField).toBeVisible({ timeout: LONG_TIMEOUT });
  await outcomeField.selectOption({ label: outcome });
}

/** Chooses a return name in whichever of the three fields is showing. */
export async function chooseReturnName(page: Page, returnName: string): Promise<void> {
  await ensureOnWarrantReturnPage(page);
  const field = await visibleReturnNameDropDown(page);
  await field.selectOption({ label: returnName });
}
