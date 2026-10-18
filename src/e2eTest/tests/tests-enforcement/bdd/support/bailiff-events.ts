/**
 * The bailiff events that are built and drivable: `allocateBailiff`, `setRiskLevel` and `addBailiffNote`.
 *
 * `setRiskLevel` and `addBailiffNote` are single-page events that go straight from Continue to a
 * confirmation screen — neither uses `showSummary()`, so there is no check-your-answers step. Their
 * confirmations name the dashed case number and the warrant number.
 *
 * `allocateBailiff` is one page, "Find the bailiff's name", then check your answers. Its one field is XUI's
 * StaffUser type-ahead (`FindBailiffPage`, display context `#ARGUMENT(CATEGORY-ADMIN)`), which lists the
 * PCS staff in staff reference data that hold an ADMIN role. On a local stack staff reference data is
 * wiremock's stub (`wiremock/mappings/rd-professional/get-users-by-service-name.json`), which lists the
 * cftlib bailiff users. XUI matches on first and last name only, from the third character typed.
 */
import { Page, expect } from '@playwright/test';
import { LONG_TIMEOUT } from 'playwright.config';
import { localUserName, localUsers } from '@config/local-stack.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { openCaseSummary, requireCaseReference, startEvent } from './enforcement-journeys';
import { answerRow } from './schedule-eviction';

export const ALLOCATE_BAILIFF = 'Allocate case to bailiff';
export const SET_RISK_LEVEL = 'Set risk level';
export const ADD_BAILIFF_NOTE = 'Add a bailiff note';

export const setRiskLevel = {
  pageLabel: 'Set risk level',
  question: 'What is the risk level?',
  high: 'High',
  medium: 'Medium',
  reasonLabel: "Why did you set the risk level to 'high'?",
  reasonHint: 'You can enter up to 6,800 characters.',
  /** `RiskLevelDetails` caps the reason at 6800 characters. */
  reasonMaxLength: 6800,
};

export const addBailiffNote = {
  pageLabel: 'Add bailiff note',
  noteLabel: 'Add note',
  noteMaxLength: 6800,
};

export const findBailiff = {
  pageLabel: "Find the bailiff's name",
  /** `BailiffAllocationDetails.allocatedBailiff`'s label, the same text as the page's. */
  fieldLabel: "Find the bailiff's name",
  emailLabel: 'Email address',
  checkYourAnswers: 'Check your answers',
  submitButton: 'Submit',
  changeLink: 'Change',
};

/**
 * The local user standing in for a bailiff the feature files name. Only one cftlib user has the `bailiff`
 * role, so only the feature's main bailiff has a stand-in. The feature's email addresses are illustrative.
 */
const featureBailiffs: Record<string, string> = {
  'Sam Jones': localUsers.bailiff,
};

export const standInBailiff = (featureName: string): string | undefined => featureBailiffs[featureName];

/** The StaffUser type-ahead on "Find the bailiff's name". */
export const bailiffSearchBox = (page: Page) => page.getByLabel(findBailiff.fieldLabel, { exact: true });

/** The option XUI's type-ahead offers for a staff user: "Name (email)". */
export const bailiffOption = (page: Page, email: string) =>
  page.getByRole('option', { name: `${localUserName(email)} (${email})`, exact: true });

/** Types the bailiff's first name into the type-ahead and waits for them to be offered. */
export async function searchForBailiff(page: Page, email: string): Promise<void> {
  const searchBox = bailiffSearchBox(page);
  await searchBox.click();
  await searchBox.pressSequentially(localUserName(email).split(' ')[0]);
  await expect(bailiffOption(page, email)).toBeVisible({ timeout: LONG_TIMEOUT });
}

/** Chooses the bailiff in the type-ahead, which then shows their name. */
export async function chooseBailiff(page: Page, email: string): Promise<void> {
  await searchForBailiff(page, email);
  await bailiffOption(page, email).click();
  await expect(bailiffSearchBox(page)).toHaveValue(localUserName(email));
}

