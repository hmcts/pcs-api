/**
 * Driving a seeded warrant application up to an approved, live warrant.
 *
 * pcs-api's only enforcement testing-support endpoint is `fee-paid`, so every status past "Awaiting review"
 * has to be reached by submitting the real events (HDPI-7419). The bailiff events all require a warrant in
 * one of `BailiffEnforcementRules.APPROVED_LIVE_STATUSES`, so any bailiff scenario starts here.
 *
 * Kept out of both `decide-enforcement.ts` and `enforcement-journeys.ts` because it needs each of them.
 */
import { BrowserContext, Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition } from '../fixtures/pending';
import {
  allocateBailiffViaTestingSupport,
  localIdamUserId,
  localUserName,
  localUsers,
} from '@config/local-stack.config';
import { createCaseWithWarrant, openCaseSummary, requireCaseReference, startEvent } from './enforcement-journeys';
import {
  DECIDE_ENFORCEMENT,
  chooseDecision,
  decideEnforcement,
  expectConfirmation,
  goToCheckYourAnswers,
  recordEnforcementFeePaid,
  signInAsHearingCentreAdmin,
  submitDecision,
} from './decide-enforcement';

/**
 * Records the enforcement fee as paid, signs in as a hearing centre admin and accepts the application.
 * Leaves the warrant in "Awaiting bailiff allocation" with a warrant number, and the browser signed in
 * as the hearing centre admin.
 *
 * Pass `feeOutstanding: false` for an application that is already awaiting review: one whose fee was
 * paid or waived, or a type with no fee.
 */
export async function approveWarrantAsCaseworker(
  page: Page,
  world: EnforcementWorld,
  options: { feeOutstanding?: boolean } = {}
): Promise<void> {
  // The number is generated on approval and is what the later bailiff confirmations quote.
  world.warrantNumber = await approveApplicationAsCaseworker(page, world, 'Warrant', options);
  world.warrantApproved = true;
}

/**
 * Records the fee for the case's application awaiting payment as paid, signs in as a hearing centre admin and
 * accepts it. Returns the warrant or writ number the confirmation screen gives, and leaves the browser signed in
 * as the hearing centre admin.
 *
 * Pass `feeOutstanding: false` for an application that is already awaiting review.
 */
export async function approveApplicationAsCaseworker(
  page: Page,
  world: EnforcementWorld,
  kind: 'Warrant' | 'Writ',
  { feeOutstanding = true }: { feeOutstanding?: boolean } = {}
): Promise<string> {
  if (feeOutstanding) {
    await recordEnforcementFeePaid();
  }
  await signInAsHearingCentreAdmin(page);
  await openCaseSummary(page);
  await startEvent(DECIDE_ENFORCEMENT);
  world.currentEvent = DECIDE_ENFORCEMENT;
  await chooseDecision(page, decideEnforcement.acceptRadioOption);
  await goToCheckYourAnswers(page);
  await submitDecision(page);
  await expectConfirmation(page, new RegExp(`^${kind} of (possession|restitution) approved$`));
  // The number is generated on approval and is what the later bailiff confirmations quote.
  const numberLabel = `${kind} number:`;
  const numberLine = await page.locator('#confirmation-body').getByText(new RegExp(`${numberLabel} \\S+`)).first().innerText();
  const enforcementNumber = numberLine.slice(numberLine.indexOf(numberLabel) + numberLabel.length).trim().split(/\s+/)[0];
  world.warrantNumber = enforcementNumber;
  world.currentEvent = undefined;
  return enforcementNumber;
}

/**
 * Schedules the eviction as the allocated bailiff, which moves the warrant from "Awaiting scheduling" to
 * "Awaiting claimant confirmation" and sets the bailiff date that `markNoticeOfEvictionServed` needs.
 *
 * Answers "No" to dispensing with service unless told otherwise, because a warrant whose service is
 * dispensed with has no notice of eviction to mark as served.
 */
export async function scheduleEvictionAsBailiff(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  daysAhead = 14,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  const { signInAs } = await import('./enforcement-journeys');
  const {
    SCHEDULE_EVICTION,
    dispenseWithServiceRadio,
    enterEvictionDateTime,
    expectCheckYourAnswers,
    scheduleEviction,
  } = await import('./schedule-eviction');

  await signInAs('bailiff', page, context);
  await openCaseSummary(page);
  await startEvent(SCHEDULE_EVICTION);

  const when = new Date();
  when.setDate(when.getDate() + daysAhead);
  const date = `${String(when.getDate()).padStart(2, '0')}/${String(when.getMonth() + 1).padStart(2, '0')}/${when.getFullYear()}`;
  await enterEvictionDateTime(page, date, '10:00');
  await dispenseWithServiceRadio(page, dispenseWithService).check();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectCheckYourAnswers(page);
  await page.getByRole('button', { name: scheduleEviction.saveAndContinue, exact: true }).click();
  await expect(page.getByRole('heading', { name: /Eviction date scheduled for/ }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });

  world.evictionDate = `${date} 10:00`;
  world.evictionScheduled = true;
  world.serviceDispensedWith = dispenseWithService === 'Yes';
}

