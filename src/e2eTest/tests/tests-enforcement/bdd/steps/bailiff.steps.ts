// Bailiff journeys (features 03-05, 07-11, 17).
//
// What is drivable today: `setRiskLevel` (feature 04) and `addBailiffNote` (feature 10). Both are built,
// and the cftlib bailiff users (CftlibConfig + cftlib-am-role-assignments.json) can now reach them, so
// their steps are implemented.
//
// What is not, and why:
//   * `allocateBailiff` (03) — its StaffUser type-ahead needs staff reference data that cftlib does not
//     run, so the page degrades to a raw `IdamId` field and the real validation never runs. HDPI-7356.
//   * every status after "Awaiting bailiff allocation" — reaching one needs a bailiff allocated, so
//     `scheduleEviction` (05), `rescheduleEviction` (07), `markNoticeOfEvictionServed` (08) and
//     `recordWarrantReturn` (11) cannot be started. HDPI-7419.
//   * the `allocated-bailiff` case role — `BailiffAllocationService.allocate()` writes only to
//     `enf_bailiff_allocation`; the AM case role is a TODO. HDPI-8189.
//   * WA tasks — nothing creates them. HDPI-8627.
//   * batch print (17) — HDPI-8012.
import { BrowserContext, Page } from '@playwright/test';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import { openCaseSummary, requireCaseReference, signInAs, startEvent } from '../support/enforcement-journeys';
import { CLAIMANT_LEGAL_REPRESENTATIVE } from '../support/session';
import { RISK_ASSESSMENT_TAB, expectTabField, openTabFields } from '../support/case-tabs';
import { openCaseTab, tabSections } from '../support/enforcement-status-tab';
import {
  ADD_BAILIFF_NOTE,
  SET_RISK_LEVEL,
  addBailiffNote,
  chooseRiskLevel,
  continueButton,
  dashedCaseNumber,
  errorSummary,
  expectBailiffConfirmation,
  expectBailiffConfirmationWithWarrantNumber,
  riskAssessmentFields,
  setRiskLevel,
} from '../support/bailiff-events';
import { expectEvictionResult, isEvictionResult } from './schedule-eviction.steps';


// ---------------------------------------------------------------------------
// Feature 04: the bailiff sets the eviction risk level
// ---------------------------------------------------------------------------

// The allocation itself cannot be made (HDPI-7356 / HDPI-8189), but a bailiff reaches the warrant through
// their `bailiff` organisation role regardless, which is what the rest of the scenario needs.
Given('a PCS case {string} with a warrant of possession allocated to me', async ({ page, context, world }, _reference: string) => {
  const { setUpWarrantAtStatus, AWAITING_BAILIFF_ALLOCATION } = await import('../support/approve-warrant');
  await setUpWarrantAtStatus(page, context, world, AWAITING_BAILIFF_ALLOCATION);
  approximatePrecondition(
    'HDPI-8189',
    'no bailiff is allocated: allocateBailiff needs staff reference data cftlib does not run, and the '
      + 'allocated-bailiff case role is not granted yet. The bailiff reaches the warrant by their bailiff role.'
  );
});

When('I select {string} without choosing a risk level', async ({ page }, button: string) => {
  expect(button).toBe('Continue');
  await continueButton(page).click();
});

Then('I see an error asking me to choose a risk level', async ({ page }) => {
  await expect(errorSummary(page)).toContainText(setRiskLevel.question);
  await expect(page.getByRole('heading', { name: setRiskLevel.pageLabel })).toBeVisible();
});

Then('I see the text box {string}', async ({ page }, label: string) => {
  expect(label).toBe(setRiskLevel.reasonLabel);
  await expect(page.getByLabel(label)).toBeVisible();
});

Then('the hint {string}', async ({ page }, hint: string) => {
  await expect(page.getByText(hint, { exact: false }).first()).toBeVisible();
});

Then('the text box accepts letters, numbers and special characters up to 6,800 characters', async ({ page }) => {
  const reasonBox = page.getByLabel(setRiskLevel.reasonLabel);
  const mixed = `Risk 1 & 2 — "quoted", <tagged>, 50% @ #3; ${'a'.repeat(200)}`;
  await reasonBox.fill(mixed);
  await expect(reasonBox).toHaveValue(mixed);

  const atLimit = 'x'.repeat(setRiskLevel.reasonMaxLength);
  await reasonBox.fill(atLimit);
  expect(await reasonBox.inputValue(), 'the full 6,800 characters are kept').toHaveLength(setRiskLevel.reasonMaxLength);
});

Given('I set the risk level to {string} with reason {string}', async ({ page, world }, level: string, reason: string) => {
  await openCaseSummary(page);
  await startEvent(SET_RISK_LEVEL);
  world.currentEvent = SET_RISK_LEVEL;
  await chooseRiskLevel(page, level, reason);
  await continueButton(page).click();
  await expectBailiffConfirmation(page, `Risk level set to ${level}`, requireCaseReference(world));
  world.currentEvent = undefined;
});

