// Bailiff marks the notice of eviction as served (feature 08).
//
// Reachable because the warrant can be driven to "Awaiting claimant confirmation": allocate a bailiff
// through pcs-api testing support, then schedule an eviction answering "No" to dispensing with service.
// What is still missing is the generated notice of eviction (HDPI-8442 / no Docmosis templates), the WA
// task (HDPI-8627), defendant visibility (HDPI-7539) and control of the clock, which several scenarios
// need in order to put the served time in the future.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { markPending } from '../fixtures/pending';
import { requireCaseReference } from '../support/enforcement-journeys';
import { AWAITING_CLAIMANT_CONFIRMATION, setUpWarrantAtStatus } from '../support/approve-warrant';
import { ensureOnNoticeServedPage, noticeServed } from '../support/notice-served';

Given('a PCS case {string} with a scheduled eviction', async ({ page, context, world }, _reference: string) => {
  await setUpWarrantAtStatus(page, context, world, AWAITING_CLAIMANT_CONFIRMATION);
});

// True by construction: scheduleEvictionAsBailiff answers "No" to dispensing with service, which is what
// leaves a notice of eviction to serve.
Given('the court has not given permission to dispense with service', async ({ world }) => {
  expect(world.evictionScheduled, 'an eviction scheduled without dispensing with service').toBe(true);
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
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  const parts: Record<string, string> = {
    day: String(yesterday.getDate()),
    month: String(yesterday.getMonth() + 1),
    year: String(yesterday.getFullYear()),
    hour: '09',
    minute: '00',
  };
  for (const [part, value] of Object.entries(parts)) {
    await page.locator(`${noticeServed.fields.servedAt}-${part}`).fill(value);
  }
  await page.locator(`${noticeServed.fields.addNote}_No`).check();
});

Then('I see an error that the answer must be 500 characters or fewer', async ({ page }) => {
  await expect(page.getByText(noticeServed.tooLongError).first()).toBeVisible();
});

Then('the notice of eviction has been served', async ({ page, world }) => {
  await expect(page.getByRole('heading', { name: noticeServed.confirmationHeader })).toBeVisible();
  expect(requireCaseReference(world)).toBeDefined();
});
