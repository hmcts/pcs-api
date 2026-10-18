/**
 * Driving a seeded warrant application up to an approved, live warrant, and on through the bailiff events.
 *
 * pcs-api's enforcement testing support only records the fee and allocates a bailiff, so every other status is
 * reached by submitting the real events (HDPI-7419). Set-up submits them through CCD's data store API as the
 * user who would submit them in XUI (event-api.ts), so the browser is left alone. Each helper then expects that
 * user next, as the XUI journey it replaces left them signed in; the browser only signs them in if a later step
 * acts in XUI without naming its user (session.ts). The XUI journeys themselves, for scenarios whose When is the
 * journey, are in the other support files.
 *
 * Kept out of both `decide-enforcement.ts` and `enforcement-journeys.ts` because it needs each of them.
 */
import { Page, expect } from '@playwright/test';
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
import { recordEnforcementFeePaid } from './decide-enforcement';
import { ccdDateTime, confirmationValue, slashedDate, submitEventAs } from './event-api';
import {
  CLAIMANT_LEGAL_REPRESENTATIVE,
  expectSignedInAs,
  expectedSignedInRole,
  requireEmailFor,
  signInAs,
} from './session';
import { RISK_ANSWER_FIELDS, riskAnswerChange, tabQuestion } from './claimant-risk-assessment';
import { recordWarrantReturnAs } from './warrant-return';

const HEARING_CENTRE_ADMIN = 'hearing-centre-admin';
const BAILIFF = 'bailiff';

/**
 * Records the enforcement fee as paid and has a hearing centre admin accept the application. Leaves the warrant
 * in "Awaiting bailiff allocation" with a warrant number.
 *
 * Pass `feeOutstanding: false` for an application that is already awaiting review: one whose fee was
 * paid or waived, or a type with no fee.
 */
export async function approveWarrantAsCaseworker(
  world: EnforcementWorld,
  options: { feeOutstanding?: boolean } = {}
): Promise<void> {
  // The number is generated on approval and is what the later bailiff confirmations quote.
  world.warrantNumber = await approveApplicationAsCaseworker(world, 'Warrant', options);
  world.warrantApproved = true;
}

/**
 * Records the fee for the case's application awaiting payment as paid, and has a hearing centre admin accept it
 * through "Decide enforcement". Returns the warrant or writ number pcs-api's confirmation gives.
 *
 * Pass `feeOutstanding: false` for an application that is already awaiting review.
 */
export async function approveApplicationAsCaseworker(
  world: EnforcementWorld,
  kind: 'Warrant' | 'Writ',
  { feeOutstanding = true }: { feeOutstanding?: boolean } = {}
): Promise<string> {
  if (feeOutstanding) {
    await recordEnforcementFeePaid();
  }
  const confirmation = await submitEventAs(requireEmailFor(HEARING_CENTRE_ADMIN), 'decideEnforcement', {
    enforcementDecision: 'ACCEPT',
  });
  expect(confirmation.body, 'the "Decide enforcement" confirmation').toContain(`${kind} approved`);
  const enforcementNumber = confirmationValue(confirmation, `${kind} number:`);
  world.warrantNumber = enforcementNumber;
  world.currentEvent = undefined;
  expectSignedInAs(HEARING_CENTRE_ADMIN);
  return enforcementNumber;
}

/**
 * Has the bailiff schedule the eviction `daysAhead` days from today at 10:00, which moves the warrant from
 * "Awaiting scheduling" to "Awaiting claimant confirmation" and sets the bailiff date that marking the notice
 * served needs.
 *
 * Answers "No" to dispensing with service unless told otherwise, because a warrant whose service is
 * dispensed with has no notice of eviction to mark as served.
 */
export async function scheduleEvictionAsBailiff(
  world: EnforcementWorld,
  daysAhead = 14,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  await submitEventAs(requireEmailFor(BAILIFF), 'scheduleEviction', {
    evictionDateTime: ccdDateTime(daysAhead, '10:00'),
    evictionDispenseWithService: dispenseWithService,
  });
  recordEvictionScheduled(world, slashedDate(daysAhead), dispenseWithService);
  expectSignedInAs(BAILIFF);
}

