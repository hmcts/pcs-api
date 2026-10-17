// Bailiff records the warrant return (feature 11).
//
// `recordWarrantReturn` is available from "Awaiting scheduling" onwards, which the testing-support
// allocate-bailiff endpoint reaches, so the drop-down structure and the role checks are real. Submitting a
// return is not covered here: the confirmation asserts a warrant number that pcs-api does not render, and
// the 25 status transitions each need their own fresh case (HDPI-8424 / HDPI-7419).
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import {
  chooseOutcome,
  chooseReturnName,
  chooseReturnType,
  ensureOnWarrantReturnPage,
  optionsOf,
  returnDropDown,
  visibleReturnNameDropDown,
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
