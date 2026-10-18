/**
 * Reading the "Enforcement status" tab. The tab is the only place the enforcement's status, type and
 * warrant/writ number are visible, so it is how the steps assert them.
 *
 * `CaseType` renders the `enforcements` collection there, so each enforcement is a CCD complex-field
 * panel: a `Warrants and writs N` title followed by one `Type` / `Warrant or writ number` / `Status` /
 * `Status updated` / `Scheduled eviction` / `Date of transfer to the High Court` row per populated field.
 * CCD omits a row entirely when the field is empty, so callers must treat a missing key as "not set"
 * rather than expecting a blank string.
 */
import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { getCaseTypeId } from '@utils/common/caseType.utils';

export const ENFORCEMENT_STATUS_TAB = 'Enforcement status';
export const ENFORCEMENT_COLLECTION_LABEL = 'Warrants and writs';

/** One enforcement as the tab shows it: the field labels mapped to their rendered values. */
export type EnforcementRow = Record<string, string>;

export async function openCaseTab(page: Page, caseReference: string, tabName: string): Promise<void> {
  const url = `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${caseReference}#${encodeURIComponent(tabName)}`;
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('tab', { name: tabName, exact: true })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  // Selecting the tab by hash alone is unreliable once XUI has already rendered another tab.
  await page.getByRole('tab', { name: tabName, exact: true }).click();
}

/**
 * Every enforcement on the open case, in the order the tab lists them. Polls because XUI renders the
 * collection asynchronously after the tab is selected.
 */
export async function enforcementRows(page: Page): Promise<EnforcementRow[]> {
  const panels = page.locator('ccd-read-complex-field-table .complex-panel');
  await expect.poll(async () => panels.count(), { timeout: VERY_LONG_TIMEOUT }).toBeGreaterThan(0);

  const rows: EnforcementRow[] = [];
  for (let index = 0; index < (await panels.count()); index++) {
    const panel = panels.nth(index);
    const fields = panel.locator('tr.complex-panel-simple-field');
    const row: EnforcementRow = {};
    for (let field = 0; field < (await fields.count()); field++) {
      const label = (await fields.nth(field).locator('th').innerText()).trim();
      const value = (await fields.nth(field).locator('td').innerText()).trim();
      row[label] = value;
    }
    rows.push(row);
  }
  return rows;
}

/** The case's only enforcement. Fails if the tab shows none, or more than one. */
export async function soleEnforcement(page: Page): Promise<EnforcementRow> {
  const rows = await enforcementRows(page);
  expect(rows, 'exactly one enforcement on the "Enforcement status" tab').toHaveLength(1);
  return rows[0];
}

/**
 * The enforcement the case is working on: its only one, or, when `earlierCount` enforcements came before it,
 * the first the tab lists (the live one, else the most recently changed). Fails if the tab does not show
 * exactly `earlierCount + 1` enforcements.
 */
export async function currentEnforcement(page: Page, earlierCount = 0): Promise<EnforcementRow> {
  if (earlierCount === 0) {
    return soleEnforcement(page);
  }
  const rows = await enforcementRows(page);
  expect(rows, `${earlierCount + 1} enforcements on the "Enforcement status" tab`).toHaveLength(earlierCount + 1);
  return rows[0];
}

/**
 * Asserts the status of the case's current enforcement (see {@link currentEnforcement}), opening the tab
 * first. Polls the whole read so that a status set by a just-submitted event is picked up once CCD has it.
 */
export async function expectEnforcementStatus(
  page: Page,
  caseReference: string,
  status: string,
  earlierCount = 0
): Promise<void> {
  await openCaseTab(page, caseReference, ENFORCEMENT_STATUS_TAB);
  await expect
    .poll(async () => (await currentEnforcement(page, earlierCount))['Status'], { timeout: VERY_LONG_TIMEOUT })
    .toBe(status);
}

/** The warrant or writ number of the case's only enforcement, or undefined when none has been generated. */
export async function enforcementNumber(page: Page, caseReference: string): Promise<string | undefined> {
  await openCaseTab(page, caseReference, ENFORCEMENT_STATUS_TAB);
  return (await soleEnforcement(page))['Warrant or writ number'];
}