Then('I see the reason {string}', async ({ page, world }, reason: string) => {
  await expectTabField(page, requireCaseReference(world), RISK_ASSESSMENT_TAB, riskAssessmentFields.riskLevelReason, reason);
});

Then('the {string} tab shows the risk level {string}', async ({ page, world }, tabName: string, level: string) => {
  expect(tabName).toBe(RISK_ASSESSMENT_TAB);
  await expectTabField(page, requireCaseReference(world), RISK_ASSESSMENT_TAB, riskAssessmentFields.riskLevel, level);
});

Then('no risk level is saved', async ({ page, world }) => {
  const fields: Record<string, string> = await openTabFields(page, requireCaseReference(world), RISK_ASSESSMENT_TAB).catch(
    () => ({}) as Record<string, string>
  );
  expect(fields[riskAssessmentFields.riskLevel], 'no risk level on the risk assessment tab').toBeUndefined();
});

Given('the risk level is already {string}', async ({ page, world }, level: string) => {
  await openCaseSummary(page);
  await startEvent(SET_RISK_LEVEL);
  world.currentEvent = SET_RISK_LEVEL;
  await chooseRiskLevel(page, level);
  await continueButton(page).click();
  await expectBailiffConfirmation(page, `Risk level set to ${level}`, requireCaseReference(world));
  world.currentEvent = undefined;
});

Then('{string} is pre-selected', async ({ page }, level: string) => {
  await expect(page.getByRole('radio', { name: level, exact: true })).toBeChecked();
});

Then('I can change it to {string}', async ({ page }, level: string) => {
  await chooseRiskLevel(page, level);
  await expect(page.getByRole('radio', { name: level, exact: true })).toBeChecked();
});

/**
 * The built `confirmEviction` only asks "Can you attend the eviction on this date?" — it does not collect
 * the dates the claimant is not available. Recorded as approximated so the rest of the scenario can run;
 * the scenario that actually lists those dates stays pending.
 */
Given('the claimant said they are not available on {string} and {string}', async ({}, first: string, second: string) => {
  approximatePrecondition(
    'HDPI-7547',
    `the claimant's unavailable dates (${first}, ${second}) are not captured: confirmEviction only asks `
      + 'whether they can attend'
  );
});

/**
 * The confirmation screens that name both numbers: schedule and reschedule eviction, mark notice served and
 * record warrant return (#3016). `SetRiskLevel` and `AddBailiffNote` still show only the case number, so a
 * scenario asserting a warrant number on those two fails here rather than being quietly weakened.
 */
Then('I see the confirmation {string} with the case number and warrant number', async ({ page, world }, header: string) => {
  expect(world.warrantNumber, 'a warrant number generated when the application was approved').toBeDefined();
  await expectBailiffConfirmationWithWarrantNumber(
    page,
    header,
    requireCaseReference(world),
    world.warrantNumber as string
  );
});

Then('I see the case number and the property address', async ({ page, world }) => {
  const details = page.locator('dl.govuk-summary-list').filter({ hasText: 'Property address' }).first();
  await expect(details).toContainText(dashedCaseNumber(requireCaseReference(world)));
  await expect(details).toContainText('Property address');
});

Then('I see the question {string} with options {string} and {string}', async ({ page }, question: string, first: string, second: string) => {
  await expect(page.getByText(question, { exact: false }).first()).toBeVisible();
  await expect(page.getByRole('radio', { name: first, exact: true })).toBeVisible();
  await expect(page.getByRole('radio', { name: second, exact: true })).toBeVisible();
});

When('I select {string} and provide any required reason', async ({ page, world }, level: string) => {
  expect(world.currentEvent).toBe(SET_RISK_LEVEL);
  await chooseRiskLevel(page, level);
  world.riskLevel = level;
});

/**
 * The risk level, its reason and the bailiff notes are internal. The claimant side sees the "Risk assessment"
 * tab too, with only the claimant's own risk answers (13-risk-assessment-tab.feature), so "internal users
 * only" is checked by finding the internal sections as the signed-in bailiff, then signing in as the
 * claimant's legal representative and finding the tab without them.
 */
async function expectRiskAssessmentTabInternalOnly(page: Page, context: BrowserContext, caseReference: string): Promise<void> {
  await openCaseTab(page, caseReference, RISK_ASSESSMENT_TAB);
  expect(Object.keys(await tabSections(page))).toEqual(expect.arrayContaining(['Bailiff note', 'Risk assessment level']));
  await signInAs(CLAIMANT_LEGAL_REPRESENTATIVE, page, context);
  await openCaseTab(page, caseReference, RISK_ASSESSMENT_TAB);
  const claimantSections = Object.keys(await tabSections(page));
  expect(claimantSections).toContain('Claimant risk level');
  expect(claimantSections).not.toContain('Bailiff note');
  expect(claimantSections).not.toContain('Risk assessment level');
}

