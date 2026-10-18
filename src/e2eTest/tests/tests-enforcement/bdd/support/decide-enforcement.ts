/**
 * The caseworker "Decide enforcement" event (feature 02). pcs-api: uk.gov.hmcts.reform.pcs.enforcement.ccd.event.DecideEnforcement.
 */
import Axios from 'axios';
import { Locator, Page } from '@playwright/test';
import { expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { LONG_TIMEOUT } from 'playwright.config';
import { dismissCookieBanner } from '@config/cookie-banner';
import { user } from '@data/user-data';
import { performAction } from '@utils/controller-enforcement';
import { isLocalStack, localUsers, signInToIdamSimulator } from '@config/local-stack.config';
import { recordSignedInRole } from './session';

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
 * A local stack has no Payments service to take an enforcement fee (HDPI-2877), so this pays the fee for the case's
 * application through pcs-api testing support, which sends a paid callback through the enforcement payment
 * handler. The application moves from "Awaiting payment" to "Awaiting review".
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
  if (isLocalStack()) {
    await signInToIdamSimulator(page, localUsers.hearingCentreAdmin);
  } else {
    await performAction('login', user.hearingCenterAdmin);
  }
  await dismissCookieBanner(page, 'analytics');
  recordSignedInRole('hearing-centre-admin');
}

function caseDetails(page: Page) {
  return page.locator('dl.govuk-summary-list').filter({ hasText: decideEnforcement.propertyAddressKey });
}

/** The 16-digit reference as XUI renders it in a case-details summary list: four dash-separated groups. */
export const withDashes = (caseReference: string): string => caseReference.replace(/(.{4})(?=.)/g, '$1-');

/**
 * The read-only case details at the top of an enforcement event page.
 *
 * `decideEnforcement` renders them as a GOV.UK summary list with `Case number` / `Property address` /
 * `Parties` keys. The bailiff events (`setRiskLevel`, `addBailiffNote`, …) render the same three facts from
 * a markdown page label instead, so they come out as paragraphs. Both are checked.
 */
export async function expectCaseDetailsHeader(page: Page, world: EnforcementWorld, partyNames: string): Promise<void> {
  const dashedReference = withDashes(String(world.caseReference));
  const summaryList = caseDetails(page).first();

  if (await summaryList.count()) {
    await expect(summaryList).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(summaryList).toContainText(decideEnforcement.caseNumberKey);
    await expect(summaryList).toContainText(dashedReference);
    // The values as well as the keys: XUI renders a ${...} it cannot fill as an empty string.
    await expect(summaryValue(summaryList, decideEnforcement.propertyAddressKey)).toHaveText(/\S/);
    await expect(summaryValue(summaryList, decideEnforcement.partiesKey)).toHaveText(/\S/);
  } else {
    // XUI also shows the case number in the case header above the event, hence .first().
    await expect(page.getByRole('paragraph').filter({ hasText: `Case number: ${dashedReference}` }).first())
      .toBeVisible({ timeout: LONG_TIMEOUT });
    // XUI renders these paragraphs with surrounding whitespace, so the text is not anchored.
    await expect(page.getByRole('paragraph').filter({ hasText: 'Property address:' }).first()).toBeVisible();
  }
  // "claimant name vs defendant name": the case name pcs-api builds for the case.
  if (/ vs /.test(partyNames)) {
    const partiesOn = (await summaryList.count())
      ? summaryValue(summaryList, decideEnforcement.partiesKey)
      : page.locator('body');
    await expect(partiesOn).toContainText(' vs ');
  }
}

function summaryValue(summaryList: Locator, key: string): Locator {
  return summaryList.locator('.govuk-summary-list__row').filter({ hasText: key }).locator('.govuk-summary-list__value');
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

/**
 * The confirmation panel XUI renders from `DecideEnforcementConfirmation`. Scoped to `#confirmation-body`
 * because the case number and property address also appear in the case header above it.
 */
export function confirmationPanel(page: Page) {
  return page.locator('#confirmation-body');
}

/** The confirmation markup from DecideEnforcementConfirmation, e.g. "Warrant approved" or "Writ rejected". */
export async function expectConfirmation(page: Page, title: string): Promise<void> {
  await expect(confirmationPanel(page).getByText(title, { exact: true })).toBeVisible({ timeout: LONG_TIMEOUT });
}
