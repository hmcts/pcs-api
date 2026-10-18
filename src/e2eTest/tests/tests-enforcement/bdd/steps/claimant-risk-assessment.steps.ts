// The claimant updates the risk assessment in "Confirm the eviction details" and the bailiff reviews it on the
// "Risk assessment" tab (feature 09, and the risk assessment scenarios of feature 06). HDPI-7552.
//
// The dates in the feature files cannot be set up: the original answers are the seeded application's, given
// today, and the update is made today. The steps assert today's date and record the gap as an approximated
// precondition. The "Review updated risk information" task is not created (HDPI-8627).
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import { openCaseSummary, requireCaseReference, signInAs, startEvent } from '../support/enforcement-journeys';
import { CLAIMANT_LEGAL_REPRESENTATIVE } from '../support/session';
import { RISK_ASSESSMENT_TAB, tabFields } from '../support/case-tabs';
import { openCaseTab } from '../support/enforcement-status-tab';
import { SET_RISK_LEVEL, chooseRiskLevel, continueButton, expectBailiffConfirmation } from '../support/bailiff-events';
import { allocateBailiff, confirmEvictionAsClaimant, scheduleEvictionAsBailiff } from '../support/approve-warrant';
import {
  CONFIRM_EVICTION,
  answerAttendance,
  changeRiskAnswer,
  claimantRiskAnswerRow,
  claimantRiskAssessmentFields,
  continueToCheckYourAnswers,
  expectConfirmEvictionSubmitted,
  riskQuestion,
  riskQuestions,
  todayAsTabDate,
} from '../support/claimant-risk-assessment';

/** Placeholder banner text pcs-api shows while an update is waiting for the bailiff's review. */
const UPDATE_BANNER = 'The claimant has updated their risk assessment.';

function inConfirmEviction(currentEvent: string | undefined): boolean {
  return currentEvent === CONFIRM_EVICTION;
}

// ---------------------------------------------------------------------------
// Feature 06: the claimant changes (or keeps) their answers while confirming
// ---------------------------------------------------------------------------

When('I change my answer to {string} to {string}', async ({ page, world, $tags }, question: string, answer: string) => {
  if (!inConfirmEviction(world.currentEvent)) {
    return markPending({ summary: `changing a risk answer outside "${CONFIRM_EVICTION}"` }, $tags);
  }
  const tabQuestion = riskQuestion(question);
  await answerAttendance(page, true);
  await changeRiskAnswer(page, tabQuestion, answer);
  await continueToCheckYourAnswers(page);
  world.riskQuestion = tabQuestion;
  world.changedRiskAnswer = answer;
  world.changedRiskQuestions = [...(world.changedRiskQuestions ?? []), tabQuestion];
});

When('I do not change any risk assessment answers', async ({ page, world, $tags }) => {
  if (!inConfirmEviction(world.currentEvent)) {
    return markPending({ summary: `going through the risk answers outside "${CONFIRM_EVICTION}"` }, $tags);
  }
  await answerAttendance(page, true);
  await continueToCheckYourAnswers(page);
});

// The claimant cannot see the "Risk assessment" tab (OPEN-QUESTIONS 65), so the bailiff reads it.
Then('the updated answer is stored alongside my original answer with today\'s date', async ({ page, context, world }) => {
  expect(world.riskQuestion, 'a risk answer changed by this scenario').toBeDefined();
  await expectConfirmEvictionSubmitted(page);
  await signInAs('bailiff', page, context);
  await openCaseTab(page, requireCaseReference(world), RISK_ASSESSMENT_TAB);

  const row = await claimantRiskAnswerRow(page, world.riskQuestion as string);
  expect(row[claimantRiskAssessmentFields.updatedAnswer]).toBe(world.changedRiskAnswer);
  expect(row[claimantRiskAssessmentFields.originalAnswer]).not.toBe(world.changedRiskAnswer);
  expect((await tabFields(page))[claimantRiskAssessmentFields.updatedOn]).toBe(todayAsTabDate());

  await signInAs(CLAIMANT_LEGAL_REPRESENTATIVE, page, context);
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
  const tabQuestion = riskQuestion(question);
  expect(world.changedRiskQuestions ?? [], 'the questions the claimant changed').not.toContain(tabQuestion);
  world.riskQuestion = tabQuestion;
});

Then('the {string} for that question is blank', async ({ page, world }, column: string) => {
  expect(column).toBe(claimantRiskAssessmentFields.updatedAnswer);
  expect(world.riskQuestion, 'the question the scenario is about').toBeDefined();
  const row = await claimantRiskAnswerRow(page, world.riskQuestion as string);
  expect(row[claimantRiskAssessmentFields.updatedAnswer] ?? '').toBe('');
});

Then('the original answers are dated {string}', async ({ page }, date: string) => {
  approximatePrecondition('HDPI-7552', `the original answers are dated today rather than ${date}`);
  await expect
    .poll(async () => (await tabFields(page))[claimantRiskAssessmentFields.givenOn])
    .toBe(todayAsTabDate());
});

Then('the updated answers are dated {string}', async ({ page }, date: string) => {
  approximatePrecondition('HDPI-7552', `the updated answers are dated today rather than ${date}`);
  await expect
    .poll(async () => (await tabFields(page))[claimantRiskAssessmentFields.updatedOn])
    .toBe(todayAsTabDate());
});

Then('the important banner is removed from the {string} tab', async ({ page, world }, tabName: string) => {
  expect(tabName).toBe(RISK_ASSESSMENT_TAB);
  await openCaseTab(page, requireCaseReference(world), RISK_ASSESSMENT_TAB);
  await expect.poll(async () => Object.keys(await tabFields(page)).length).toBeGreaterThan(0);
  await expect(page.getByText(UPDATE_BANNER)).toBeHidden();
});
