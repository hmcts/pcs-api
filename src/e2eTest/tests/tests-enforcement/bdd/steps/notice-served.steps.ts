// Bailiff marks the notice of eviction as served (feature 08).
//
// Reachable because the warrant can be driven to "Awaiting claimant confirmation": allocate a bailiff
// through pcs-api testing support, then schedule an eviction answering "No" to dispensing with service.
// What is still missing is the generated notice of eviction (HDPI-8442 / no Docmosis templates), the WA
// task (HDPI-8627), defendant visibility (HDPI-7539) and control of the clock, which several scenarios
// need in order to put the served time in the future.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import { openCaseSummary, requireCaseReference, signInAs, startEvent } from '../support/enforcement-journeys';
import {
  AWAITING_CLAIMANT_CONFIRMATION,
  confirmEvictionAsClaimant,
  setUpWarrantAtStatus,
} from '../support/approve-warrant';
import { MARK_NOTICE_SERVED, ensureOnNoticeServedPage, noticeServed } from '../support/notice-served';
import {
  RESCHEDULE_EVICTION,
  dispenseWithServiceRadio,
  enterEvictionDateTime,
  expectCheckYourAnswers,
  resolveEvictionDate,
  resolveEvictionTime,
  scheduleEviction,
} from '../support/schedule-eviction';
import { expectBailiffConfirmationWithWarrantNumber } from '../support/bailiff-events';
import { RISK_ASSESSMENT_TAB } from '../support/case-tabs';
import { expectEnforcementSection, openCaseTab, tabSections } from '../support/enforcement-status-tab';
import { localUserName, localUsers } from '@config/local-stack.config';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

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

/** Answers the page and goes on to CCD's check-your-answers, ready for "I submit the event". */
Given('I served the notice on {string} at {string} {string}', async ({ page, world }, date: string, time: string, how: string) => {
  await ensureOnNoticeServedPage(page);
  const parts: Record<string, string> = {
    ...(resolveEvictionDate(date) ?? {}),
    ...(resolveEvictionTime(time) ?? {}),
  };
  for (const part of ['day', 'month', 'year', 'hour', 'minute']) {
    await page.locator(`${noticeServed.fields.servedAt}-${part}`).fill(parts[part] ?? '');
  }
  await page.locator(noticeServed.fields.servedHow).fill(how);
  await page.locator(`${noticeServed.fields.addNote}_No`).check();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectCheckYourAnswers(page);
  world.currentEvent = MARK_NOTICE_SERVED;
});

Then('I see the confirmation with the case number and warrant number', async ({ page, world }) => {
  expect(world.warrantNumber, 'a warrant number generated when the application was approved').toBeDefined();
  await expectBailiffConfirmationWithWarrantNumber(
    page, noticeServed.confirmationHeader, requireCaseReference(world), world.warrantNumber as string);
});

Then(
  'the {string} tab shows {string} at {string} and {string} under {string}',
  async ({ page, world }, _tabName: string, date: string, time: string, how: string, section: string) => {
    await expectEnforcementSection(page, requireCaseReference(world), section, block => {
      expect(block?.rows['Date served']).toBe(date);
      expect(block?.rows['Time served']).toBe(time);
      expect(block?.rows['How it was served']).toBe(how);
    });
  }
);

/**
 * The Background scheduled the eviction without permission to dispense with service, and only a reschedule
 * can change that answer. A reschedule is offered once the claimant has said whether they can attend, so the
 * claimant confirms first.
 */
Given('the court has given permission to dispense with service', async ({ page, context, world }) => {
  approximatePrecondition(
    'HDPI-7549',
    'the eviction was scheduled without dispensing with service; the claimant confirmed, then the bailiff '
      + 'rescheduled it with permission to dispense with service'
  );
  await confirmEvictionAsClaimant(page, context, world, true);
  await signInAs('bailiff', page, context);
  await openCaseSummary(page);
  await startEvent(RESCHEDULE_EVICTION);
  const when = new Date();
  when.setDate(when.getDate() + 21);
  const date = `${String(when.getDate()).padStart(2, '0')}/${String(when.getMonth() + 1).padStart(2, '0')}/${when.getFullYear()}`;
  await enterEvictionDateTime(page, date, '10:00');
  await dispenseWithServiceRadio(page, 'Yes').check();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectCheckYourAnswers(page);
  await page.getByRole('button', { name: scheduleEviction.saveAndContinue, exact: true }).click();
  await expect(page.getByRole('heading', { name: /Eviction date rescheduled for/ })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  world.evictionDate = `${date} 10:00`;
});

Then('it shows that the eviction is without notice', async ({ page, world }) => {
  await expectEnforcementSection(page, requireCaseReference(world), 'Eviction notice', block => {
    expect(block?.rows['Service']).toContain('Without notice');
  });
});

Then('the notice served date and time show as {string}', async ({ page, world }, value: string) => {
  await expectEnforcementSection(page, requireCaseReference(world), 'Eviction notice', block => {
    expect(block?.rows['Date served']).toBe(value);
    expect(block?.rows['Time served']).toBe(value);
  });
});

/**
 * The newest note on the "Risk assessment" tab: the one this scenario added, by the signed-in bailiff, with
 * the "1 Feb 2026 16:20pm" layout the bailiffs use.
 */
Then(
  'the note is shown under {string} on the {string} tab with my name, date and time',
  async ({ page, world }, section: string, tabName: string) => {
    expect(tabName).toBe(RISK_ASSESSMENT_TAB);
    expect(world.enteredNote, 'a note added by this scenario').toBeDefined();
    await openCaseTab(page, requireCaseReference(world), tabName);
    const [newest] = (await tabSections(page))[section]?.lists ?? [];
    expect(newest?.['Note']).toBe(world.enteredNote);
    expect(newest?.['Added by']).toBe(localUserName(localUsers.bailiff));
    expect(newest?.['Date and time']).toMatch(/^\d{1,2} [A-Z][a-z]{2} \d{4} \d{2}:\d{2}(am|pm)$/);
  }
);
