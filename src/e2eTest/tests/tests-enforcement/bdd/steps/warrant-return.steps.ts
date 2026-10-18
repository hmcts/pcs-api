// Bailiff records the warrant return (feature 11).
//
// `recordWarrantReturn` is available from "Awaiting scheduling" onwards, which the testing-support
// allocate-bailiff endpoint reaches, so the drop-down structure, the role checks and every return name's
// status transition are real. Each example of the transition outline gets its own case from the feature's
// background.
import { Page } from '@playwright/test';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import { requireCaseReference } from '../support/enforcement-journeys';
import {
  currentEnforcement,
  expectEnforcementSection,
  expectEnforcementStatus,
  tabDateOf,
  tabTime,
} from '../support/enforcement-status-tab';
import { expectCheckYourAnswers, resolveEvictionDate, resolveEvictionTime } from '../support/schedule-eviction';
import {
  REOPEN_WARRANT,
  RECORD_WARRANT_RETURN,
  answerWarrantReturn,
  chooseOutcome,
  chooseReturnName,
  chooseReturnType,
  ensureOnWarrantReturnPage,
  expectEvictionDetailsTable,
  optionsOf,
  recordWarrantReturn,
  returnDropDown,
  visibleReturnNameDropDown,
  warrantReturn,
} from '../support/warrant-return';

When('I choose the {string} return', async ({ page }, type: string) => {
  await chooseReturnType(page, type);
});

Given('I chose the {string} return', async ({ page }, type: string) => {
  await chooseReturnType(page, type);
});

When('I choose the outcome {string}', async ({ page }, outcome: string) => {
  await chooseOutcome(page, outcome);
});

Given('I chose the outcome {string}', async ({ page }, outcome: string) => {
  await chooseOutcome(page, outcome);
});

When('I choose the return name {string}', async ({ page }, returnName: string) => {
  await chooseReturnName(page, returnName);
});

/**
 * "I see a mandatory X drop down" — present, and with no value chosen it cannot be submitted.
 *
 * A final return's "Return name" list depends on the outcome, so pcs-api shows it only once an outcome is
 * chosen. When the scenario has not chosen one, the first outcome is chosen to bring the drop down up.
 */
Then('I see a mandatory {string} drop down', async ({ page }, label: string) => {
  await ensureOnWarrantReturnPage(page);
  if (label !== 'Return name') {
    await expect(returnDropDown(page, label)).toBeVisible();
    return;
  }
  const outcome = page.locator(warrantReturn.outcomeField);
  const outcomes = (await outcome.isVisible()) ? await optionsOf(outcome) : [];
  const chosenOutcome = outcomes.length ? (await outcome.locator('option:checked').textContent())?.trim() : undefined;
  if (outcomes.length && !outcomes.includes(chosenOutcome ?? '')) {
    approximatePrecondition(
      'HDPI-7347',
      `a final return's "Return name" is shown once an outcome is chosen, so "${warrantReturn.evictionSuccessful}" was chosen`
    );
    await chooseOutcome(page, warrantReturn.evictionSuccessful);
  }
  await expect(await visibleReturnNameDropDown(page)).toBeVisible();
});

Then('I see a mandatory {string} drop down with {string} and {string}', async ({ page }, label: string, first: string, second: string) => {
  await ensureOnWarrantReturnPage(page);
  const field = returnDropDown(page, label);
  expect(await optionsOf(field)).toEqual([first, second]);
});

Then(
  'I see a mandatory {string} drop down with {string}, {string} and {string}',
  async ({ page }, label: string, first: string, second: string, third: string) => {
    await ensureOnWarrantReturnPage(page);
    const field = returnDropDown(page, label);
    expect(await optionsOf(field)).toEqual([first, second, third]);
  }
);

// The interim return names: twelve, listed in order. playwright-bdd checks arity, so each is declared.
Then(
  'I see a mandatory {string} drop down with {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string} and {string}',
  async (
    { page },
    label: string,
    name1: string,
    name2: string,
    name3: string,
    name4: string,
    name5: string,
    name6: string,
    name7: string,
    name8: string,
    name9: string,
    name10: string,
    name11: string,
    name12: string
  ) => {
    const field = label === 'Return name' ? await visibleReturnNameDropDown(page) : returnDropDown(page, label);
    expect(await optionsOf(field)).toEqual([
      name1, name2, name3, name4, name5, name6, name7, name8, name9, name10, name11, name12,
    ]);
  }
);

