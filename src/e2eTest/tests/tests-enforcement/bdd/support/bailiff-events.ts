/**
 * The bailiff events that are built and drivable: `setRiskLevel` and `addBailiffNote`.
 *
 * Both are single-page events that go straight from Continue to a confirmation screen — neither uses
 * `showSummary()`, so there is no check-your-answers step. Their confirmations carry the dashed case
 * number and nothing else: the warrant number the feature files describe is not rendered by
 * `SetRiskLevel` or `AddBailiffNote`, and no ticket covers adding it.
 *
 * `allocateBailiff` is deliberately absent from this module. Its one field is a StaffUser type-ahead
 * (`FindBailiffPage`, display-context `#ARGUMENT(CATEGORY-ADMIN)`) which needs staff reference data;
 * cftlib runs no rd-staff, so XUI degrades the component to a raw `IdamId` input and CCD's own
 * "IdamId is required" validation fires before the page's "Choose a bailiff" mid-event ever runs.
 */
import { Page, expect } from '@playwright/test';
import { LONG_TIMEOUT } from 'playwright.config';

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

/** Tab field labels the risk assessment tab uses for what these events write. */
export const riskAssessmentFields = {
  riskLevel: 'Risk level',
  riskLevelReason: 'Why the risk level is high',
  note: 'Note',
  noteAddedBy: 'Added by',
  noteDateTime: 'Date and time',
};

export const continueButton = (page: Page) => page.getByRole('button', { name: 'Continue', exact: true });

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
