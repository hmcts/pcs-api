// Further enforcement on the same case (feature 15, and the free repeat warrant in feature 01; HDPI-8424).
//
// A closed warrant is built by driving the real events (see support/repeat-enforcement.ts). The warrant
// numbers in the feature files are illustrative, so the steps map them to the numbers pcs-api gives.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { markPending } from '../fixtures/pending';
import { approveWarrantAsCaseworker, restoreClaimantSession } from '../support/approve-warrant';
import { recordEnforcementFeePaid } from '../support/decide-enforcement';
import { ENFORCEMENT_STATUS_TAB, currentEnforcement, openCaseTab } from '../support/enforcement-status-tab';
import {
  applyForFurtherWarrant,
  closeWarrantWithReturn,
  enforcementsOnTheCase,
} from '../support/repeat-enforcement';
import { backdateWarrantGeneration, monthsIn } from '../support/warrant-backdate';
import { requireCaseReference } from '../support/enforcement-journeys';

const NOT_LIVE = ['Closed', 'Rejected', 'Transferred to the High Court'];
const AWAITING_PAYMENT = 'Awaiting payment';
const AWAITING_REVIEW = 'Awaiting review';

const furtherTypeNotBuilt = (type: string) => ({
  summary: `a further "${type}" through these steps; only a warrant of possession is driven`,
});

function actualNumber(world: EnforcementWorld, alias: string): string {
  const number = world.warrantAliases[alias];
  if (!number) {
    throw new Error(`No warrant "${alias}" has been set up in this scenario`);
  }
  return number;
}

async function currentStatus(world: EnforcementWorld, page: Parameters<typeof openCaseTab>[0]): Promise<string> {
  await openCaseTab(page, requireCaseReference(world), ENFORCEMENT_STATUS_TAB);
  return (await currentEnforcement(page, world.earlierWarrantNumbers.length))['Status'];
}

Given('the case has a closed warrant with return name {string}', async ({ page, context, world }, returnName: string) => {
  await closeWarrantWithReturn(page, context, world, returnName);
});

Given('the case has a warrant closed with return name {string}', async ({ page, context, world }, returnName: string) => {
  await closeWarrantWithReturn(page, context, world, returnName);
});

Given(
  'the case has a warrant {string} closed with return name {string}',
  async ({ page, context, world }, alias: string, returnName: string) => {
    await closeWarrantWithReturn(page, context, world, returnName);
    world.warrantAliases[alias] = world.earlierWarrantNumbers.at(-1) as string;
  }
);

Given(
  'the case has a closed warrant with return name {string} generated {string} ago',
  async ({ page, context, world }, returnName: string, age: string) => {
    await closeWarrantWithReturn(page, context, world, returnName);
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
  if ((await currentStatus(world, page)) === AWAITING_PAYMENT) {
    await recordEnforcementFeePaid();
  }
});

When('the caseworker approves it', async ({ page, context, world }) => {
  try {
    await approveWarrantAsCaseworker(page, world, { feeOutstanding: false });
  } finally {
    await restoreClaimantSession(page, context);
  }
});

Then('I am {string} the warrant fee', async ({ page, world }, charged: string) => {
  if (charged !== 'charged' && charged !== 'not charged') {
    throw new Error(`Expected "charged" or "not charged", got "${charged}"`);
  }
  if (charged === 'not charged') {
    expect(world.askedToPayFee, 'the submit confirmation asked for the fee').toBe(false);
  }
  await expect
    .poll(() => currentStatus(world, page))
    .toBe(charged === 'charged' ? AWAITING_PAYMENT : AWAITING_REVIEW);
});

Then('I am not asked to pay a fee', async ({ page, world }) => {
  expect(world.askedToPayFee, 'the submit confirmation asked for the fee').toBe(false);
  expect(await currentStatus(world, page)).not.toBe(AWAITING_PAYMENT);
});

Then(
  'the case has two warrants, {string} closed and {string} live',
  async ({ page, world }, closedAlias: string, liveAlias: string) => {
    expect(world.warrantNumber, 'a warrant number generated when the further warrant was approved').toBeDefined();
    world.warrantAliases[liveAlias] = world.warrantNumber as string;
    const rows = await enforcementsOnTheCase(page, world);
    expect(rows).toHaveLength(2);

    const byNumber = (alias: string) =>
      rows.find(row => row['Warrant or writ number'] === actualNumber(world, alias));
    expect(byNumber(closedAlias)?.['Status'], `${closedAlias} is closed`).toBe('Closed');
    const live = byNumber(liveAlias);
    expect(live, `${liveAlias} is on the case`).toBeDefined();
    expect(NOT_LIVE, `${liveAlias} is live`).not.toContain(live?.['Status']);
  }
);

Then(
  'the {string} tab lists {string} above {string}',
  async ({ page, world, $tags }, tabName: string, upperAlias: string, lowerAlias: string) => {
    if (tabName !== ENFORCEMENT_STATUS_TAB) {
      return markPending({ summary: `ordering on the "${tabName}" tab` }, $tags);
    }
    const numbers = (await enforcementsOnTheCase(page, world)).map(row => row['Warrant or writ number']);
    const upper = numbers.indexOf(actualNumber(world, upperAlias));
    const lower = numbers.indexOf(actualNumber(world, lowerAlias));
    expect(upper, `${upperAlias} is listed`).toBeGreaterThanOrEqual(0);
    expect(lower, `${lowerAlias} is listed`).toBeGreaterThanOrEqual(0);
    expect(upper, `${upperAlias} is listed above ${lowerAlias}`).toBeLessThan(lower);
  }
);
