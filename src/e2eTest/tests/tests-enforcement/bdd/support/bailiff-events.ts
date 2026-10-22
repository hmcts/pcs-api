import { Page, expect } from '@playwright/test';
import { LONG_TIMEOUT } from 'playwright.config';
import { localUserName, localUsers } from '@config/local-stack.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { openCaseSummary, requireCaseReference, startEvent } from './enforcement-journeys';
import { errorSummaries } from './error-summary';
import { answerRow } from './schedule-eviction';
import { submitEventAs } from './event-api';
import { expectSignedInAs, requireEmailFor } from './session';
import { tabBlocks } from './enforcement-status-tab';

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
  fieldLabel: "Find the bailiff's name",
  emailLabel: 'Email address',
  checkYourAnswers: 'Check your answers',
  submitButton: 'Save and continue',
  changeLink: 'Change',
};

const featureBailiffs: Record<string, string> = {
  'Sam Jones': localUsers.bailiff,
};

export const standInBailiff = (featureName: string): string | undefined => featureBailiffs[featureName];

export const bailiffSearchBox = (page: Page) => page.getByLabel(findBailiff.fieldLabel, { exact: true });

export const bailiffOption = (page: Page, email: string) =>
  page.getByRole('option', { name: `${localUserName(email)} (${email})`, exact: true });

export async function searchForBailiff(page: Page, email: string): Promise<void> {
  const searchBox = bailiffSearchBox(page);
  await searchBox.click();
  await searchBox.pressSequentially(localUserName(email).split(' ')[0]);
  await expect(bailiffOption(page, email)).toBeVisible({ timeout: LONG_TIMEOUT });
}

export async function chooseBailiff(page: Page, email: string): Promise<void> {
  await searchForBailiff(page, email);
  await bailiffOption(page, email).click();
  await expect(bailiffSearchBox(page)).toHaveValue(localUserName(email));
}

export async function expectAllocationCheckYourAnswers(page: Page, email: string): Promise<void> {
  await expect(page.getByRole('heading', { name: findBailiff.checkYourAnswers })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expect(answerRow(page, findBailiff.fieldLabel)).toContainText(localUserName(email));
  await expect(answerRow(page, findBailiff.emailLabel)).toContainText(email);
}

export async function changeAllocatedBailiff(page: Page): Promise<void> {
  await answerRow(page, findBailiff.fieldLabel).getByText(findBailiff.changeLink, { exact: true }).click();
  await expect(page.getByRole('heading', { name: findBailiff.pageLabel })).toBeVisible({ timeout: LONG_TIMEOUT });
}

export const riskAssessmentFields = {
  riskLevel: 'Risk level',
  riskLevelReason: 'Why this is high risk',
  note: 'Note added',
  noteAddedBy: 'Added by',
  noteDateTime: 'Date added',
};

/** Each note on the risk assessment tab is its own numbered sub-section, "Bailiff Note 1" being the newest. */
export async function bailiffNotes(page: Page): Promise<{ number: number; rows: Record<string, string> }[]> {
  const blocks = await tabBlocks(page);
  return blocks
    .map(block => ({ number: Number(/^Bailiff Note (\d+)$/.exec(block.heading)?.[1]), rows: block.rows }))
    .filter(note => !Number.isNaN(note.number));
}

export const continueButton = (page: Page) => page.getByRole('button', { name: 'Continue', exact: true });

export async function expectBailiffConfirmationWithWarrantNumber(
  page: Page,
  header: string,
  caseReference: string,
  warrantNumber: string
): Promise<void> {
  await expectBailiffConfirmation(page, header, caseReference);
  await expect(page.locator('#confirmation-body').getByText(`Warrant number: ${warrantNumber}`)).toBeVisible();
}

export const dashedCaseNumber = (caseReference: string): string => caseReference.replace(/(.{4})(?=.)/g, '$1-');

/** Scoped to the confirmation containers, so the case header above cannot satisfy the assertion. */
export async function expectBailiffConfirmation(page: Page, header: string, caseReference: string): Promise<void> {
  await expect(page.getByRole('heading', { name: header, exact: true })).toBeVisible({ timeout: LONG_TIMEOUT });
  await expect(page.locator('#confirmation-body').getByText(`Case number: ${dashedCaseNumber(caseReference)}`)).toBeVisible();
}

export async function chooseRiskLevel(page: Page, level: string, reason?: string): Promise<void> {
  await page.getByRole('radio', { name: level, exact: true }).check();
  if (level === setRiskLevel.high) {
    const reasonBox = page.getByLabel(setRiskLevel.reasonLabel);
    await expect(reasonBox).toBeVisible({ timeout: LONG_TIMEOUT });
    await reasonBox.fill(reason ?? 'Known history of violence');
  }
}

export function errorSummary(page: Page) {
  return errorSummaries(page).first();
}

export async function startAllocation(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent(ALLOCATE_BAILIFF);
  world.currentEvent = ALLOCATE_BAILIFF;
}

export async function goToAllocationCheckYourAnswers(page: Page, world: EnforcementWorld, email: string): Promise<void> {
  await startAllocation(page, world);
  await chooseBailiff(page, email);
  world.chosenBailiff = email;
  await continueButton(page).click();
  await expectAllocationCheckYourAnswers(page, email);
}

export async function submitAllocation(page: Page, world: EnforcementWorld): Promise<void> {
  const email = world.chosenBailiff;
  expect(email, 'a bailiff chosen in "Allocate case to bailiff"').toBeDefined();
  await page.getByRole('button', { name: findBailiff.submitButton, exact: true }).click();
  await expectBailiffConfirmation(page, `Case assigned to ${localUserName(email as string)}`, requireCaseReference(world));
  world.allocatedBailiff = email;
  world.currentEvent = undefined;
}

export async function setRiskLevelAsBailiff(level: string, reason = 'Known history of violence'): Promise<void> {
  const confirmation = await submitEventAs(requireEmailFor('bailiff'), 'setRiskLevel', {
    riskLevel: level.toUpperCase(),
    ...(level === setRiskLevel.high ? { riskLevelReason: reason } : {}),
  });
  expect(`${confirmation.header ?? ''} ${confirmation.body ?? ''}`).toContain(`Risk level set to ${level.toLowerCase()}`);
  expectSignedInAs('bailiff');
}

export async function addBailiffNoteAsBailiff(note: string): Promise<void> {
  const confirmation = await submitEventAs(requireEmailFor('bailiff'), 'addBailiffNote', { bailiffNoteText: note });
  expect(`${confirmation.header ?? ''} ${confirmation.body ?? ''}`).toContain('Bailiff note added');
  expectSignedInAs('bailiff');
}
