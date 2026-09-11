import { Page, expect } from '@playwright/test';

import { workAccess } from '@data/page-data-figma/page-data-common-component/workAccess.page.data';
import { caseList } from '@data/page-data/caseList.page.data';
import { LONG_TIMEOUT } from '../../playwright.config';
import { pageHeading } from './locator.utils';

function viewTasksRadio(page: Page) {
  return page.getByRole('radio', { name: workAccess.viewTasksAndCasesOption, exact: true });
}

/**
 * After a judicial login XUI shows either the "Work access" booking page or goes straight
 * to the case list. Waits for whichever arrives instead of asserting one of them, then
 * reports true when the booking page was skipped.
 */
export async function isJudgeBookingSkipped(page: Page): Promise<boolean> {
  const radio = viewTasksRadio(page);
  const caseListHeading = pageHeading(page, caseList.mainHeader);

  await expect(radio.or(caseListHeading).first()).toBeVisible({ timeout: LONG_TIMEOUT });

  return !(await radio.isVisible());
}

/** Completes the "Work access" booking page when it is shown. No-op otherwise. */
export async function completeJudgeBooking(page: Page): Promise<void> {
  if (await isJudgeBookingSkipped(page)) {
    return;
  }
  const radio = viewTasksRadio(page);
  await radio.check();
  await page.getByRole('button', { name: workAccess.continueButton, exact: true }).click();
}
