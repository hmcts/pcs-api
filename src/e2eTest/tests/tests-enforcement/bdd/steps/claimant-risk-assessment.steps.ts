// The claimant updates the risk assessment in "Confirm the eviction details" and the bailiff reviews it on the
// "Risk assessment" tab (feature 09, and the risk assessment scenarios of feature 06). HDPI-7552.
//
// The dates in the feature files cannot be set up: the original answers are the seeded application's, given
// today, and the update is made today. The steps assert today's date and record the gap as an approximated
// precondition. The "Review updated risk information" task is not created (HDPI-8627).
import { BrowserContext, Page } from '@playwright/test';
import { Given, Then, When, expect, test } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import { openCaseSummary, requireCaseReference, signInAs, startEvent } from '../support/enforcement-journeys';
import { CLAIMANT_LEGAL_REPRESENTATIVE } from '../support/session';
import { RISK_ASSESSMENT_TAB } from '../support/case-tabs';
import { activeTab, openCaseTab, tabSections, todayTabDate } from '../support/enforcement-status-tab';
import { SET_RISK_LEVEL, chooseRiskLevel, continueButton, expectBailiffConfirmation } from '../support/bailiff-events';
import { allocateBailiff, confirmEvictionAsClaimant, scheduleEvictionAsBailiff } from '../support/approve-warrant';
import {
  CLAIMANT_RISK_SECTION,
  CONFIRM_EVICTION,
  UPDATE_BANNER,
  answerAttendance,
  answerDates,
  changeRiskAnswer,
  claimantRiskAnswerRow,
  claimantRiskAssessmentFields,
  continueToCheckYourAnswers,
  expectConfirmEvictionSubmitted,
  riskQuestion,
  riskQuestions,
  tabQuestion,
} from '../support/claimant-risk-assessment';

/** The internal sections of the "Risk assessment" tab, and the heading of the review banner. */
const INTERNAL_SECTIONS = ['Bailiff note', 'Risk assessment level', 'Important'];

function inConfirmEviction(currentEvent: string | undefined): boolean {
  return currentEvent === CONFIRM_EVICTION;
}

async function claimantSectionText(page: Page): Promise<string> {
  return (await tabSections(page))[CLAIMANT_RISK_SECTION]?.text ?? '';
}

/** The changed question's row: the original answer differs from the updated one, which is the changed answer. */
async function expectChangedAnswer(page: Page, world: EnforcementWorld): Promise<void> {
  expect(world.riskQuestion, 'a risk answer changed by this scenario').toBeDefined();
  const row = await claimantRiskAnswerRow(page, world.riskQuestion as string);
  expect(row[claimantRiskAssessmentFields.updatedAnswer]).toBe(world.changedRiskAnswer);
  expect(row[claimantRiskAssessmentFields.originalAnswer]).not.toBe(world.changedRiskAnswer);
}

async function openRiskAssessmentAs(role: string, page: Page, context: BrowserContext, world: EnforcementWorld) {
  await signInAs(role, page, context);
  await openCaseTab(page, requireCaseReference(world), RISK_ASSESSMENT_TAB);
}

// ---------------------------------------------------------------------------
// Feature 06: the claimant changes (or keeps) their answers while confirming
// ---------------------------------------------------------------------------

When('I change my answer to {string} to {string}', async ({ page, world, $tags }, question: string, answer: string) => {
  if (!inConfirmEviction(world.currentEvent)) {
    return markPending({ summary: `changing a risk answer outside "${CONFIRM_EVICTION}"` }, $tags);
  }
  await answerAttendance(page, true);
  await changeRiskAnswer(page, riskQuestion(question), answer);
  await continueToCheckYourAnswers(page);
  world.riskQuestion = tabQuestion(question);
  world.changedRiskAnswer = answer;
  world.changedRiskQuestions = [...(world.changedRiskQuestions ?? []), world.riskQuestion];
});

When('I do not change any risk assessment answers', async ({ page, world, $tags }) => {
  if (!inConfirmEviction(world.currentEvent)) {
    return markPending({ summary: `going through the risk answers outside "${CONFIRM_EVICTION}"` }, $tags);
  }
  await answerAttendance(page, true);
  await continueToCheckYourAnswers(page);
});

// The bailiff reads the internal tab, which shows the same answers as the claimant's.
Then('the updated answer is stored alongside my original answer with today\'s date', async ({ page, context, world }) => {
  await expectConfirmEvictionSubmitted(page);
  await openRiskAssessmentAs('bailiff', page, context, world);
  await expectChangedAnswer(page, world);
  expect(await claimantSectionText(page)).toContain(answerDates.updatedForInternal(todayTabDate()));
  await signInAs(CLAIMANT_LEGAL_REPRESENTATIVE, page, context);
});

Given('I have updated my risk assessment', async ({ page, context, world }) => {
  await confirmEvictionAsClaimant(page, context, world, true, riskQuestions.vulnerablePeople);
  world.riskQuestion = tabQuestion(riskQuestions.vulnerablePeople);
  world.changedRiskAnswer = 'Yes';
});

Then('I see my original answers alongside my updated answers', async ({ page, world }) => {
  await expectChangedAnswer(page, world);
  expect(await claimantSectionText(page)).toContain(answerDates.updatedForClaimant(todayTabDate()));
});

