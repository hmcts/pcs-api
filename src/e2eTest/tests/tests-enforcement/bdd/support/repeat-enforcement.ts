/**
 * Further enforcement on the same case (features 01 and 15, HDPI-8424).
 *
 * A closed warrant is reached by driving the real journey: a seeded application, "Decide enforcement", the
 * allocate-bailiff testing-support shortcut and then a final return through "Record warrant return", which
 * a hearing centre admin can submit. Nothing else puts a warrant into "Closed".
 */
import { BrowserContext, Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import {
  AWAITING_SCHEDULING,
  allocateBailiff,
  approveWarrantAsCaseworker,
  restoreClaimantSession,
  setUpWarrantAtStatus,
} from './approve-warrant';
import {
  answerApplicationQuestions,
  chooseApplicationType,
  openCaseSummary,
  requireCaseReference,
  signInAs,
  signStatementOfTruth,
  startEvent,
  submitApplication,
} from './enforcement-journeys';
import { EnforcementRow, enforcementRows } from './enforcement-status-tab';
import { RECORD_WARRANT_RETURN, recordWarrantReturn } from './warrant-return';

/**
 * Puts a closed warrant of possession on a new case: approved, allocated, then closed by a final return
 * named `returnName`. Records the warrant's number on the world as an earlier enforcement and hands the
 * session back to the claimant's legal representative.
 */
export async function closeWarrantWithReturn(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  returnName: string
): Promise<void> {
  await setUpWarrantAtStatus(page, context, world, AWAITING_SCHEDULING);
  await closeApprovedWarrant(page, context, world, returnName);
}

/**
 * Closes the case's approved warrant with a final return named `returnName`. "Record warrant return" needs a
 * warrant in "Awaiting scheduling" or later, so a warrant nobody has been allocated is first allocated to the
 * local bailiff through testing support. Records the warrant's number on the world as an earlier enforcement
 * and hands the session back to the claimant's legal representative.
 */
export async function closeApprovedWarrant(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  returnName: string
): Promise<void> {
  expect(world.warrantApproved, 'an approved warrant to close').toBe(true);
  try {
    if (!world.allocatedBailiff) {
      await allocateBailiff(world);
    }
    await recordFinalReturn(page, context, returnName);
  } finally {
    await restoreClaimantSession(page, context);
  }
  expect(world.warrantNumber, 'a warrant number generated when the first warrant was approved').toBeDefined();
  world.earlierWarrantNumbers.push(world.warrantNumber as string);
  world.warrantNumber = undefined;
  world.warrantApproved = false;
  world.allocatedBailiff = undefined;
}

/** Records a final return as a hearing centre admin, one of the bailiff staff "Record warrant return" allows. */
async function recordFinalReturn(page: Page, context: BrowserContext, returnName: string): Promise<void> {
  await signInAs('hearing-centre-admin', page, context);
  await openCaseSummary(page);
  await startEvent(RECORD_WARRANT_RETURN);
  await recordWarrantReturn(page, returnName);
}

/**
 * The claimant applies for a further warrant of possession through "Enforce the order". Records on the
 * world whether pcs-api's confirmation asked for the fee.
 */
export async function applyForFurtherWarrant(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType('warrant of possession', world, page);
  await answerApplicationQuestions('warrant of possession', world);
  await signStatementOfTruth('warrant of possession');
  world.askedToPayFee = await submitApplication(page);
}

/**
 * The claimant applies for a warrant of restitution through "Enforce the order", which pcs-api only offers once
 * the case has had a warrant. The journey ends on the "Anything else that could help with the eviction"
 * placeholder, with no statement of truth (OPEN-QUESTIONS 30), so it goes from there to check your answers.
 * Restitution has no fee, so the application starts in "Awaiting review" (OPEN-QUESTIONS 60).
 */
export async function applyForWarrantOfRestitution(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType('warrant of restitution', world, page);
  await answerApplicationQuestions('warrant of restitution', world);
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Submit application', exact: true }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  world.askedToPayFee = await submitApplication(page);
}

/**
 * Puts a live, approved warrant of restitution on the case: the case's approved warrant, if it has one, is
 * closed with a successful eviction, then the claimant applies for restitution and a hearing centre admin
 * approves it. Leaves the restitution's number on the world as the current warrant and the browser signed in
 * as the claimant's legal representative.
 */
export async function generateWarrantOfRestitution(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld
): Promise<string> {
  if (world.warrantApproved) {
    await closeApprovedWarrant(page, context, world, 'Eviction successful');
  }
  await restoreClaimantSession(page, context);
  await applyForWarrantOfRestitution(page, world);
  try {
    await approveWarrantAsCaseworker(page, world, { feeOutstanding: false });
  } finally {
    await restoreClaimantSession(page, context);
  }
  return world.warrantNumber as string;
}

/** Every enforcement on the case as the case data lists them, current first. */
export async function enforcementsOnTheCase(world: EnforcementWorld): Promise<EnforcementRow[]> {
  const caseReference = requireCaseReference(world);
  const expected = world.earlierWarrantNumbers.length + 1;
  await expect
    .poll(async () => (await enforcementRows(caseReference)).length, { timeout: VERY_LONG_TIMEOUT })
    .toBe(expected);
  return enforcementRows(caseReference);
}
