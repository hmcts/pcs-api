// Bailiff schedules the eviction (feature 05).
//
// Reachable since the warrant can be put into "Awaiting scheduling" through the pcs-api testing-support
// allocate-bailiff endpoint. What is still missing from this journey is the generated notice of eviction and
// notice of appointment (HDPI-8442 / no Docmosis templates locally), the notifications (HDPI-7538), the WA
// tasks (HDPI-8627), and the warrant number on the confirmation.
import { Page } from '@playwright/test';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { markPending } from '../fixtures/pending';
import { openCaseSummary, requireCaseReference } from '../support/enforcement-journeys';
import { basicClaimFixtureClaim } from '@config/local-stack.config';
import {
  ENFORCEMENT_STATUS_TAB,
  expectEnforcementSection,
  soleEnforcementSection,
  tabDateOf,
  tabTime,
} from '../support/enforcement-status-tab';
import {
  RESCHEDULE_EVICTION,
  SCHEDULE_EVICTION,
  answerRow,
  dispenseWithServiceRadio,
  evictionDetailsRows,
  enterEvictionDateTime,
  evictionDateErrors,
  expectCheckYourAnswers,
  expectEvictionDateNeeded,
  expectEvictionError,
  scheduleEviction,
} from '../support/schedule-eviction';

// The warrant was allocated to the bailiff the scenario signs in as, through testing support.
Given('I am the allocated bailiff', async ({ world }) => {
  expect(world.allocatedBailiff, 'a bailiff allocated to the warrant').toBeDefined();
});

When('I enter the date {string} and time {string}', async ({ page, world }, date: string, time: string) => {
  expect(world.currentEvent).toBe(SCHEDULE_EVICTION);
  await enterEvictionDateTime(page, date, time);
});

Given('I have entered an eviction date of {string} at {string}', async ({ page, world }, date: string, time: string) => {
  await openCaseSummary(page);
  const { startEvent } = await import('../support/enforcement-journeys');
  await startEvent(SCHEDULE_EVICTION);
  world.currentEvent = SCHEDULE_EVICTION;
  await enterEvictionDateTime(page, date, time);
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectCheckYourAnswers(page);
  world.evictionDate = `${date} ${time}`;
});

Then('the question {string} is mandatory', async ({ page, $tags }, question: string) => {
  if (question !== scheduleEviction.dispenseQuestion) {
    return markPending({ summary: `checking that "${question}" is mandatory` }, $tags);
  }
  // Both options are offered and one is always selected, so the answer can never be submitted empty.
  await expect(dispenseWithServiceRadio(page, 'Yes')).toBeVisible();
  await expect(dispenseWithServiceRadio(page, 'No')).toBeVisible();
});

Then('the answer defaults to {string}', async ({ page }, answer: string) => {
  await expect(dispenseWithServiceRadio(page, answer)).toBeChecked();
});

When('I select {string} next to the eviction date', async ({ page }, link: string) => {
  await answerRow(page, scheduleEviction.dateQuestion).getByText(link, { exact: true }).click();
});

Then('I return to the {string} screen with my previous answer', async ({ page }, screen: string) => {
  await expect(page.getByRole('heading', { name: screen })).toBeVisible();
  // The date the scenario entered is still in the inputs.
  await expect(page.locator('#evictionDateTime-day')).not.toHaveValue('');
  await expect(page.locator('#evictionDateTime-year')).not.toHaveValue('');
});

Then('selecting {string} returns me to {string}', async ({ page }, button: string, screen: string) => {
  await page.getByRole('button', { name: button, exact: true }).click();
  await expect(page.getByRole('heading', { name: screen })).toBeVisible();
});

Then('I see the case summary', async ({ page, world }) => {
  await expect(page).toHaveURL(new RegExp(`/cases/case-details/.*${requireCaseReference(world)}(#.*)?$`));
});

Then('no eviction date is saved', async ({ page, world }) => {
  const enforcement = await soleEnforcementSection(page, requireCaseReference(world));
  expect(enforcement.sections['Scheduled eviction']?.text, 'no scheduled eviction on the warrant')
    .toBe('Not yet scheduled');
});

/** The validation outcomes feature 05 names, mapped to the text XUI actually shows. */
export const EVICTION_RESULTS: Record<string, string> = {
  'an error that date and time are needed': evictionDateErrors.needed,
  'an error that the date is not valid': evictionDateErrors.invalid,
  'an error that the date must be today or in the future': evictionDateErrors.inThePast,
  'an error that the time is not valid': evictionDateErrors.invalid,
};

export const isEvictionResult = (result: string): boolean => result in EVICTION_RESULTS
  || result === CHECK_YOUR_ANSWERS_RESULT;

export const CHECK_YOUR_ANSWERS_RESULT = "the 'Check your answers' screen";

/** Asserts one of feature 05's outcomes. Called from the shared `I see {string}` step. */
export async function expectEvictionResult(page: Page, result: string): Promise<void> {
  if (result === CHECK_YOUR_ANSWERS_RESULT) {
    await expectCheckYourAnswers(page);
    return;
  }
  if (result === 'an error that date and time are needed') {
    await expectEvictionDateNeeded(page);
    return;
  }
  await expectEvictionError(page, EVICTION_RESULTS[result]);
}

// ---------------------------------------------------------------------------
// Feature 07: the bailiff reschedules the eviction
//
// `rescheduleEviction` uses the same page as `scheduleEviction`, and is offered once the claimant has
// answered whether they can attend. The claimant's unavailable dates are not captured by the built
// `confirmEviction`, so the scenario that lists them stays pending.
// ---------------------------------------------------------------------------

