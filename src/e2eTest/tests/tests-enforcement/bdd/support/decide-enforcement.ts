/**
 * The caseworker "Decide enforcement" event (feature 02). pcs-api: uk.gov.hmcts.reform.pcs.enforcement.ccd.event.DecideEnforcement.
 */
import Axios from 'axios';
import { Page } from '@playwright/test';
import { expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { LONG_TIMEOUT } from 'playwright.config';
import { dismissCookieBanner } from '@config/cookie-banner';
import { user } from '@data/user-data';
import { performAction } from '@utils/controller-enforcement';

export const DECIDE_ENFORCEMENT = 'Decide enforcement';

export const decideEnforcement = {
  eventName: DECIDE_ENFORCEMENT,
  mainHeader: DECIDE_ENFORCEMENT,
  decisionQuestion: 'Do you want to accept or reject this application?',
  acceptRadioOption: 'Accept',
  rejectRadioOption: 'Reject',
  rejectionReasonLabel: 'Reason for rejecting the application',
  rejectionReasonInput: 'The possession order has been set aside.',
  caseNumberKey: 'Case number',
  propertyAddressKey: 'Property address',
  partiesKey: 'Parties',
  continueButton: 'Continue',
  previousButton: 'Previous',
  submitButton: 'Submit',
  cancelLink: 'Cancel',
  changeLink: 'Change',
  checkYourAnswersHeader: 'Check your answers',
};

/**
 * pcs-api cannot take an enforcement fee yet (HDPI-2877), so this records the fee for the case's application as
 * paid through pcs-api testing support. The application moves from "Awaiting payment" to "Awaiting review".
 */
export async function recordEnforcementFeePaid(): Promise<void> {
  const api = Axios.create({
    baseURL: process.env.CASE_API_URL,
    headers: { ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}` },
  });
  const response = await api.post(`/testing-support/enforcement/${process.env.CASE_NUMBER}/fee-paid`);
  expect(response.status, 'the enforcement fee is recorded as paid').toBe(204);
}

/** Drops the claimant legal representative's session from global set-up and signs in as a hearing centre admin. */
export async function signInAsHearingCentreAdmin(page: Page): Promise<void> {
  await page.context().clearCookies();
  await performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL);
  await dismissCookieBanner(page, 'additional');
  await performAction('login', user.hearingCenterAdmin);
  await dismissCookieBanner(page, 'analytics');
}

function caseDetails(page: Page) {
  return page.locator('dl.govuk-summary-list').filter({ hasText: decideEnforcement.propertyAddressKey });
}

export async function expectCaseDetailsHeader(page: Page, world: EnforcementWorld, partyNames: string): Promise<void> {
  const header = caseDetails(page).first();
  await expect(header).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(header).toContainText(decideEnforcement.caseNumberKey);
  await expect(header).toContainText(String(world.caseReference));
  await expect(header).toContainText(decideEnforcement.propertyAddressKey);
  await expect(header).toContainText(decideEnforcement.partiesKey);
  // "claimant name vs defendant name": the case name pcs-api builds for the case.
  if (/ vs /.test(partyNames)) {
    await expect(header).toContainText(' vs ');
  }
}

export async function expectCaseDetailsNotEditable(page: Page): Promise<void> {
  await expect(caseDetails(page).first().locator('input, textarea, select')).toHaveCount(0);
}

export async function chooseDecision(page: Page, option: string): Promise<void> {
  await page.getByRole('radio', { name: option, exact: true }).check();
}

export async function clickContinue(page: Page): Promise<void> {
  await page.getByRole('button', { name: decideEnforcement.continueButton, exact: true }).click();
}

export async function goToCheckYourAnswers(page: Page): Promise<void> {
  await clickContinue(page);
  await expect(page.getByRole('heading', { name: decideEnforcement.checkYourAnswersHeader })).toBeVisible({ timeout: LONG_TIMEOUT });
}

export async function submitDecision(page: Page): Promise<void> {
  if (await page.getByRole('radio', { name: decideEnforcement.acceptRadioOption, exact: true }).isVisible()) {
    await goToCheckYourAnswers(page);
  }
  await page.getByRole('button', { name: decideEnforcement.submitButton, exact: true }).click();
}

export function decisionRow(page: Page) {
  return page.locator('tr, .govuk-summary-list__row').filter({ hasText: decideEnforcement.decisionQuestion }).first();
}

/** The confirmation markup from DecideEnforcementConfirmation, e.g. "Warrant approved" or "Writ rejected". */
export async function expectConfirmation(page: Page, title: string): Promise<void> {
  await expect(page.getByText(title, { exact: true })).toBeVisible({ timeout: LONG_TIMEOUT });
}