Then('I do not see the risk level or bailiff notes', async ({ page }) => {
  const headings = Object.keys(await tabSections(page));
  expect(headings).toContain(CLAIMANT_RISK_SECTION);
  for (const internal of INTERNAL_SECTIONS) {
    expect(headings, `no "${internal}" on the claimant's tab`).not.toContain(internal);
  }
  await expect(page.getByText(UPDATE_BANNER)).toBeHidden();
});

// ---------------------------------------------------------------------------
// Feature 09: the bailiff reviews the updated risk assessment
// ---------------------------------------------------------------------------

// Set before the claimant's update, as in the feature, so the update is waiting for review.
Given('the risk level is {string}', async ({ page, context, world }, level: string) => {
  await signInAs('bailiff', page, context);
  await openCaseSummary(page);
  await startEvent(SET_RISK_LEVEL);
  await chooseRiskLevel(page, level);
  await continueButton(page).click();
  await expectBailiffConfirmation(page, `Risk level set to ${level}`, requireCaseReference(world));
  world.riskLevel = level;
});

Given('the claimant submitted the original risk assessment on {string}', async ({}, date: string) => {
  approximatePrecondition('HDPI-7552', `the original answers are the seeded application's, given today rather than on ${date}`);
});

/**
 * Allocates and schedules the warrant, so the claimant is asked to confirm, then confirms with "Is anyone
 * living at the property vulnerable?" changed to "Yes".
 */
Given('the claimant updated the risk assessment on {string} in {string}', async ({ page, context, world }, date: string, journey: string) => {
  expect(journey).toBe(CONFIRM_EVICTION);
  approximatePrecondition('HDPI-7552', `the claimant updated the risk assessment today rather than on ${date}`);
  if (!world.allocatedBailiff) {
    await allocateBailiff(world);
    approximatePrecondition(
      'HDPI-7356',
      'the bailiff was allocated through pcs-api testing support rather than the allocateBailiff event'
    );
  }
  await scheduleEvictionAsBailiff(page, context, world);
  await confirmEvictionAsClaimant(page, context, world, true, riskQuestions.vulnerablePeople);
});

Given('the claimant did not change {string}', async ({ world }, question: string) => {
  const unchanged = tabQuestion(question);
  expect(world.changedRiskQuestions ?? [], 'the questions the claimant changed').not.toContain(unchanged);
  world.riskQuestion = unchanged;
});

Then('the {string} for that question is blank', async ({ page, world }, column: string) => {
  expect(column).toBe(claimantRiskAssessmentFields.updatedAnswer);
  expect(world.riskQuestion, 'the question the scenario is about').toBeDefined();
  const row = await claimantRiskAnswerRow(page, world.riskQuestion as string);
  expect(row[claimantRiskAssessmentFields.updatedAnswer]).toBe('');
});

Then('the original answers are dated {string}', async ({ page }, date: string) => {
  approximatePrecondition('HDPI-7552', `the original answers are dated today rather than ${date}`);
  await expect.poll(() => claimantSectionText(page)).toContain(answerDates.originalForInternal(todayTabDate()));
});

Then('the updated answers are dated {string}', async ({ page }, date: string) => {
  approximatePrecondition('HDPI-7552', `the updated answers are dated today rather than ${date}`);
  await expect.poll(() => claimantSectionText(page)).toContain(answerDates.updatedForInternal(todayTabDate()));
});

Then('the important banner is removed from the {string} tab', async ({ page, world }, tabName: string) => {
  expect(tabName).toBe(RISK_ASSESSMENT_TAB);
  await openCaseTab(page, requireCaseReference(world), RISK_ASSESSMENT_TAB);
  expect(Object.keys(await tabSections(page))).toContain(CLAIMANT_RISK_SECTION);
  await expect(page.getByText(UPDATE_BANNER)).toBeHidden();
});

Then('I see the print button, the bailiff notes and the current risk level {string}', async ({ page }, level: string) => {
  await expect(page.getByRole('link', { name: 'Print', exact: true })).toBeVisible();
  const sections = await tabSections(page);
  expect(Object.keys(sections)).toContain('Bailiff note');
  expect(sections['Risk assessment level']?.rows['Risk level']).toBe(level);
});

Then('I see the original and updated answers side by side in a table', async ({ page }) => {
  for (const column of Object.values(claimantRiskAssessmentFields)) {
    await expect(page.locator('.mat-tab-body-active th[scope="col"]', { hasText: column }).first()).toBeVisible();
  }
});

/**
 * The "Important" banner above the internal sections lists every question the claimant changed. The items
 * are not links: XUI picks the tab from the URL fragment, so an in-page link would change tab. OPEN-QUESTIONS
 * 115 proposes a plain list, which is what is checked; the links are recorded as not checked.
 */
Then('I see an important banner with links to each question whose answer changed', async ({ page, world }) => {
  expect(world.changedRiskQuestions?.length, 'risk questions the claimant changed').toBeGreaterThan(0);
  const banner = activeTab(page).locator('.govuk-notification-banner');
  await expect(banner.getByRole('heading', { name: 'Important' })).toBeVisible();
  await expect(banner).toContainText(UPDATE_BANNER);
  const listed = (await banner.getByRole('listitem').allInnerTexts()).map(item => item.trim());
  expect(listed).toEqual(expect.arrayContaining(world.changedRiskQuestions as string[]));
  test.info().annotations.push({
    type: 'open question',
    description: 'OPEN-QUESTIONS 115: the banner lists the changed questions without linking to them',
  });
});