When('I enter a date in the past', async ({ page }) => {
  await enterEvictionDateTime(page, 'yesterday', '10:00');
});

Then('I see an error that the date must be today or in the future', async ({ page }) => {
  await expectEvictionError(page, evictionDateErrors.inThePast);
});

Given('I have entered a new eviction date', async ({ page, world }) => {
  const event = world.currentEvent ?? RESCHEDULE_EVICTION;
  if (!(await page.locator('#evictionDateTime-day').isVisible().catch(() => false))) {
    await openCaseSummary(page);
    const { startEvent } = await import('../support/enforcement-journeys');
    await startEvent(event);
    world.currentEvent = event;
  }
  const when = new Date();
  when.setDate(when.getDate() + 21);
  const date = `${String(when.getDate()).padStart(2, '0')}/${String(when.getMonth() + 1).padStart(2, '0')}/${when.getFullYear()}`;
  await enterEvictionDateTime(page, date, '09:30');
  world.evictionDate = `${date} 09:30`;
});

Then('the screen is titled {string}', async ({ page }, title: string) => {
  await expect(page.getByRole('heading', { name: title })).toBeVisible();
});

Then('I can {string} my answers', async ({ page }, link: string) => {
  await expect(answerRow(page, scheduleEviction.dateQuestion).getByText(link, { exact: true })).toBeVisible();
});

/**
 * The "You need to" list and the case file view link on the schedule / reschedule confirmation
 * (`EvictionConfirmationFormat.scheduledBody`).
 */
Then('I see {string} under {string}', async ({ page }, item: string, heading: string) => {
  const body = page.locator('#confirmation-body');
  await expect(body.getByRole('heading', { name: heading })).toBeVisible();
  await expect(body.getByRole('listitem').filter({ hasText: item })).toBeVisible();
});

Then('I see a link {string}', async ({ page }, text: string) => {
  await expect(page.locator('#confirmation-body').getByRole('link', { name: text })).toBeVisible();
});

Then('the {string} tab shows the scheduled eviction date and time under {string}', async ({ page, world }, tabName: string, section: string) => {
  expect(tabName).toBe(ENFORCEMENT_STATUS_TAB);
  expect(world.evictionDate, 'an eviction date entered by this scenario').toBeDefined();
  await expectEvictionShown(page, requireCaseReference(world), section, world.evictionDate as string);
});

/**
 * Saves and continues from CCD's check-your-answers, which is how these events are submitted. Pending when
 * the scenario never started an event, which is the case for the ones that are really about the documents
 * or notifications a submit is meant to produce.
 */
When('I submit the event', async ({ page, world, $tags }) => {
  if (!world.currentEvent) {
    return markPending({ summary: 'submitting an event the scenario never started' }, $tags);
  }
  await page.getByRole('button', { name: scheduleEviction.saveAndContinue, exact: true }).click();
  world.currentEvent = undefined;
});

/**
 * The "Scheduled eviction" section shows exactly the date and time entered, given as "Wednesday 30 June 2027
 * 09:30" or "30/06/2027 09:30".
 */
async function expectEvictionShown(page: Page, caseReference: string, section: string, entered: string): Promise<void> {
  const time = entered.slice(entered.lastIndexOf(' ') + 1);
  const date = entered.slice(0, entered.lastIndexOf(' '));
  await expectEnforcementSection(page, caseReference, section, block => {
    expect(block?.rows).toEqual({ 'Eviction date': tabDateOf(date), 'Eviction time': tabTime(time) });
  });
}

Given('I have entered a new eviction date of {string} at {string}', async ({ page, world }, date: string, time: string) => {
  await openCaseSummary(page);
  const { startEvent } = await import('../support/enforcement-journeys');
  await startEvent(RESCHEDULE_EVICTION);
  world.currentEvent = RESCHEDULE_EVICTION;
  await enterEvictionDateTime(page, date, time);
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectCheckYourAnswers(page);
  world.evictionDate = `${date} ${time}`;
});

Then('the {string} tab {string} shows only the new date and time', async ({ page, world }, tabName: string, section: string) => {
  expect(tabName).toBe(ENFORCEMENT_STATUS_TAB);
  expect(world.evictionDate, 'the new eviction date entered by this scenario').toBeDefined();
  await expectEvictionShown(page, requireCaseReference(world), section, world.evictionDate as string);
});

// The claimant and defendant come from the fixture claim every local case is built from.
Then('I see an {string} table with claimant type, claimant name and defendant names', async ({ page, world }, table: string) => {
  expect(table).toBe('Eviction details');
  expect(world.currentEvent).toBe(SCHEDULE_EVICTION);
  const rows = await evictionDetailsRows(page);
  expect(Object.keys(rows)).toEqual(['Claimant type', 'Claimant name', 'Defendant names']);
  expect(rows['Claimant type'], 'the claimant type').toMatch(/\S/);
  expect(rows['Claimant name'], 'the claimant name').toMatch(/\S/);
  const { firstName, lastName } = basicClaimFixtureClaim.defendant1;
  expect(rows['Defendant names']).toBe(`${firstName} ${lastName}`);
  // The details are a rendered label, not fields.
  await expect(page.locator('dl.govuk-summary-list').filter({ hasText: 'Defendant names' }).locator('input, select, textarea'))
    .toHaveCount(0);
});
