// Further enforcement on the same case (feature 15, and the free repeat warrant in feature 01; HDPI-8424).
//
// A closed warrant is built by driving the real events (see support/repeat-enforcement.ts). The warrant
// numbers in the feature files are illustrative, so the steps map them to the numbers pcs-api gives.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { markPending } from '../fixtures/pending';
import { approveWarrantAsCaseworker, expectClaimantNext } from '../support/approve-warrant';
import { recordEnforcementFeePaid } from '../support/decide-enforcement';
import {
  ENFORCEMENT_STATUS_TAB,
  currentEnforcement,
  enforcementSections,
  openCaseTab,
} from '../support/enforcement-status-tab';
import {
  applyForFurtherWarrant,
  closeWarrantWithReturn,
  enforcementsOnTheCase,
} from '../support/repeat-enforcement';
import { backdateWarrantGeneration, monthsIn } from '../support/warrant-backdate';
import { requireCaseReference } from '../support/enforcement-journeys';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

const NOT_LIVE = ['Closed', 'Rejected', 'Transferred to the High Court'];
const AWAITING_PAYMENT = 'Awaiting payment';
const AWAITING_REVIEW = 'Awaiting review';

const furtherTypeNotBuilt = (type: string) => ({
  summary: `a further "${type}" through these steps; only a warrant of possession is driven`,
});

function actualNumber(world: EnforcementWorld, alias: string): string {
  const number = world.aliases[alias];
  if (!number) {
    throw new Error(`No warrant "${alias}" has been set up in this scenario`);
  }
  return number;
}

async function currentStatus(world: EnforcementWorld): Promise<string | undefined> {
  return (await currentEnforcement(requireCaseReference(world), world.earlierWarrantNumbers.length)).status;
}

Given('the case has a closed warrant with return name {string}', async ({ world }, returnName: string) => {
  await closeWarrantWithReturn(world, returnName);
});

Given('the case has a warrant closed with return name {string}', async ({ world }, returnName: string) => {
  await closeWarrantWithReturn(world, returnName);
});

Given(
  'the case has a warrant {string} closed with return name {string}',
  async ({ world }, alias: string, returnName: string) => {
    await closeWarrantWithReturn(world, returnName);
    world.aliases[alias] = world.earlierWarrantNumbers.at(-1) as string;
  }
);

Given(
  'the case has a closed warrant with return name {string} generated {string} ago',
  async ({ world }, returnName: string, age: string) => {
    await closeWarrantWithReturn(world, returnName);
    await backdateWarrantGeneration(world, monthsIn(age));
  }
);

When('I apply for a new {string}', async ({ page, world, $tags }, type: string) => {
  if (type !== 'warrant of possession') {
    return markPending(furtherTypeNotBuilt(type), $tags);
  }
  await applyForFurtherWarrant(page, world);
});

When('I apply and pay for a new {string}', async ({ page, world, $tags }, type: string) => {
  if (type !== 'warrant of possession') {
    return markPending(furtherTypeNotBuilt(type), $tags);
  }
  await applyForFurtherWarrant(page, world);
  // A free repeat warrant has nothing to pay and is already awaiting review.
  if ((await currentStatus(world)) === AWAITING_PAYMENT) {
    await recordEnforcementFeePaid();
  }
});

When('the caseworker approves it', async ({ world }) => {
  try {
    await approveWarrantAsCaseworker(world, { feeOutstanding: false });
  } finally {
    expectClaimantNext();
  }
});

Then('I am {string} the warrant fee', async ({ world }, charged: string) => {
  if (charged !== 'charged' && charged !== 'not charged') {
    throw new Error(`Expected "charged" or "not charged", got "${charged}"`);
  }
  if (charged === 'not charged') {
    expect(world.askedToPayFee, 'the submit confirmation asked for the fee').toBe(false);
  }
  await expect
    .poll(() => currentStatus(world))
    .toBe(charged === 'charged' ? AWAITING_PAYMENT : AWAITING_REVIEW);
});

Then('I am not asked to pay a fee', async ({ world }) => {
  expect(world.askedToPayFee, 'the submit confirmation asked for the fee').toBe(false);
  expect(await currentStatus(world)).not.toBe(AWAITING_PAYMENT);
});

Then(
  'the case has two warrants, {string} closed and {string} live',
  async ({ world }, closedAlias: string, liveAlias: string) => {
    expect(world.warrantNumber, 'a warrant number generated when the further warrant was approved').toBeDefined();
    world.aliases[liveAlias] = world.warrantNumber as string;
    const rows = await enforcementsOnTheCase(world);
    expect(rows).toHaveLength(2);

    const byNumber = (alias: string) =>
      rows.find(row => row.number === actualNumber(world, alias));
    expect(byNumber(closedAlias)?.status, `${closedAlias} is closed`).toBe('Closed');
    const live = byNumber(liveAlias);
    expect(live, `${liveAlias} is on the case`).toBeDefined();
    expect(NOT_LIVE, `${liveAlias} is live`).not.toContain(live?.status);
  }
);

Then(
  'the {string} tab lists {string} above {string}',
  async ({ page, world, $tags }, tabName: string, upperAlias: string, lowerAlias: string) => {
    if (tabName !== ENFORCEMENT_STATUS_TAB) {
      return markPending({ summary: `ordering on the "${tabName}" tab` }, $tags);
    }
    const expected = (await enforcementsOnTheCase(world)).length;
    await openCaseTab(page, requireCaseReference(world), ENFORCEMENT_STATUS_TAB);
    // Each warrant or writ on the tab is headed with its number.
    await expect
      .poll(async () => (await enforcementSections(page)).length, { timeout: VERY_LONG_TIMEOUT })
      .toBe(expected);
    const numbers = (await enforcementSections(page)).map(section => section.heading);
    const upper = numbers.indexOf(actualNumber(world, upperAlias));
    const lower = numbers.indexOf(actualNumber(world, lowerAlias));
    expect(upper, `${upperAlias} is listed`).toBeGreaterThanOrEqual(0);
    expect(lower, `${lowerAlias} is listed`).toBeGreaterThanOrEqual(0);
    expect(upper, `${upperAlias} is listed above ${lowerAlias}`).toBeLessThan(lower);
  }
);
