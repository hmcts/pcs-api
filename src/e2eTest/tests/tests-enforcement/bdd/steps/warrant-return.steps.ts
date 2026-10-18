// Bailiff records the warrant return (feature 11).
//
// `recordWarrantReturn` is available from "Awaiting scheduling" onwards, which the testing-support
// allocate-bailiff endpoint reaches, so the drop-down structure and the role checks are real. One final
// return is submitted and checked on the "Enforcement status" tab; the 25 status transitions each need their
// own fresh case, so they stay pending (HDPI-8424 / HDPI-7419).
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { requireCaseReference } from '../support/enforcement-journeys';
import { expectEnforcementSection, tabDateOf, tabTime } from '../support/enforcement-status-tab';
import { expectCheckYourAnswers, resolveEvictionDate, resolveEvictionTime } from '../support/schedule-eviction';
import {
  RECORD_WARRANT_RETURN,
  chooseOutcome,
  chooseReturnName,
  chooseReturnType,
  ensureOnWarrantReturnPage,
  optionsOf,
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

/** "I see a mandatory X drop down" — present, and with no value chosen it cannot be submitted. */
Then('I see a mandatory {string} drop down', async ({ page }, label: string) => {
  await ensureOnWarrantReturnPage(page);
  const field = label === 'Return name' ? await visibleReturnNameDropDown(page) : returnDropDown(page, label);
  await expect(field).toBeVisible();
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
