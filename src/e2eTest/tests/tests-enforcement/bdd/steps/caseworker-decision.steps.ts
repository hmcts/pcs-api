// Caseworker decides an enforcement application (feature 02), through the pcs-api "Decide enforcement" event.
import { Page } from '@playwright/test';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import {
  CLAIMANT_LEGAL_REPRESENTATIVE,
  createCaseWithWarrant,
  createCaseWithWrit,
  expectCaseTab,
  openCaseSummary,
  requireCaseReference,
  signInAs,
  startEvent,
} from '../support/enforcement-journeys';
import { enforcementNumber } from '../support/enforcement-status-tab';
import { expectSingleErrorMessage } from '../support/error-summary';
import {
  DECIDE_ENFORCEMENT,
  chooseDecision,
  clickContinue,
  decideEnforcement,
  decisionRow,
  expectConfirmation,
  goToCheckYourAnswers,
  confirmationPanel,
  recordEnforcementFeePaid,
  withDashes,
} from '../support/decide-enforcement';

const SEEDED_STATUSES = ['Awaiting payment', 'Awaiting review'];

// Only a warrant of possession can be seeded through the API today (enforceWarrantApiData).
Given('a PCS case {string} with an enforcement of type {string} with status {string}', async ({ world, $tags }, _reference: string, type: string, status: string) => {
  if (type !== 'warrant of possession' || !SEEDED_STATUSES.includes(status)) {
    return markPending({ ticket: 'HDPI-7419', summary: `seeding a ${type} with status "${status}"` }, $tags);
  }
  await createCaseWithWarrant(world);
  world.applicationType = type;
  if (status === 'Awaiting review') {
    approximatePrecondition('HDPI-2877', 'a local stack has no Payments service; the enforcement fee is paid through testing support');
    await recordEnforcementFeePaid();
  }
});

// The task is not created yet (WA DMNs). Scenarios that use it stop at the task step, which stays pending.
Given('a {string} task exists on the case', async ({}, task: string) => {
  approximatePrecondition('HDPI-8627', `the "${task}" task is not created yet`);
});

/**
 * The Background seeds a warrant of possession. A writ of possession can only be seeded from the
 * Create-Enforcement-Writ fixture, so the scenario carries on with a new case that has a writ awaiting review
 * instead.
 */
Given('the enforcement is of type {string}', async ({ world, $tags }, type: string) => {
  if (type === world.applicationType) {
    return;
  }
  if (type !== 'writ of possession') {
    return markPending({ ticket: 'HDPI-7419', summary: `seeding a ${type} through the API` }, $tags);
  }
  await createCaseWithWrit(world);
  world.applicationType = type;
  approximatePrecondition('HDPI-7419', 'the writ of possession is on a new case seeded from the writ fixture, not the Background case');
  await recordEnforcementFeePaid();
});

When('I select {string} without choosing an option', async ({ page, world }, button: string) => {
  expect(world.currentEvent).toBe(DECIDE_ENFORCEMENT);
  expect(button).toBe(decideEnforcement.continueButton);
  await clickContinue(page);
});

Then('I see an error asking me to choose accept or reject', async ({ page }) => {
  await expectSingleErrorMessage(page, decideEnforcement.decisionQuestion);
  await expect(page.getByRole('heading', { name: decideEnforcement.mainHeader })).toBeVisible();
});

When('I select {string} and {string}', async ({ page, world }, option: string, button: string) => {
  expect(world.currentEvent).toBe(DECIDE_ENFORCEMENT);
  expect(button).toBe(decideEnforcement.continueButton);
  await chooseDecision(page, option);
  await goToCheckYourAnswers(page);
});

Then('the {string} screen shows my decision with a {string} link', async ({ page }, screen: string, link: string) => {
  await expect(page.getByRole('heading', { name: screen })).toBeVisible();
  const row = decisionRow(page);
  await expect(row).toContainText(decideEnforcement.acceptRadioOption);
  await expect(row.getByText(link, { exact: true })).toBeVisible();
});

