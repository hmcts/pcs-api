// A tab of CCD fields renders one row per field; the enforcement tabs are pcs-api markup with GOV.UK summary
// list rows instead, so both are read.
import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { activeTab, openCaseTab } from './enforcement-status-tab';
import { ensureSignedIn } from './session';

export const RISK_ASSESSMENT_TAB = 'Risk assessment';

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

export async function openTabFields(page: Page, caseReference: string, tabName: string): Promise<Record<string, string>> {
  await openCaseTab(page, caseReference, tabName);
  return tabFields(page);
}

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

export async function isTabVisible(page: Page, caseReference: string, tabName: string): Promise<boolean> {
  await ensureSignedIn(page);
  await page.goto(`${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/PCS/${caseReference}#Summary`, {
    waitUntil: 'domcontentloaded',
  });
  await expect(page.getByRole('tab', { name: 'Summary', exact: true })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  return (await page.getByRole('tab', { name: tabName, exact: true }).count()) > 0;
}