/**
 * The claimant answers "Can you attend the eviction on this date?" through `confirmEviction`, moving the
 * warrant from "Awaiting claimant confirmation" to "Claimant confirmed" or "Claimant unable to attend".
 *
 * An information page, the question, then the warrant's risk pages pre-filled with the claimant's answers,
 * followed by CCD's check-your-answers. `changeRiskAnswer` names one risk question whose answer the claimant
 * changes to "Yes" on the way through (HDPI-7552); otherwise every risk answer is left as it is.
 */
export async function confirmEvictionAsClaimant(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  canAttend: boolean,
  changeRiskAnswer?: string
): Promise<void> {
  const { CLAIMANT_LEGAL_REPRESENTATIVE } = await import('./session');
  const { signInAs } = await import('./enforcement-journeys');
  const claimantRiskAssessment = await import('./claimant-risk-assessment');

  await signInAs(CLAIMANT_LEGAL_REPRESENTATIVE, page, context);
  await openCaseSummary(page);
  await startEvent(claimantRiskAssessment.CONFIRM_EVICTION);

  await claimantRiskAssessment.answerAttendance(page, canAttend);
  if (changeRiskAnswer) {
    await claimantRiskAssessment.changeRiskAnswer(page, changeRiskAnswer, 'Yes');
    world.changedRiskQuestions = [
      ...(world.changedRiskQuestions ?? []),
      claimantRiskAssessment.tabQuestion(changeRiskAnswer),
    ];
  }
  await claimantRiskAssessment.continueToCheckYourAnswers(page);
  await claimantRiskAssessment.submitConfirmEviction(page);

  world.claimantConfirmedEviction = canAttend;
}

/** Signs back in as the claimant's legal representative, the session every scenario starts from. */
export async function restoreClaimantSession(page: Page, context: BrowserContext): Promise<void> {
  const { CLAIMANT_LEGAL_REPRESENTATIVE } = await import('./session');
  const { signInAs } = await import('./enforcement-journeys');
  await signInAs(CLAIMANT_LEGAL_REPRESENTATIVE, page, context);
}

/** The statuses a scenario can actually set up, in the order the events reach them. */
export const AWAITING_PAYMENT = 'Awaiting payment';
export const AWAITING_REVIEW = 'Awaiting review';
export const AWAITING_BAILIFF_ALLOCATION = 'Awaiting bailiff allocation';
export const AWAITING_SCHEDULING = 'Awaiting scheduling';
export const AWAITING_CLAIMANT_CONFIRMATION = 'Awaiting claimant confirmation';
export const CLAIMANT_CONFIRMED = 'Claimant confirmed';
export const CLAIMANT_UNABLE_TO_ATTEND = 'Claimant unable to attend';

/** Feature-file spellings of a status that differ from the EnforcementStatus label. */
const STATUS_ALIASES: Record<string, string> = {
  'Claimant unable to attend on this date': CLAIMANT_UNABLE_TO_ATTEND,
};

export const BREATHING_SPACE = 'Breathing space';
export const CLOSED = 'Closed';

const SETTABLE_STATUSES = [
  AWAITING_PAYMENT,
  AWAITING_REVIEW,
  AWAITING_BAILIFF_ALLOCATION,
  AWAITING_SCHEDULING,
  AWAITING_CLAIMANT_CONFIRMATION,
  CLAIMANT_CONFIRMED,
  CLAIMANT_UNABLE_TO_ATTEND,
  BREATHING_SPACE,
  CLOSED,
];

/** Whether `setUpWarrantAtStatus` reaches `status` itself, rather than approximating it. */
export const canSetUpStatus = (status: string): boolean => SETTABLE_STATUSES.includes(STATUS_ALIASES[status] ?? status);

/**
 * The warrant returns that move a warrant to "Closed" and to "Breathing space". A warrant can take a return
 * from "Awaiting scheduling" onwards.
 */
const RETURNS_REACHING: Record<string, string> = {
  [CLOSED]: 'Eviction successful',
  [BREATHING_SPACE]: 'Claimant advised Defendant in Breathing Space',
};

/** Statuses a warrant can take a return in, and so be moved on to "Closed" or "Breathing space". */
const RETURN_RECORDABLE_STATUSES = [
  AWAITING_SCHEDULING,
  AWAITING_CLAIMANT_CONFIRMATION,
  CLAIMANT_CONFIRMED,
  CLAIMANT_UNABLE_TO_ATTEND,
  BREATHING_SPACE,
];

async function recordReturnReaching(page: Page, context: BrowserContext, status: string): Promise<void> {
  const { recordReturnAsHearingCentreAdmin } = await import('./repeat-enforcement');
  await recordReturnAsHearingCentreAdmin(page, context, RETURNS_REACHING[status]);
}

/**
 * Moves the scenario's warrant on to `status` where a real event can, and otherwise sets up a new case with a
 * warrant in that status. Keeps the user who was signed in. For a "Given the enforcement status is ..." that
 * follows the Background's set-up.
 */
