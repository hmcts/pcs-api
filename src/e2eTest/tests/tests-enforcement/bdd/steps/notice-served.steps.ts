// Bailiff marks the notice of eviction as served (feature 08).
//
// Reachable because the warrant can be driven to "Awaiting claimant confirmation": allocate a bailiff
// through pcs-api testing support, then schedule an eviction answering "No" to dispensing with service.
// What is still missing is the WA task (HDPI-8627), a "claimant name vs defendant name" line on the page, and
// control of the clock, which several scenarios need in order to put the served time in the future. What
// defendants see once the notice is served uses feature 14's checks (support/defendant-visibility.ts).
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import { requireCaseReference } from '../support/enforcement-journeys';
import {
  AWAITING_CLAIMANT_CONFIRMATION,
  confirmEvictionAsClaimant,
  rescheduleEvictionAsBailiff,
  scheduleEvictionOnApprovedWarrant,
  setUpWarrantAtStatus,
} from '../support/approve-warrant';
import {
  DEFAULT_SERVED_HOW,
  MARK_NOTICE_SERVED,
  ensureOnNoticeServedPage,
  enterServedAt,
  enterServedYesterday,
  markNoticeServedAsBailiff,
  noticeServed,
} from '../support/notice-served';
import {
  CITIZEN_DEFENDANT,
  ensureDefendantsLinked,
  expectEnforcementTab,
  expectNoEnforcementFields,
  expectNoticeOfEvictionVisibleTo,
  viewAsEachDefendant,
} from '../support/defendant-visibility';
import {
  expectCheckYourAnswers,
  resolveEvictionDate,
  resolveEvictionTime,
} from '../support/schedule-eviction';
import { expectBailiffConfirmationWithWarrantNumber } from '../support/bailiff-events';
import { RISK_ASSESSMENT_TAB } from '../support/case-tabs';
import { expectEnforcementSection, openCaseTab, tabSections } from '../support/enforcement-status-tab';
import { localUserName, localUsers } from '@config/local-stack.config';
import { expectSingleErrorMessage } from '../support/error-summary';

Given('a PCS case {string} with a scheduled eviction', async ({ world }, _reference: string) => {
  await setUpWarrantAtStatus(world, AWAITING_CLAIMANT_CONFIRMATION);
});

// Feature 08's Background has already scheduled the eviction, answering "No" to dispensing with service, so
// this only checks it. Feature 14 starts from an approved warrant, so this schedules it.
Given('the court has not given permission to dispense with service', async ({ world }) => {
  if (!world.evictionScheduled) {
    await scheduleEvictionOnApprovedWarrant(world);
  }
  expect(world.evictionScheduled, 'an eviction scheduled without dispensing with service').toBe(true);
  expect(world.serviceDispensedWith ?? false).toBe(false);
});

When('I select {string} without answering', async ({ page }, button: string) => {
  expect(button).toBe('Continue');
  await page.getByRole('button', { name: button, exact: true }).click();
});

Then('I see errors for {string} and {string}', async ({ page }, first: string, second: string) => {
  await expectSingleErrorMessage(page, first);
  await expectSingleErrorMessage(page, second);
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
  await expectSingleErrorMessage(page, noticeServed.tooLongError);
});

