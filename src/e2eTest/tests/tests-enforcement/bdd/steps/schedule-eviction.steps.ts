import { Page } from '@playwright/test';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
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
  evictionDetailsList,
  evictionDetailsRows,
  enterEvictionDateTime,
  evictionDateErrors,
  evictionDateQuestion,
  expectCheckYourAnswers,
  expectEvictionDateNeeded,
  expectEvictionError,
  scheduleEviction,
} from '../support/schedule-eviction';
import {
  MARK_NOTICE_SERVED,
  markNoticeServedAsBailiff,
  markNoticeServedInXui,
  noticeServed,
  submitNoticeServed,
} from '../support/notice-served';
import { caseChangedForDefendants } from '../support/defendant-visibility';
import { ADD_BAILIFF_NOTE, SET_RISK_LEVEL } from '../support/bailiff-events';

const SINGLE_PAGE_EVENTS = [SET_RISK_LEVEL, ADD_BAILIFF_NOTE];

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
  await expect(dispenseWithServiceRadio(page, 'Yes')).toBeVisible();
  await expect(dispenseWithServiceRadio(page, 'No')).toBeVisible();
});

Then('the answer defaults to {string}', async ({ page }, answer: string) => {
  await expect(dispenseWithServiceRadio(page, answer)).toBeChecked();
});

When('I select {string} next to the eviction date', async ({ page, world }, link: string) => {
  await answerRow(page, evictionDateQuestion(world.currentEvent)).getByText(link, { exact: true }).click();
});

Then('I return to the {string} screen with my previous answer', async ({ page }, screen: string) => {
  await expect(page.getByRole('heading', { name: screen })).toBeVisible();
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

export const EVICTION_RESULTS: Record<string, string> = {
  'an error that date and time are needed': evictionDateErrors.needed,
  'an error that the date is not valid': evictionDateErrors.invalid,
  'an error that the date must be today or in the future': evictionDateErrors.inThePast,
  'an error that the time is not valid': evictionDateErrors.invalid,
};

const TIME_IN_FUTURE_RESULT = 'an error that the time cannot be in the future';

const TIME_IN_FUTURE_ERRORS: Record<string, string> = {
  [MARK_NOTICE_SERVED]: noticeServed.errors.servedInTheFuture,
};

export const isEvictionResult = (result: string): boolean => result in EVICTION_RESULTS
  || result === TIME_IN_FUTURE_RESULT
  || result === CHECK_YOUR_ANSWERS_RESULT;

export const CHECK_YOUR_ANSWERS_RESULT = "the 'Check your answers' screen";

export async function expectEvictionResult(page: Page, world: EnforcementWorld, result: string): Promise<void> {
  if (result === CHECK_YOUR_ANSWERS_RESULT) {
    await expectCheckYourAnswers(page);
    return;
  }
  if (result === 'an error that date and time are needed') {
    await expectEvictionDateNeeded(page);
    return;
  }
  if (result === TIME_IN_FUTURE_RESULT) {
    const message = TIME_IN_FUTURE_ERRORS[world.currentEvent ?? ''];
    expect(message, `the "${TIME_IN_FUTURE_RESULT}" message of the "${world.currentEvent}" journey`).toBeDefined();
    await expectEvictionError(page, message);
    return;
  }
  await expectEvictionError(page, EVICTION_RESULTS[result]);
}

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

Then('I can {string} my answers', async ({ page, world }, link: string) => {
  await expect(answerRow(page, evictionDateQuestion(world.currentEvent)).getByText(link, { exact: true }))
    .toBeVisible();
});

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

When('I submit the event', async ({ page, world, $tags }) => {
  if (world.currentEvent === MARK_NOTICE_SERVED) {
    return submitNoticeServed(page, world);
  }
  if (!world.currentEvent && world.journey === MARK_NOTICE_SERVED && !world.noticeServed) {
    await markNoticeServedInXui(page, world);
    caseChangedForDefendants(world);
    return;
  }
  if (!world.currentEvent) {
    return markPending({ summary: 'submitting an event the scenario never started' }, $tags);
  }
  const submitButton = SINGLE_PAGE_EVENTS.includes(world.currentEvent) ? 'Continue' : scheduleEviction.saveAndContinue;
  await page.getByRole('button', { name: submitButton, exact: true }).click();
  world.currentEvent = undefined;
});

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

Then('I see an {string} table with claimant type, claimants and defendants', async ({ page, world }, table: string) => {
  expect(table).toBe('Eviction details');
  const rows = await evictionDetailsRows(page);
  expect(Object.keys(rows)).toEqual(scheduleEviction.evictionDetailsRows);
  expect(rows['Claimant type'], 'the claimant type').toMatch(/\S/);
  expect(rows['Claimants'], 'the claimants').toMatch(/\S/);
  if (world.currentEvent === SCHEDULE_EVICTION) {
    const { firstName, lastName } = basicClaimFixtureClaim.defendant1;
    expect(rows['Defendants']).toBe(`${firstName} ${lastName}`);
  } else {
    expect(rows['Defendants'], 'the defendants').toMatch(/\S/);
  }
  await expect(evictionDetailsList(page).locator('input, select, textarea')).toHaveCount(0);
});

Then('the date question is {string}', async ({ page }, question: string) => {
  await expect(page.getByText(question, { exact: false }).first()).toBeVisible();
});

Then('the confirmation says {string}', async ({ page }, sentence: string) => {
  await expect(page.locator('#confirmation-body').getByText(sentence, { exact: false }).first()).toBeVisible();
});
