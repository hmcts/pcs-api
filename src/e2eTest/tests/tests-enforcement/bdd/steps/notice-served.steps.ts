// Bailiff marks the notice of eviction as served (feature 08).
//
// Reachable because the warrant can be driven to "Awaiting claimant confirmation": allocate a bailiff
// through pcs-api testing support, then schedule an eviction answering "No" to dispensing with service.
// What is still missing is the generated notice of eviction (HDPI-8442 / no Docmosis templates), the WA
// task (HDPI-8627) and control of the clock, which several scenarios need in order to put the served time
// in the future. What defendants see once the notice is served is feature 14 (defendant-visibility.steps.ts).
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { markPending } from '../fixtures/pending';
import { requireCaseReference } from '../support/enforcement-journeys';
import {
  AWAITING_CLAIMANT_CONFIRMATION,
  scheduleEvictionOnApprovedWarrant,
  setUpWarrantAtStatus,
} from '../support/approve-warrant';
import {
  ensureOnNoticeServedPage,
  enterServedYesterday,
  markNoticeServedAsBailiff,
  noticeServed,
} from '../support/notice-served';

Given('a PCS case {string} with a scheduled eviction', async ({ page, context, world }, _reference: string) => {
  await setUpWarrantAtStatus(page, context, world, AWAITING_CLAIMANT_CONFIRMATION);
});

// Feature 08's Background has already scheduled the eviction, answering "No" to dispensing with service, so
// this only checks it. Feature 14 starts from an approved warrant, so this schedules it.
Given('the court has not given permission to dispense with service', async ({ page, context, world }) => {
  if (!world.evictionScheduled) {
    await scheduleEvictionOnApprovedWarrant(page, context, world);
  }
  expect(world.evictionScheduled, 'an eviction scheduled without dispensing with service').toBe(true);
  expect(world.serviceDispensedWith ?? false).toBe(false);
});

When('I select {string} without answering', async ({ page }, button: string) => {
  expect(button).toBe('Continue');
  await page.getByRole('button', { name: button, exact: true }).click();
});

Then('I see errors for {string} and {string}', async ({ page }, first: string, second: string) => {
  const summary = page.locator('.govuk-error-summary, .error-summary').first();
  await expect(summary).toBeVisible();
  await expect(summary).toContainText(first);
  await expect(summary).toContainText(second);
});

Then('I am not asked about permission to dispense with service', async ({ page }) => {
  await expect(page.getByText('Has the court given permission to dispense with service?')).toHaveCount(0);
});

/**
 * Puts 501 characters into the "how" box. The served date and the bailiff-note question are filled too,
 * because they are mandatory and their errors would otherwise stop the page before the length rule runs.
 */
When('I enter 501 characters for {string}', async ({ page, $tags }, field: string) => {
  if (field !== noticeServed.servedHowQuestion) {
    return markPending({ summary: `entering 501 characters into "${field}"` }, $tags);
  }
  await ensureOnNoticeServedPage(page);
  await page.locator(noticeServed.fields.servedHow).fill('x'.repeat(noticeServed.servedHowMaxLength + 1));
  await enterServedYesterday(page);
  await page.locator(`${noticeServed.fields.addNote}_No`).check();
});

Then('I see an error that the answer must be 500 characters or fewer', async ({ page }) => {
  await expect(page.getByText(noticeServed.tooLongError).first()).toBeVisible();
});

// Two uses. In feature 08 it follows the bailiff's own submit, so it checks the confirmation. In features 12
// and 14 it is a Given on a case whose eviction has not been scheduled yet, so it schedules the eviction and
// serves the notice, with a bailiff note so that the defendant checks have one to look for.
Then('the notice of eviction has been served', async ({ page, context, world }) => {
  if (world.evictionScheduled) {
    await expect(page.getByRole('heading', { name: noticeServed.confirmationHeader })).toBeVisible();
    expect(requireCaseReference(world)).toBeDefined();
    return;
  }
  if (world.caseReference) {
    await scheduleEvictionOnApprovedWarrant(page, context, world);
  } else {
    await setUpWarrantAtStatus(page, context, world, AWAITING_CLAIMANT_CONFIRMATION);
  }
  await markNoticeServedAsBailiff(page, context, world, 'Dog on the premises');
  world.defendantViews = undefined;
});
