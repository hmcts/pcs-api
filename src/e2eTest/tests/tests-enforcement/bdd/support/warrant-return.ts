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
import { LONG_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';

export const RECORD_WARRANT_RETURN = 'Record warrant return';

export const warrantReturn = {
  pageLabel: 'Record warrant return',
  typeField: '#warrantReturnType',
  outcomeField: '#warrantReturnOutcome',
  successfulReturnNameField: '#warrantReturnSuccessfulReturnName',
  unsuccessfulReturnNameField: '#warrantReturnUnsuccessfulReturnName',
  interimReturnNameField: '#warrantReturnInterimReturnName',
  evictionDateTimeField: '#warrantReturnEvictionDateTime',
  additionalDetailsLabel: 'Additional details',
  evictionDetailsHeading: 'Eviction details',
  evictionDetailsRows: ['Claimant type', 'Claimant name', 'Defendant names'],
  checkYourAnswers: 'Check your answers',
  saveAndContinue: 'Save and continue',
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

/** The interim return names, lower-cased: pcs-api's InterimWarrantReturnName. */
const INTERIM_RETURN_NAMES = [
  'application unsuccessful & warrant returned to bailiff',
  'request for additional information',
  'no animal handler in attendance (parties unaware of animal)',
  'no police presence (police refused to attend when asked to)',
  'children left unattended in property',
  'crowds or protests',
  'obstruction by defendants',
  'safeguarding or vulnerability concerns',
  'defendant states they are in breathing space (standard or mental health)',
  'claimant advised defendant in breathing space',
  'other interim return',
  'reopen warrant',
];

/** The one final return name for a successful eviction; every other final name is "Eviction unsuccessful". */
const SUCCESSFUL_RETURN_NAME = 'eviction successful';

export const REOPEN_WARRANT = 'Reopen warrant';

/** The "Additional details" the suite gives with every return; some return names require them. */
export const SUITE_ADDITIONAL_DETAILS = 'Recorded by the enforcement BDD suite';

/** The return, and for a final return the outcome, whose drop down offers `returnName` (ignoring case). */
export function returnFor(returnName: string): { type: string; outcome?: string } {
  const name = returnName.toLowerCase();
  if (INTERIM_RETURN_NAMES.includes(name)) {
    return { type: warrantReturn.interim };
  }
  if (name === SUCCESSFUL_RETURN_NAME) {
    return { type: warrantReturn.final, outcome: warrantReturn.evictionSuccessful };
  }
  return { type: warrantReturn.final, outcome: warrantReturn.evictionUnsuccessful };
}

/**
 * The feature files write return names in their own case ("Warrant Suspended by Order"); pcs-api's labels
 * keep the capitals of the HDPI-8424 list ("Warrant suspended by Order"). Chooses the option ignoring case
 * and returns pcs-api's label.
 */
export async function chooseReturnNameIgnoringCase(page: Page, returnName: string): Promise<string> {
  const field = await visibleReturnNameDropDown(page);
  const labels = await optionsOf(field);
  const label = labels.find(option => option.toLowerCase() === returnName.toLowerCase());
  if (!label) {
    throw new Error(`No return name "${returnName}" offered. Offered: ${labels.join(', ')}`);
  }
  await field.selectOption({ label });
  return label;
}

/** Yesterday at 10:00, as "dd/MM/yyyy" and "HH:mm": a successful eviction cannot be in the future. */
export function pastEviction(): { date: string; time: string } {
  const when = new Date();
  when.setDate(when.getDate() - 1);
  const pad = (value: number): string => String(value).padStart(2, '0');
  return { date: `${pad(when.getDate())}/${pad(when.getMonth() + 1)}/${when.getFullYear()}`, time: '10:00' };
}

/** Fills the "Eviction date and time" inputs from a "dd/MM/yyyy" date and an "HH:mm" time. */
export async function enterReturnEvictionDateTime(page: Page, date: string, time: string): Promise<void> {
  const [day, month, year] = date.split('/');
  const [hour, minute] = time.split(':');
  const parts: Record<string, string> = { day, month, year, hour, minute };
  for (const [part, value] of Object.entries(parts)) {
    const input = page.locator(`${warrantReturn.evictionDateTimeField}-${part}`);
    await expect(input).toBeVisible({ timeout: LONG_TIMEOUT });
    await input.fill(value);
  }
}

/**
 * Answers the "Record warrant return" page for `returnName`: the return and outcome that offer it, the name,
 * a past eviction date and time when the eviction was successful, and additional details. Additional details
 * are optional for most names and mandatory for some; always giving them keeps one route for all of them.
 * Returns pcs-api's label for the name.
 */
export async function answerWarrantReturn(
  page: Page,
  returnName: string,
  additionalDetails = SUITE_ADDITIONAL_DETAILS
): Promise<string> {
  const { type, outcome } = returnFor(returnName);
  await chooseReturnType(page, type);
  if (outcome) {
    await chooseOutcome(page, outcome);
  }
  const label = await chooseReturnNameIgnoringCase(page, returnName);
  if (outcome === warrantReturn.evictionSuccessful) {
    const { date, time } = pastEviction();
    await enterReturnEvictionDateTime(page, date, time);
  }
  await page.getByLabel(warrantReturn.additionalDetailsLabel).fill(additionalDetails);
  return label;
}

export async function expectReturnCheckYourAnswers(page: Page): Promise<void> {
  await expect(page.getByRole('heading', { name: warrantReturn.checkYourAnswers })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
}

/**
 * Records a return named `returnName` on the case's warrant, opening the event if it is not already open,
 * and waits for pcs-api's "Outcome recorded as" confirmation. Returns pcs-api's label for the name.
 */
export async function recordWarrantReturn(
  page: Page,
  returnName: string,
  additionalDetails = SUITE_ADDITIONAL_DETAILS
): Promise<string> {
  await ensureOnWarrantReturnPage(page);
  const label = await answerWarrantReturn(page, returnName, additionalDetails);
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectReturnCheckYourAnswers(page);
  await page.getByRole('button', { name: warrantReturn.saveAndContinue, exact: true }).click();
  await expect(page.getByRole('heading', { name: `Outcome recorded as ${label}` })).toBeVisible({
    timeout: VERY_LONG_TIMEOUT,
  });
  return label;
}

/** The "Eviction details" table: claimant type, claimant name and defendant names, each filled in. */
export async function expectEvictionDetailsTable(page: Page): Promise<void> {
  await expect(page.getByRole('heading', { name: warrantReturn.evictionDetailsHeading })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  const table = page.locator('table').filter({ hasText: warrantReturn.evictionDetailsRows[0] }).first();
  for (const header of warrantReturn.evictionDetailsRows) {
    const row = table.getByRole('row').filter({ has: page.getByRole('rowheader', { name: header, exact: true }) });
    await expect(row.getByRole('cell')).toHaveText(/\S/);
  }
}
