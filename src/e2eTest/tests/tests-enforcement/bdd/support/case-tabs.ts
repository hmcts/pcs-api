/**
 * Reading simple field rows off a CCD case tab.
 *
 * A tab of CCD fields renders one `<tr>` per populated field, with the field label in the `<th>` and the
 * rendered value in the `<td>`. The enforcement tabs are pcs-api markup instead, with GOV.UK summary list rows
 * (`dt` / `dd`); both are read. A missing key means "not set" rather than "empty string": CCD omits an empty
 * field, and the markup shows a placeholder paragraph instead of a row.
 */
import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { activeTab, openCaseTab } from './enforcement-status-tab';
import { ensureSignedIn } from './session';

export const RISK_ASSESSMENT_TAB = 'Risk assessment';

/** The label/value pairs on the tab that is currently selected. */
export async function tabFields(page: Page): Promise<Record<string, string>> {
  const panel = activeTab(page);
  const tableRows = panel.locator('tr').filter({ has: page.locator('th') });
  const summaryRows = panel.locator('.govuk-summary-list__row');
  await expect
    .poll(async () => (await tableRows.count()) + (await summaryRows.count()), { timeout: VERY_LONG_TIMEOUT })
    .toBeGreaterThan(0);

  const fields: Record<string, string> = {};
  for (let index = 0; index < (await tableRows.count()); index++) {
    const row = tableRows.nth(index);
    const label = (await row.locator('th').first().innerText()).trim();
    const cells = row.locator('td');
    if (!(await cells.count()) || !label) {
      continue;
    }
    fields[label] = (await cells.first().innerText()).trim();
  }
  for (let index = 0; index < (await summaryRows.count()); index++) {
    const row = summaryRows.nth(index);
    fields[(await row.locator('dt').innerText()).trim()] = (await row.locator('dd').innerText()).trim();
  }
  return fields;
}

/** Opens a tab on the case and returns its field rows. */
export async function openTabFields(page: Page, caseReference: string, tabName: string): Promise<Record<string, string>> {
  await openCaseTab(page, caseReference, tabName);
  return tabFields(page);
}

/** Asserts one field's value on a tab, polling so a value written by a just-submitted event is picked up. */
export async function expectTabField(
  page: Page,
  caseReference: string,
  tabName: string,
  label: string,
  value: string
): Promise<void> {
  await openCaseTab(page, caseReference, tabName);
  await expect.poll(async () => (await tabFields(page))[label], { timeout: VERY_LONG_TIMEOUT }).toBe(value);
}

/** Whether the signed-in user can see a tab at all. Waits for the tab strip before concluding "no". */
export async function isTabVisible(page: Page, caseReference: string, tabName: string): Promise<boolean> {
  await ensureSignedIn(page);
  await page.goto(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/PCS/${caseReference}#Summary`, {
    waitUntil: 'domcontentloaded',
  });
  await expect(page.getByRole('tab', { name: 'Summary', exact: true })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  return (await page.getByRole('tab', { name: tabName, exact: true }).count()) > 0;
}