Given('I am on the {string} screen having accepted', async ({ page, world }, screen: string) => {
  expect(screen).toBe(decideEnforcement.checkYourAnswersHeader);
  await acceptTheApplication(page, world);
});

When('I select {string} next to the decision', async ({ page }, link: string) => {
  await decisionRow(page).getByText(link, { exact: true }).click();
});

Then('I return to the {string} screen with {string} still selected', async ({ page }, screen: string, option: string) => {
  await expect(page.getByRole('heading', { name: screen })).toBeVisible();
  await expect(page.getByRole('radio', { name: option, exact: true })).toBeChecked();
});

Given('I have accepted the application', async ({ page, world }) => {
  await acceptTheApplication(page, world);
});

When('I enter a reason for rejecting', async ({ page }) => {
  await page.getByLabel(decideEnforcement.rejectionReasonLabel).fill(decideEnforcement.rejectionReasonInput);
});

Then('I see the {string} confirmation screen with the case number, property address and party names', async ({ page, world }, title: string) => {
  await expectConfirmation(page, title);
  const panel = confirmationPanel(page);
  await expect(panel.getByText(`Case number: ${withDashes(String(world.caseReference))}`)).toBeVisible();
  await expect(panel.getByText(/^Property address: .+/)).toBeVisible();
  await expect(panel.getByText(/ vs /).first()).toBeVisible();
});

Then('I see the {string} confirmation screen', async ({ page }, title: string) => {
  await expectConfirmation(page, title);
});

// The number format is a placeholder until HDPI-7352 agrees it (pcs-api EnforcementNumberGenerator).
Then('a warrant number is assigned to the enforcement', async ({ page, world }) => {
  await expect(confirmationPanel(page).getByText(/^Warrant number: W[PR]-\d{8,}$/)).toBeVisible();
  // The same number is the one the "Enforcement status" tab records against the enforcement.
  await expect.poll(() => enforcementNumber(page, requireCaseReference(world))).toMatch(/^W[PR]-\d{8,}$/);
});

// The number format is a placeholder until HDPI-7353 agrees it (pcs-api EnforcementNumberGenerator).
Then('a writ number is assigned to the enforcement', async ({ page, world }) => {
  await expect(confirmationPanel(page).getByText(/^Writ number: H[PR]-\d{8,}$/)).toBeVisible();
  await expect.poll(() => enforcementNumber(page, requireCaseReference(world))).toMatch(/^H[PR]-\d{8,}$/);
});

async function acceptTheApplication(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent(DECIDE_ENFORCEMENT);
  world.currentEvent = DECIDE_ENFORCEMENT;
  await chooseDecision(page, decideEnforcement.acceptRadioOption);
  await goToCheckYourAnswers(page);
}

pendingSteps({ ticket: 'HDPI-9566', summary: 'Sealed warrant and writ documents' }, [
  'the claimant has the {string} case flag',
  'the warrant is generated in Welsh',
  'no warrant is generated',
]);

pendingSteps({ summary: 'Checking the case history after the decision' }, [
  'the case {string} tab shows the {string} event',
]);

// The caseworker who submitted the decision is internal; the claimant side then signs in to check its own view.
Then('the {string} tab is shown on the case to internal users and the claimant', async ({ page, context }, tabName: string) => {
  await openCaseSummary(page);
  await expectCaseTab(page, tabName, true);
  await signInAs(CLAIMANT_LEGAL_REPRESENTATIVE, page, context);
  await openCaseSummary(page);
  await expectCaseTab(page, tabName, true);
});

Then('the {string} tab is not shown', async ({ page }, tabName: string) => {
  await openCaseSummary(page);
  await expectCaseTab(page, tabName, false);
});

pendingSteps({ ticket: 'HDPI-7539', summary: 'Signing in again as the claimant legal representative to reapply' }, [
  'the claimant can apply again using {string}',
]);