// Two uses. In feature 08 it follows the bailiff's own submit, so it checks the confirmation. In features 12
// and 14 it is a Given on a case whose eviction has not been scheduled yet, so it schedules the eviction and
// serves the notice, with a bailiff note so that the defendant checks have one to look for.
Then('the notice of eviction has been served', async ({ page, world }) => {
  if (world.evictionScheduled) {
    await expect(page.getByRole('heading', { name: noticeServed.confirmationHeader })).toBeVisible();
    expect(requireCaseReference(world)).toBeDefined();
    return;
  }
  if (world.caseReference) {
    await scheduleEvictionOnApprovedWarrant(world);
  } else {
    await setUpWarrantAtStatus(world, AWAITING_CLAIMANT_CONFIRMATION);
  }
  await markNoticeServedAsBailiff(world, 'Dog on the premises');
  world.defendantViews = undefined;
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
Given('the court has given permission to dispense with service', async ({ world }) => {
  approximatePrecondition(
    'HDPI-7549',
    'the eviction was scheduled without dispensing with service; the claimant confirmed, then the bailiff '
      + 'rescheduled it with permission to dispense with service'
  );
  await confirmEvictionAsClaimant(world, true);
  await rescheduleEvictionAsBailiff(world, 21, 'Yes');
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

/** Enters the served date and time, and a "how" and "No" to a bailiff note so that only the date is being tested. */
When('I enter a served date of {string} and time {string}', async ({ page, world }, date: string, time: string) => {
  await ensureOnNoticeServedPage(page);
  world.currentEvent = MARK_NOTICE_SERVED;
  await enterServedAt(page, date, time);
  await page.locator(noticeServed.fields.servedHow).fill(DEFAULT_SERVED_HOW);
  await page.locator(`${noticeServed.fields.addNote}_No`).check();
});

When('I answer {string} to {string}', async ({ page, world, $tags }, answer: string, question: string) => {
  if (question !== noticeServed.addNoteQuestion || (answer !== 'Yes' && answer !== 'No')) {
    return markPending({ summary: `answering "${answer}" to "${question}"` }, $tags);
  }
  await ensureOnNoticeServedPage(page);
  world.currentEvent = MARK_NOTICE_SERVED;
  await page.locator(`${noticeServed.fields.addNote}_${answer}`).check();
});

When('I enter the note {string}', async ({ page, world }, note: string) => {
  expect(world.currentEvent).toBe(MARK_NOTICE_SERVED);
  await page.locator(noticeServed.fields.noteText).fill(note);
  world.enteredNote = note;
});

Given('I have answered the questions', async ({ page, world, $tags }) => {
  if (world.journey !== MARK_NOTICE_SERVED) {
    return markPending({ summary: `answering the questions of "${world.journey}"` }, $tags);
  }
  await ensureOnNoticeServedPage(page);
  world.currentEvent = MARK_NOTICE_SERVED;
  await enterServedYesterday(page);
  await page.locator(noticeServed.fields.servedHow).fill(DEFAULT_SERVED_HOW);
  await page.locator(`${noticeServed.fields.addNote}_No`).check();
});

// What defendants see once the notice is served. The checks are feature 14's: the XUI tabs for the defendant's
// legal representative, and the case data CCD gives each defendant.
Then('defendants can see the {string} tab', async ({ page, context, world }, tabName: string) => {
  await ensureDefendantsLinked(world);
  await expectEnforcementTab(page, await viewAsEachDefendant(page, context, world), tabName, true);
});

Then('defendants cannot see the {string} tab', async ({ page, context, world }, tabName: string) => {
  await ensureDefendantsLinked(world);
  const views = await viewAsEachDefendant(page, context, world);
  await expectEnforcementTab(page, views, tabName, false);
  expectNoEnforcementFields(views);
});

// XUI draws the Enforcement folder for everyone (OPEN-QUESTIONS 50); what pcs-api controls is that the
// defendant gets a document in it.
Then('defendants can see the {string} folder in case file view', async ({ world }, folder: string) => {
  expect(folder).toBe('Enforcement');
  await ensureDefendantsLinked(world);
  await expectNoticeOfEvictionVisibleTo(world);
});

Then('the {string} is visible to defendants', async ({ world }, document: string) => {
  expect(document).toBe('notice of eviction');
  await ensureDefendantsLinked(world);
  await expectNoticeOfEvictionVisibleTo(world);
});

Then(
  'citizen defendants see the {string} under {string} in pcs-frontend',
  async ({ world }, document: string, _heading: string) => {
    expect(document).toBe('notice of eviction');
    approximatePrecondition('HDPI-7539', 'pcs-frontend is not run; the citizen\'s view is the case data CCD gives them');
    await ensureDefendantsLinked(world);
    await expectNoticeOfEvictionVisibleTo(world, CITIZEN_DEFENDANT);
  }
);
