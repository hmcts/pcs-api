import { Given, Step, Then, expect } from '../fixtures/enforcement.fixtures';
import { markPending, pendingSteps } from '../fixtures/pending';
import { currentEnforcement, enforcementRows, expectEnforcementStatus } from '../support/enforcement-status-tab';
import { bringWarrantToStatus, canSetUpStatus, setUpWarrantAtStatus } from '../support/approve-warrant';
import { createIssuedCase, requireCaseReference } from '../support/enforcement-journeys';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

Then('the enforcement status changes to {string}', async ({ page, world }, status: string) => {
  const expected = status === 'unchanged' ? world.statusBeforeReturn : status;
  expect(expected, 'the status before the return the scenario submitted').toBeDefined();
  await expectEnforcementStatus(page, requireCaseReference(world), expected as string, world.earlierWarrantNumbers.length);
});

Step('the enforcement status is {string}', async ({ page, world, $bddContext, $tags }, status: string) => {
  if ($bddContext.bddTestData.steps[$bddContext.stepIndex]?.keywordType === 'Context') {
    if (!canSetUpStatus(status)) {
      return markPending({ ticket: 'HDPI-7419', summary: `a warrant in "${status}" cannot be set up` }, $tags);
    }
    return bringWarrantToStatus(world, status);
  }
  await expectEnforcementStatus(page, requireCaseReference(world), status, world.earlierWarrantNumbers.length);
});

Then('the enforcement status is still {string}', async ({ page, world }, status: string) => {
  await expectEnforcementStatus(page, requireCaseReference(world), status, world.earlierWarrantNumbers.length);
});

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

Given('the case has a warrant of possession with status {string}', async ({ world }, status: string) => {
  await setUpWarrantAtStatus(world, status);
});

Given('the case has a warrant with status {string}', async ({ world }, status: string) => {
  await setUpWarrantAtStatus(world, status);
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

Then('a new enforcement of type {string} is added to the case with status {string}', async ({ world }, type: string, status: string) => {
  await expect
    .poll(async () => {
      const enforcement = await currentEnforcement(requireCaseReference(world));
      return { type: enforcement.type, status: enforcement.status };
    }, { timeout: VERY_LONG_TIMEOUT })
    .toEqual({ type: type.charAt(0).toUpperCase() + type.slice(1), status });
});
