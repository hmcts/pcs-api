/**
 * Further enforcement on the same case (features 01 and 15, HDPI-8424).
 *
 * A closed warrant is reached by driving the real journey: a seeded application, "Decide enforcement", the
 * allocate-bailiff testing-support shortcut and then a final return through "Record warrant return", which
 * a hearing centre admin can submit. Nothing else puts a warrant into "Closed".
 */
import { BrowserContext, Page, expect } from '@playwright/test';
import { LONG_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';
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
import {
  RECORD_WARRANT_RETURN,
  chooseOutcome,
  chooseReturnType,
  visibleReturnNameDropDown,
  warrantReturn,
} from './warrant-return';

/** The panel heading pcs-api's submit confirmation shows when the application still owes its fee. */
export const PAY_PANEL_TITLE = /Pay .+ for HM Courts and Tribunals Service/;

/** Final return names that record a successful eviction; every other closing name is "Eviction unsuccessful". */
const SUCCESSFUL_RETURN_NAMES = ['eviction successful'];

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

  const successful = SUCCESSFUL_RETURN_NAMES.includes(returnName.toLowerCase());
  await chooseReturnType(page, warrantReturn.final);
  await chooseOutcome(page, successful ? warrantReturn.evictionSuccessful : warrantReturn.evictionUnsuccessful);
  await chooseReturnNameIgnoringCase(page, returnName);
  if (successful) {
    await enterEvictionDateTime(page, yesterday());
  }
  // Optional for most names and mandatory for some; always giving it keeps one route for all of them.
  await page.getByLabel(warrantReturn.additionalDetailsLabel).fill('Recorded by the enforcement BDD suite');

  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await page.getByRole('button', { name: 'Save and continue', exact: true }).click();
  await expect(page.getByRole('heading', { name: /Outcome recorded as/ })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
}

/**
 * The feature files write return names in sentence case ("Warrant suspended by order"); pcs-api's labels
 * keep the capitals of the HDPI-8424 list ("Warrant suspended by Order"). Matches the option ignoring case.
 */
async function chooseReturnNameIgnoringCase(page: Page, returnName: string): Promise<void> {
  const field = await visibleReturnNameDropDown(page);
  const labels = (await field.locator('option').allTextContents()).map(label => label.trim());
  const label = labels.find(option => option.toLowerCase() === returnName.toLowerCase());
  if (!label) {
    throw new Error(`No return name "${returnName}" offered. Offered: ${labels.join(', ')}`);
  }
  await field.selectOption({ label });
}

function yesterday(): Date {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return date;
}

async function enterEvictionDateTime(page: Page, when: Date): Promise<void> {
  const parts: Record<string, string> = {
    day: String(when.getDate()).padStart(2, '0'),
    month: String(when.getMonth() + 1).padStart(2, '0'),
    year: String(when.getFullYear()),
    hour: '10',
    minute: '00',
  };
  for (const [field, value] of Object.entries(parts)) {
    const input = page.locator(`#warrantReturnEvictionDateTime-${field}`);
    await expect(input).toBeVisible({ timeout: LONG_TIMEOUT });
    await input.fill(value);
  }
}

/**
 * The claimant applies for a further warrant of possession through "Enforce the order". Records on the
 * world whether pcs-api's confirmation asked for the fee, before anything navigates away from it.
 */
export async function applyForFurtherWarrant(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType('warrant of possession', world, page);
  await answerApplicationQuestions('warrant of possession', world);
  await signStatementOfTruth('warrant of possession');
  await submitApplication();
  world.askedToPayFee = await page.getByText(PAY_PANEL_TITLE).first().isVisible().catch(() => false);
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
