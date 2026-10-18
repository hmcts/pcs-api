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
import { AWAITING_SCHEDULING, restoreClaimantSession, setUpWarrantAtStatus } from './approve-warrant';
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
  try {
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

/** Every enforcement on the case as the case data lists them, current first. */
export async function enforcementsOnTheCase(world: EnforcementWorld): Promise<EnforcementRow[]> {
  const caseReference = requireCaseReference(world);
  const expected = world.earlierWarrantNumbers.length + 1;
  await expect
    .poll(async () => (await enforcementRows(caseReference)).length, { timeout: VERY_LONG_TIMEOUT })
    .toBe(expected);
  return enforcementRows(caseReference);
}
