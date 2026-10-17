// Enforcement status model (feature 19 and status assertions elsewhere).
//
// The status lives on the `enforcements` collection and is visible on the "Enforcement status" tab, so the
// assertions read it from there. What is still missing is any way to *put* an enforcement into an arbitrary
// status: pcs-api's only enforcement testing-support endpoint is `fee-paid`, so a scenario can only reach a
// status by driving the real events. Steps that need a status set up for them stay pending on HDPI-7419.
import { Given, Then, expect } from '../fixtures/enforcement.fixtures';
import { pendingSteps } from '../fixtures/pending';
import { enforcementRows, expectEnforcementStatus, openCaseTab } from '../support/enforcement-status-tab';
import { setUpWarrantAtStatus } from '../support/approve-warrant';
import { requireCaseReference } from '../support/enforcement-journeys';

Then('the enforcement status changes to {string}', async ({ page, world }, status: string) => {
  await expectEnforcementStatus(page, requireCaseReference(world), status);
});

Then('the enforcement status is {string}', async ({ page, world }, status: string) => {
  await expectEnforcementStatus(page, requireCaseReference(world), status);
});

Then('the enforcement status is still {string}', async ({ page, world }, status: string) => {
  await expectEnforcementStatus(page, requireCaseReference(world), status);
});

Then('the case has no live warrant', async ({ page, world }) => {
  await openCaseTab(page, requireCaseReference(world), 'Enforcement status');
  const live = (await enforcementRows(page)).filter(
    row => !['Closed', 'Rejected', 'Transferred to the High Court'].includes(row['Status'])
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
  'a new enforcement of type {string} is added to the case with status {string}',
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
