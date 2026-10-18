// Enforcement status model (feature 19 and status assertions elsewhere).
//
// The status of every application is on the `enforcements` collection pcs-api sets on the case view, so the
// assertions read it from the case data: the "Enforcement status" tab only appears once a warrant or writ has
// been generated, and an application awaiting payment or review has a status too.
//
// A scenario reaches a status by driving the real events, with the testing-support shortcuts for the fee and the
// bailiff allocation (`support/approve-warrant.ts`). Statuses no event reaches yet stay pending on HDPI-7419.
import { Given, Step, Then, expect } from '../fixtures/enforcement.fixtures';
import { markPending, pendingSteps } from '../fixtures/pending';
import { currentEnforcement, enforcementRows, expectEnforcementStatus } from '../support/enforcement-status-tab';
import { bringWarrantToStatus, canSetUpStatus, setUpWarrantAtStatus } from '../support/approve-warrant';
import { createIssuedCase, requireCaseReference } from '../support/enforcement-journeys';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

/** "unchanged" is the status the warrant had when the scenario last submitted a return. */
Then('the enforcement status changes to {string}', async ({ page, world }, status: string) => {
  const expected = status === 'unchanged' ? world.statusBeforeReturn : status;
  expect(expected, 'the status before the return the scenario submitted').toBeDefined();
  await expectEnforcementStatus(page, requireCaseReference(world), expected as string, world.earlierWarrantNumbers.length);
});

/**
 * As a Given (or an And after one), puts the scenario's warrant in the status, which the Background may have
 * set up differently. Elsewhere, asserts it. A status no event can reach yet is pending (HDPI-7419).
 */
Step('the enforcement status is {string}', async ({ page, context, world, $bddContext, $tags }, status: string) => {
  if ($bddContext.bddTestData.steps[$bddContext.stepIndex]?.keywordType === 'Context') {
    if (!canSetUpStatus(status)) {
      return markPending({ ticket: 'HDPI-7419', summary: `a warrant in "${status}" cannot be set up` }, $tags);
    }
    return bringWarrantToStatus(page, context, world, status);
  }
  await expectEnforcementStatus(page, requireCaseReference(world), status, world.earlierWarrantNumbers.length);
});

Then('the enforcement status is still {string}', async ({ page, world }, status: string) => {
  await expectEnforcementStatus(page, requireCaseReference(world), status, world.earlierWarrantNumbers.length);
});

/**
 * A warrant cannot be taken off a case, and a closed one still takes "Reopen warrant", so the scenario moves
 * to a new issued case with no enforcement on it.
 */
Given('the case has no live warrant', async ({ world }) => {
  await createIssuedCase(world);
  world.enforcementSeeded = false;
  world.warrantApproved = false;
  world.warrantNumber = undefined;
  world.allocatedBailiff = undefined;
  const live = (await enforcementRows(requireCaseReference(world))).filter(
    row => !['Closed', 'Rejected', 'Transferred to the High Court'].includes(row.status ?? '')
  );
  expect(live, 'no live enforcement on the case').toHaveLength(0);
});

/**
 * Puts a warrant on the case in the given status. Used mid-scenario, after a case already exists, so it
 * builds a fresh case with a warrant: the enforcement fixture is the only local route to one and the case
 * the scenario started with carries no enforcement.
 */
Given('the case has a warrant of possession with status {string}', async ({ page, context, world }, status: string) => {
  await setUpWarrantAtStatus(page, context, world, status);
});

Given('the case has a warrant with status {string}', async ({ page, context, world }, status: string) => {
  await setUpWarrantAtStatus(page, context, world, status);
});

pendingSteps({ ticket: 'HDPI-7419', summary: 'Setting up an enforcement in an arbitrary status' }, [
  'a {string} enforcement is added to the case with status {string}',
  'a warrant moves from {string} to {string}',
  'the enforcement status changes',
]);

pendingSteps({ ticket: 'HDPI-7419', summary: 'Asserting the CCD case state alongside the enforcement status' }, [
  'the case state is {string}',
  'the case state is still {string}',
]);

pendingSteps({ summary: 'Asserting event availability for a role the suite cannot reach that status as' }, [
  'the {string} event is {string}',
  'the {string}, {string}, {string} and {string} events are available',
]);

/**
 * The scenario's case starts with no enforcement, so the new one is the only one. Read from the case data:
 * the "Enforcement status" tab only appears once a warrant or writ has been generated. The type is compared
 * as pcs-api's SelectEnforcementType label, e.g. "Warrant of possession".
 */
Then('a new enforcement of type {string} is added to the case with status {string}', async ({ world }, type: string, status: string) => {
  await expect
    .poll(async () => {
      const enforcement = await currentEnforcement(requireCaseReference(world));
      return { type: enforcement.type, status: enforcement.status };
    }, { timeout: VERY_LONG_TIMEOUT })
    .toEqual({ type: type.charAt(0).toUpperCase() + type.slice(1), status });
});