Then('the risk level is visible to internal users only', async ({ page, context, world }) => {
  await expectRiskAssessmentTabInternalOnly(page, context, requireCaseReference(world));
});

Then('the reason is visible to internal users only', async ({ page, context, world }) => {
  await expectRiskAssessmentTabInternalOnly(page, context, requireCaseReference(world));
});

// ---------------------------------------------------------------------------
// Feature 10: add a bailiff note
// ---------------------------------------------------------------------------

Then('I see the {string} text box', async ({ page }, label: string) => {
  await expect(page.getByLabel(label)).toBeVisible();
});

When('I enter {string} and select {string}', async ({ page, world }, note: string, button: string) => {
  expect(button).toBe('Continue');
  expect(world.currentEvent).toBe(ADD_BAILIFF_NOTE);
  // The feature describes the long case as "6,801 characters" rather than spelling it out.
  const text = note === '6,801 characters' ? 'x'.repeat(addBailiffNote.noteMaxLength + 1) : note;
  if (text) {
    await page.getByLabel(addBailiffNote.noteLabel).fill(text);
  }
  await continueButton(page).click();
  world.enteredNote = text;
});

When('I add the bailiff note {string}', async ({ page, world }, note: string) => {
  await openCaseSummary(page);
  await startEvent(ADD_BAILIFF_NOTE);
  world.currentEvent = ADD_BAILIFF_NOTE;
  await page.getByLabel(addBailiffNote.noteLabel).fill(note);
  await continueButton(page).click();
  await expectBailiffConfirmation(page, 'Bailiff note added', requireCaseReference(world));
  world.currentEvent = undefined;
  world.enteredNote = note;
});

Then('I see {string}', async ({ page, world, $tags }, result: string) => {
  switch (result) {
    case 'the standard XUI error that a note is needed':
      await expect(errorSummary(page)).toContainText(`${addBailiffNote.noteLabel} is required`);
      return;
    case 'the standard XUI error for too many characters':
      await expect(errorSummary(page)).toBeVisible();
      await expect(page.getByRole('heading', { name: addBailiffNote.pageLabel })).toBeVisible();
      return;
    case 'the confirmation screen':
      await expectBailiffConfirmation(page, 'Bailiff note added', requireCaseReference(world));
      return;
    default:
      if (isEvictionResult(result)) {
        return expectEvictionResult(page, result);
      }
      return markPending({ summary: `asserting "${result}"` }, $tags);
  }
});

// ---------------------------------------------------------------------------
// Still pending: the events and plumbing that do not exist
// ---------------------------------------------------------------------------

pendingSteps({ ticket: 'HDPI-8627', summary: 'Opening an enforcement journey from a work allocation task' }, [
  'the {string} screen opens with heading {string}',
]);

pendingSteps({ ticket: 'HDPI-7593', summary: 'The bailiff note confirmation does not link to the risk assessment tab' }, [
  '{string} takes me to the {string} tab',
]);

/**
 * The newest note under the section, with its author and a time in the layout of the quoted example: "1 Feb
 * 2026 16:20pm" is a day, a short month, a year, a 24-hour time and a lower case am / pm marker.
 */
Then(
  'the note {string} is shown under {string} with {string} and the date and time in {string} format',
  async ({ page, world }, note: string, section: string, author: string, example: string) => {
    expect(example).toMatch(/^\d{1,2} [A-Z][a-z]{2} \d{4} \d{2}:\d{2}(am|pm)$/);
    await openCaseTab(page, requireCaseReference(world), RISK_ASSESSMENT_TAB);
    const [newest] = (await tabSections(page))[section]?.lists ?? [];
    expect(newest?.[riskAssessmentFields.note]).toBe(note);
    expect(newest?.[riskAssessmentFields.noteAddedBy]).toBe(author);
    expect(newest?.[riskAssessmentFields.noteDateTime]).toMatch(/^\d{1,2} [A-Z][a-z]{2} \d{4} \d{2}:\d{2}(am|pm)$/);
  }
);

Then('the note is visible to internal users only', async ({ page, context, world }) => {
  await expectRiskAssessmentTabInternalOnly(page, context, requireCaseReference(world));
});

// The note is markup on the tab, so there is nothing to edit; the scenario around it needs a named bailiff.
pendingSteps({ ticket: 'HDPI-7593', summary: 'Asserting that a saved bailiff note cannot be edited' }, [
  'the note cannot be edited',
]);

pendingSteps({ ticket: 'HDPI-7539', summary: 'Defendant visibility of enforcement' }, [
  'defendants cannot see the {string} tab',
]);