Then('the {string} options are {string}', async ({ page }, label: string, expected: string) => {
  expect(label).toBe('Return name');
  const field = await visibleReturnNameDropDown(page);
  expect(await optionsOf(field)).toEqual(expected.split(',').map(name => name.trim()));
});

/**
 * Records a final return. The step names the outcome but not the return name, so the first return name the
 * outcome offers is chosen: for "Eviction successful" that is the only one, "Eviction successful".
 */
Given(
  'I recorded a {string} return with outcome {string} on {string} at {string}',
  async ({ page, world }, type: string, outcome: string, date: string, time: string) => {
    await chooseReturnType(page, type);
    await chooseOutcome(page, outcome);
    const [returnName] = await optionsOf(await visibleReturnNameDropDown(page));
    await chooseReturnName(page, returnName);
    const parts: Record<string, string> = {
      ...(resolveEvictionDate(date) ?? {}),
      ...(resolveEvictionTime(time) ?? {}),
    };
    for (const part of ['day', 'month', 'year', 'hour', 'minute']) {
      await page.locator(`${warrantReturn.evictionDateTimeField}-${part}`).fill(parts[part] ?? '');
    }
    await page.getByRole('button', { name: 'Continue', exact: true }).click();
    await expectCheckYourAnswers(page);
    world.currentEvent = RECORD_WARRANT_RETURN;
    world.recordedReturn = { type, outcome, returnName, date, time };
  }
);

Then(
  'the {string} tab shows under {string} the return {string}, return name {string}, outcome and the eviction date and time',
  async ({ page, world }, _tabName: string, section: string, type: string, returnName: string) => {
    const recorded = world.recordedReturn;
    expect(recorded, 'a return recorded by this scenario').toBeDefined();
    await expectEnforcementSection(page, requireCaseReference(world), section, block => {
      expect(block?.rows).toEqual({
        Return: type,
        'Return name': returnName,
        Outcome: recorded?.outcome,
        'Eviction date': tabDateOf(recorded?.date as string),
        'Eviction time': tabTime(recorded?.time as string),
      });
    });
  }
);

Then('I see the {string} table with claimant type, claimant name and defendant names', async ({ page }, heading: string) => {
  expect(heading).toBe(warrantReturn.evictionDetailsHeading);
  await expectEvictionDetailsTable(page);
});

/** Answers the page with a successful final return, ready for "Continue". */
Given('I have completed the {string} screen', async ({ page, world, $tags }, screen: string) => {
  if (screen !== warrantReturn.pageLabel) {
    return markPending({ summary: `completing the "${screen}" screen` }, $tags);
  }
  await ensureOnWarrantReturnPage(page);
  world.currentEvent = RECORD_WARRANT_RETURN;
  await answerWarrantReturn(page, warrantReturn.evictionSuccessful);
});

const CLOSED = 'Closed';
/** A final return name that closes the warrant and needs no eviction date. */
const CLOSING_RETURN = 'Warrant expired';

/**
 * Records the return and remembers the status it was recorded against, for "unchanged". "Reopen warrant"
 * can only be recorded on a closed warrant, so a live one is closed first.
 */
async function submitReturn(
  page: Page,
  world: EnforcementWorld,
  returnName: string,
  additionalDetails?: string
): Promise<void> {
  const caseReference = requireCaseReference(world);
  if (returnName.toLowerCase() === REOPEN_WARRANT.toLowerCase()
    && (await currentEnforcement(caseReference)).status !== CLOSED) {
    approximatePrecondition('HDPI-8424', `only a closed warrant can be reopened, so it was closed first with "${CLOSING_RETURN}"`);
    await recordWarrantReturn(page, CLOSING_RETURN);
    await expectEnforcementStatus(page, caseReference, CLOSED);
  }
  world.statusBeforeReturn = (await currentEnforcement(caseReference)).status;
  world.currentEvent = RECORD_WARRANT_RETURN;
  await recordWarrantReturn(page, returnName, additionalDetails);
  world.currentEvent = undefined;
}

When('I submit a return with return name {string}', async ({ page, world }, returnName: string) => {
  await submitReturn(page, world, returnName);
});

When(
  'I submit a return with return name {string} and additional details {string}',
  async ({ page, world }, returnName: string, additionalDetails: string) => {
    await submitReturn(page, world, returnName, additionalDetails);
  }
);

When('I submit a return that closes the enforcement', async ({ page, world }) => {
  await submitReturn(page, world, warrantReturn.evictionSuccessful);
});
