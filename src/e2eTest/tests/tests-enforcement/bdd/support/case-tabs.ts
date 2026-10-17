/**
 * Reading simple field rows off a CCD case tab.
 *
 * A tab like "Risk assessment" renders one `<tr>` per populated field, with the field label in the `<th>`
 * and the rendered value in the `<td>`. CCD omits the row entirely when the field is empty, so a missing
 * key means "not set" rather than "empty string".
 */
import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { openCaseTab } from './enforcement-status-tab';

export const RISK_ASSESSMENT_TAB = 'Risk assessment';

/** The label/value pairs on the tab that is currently selected. */
export async function tabFields(page: Page): Promise<Record<string, string>> {
  const panel = page.locator('.mat-tab-body-active').first();
  const rows = panel.locator('tr').filter({ has: page.locator('th') });
  await expect.poll(async () => rows.count(), { timeout: VERY_LONG_TIMEOUT }).toBeGreaterThan(0);

  const fields: Record<string, string> = {};
  for (let index = 0; index < (await rows.count()); index++) {
    const row = rows.nth(index);
    const label = (await row.locator('th').first().innerText()).trim();
    const cells = row.locator('td');
    if (!(await cells.count()) || !label) {
      continue;
    }
    fields[label] = (await cells.first().innerText()).trim();
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
  await page.goto(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/PCS/${caseReference}#Summary`, {
    waitUntil: 'domcontentloaded',
  });
  await expect(page.getByRole('tab', { name: 'Summary', exact: true })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  return (await page.getByRole('tab', { name: tabName, exact: true }).count()) > 0;
}