// Two notes on two different dates need the clock moved between them; nothing can backdate a note.
pendingSteps({ ticket: 'HDPI-7593', summary: 'Bailiff notes added on different dates cannot be set up' }, [
  'the case has bailiff notes added on {string} and {string}',
  'the note from {string} is shown first',
]);

pendingSteps({ ticket: 'HDPI-7356', summary: 'Allocate case to bailiff needs staff reference data for its type-ahead' }, [
  'no {string} screen is shown',
  'I type part of a bailiff\'s name or email address',
  'I see matching bailiff users for the case\'s court location',
  'users without a bailiff role are not listed',
  'I select {string} without choosing a bailiff',
  'I see an error asking me to choose a bailiff',
  'I have chosen bailiff {string} {string}',
  'the {string} screen shows {string} and {string}',
  'I return to {string} with {string} still selected',
  'no bailiff is allocated',
  'I am on the {string} screen with {string} chosen',
  'I submit the allocation',
  'I allocate the warrant to {string}',
  '{string} is the allocated bailiff',
  '{string} already has 499 active {string} case roles',
  'the allocation succeeds',
]);

pendingSteps({ ticket: 'HDPI-8189', summary: 'The allocated-bailiff case role is not granted yet' }, [
  '{string} has the {string} case role on the case with a start date of today and no end date',
  'the {string} tab lists {string} under {string} with email, role and start date',
  'the {string} tab lists only {string} under {string}',
  '{string} no longer has the {string} case role',
  'I no longer have the {string} case role',
  'the {string} tab no longer lists me under {string}',
  'my role end date is recorded as today',
]);

pendingSteps({ ticket: 'HDPI-7344', summary: 'Schedule eviction cannot be reached: it needs a bailiff allocation' }, [
  'I see an {string} table with claimant type, claimant name and defendant names',
  'the case has two defendants whose names are not known',
  'the {string} table shows {string} once',
  'it is stored in the {string} folder in case file view',
  'it is visible to internal users only',
  'a {string} document is generated without a seal',
  'it is visible to the claimant and internal users',
  'I answered {string} to {string}',
]);

pendingSteps({ ticket: 'HDPI-7549', summary: 'Reschedule eviction cannot be reached: it needs a scheduled eviction' }, [
  'I see the dates {string} and {string} listed as dates the claimant is not available',
  'the dates are not editable',
  'a new sealed {string} is stored in the {string} folder',
  'a new unsealed {string} is stored in the {string} folder and sent to the claimant',
  'the original notices are still in the {string} folder',
]);

pendingSteps({ ticket: 'HDPI-7346', summary: 'Mark \'notice of eviction\' as served cannot be reached: it needs a scheduled eviction' }, [
  'I enter a served date of {string} and time {string}',
  'I answer {string} to {string}',
  'I enter the note {string}',
  'I have answered the questions',
  'defendants can see the {string} tab',
  'defendants can see the {string} folder in case file view',
  'the {string} is visible to defendants',
  'citizen defendants see the {string} under {string} in pcs-frontend',
]);

// The review banner lists the changed questions but does not link to them: XUI routes the tab by the URL
// fragment, so an in-page link would change tab (HDPI-7350).
pendingSteps({ ticket: 'HDPI-7350', summary: 'Links from the review banner to the changed questions' }, [
  'I see an important banner with links to each question whose answer changed',
  'I select the banner link for {string}',
  'I am taken to that question in the {string} section',
]);

pendingSteps({ ticket: 'HDPI-8627', summary: 'Set risk level from the "Review updated risk information" task' }, [
  'the {string} journey opens with {string} pre-selected',
  'I can change it',
  'I submit the risk level {string} with a reason',
]);

pendingSteps({ ticket: 'HDPI-7347', summary: 'Record warrant return cannot be reached: it needs a bailiff allocation' }, [
  'the eviction is scheduled for today',
  'I enter the eviction date {string} and time {string}',
  'I see an error that the time cannot be in the future',
  'the {string} text box is {string}',
  'I have chosen a return',
  'no return is saved',
  'I have completed the {string} screen',
  'I submit a return with return name {string}',
  'I submit a return with return name {string} and additional details {string}',
  'the notice of eviction was served on the defendants',
  'I submit a return that closes the enforcement',
  'the bailiff recorded the return {string}',
]);

pendingSteps({ ticket: 'HDPI-8012', summary: 'Batch print' }, [
  'I am the allocated bailiff on cases {string}, {string} and {string}',
  'each case has an eviction scheduled this week',
  'I choose {string}',
  'I am given one file containing, for each case in turn, the warrant, the risk assessment and the notice of eviction',
  'case {string} has an original and a rescheduled notice of eviction',
  'I batch print that case',
  'only the latest notice of eviction is included',
  'I select a case where I am not the allocated bailiff',
  'that case is left out and I am told why',
]);