/** Asserts check your answers names the chosen bailiff and the email address pcs-api looked up for them. */
export async function expectAllocationCheckYourAnswers(page: Page, email: string): Promise<void> {
  await expect(page.getByRole('heading', { name: findBailiff.checkYourAnswers })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expect(answerRow(page, findBailiff.fieldLabel)).toContainText(localUserName(email));
  await expect(answerRow(page, findBailiff.emailLabel)).toContainText(email);
}

/** Selects the bailiff's Change link on check your answers, which goes back to "Find the bailiff's name". */
export async function changeAllocatedBailiff(page: Page): Promise<void> {
  await answerRow(page, findBailiff.fieldLabel).getByText(findBailiff.changeLink, { exact: true }).click();
  await expect(page.getByRole('heading', { name: findBailiff.pageLabel })).toBeVisible({ timeout: LONG_TIMEOUT });
}

/** Tab field labels the risk assessment tab uses for what these events write. */
export const riskAssessmentFields = {
  riskLevel: 'Risk level',
  riskLevelReason: 'Why the risk level is high',
  note: 'Note',
  noteAddedBy: 'Added by',
  noteDateTime: 'Date and time',
};

export const continueButton = (page: Page) => page.getByRole('button', { name: 'Continue', exact: true });

/**
 * Asserts a bailiff confirmation that names both numbers. `ConfirmationNumbers.caseAndWarrant` renders
 * "Case number: ..." and "Warrant number: ..." as separate paragraphs, and omits the warrant number only
 * for a warrant that never had one.
 */
export async function expectBailiffConfirmationWithWarrantNumber(
  page: Page,
  header: string,
  caseReference: string,
  warrantNumber: string
): Promise<void> {
  await expectBailiffConfirmation(page, header, caseReference);
  await expect(page.locator('#confirmation-body').getByText(`Warrant number: ${warrantNumber}`)).toBeVisible();
}

/** The dashed case number the bailiff confirmations render. */
export const dashedCaseNumber = (caseReference: string): string => caseReference.replace(/(.{4})(?=.)/g, '$1-');

/**
 * Asserts a bailiff event's confirmation screen: its `confirmationHeader` and the `Case number: ...` body.
 * Scoped to the confirmation containers so the case header above it cannot satisfy the assertion.
 */
export async function expectBailiffConfirmation(page: Page, header: string, caseReference: string): Promise<void> {
  await expect(page.getByRole('heading', { name: header, exact: true })).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(page.locator('#confirmation-body').getByText(`Case number: ${dashedCaseNumber(caseReference)}`)).toBeVisible();
}

/** Chooses a risk level and fills the reason when "High" needs one. */
export async function chooseRiskLevel(page: Page, level: string, reason?: string): Promise<void> {
  await page.getByRole('radio', { name: level, exact: true }).check();
  if (level === setRiskLevel.high) {
    const reasonBox = page.getByLabel(setRiskLevel.reasonLabel);
    await expect(reasonBox).toBeVisible({ timeout: LONG_TIMEOUT });
    await reasonBox.fill(reason ?? 'Known history of violence');
  }
}

/** The error summary CCD renders for a failed page. */
export function errorSummary(page: Page) {
  return page.locator('.govuk-error-summary, .error-summary').first();
}

/** Starts "Allocate case to bailiff" from the case summary, which opens on "Find the bailiff's name". */
export async function startAllocation(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent(ALLOCATE_BAILIFF);
  world.currentEvent = ALLOCATE_BAILIFF;
}

/** Starts "Allocate case to bailiff", chooses the bailiff and continues to check your answers. */
export async function goToAllocationCheckYourAnswers(page: Page, world: EnforcementWorld, email: string): Promise<void> {
  await startAllocation(page, world);
  await chooseBailiff(page, email);
  world.chosenBailiff = email;
  await continueButton(page).click();
  await expectAllocationCheckYourAnswers(page, email);
}

/** Submits the allocation from check your answers and asserts its confirmation. */
export async function submitAllocation(page: Page, world: EnforcementWorld): Promise<void> {
  const email = world.chosenBailiff;
  expect(email, 'a bailiff chosen in "Allocate case to bailiff"').toBeDefined();
  await page.getByRole('button', { name: findBailiff.submitButton, exact: true }).click();
  await expectBailiffConfirmation(page, `Case assigned to ${localUserName(email as string)}`, requireCaseReference(world));
  world.allocatedBailiff = email;
  world.currentEvent = undefined;
}