export async function bringWarrantToStatus(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  status: string
): Promise<void> {
  const { signInAs } = await import('./enforcement-journeys');
  const { currentSignedInRole } = await import('./session');
  const { currentEnforcement, expectEnforcementStatus } = await import('./enforcement-status-tab');
  const target = STATUS_ALIASES[status] ?? status;
  const signedIn = currentSignedInRole();
  const current = world.caseReference
    ? (await currentEnforcement(world.caseReference, world.earlierWarrantNumbers.length)).status
    : undefined;
  if (current === target) {
    return;
  }
  if (current && RETURNS_REACHING[target] && RETURN_RECORDABLE_STATUSES.includes(current)) {
    try {
      await recordReturnReaching(page, context, target);
    } finally {
      await restoreClaimantSession(page, context);
    }
  } else {
    world.allocatedBailiff = undefined;
    world.evictionScheduled = false;
    await setUpWarrantAtStatus(page, context, world, target);
  }
  await expectEnforcementStatus(page, requireCaseReference(world), target, world.earlierWarrantNumbers.length);
  await signInAs(signedIn, page, context);
}

/**
 * Allocates the approved warrant to a bailiff through pcs-api testing support, which moves it to
 * "Awaiting scheduling" and unblocks the schedule / reschedule / notice-served / warrant-return journeys.
 *
 * A shortcut for scenarios that only need the warrant allocated: feature 03 drives the `allocateBailiff`
 * event itself. This goes through `BailiffAllocationService` instead, for the same outcome.
 */
export async function allocateBailiff(world: EnforcementWorld, email: string = localUsers.bailiff): Promise<void> {
  const userId = await localIdamUserId(email);
  await allocateBailiffViaTestingSupport(requireCaseReference(world), {
    userId,
    name: localUserName(email),
    email,
  });
  world.allocatedBailiff = email;
}

/**
 * Sets up a case with a warrant in `status`, or as close to it as the built events allow.
 *
 * Statuses past "Awaiting review" are reached by submitting the real events, which means signing in as a
 * hearing centre admin and then a bailiff. "Closed" and "Breathing space" are reached from "Awaiting
 * scheduling" by recording a warrant return. The session is always handed back to the claimant's legal
 * representative on the way out, because that is the user every scenario starts as.
 *
 * "Awaiting property visit", "Rejected" and "Transferred to the High Court" have no route yet; for those
 * the warrant is left as far along as it got and the gap is recorded as an approximated precondition, which
 * keeps the scenario running for the assertions that do not depend on the exact status.
 */
export async function setUpWarrantAtStatus(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  status: string
): Promise<void> {
  try {
    await driveWarrantToStatus(page, context, world, status);
  } finally {
    await restoreClaimantSession(page, context);
  }
}

async function driveWarrantToStatus(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  status: string
): Promise<void> {
  const target = STATUS_ALIASES[status] ?? status;
  await createCaseWithWarrant(world);

  if (target === AWAITING_PAYMENT) {
    return;
  }
  if (target === AWAITING_REVIEW) {
    await recordEnforcementFeePaid();
    return;
  }

  await approveWarrantAsCaseworker(page, world);
  if (target === AWAITING_BAILIFF_ALLOCATION) {
    return;
  }

  // Everything past here needs a bailiff allocated. The event cannot be driven (its staff type-ahead needs
  // reference data cftlib does not run), so the allocation goes through pcs-api testing support, which
  // calls the same service the event does.
  await allocateBailiff(world);
  approximatePrecondition(
    'HDPI-7356',
    'the bailiff was allocated through pcs-api testing support rather than the allocateBailiff event, whose '
      + 'staff type-ahead needs reference data cftlib does not run'
  );
  if (target === AWAITING_SCHEDULING) {
    return;
  }
  if (target === CLOSED || target === BREATHING_SPACE) {
    await recordReturnReaching(page, context, target);
    return;
  }

  await scheduleEvictionAsBailiff(page, context, world);
  if (target === AWAITING_CLAIMANT_CONFIRMATION) {
    return;
  }

  if (target === CLAIMANT_CONFIRMED || target === CLAIMANT_UNABLE_TO_ATTEND) {
    await confirmEvictionAsClaimant(page, context, world, target === CLAIMANT_CONFIRMED);
    return;
  }

  approximatePrecondition(
    'HDPI-7419',
    `"${status}" cannot be set up: no event or testing-support route reaches it. The warrant is in `
      + '"Awaiting claimant confirmation".'
  );
}

/**
 * Schedules the eviction on a case whose warrant has already been approved, allocating the bailiff first if
 * no one has been, and hands the session back to the claimant's legal representative.
 */
export async function scheduleEvictionOnApprovedWarrant(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  expect(world.warrantApproved, 'an approved warrant to schedule the eviction on').toBe(true);
  try {
    if (!world.allocatedBailiff) {
      await allocateBailiff(world);
      approximatePrecondition('HDPI-7356', 'the bailiff was allocated through pcs-api testing support');
    }
    await scheduleEvictionAsBailiff(page, context, world, 14, dispenseWithService);
  } finally {
    await restoreClaimantSession(page, context);
  }
  world.defendantViews = undefined;
}
