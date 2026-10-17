/**
 * Driving a seeded warrant application up to an approved, live warrant.
 *
 * pcs-api's only enforcement testing-support endpoint is `fee-paid`, so every status past "Awaiting review"
 * has to be reached by submitting the real events (HDPI-7419). The bailiff events all require a warrant in
 * one of `BailiffEnforcementRules.APPROVED_LIVE_STATUSES`, so any bailiff scenario starts here.
 *
 * Kept out of both `decide-enforcement.ts` and `enforcement-journeys.ts` because it needs each of them.
 */
import { Page } from '@playwright/test';
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
 */
export async function approveWarrantAsCaseworker(page: Page, world: EnforcementWorld): Promise<void> {
  await recordEnforcementFeePaid();
  await signInAsHearingCentreAdmin(page);
  await openCaseSummary(page);
  await startEvent(DECIDE_ENFORCEMENT);
  world.currentEvent = DECIDE_ENFORCEMENT;
  await chooseDecision(page, decideEnforcement.acceptRadioOption);
  await goToCheckYourAnswers(page);
  await submitDecision(page);
  await expectConfirmation(page, 'Warrant approved');
  world.currentEvent = undefined;
  world.warrantApproved = true;
}

/** The statuses a scenario can actually set up, in the order the events reach them. */
export const AWAITING_PAYMENT = 'Awaiting payment';
export const AWAITING_REVIEW = 'Awaiting review';
export const AWAITING_BAILIFF_ALLOCATION = 'Awaiting bailiff allocation';
export const AWAITING_SCHEDULING = 'Awaiting scheduling';

const SETTABLE_STATUSES = [AWAITING_PAYMENT, AWAITING_REVIEW, AWAITING_BAILIFF_ALLOCATION, AWAITING_SCHEDULING];

/**
 * Allocates the approved warrant to a bailiff through pcs-api testing support, which moves it to
 * "Awaiting scheduling" and unblocks the schedule / reschedule / notice-served / warrant-return journeys.
 *
 * The `allocateBailiff` event cannot be driven outside AAT (its StaffUser type-ahead needs staff reference
 * data), so this goes through `BailiffAllocationService` instead, for the same outcome.
 */
export async function allocateBailiff(world: EnforcementWorld, email = localUsers.bailiff): Promise<void> {
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
 * "Awaiting scheduling" and everything after it need a bailiff to be allocated, and `allocateBailiff`'s
 * StaffUser type-ahead needs staff reference data that cftlib does not run, so those statuses cannot be
 * reached on a local stack. For them the warrant is left in "Awaiting bailiff allocation" and the gap is
 * recorded as an approximated precondition, which keeps the scenario running for the assertions that do
 * not depend on the exact status.
 */
export async function setUpWarrantAtStatus(page: Page, world: EnforcementWorld, status: string): Promise<void> {
  await createCaseWithWarrant(world);

  if (status === AWAITING_PAYMENT) {
    return;
  }
  if (status === AWAITING_REVIEW) {
    await recordEnforcementFeePaid();
    return;
  }

  await approveWarrantAsCaseworker(page, world);
  if (status === AWAITING_BAILIFF_ALLOCATION) {
    return;
  }

  // Everything past here needs a bailiff allocated. The event cannot be driven (staff reference data), so
  // the allocation goes through testing support, which uses the same service the event does.
  await allocateBailiff(world);
  approximatePrecondition(
    'HDPI-7356',
    'the bailiff was allocated through pcs-api testing support rather than the allocateBailiff event, whose '
      + 'staff type-ahead needs reference data cftlib does not run'
  );

  if (!SETTABLE_STATUSES.includes(status)) {
    approximatePrecondition(
      'HDPI-7419',
      `"${status}" cannot be set up: no event or testing-support route reaches it. The warrant is in `
        + '"Awaiting scheduling".'
    );
  }
}