/**
 * The bailiff's "Schedule eviction" journey in XUI, for a scenario whose When is the submit itself: the date
 * `daysAhead` days from today at 10:00, then check your answers and "Save and continue".
 */
export async function scheduleEvictionInXui(
  page: Page,
  world: EnforcementWorld,
  daysAhead = 14,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  const {
    SCHEDULE_EVICTION,
    dispenseWithServiceRadio,
    enterEvictionDateTime,
    expectCheckYourAnswers,
    scheduleEviction,
  } = await import('./schedule-eviction');

  await signInAs(BAILIFF, page);
  await openCaseSummary(page);
  await startEvent(SCHEDULE_EVICTION);

  const date = slashedDate(daysAhead);
  await enterEvictionDateTime(page, date, '10:00');
  await dispenseWithServiceRadio(page, dispenseWithService).check();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectCheckYourAnswers(page);
  await page.getByRole('button', { name: scheduleEviction.saveAndContinue, exact: true }).click();
  await expect(page.getByRole('heading', { name: /Eviction date scheduled for/ }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  recordEvictionScheduled(world, date, dispenseWithService);
}

function recordEvictionScheduled(world: EnforcementWorld, date: string, dispenseWithService: 'Yes' | 'No'): void {
  world.evictionDate = `${date} 10:00`;
  world.evictionScheduled = true;
  world.serviceDispensedWith = dispenseWithService === 'Yes';
}

/**
 * Has the bailiff reschedule the eviction `daysAhead` days from today at 10:00 through "Reschedule eviction",
 * which is offered once the claimant has said whether they can attend.
 */
export async function rescheduleEvictionAsBailiff(
  world: EnforcementWorld,
  daysAhead: number,
  dispenseWithService: 'Yes' | 'No'
): Promise<void> {
  await submitEventAs(requireEmailFor(BAILIFF), 'rescheduleEviction', {
    evictionDateTime: ccdDateTime(daysAhead, '10:00'),
    evictionDispenseWithService: dispenseWithService,
  });
  recordEvictionScheduled(world, slashedDate(daysAhead), dispenseWithService);
  expectSignedInAs(BAILIFF);
}

/**
 * The claimant answers "Can you attend the eviction on this date?" through "Confirm the eviction details", moving
 * the warrant from "Awaiting claimant confirmation" to "Claimant confirmed" or "Claimant unable to attend".
 *
 * The event pre-fills the warrant's risk answers. They go back as they are, except `changeRiskAnswer`: one risk
 * question whose answer the claimant changes to "Yes", with its follow-up answers (HDPI-7552), as the XUI journey
 * in claimant-risk-assessment.ts does.
 */
export async function confirmEvictionAsClaimant(
  world: EnforcementWorld,
  canAttend: boolean,
  changeRiskAnswer?: string
): Promise<void> {
  await submitEventAs(requireEmailFor(CLAIMANT_LEGAL_REPRESENTATIVE), 'confirmEviction', startData => {
    const prefilled: Record<string, unknown> = {};
    for (const field of RISK_ANSWER_FIELDS) {
      if (startData[field] !== undefined && startData[field] !== null) {
        prefilled[field] = startData[field];
      }
    }
    return {
      ...prefilled,
      ...(changeRiskAnswer ? riskAnswerChange(changeRiskAnswer) : {}),
      claimantCanAttendEviction: canAttend ? 'Yes' : 'No',
    };
  });
  if (changeRiskAnswer) {
    world.changedRiskQuestions = [...(world.changedRiskQuestions ?? []), tabQuestion(changeRiskAnswer)];
  }
  world.claimantConfirmedEviction = canAttend;
  expectSignedInAs(CLAIMANT_LEGAL_REPRESENTATIVE);
}

/**
 * Expects the claimant's legal representative next, the user every scenario starts as. Signs nobody in: the next
 * step that acts in XUI without naming its user does, if the browser is someone else by then.
 */
export function expectClaimantNext(): void {
  expectSignedInAs(CLAIMANT_LEGAL_REPRESENTATIVE);
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

/** A hearing centre admin, one of the bailiff staff "Record warrant return" allows, records the return. */
export async function recordReturnAsHearingCentreAdmin(returnName: string): Promise<string> {
  const label = await recordWarrantReturnAs(requireEmailFor(HEARING_CENTRE_ADMIN), returnName);
  expectSignedInAs(HEARING_CENTRE_ADMIN);
  return label;
}

/**
 * Moves the scenario's warrant on to `status` where a real event can, and otherwise sets up a new case with a
 * warrant in that status. Leaves the user the scenario expected next as they were. For a "Given the enforcement
 * status is ..." that follows the Background's set-up.
 */
export async function bringWarrantToStatus(world: EnforcementWorld, status: string): Promise<void> {
  const { currentEnforcement, expectEnforcementStatus } = await import('./enforcement-status-tab');
  const target = STATUS_ALIASES[status] ?? status;
  const expected = expectedSignedInRole();
  const current = world.caseReference
    ? (await currentEnforcement(world.caseReference, world.earlierWarrantNumbers.length)).status
    : undefined;
  if (current === target) {
    return;
  }
  if (current && RETURNS_REACHING[target] && RETURN_RECORDABLE_STATUSES.includes(current)) {
    await recordReturnAsHearingCentreAdmin(RETURNS_REACHING[target]);
  } else {
    world.allocatedBailiff = undefined;
    world.evictionScheduled = false;
    await setUpWarrantAtStatus(world, target);
  }
  await expectEnforcementStatus(undefined, requireCaseReference(world), target, world.earlierWarrantNumbers.length);
  expectSignedInAs(expected);
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
 * Statuses past "Awaiting review" are reached by submitting the real events as a hearing centre admin and then
 * a bailiff. "Closed" and "Breathing space" are reached from "Awaiting scheduling" by recording a warrant return.
 * The claimant's legal representative is expected next, because that is the user every scenario starts as.
 *
 * "Awaiting property visit", "Rejected" and "Transferred to the High Court" have no route yet; for those
 * the warrant is left as far along as it got and the gap is recorded as an approximated precondition, which
 * keeps the scenario running for the assertions that do not depend on the exact status.
 */
export async function setUpWarrantAtStatus(world: EnforcementWorld, status: string): Promise<void> {
  try {
    await driveWarrantToStatus(world, status);
  } finally {
    expectClaimantNext();
  }
}

async function driveWarrantToStatus(world: EnforcementWorld, status: string): Promise<void> {
  const target = STATUS_ALIASES[status] ?? status;
  await createCaseWithWarrant(world);

  if (target === AWAITING_PAYMENT) {
    return;
  }
  if (target === AWAITING_REVIEW) {
    await recordEnforcementFeePaid();
    return;
  }

  await approveWarrantAsCaseworker(world);
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
    await recordReturnAsHearingCentreAdmin(RETURNS_REACHING[target]);
    return;
  }

  await scheduleEvictionAsBailiff(world);
  if (target === AWAITING_CLAIMANT_CONFIRMATION) {
    return;
  }

  if (target === CLAIMANT_CONFIRMED || target === CLAIMANT_UNABLE_TO_ATTEND) {
    await confirmEvictionAsClaimant(world, target === CLAIMANT_CONFIRMED);
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
 * no one has been, and expects the claimant's legal representative next.
 */
export async function scheduleEvictionOnApprovedWarrant(
  world: EnforcementWorld,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  expect(world.warrantApproved, 'an approved warrant to schedule the eviction on').toBe(true);
  try {
    if (!world.allocatedBailiff) {
      await allocateBailiff(world);
      approximatePrecondition('HDPI-7356', 'the bailiff was allocated through pcs-api testing support');
    }
    await scheduleEvictionAsBailiff(world, 14, dispenseWithService);
  } finally {
    expectClaimantNext();
  }
  world.defendantViews = undefined;
}
