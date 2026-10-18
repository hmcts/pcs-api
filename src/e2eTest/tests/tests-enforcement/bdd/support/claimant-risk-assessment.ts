/**
 * The claimant's risk assessment in "Confirm the eviction details" (HDPI-7552), and the original and updated
 * answers it leaves on the "Risk assessment" tab.
 *
 * pcs-api reuses the warrant application's risk pages in the event, pre-filled with the claimant's current
 * answers, after the attendance question. With the seeded warrant (no risk, nobody vulnerable, no access
 * difficulty, nothing else to add) that is four pages: "Everyone living at the property", "Vulnerable adults
 * and children at the property", "Access to the property" and "Anything else that could help with the eviction".
 *
 * On the tab, each question is a CCD complex panel with `Question`, `Original answer`, `Updated answer` and
 * `Answer changed` rows. CCD leaves out a row whose field is empty, so an unchanged answer has no
 * `Updated answer` row at all.
 */
import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';

export const CONFIRM_EVICTION = 'Confirm the eviction date';

/** The questions as the reused warrant pages and the tab word them. */
export const riskQuestions = {
  anyRiskToBailiff: 'Does anyone living at the property pose a risk to the bailiff?',
  vulnerablePeople: 'Is anyone living at the property vulnerable?',
  difficultToAccess: 'Is it difficult to access the property?',
};

/** Feature-file wordings that differ from the page's. */
const FEATURE_WORDINGS: Record<string, string> = {
  'Does anyone at the property pose a risk to the bailiff?': riskQuestions.anyRiskToBailiff,
};

export function riskQuestion(featureWording: string): string {
  return FEATURE_WORDINGS[featureWording] ?? featureWording;
}

export const claimantRiskAssessmentFields = {
  question: 'Question',
  originalAnswer: 'Original answer',
  updatedAnswer: 'Updated answer',
  givenOn: 'Original answers given on',
  updatedOn: 'Answers updated on',
};

/** The answers a change gives to each question's follow-ups, which are mandatory once the answer is "Yes". */
const followUps = {
  riskCategory: 'Violent or aggressive behaviour',
  violentDetails: 'Pushed a housing officer on the last visit',
  vulnerableCategory: 'Vulnerable adults',
  vulnerableReason: 'An elderly relative with dementia lives there',
  accessDifficulty: 'The entrance is a locked communal gate',
};

const pageHeadings = {
  livingInTheProperty: 'Everyone living at the property',
  vulnerablePeople: 'Vulnerable adults and children at the property',
  propertyAccess: 'Access to the property',
};

const continueButton = (page: Page) => page.getByRole('button', { name: 'Continue', exact: true });
const saveAndContinueButton = (page: Page) => page.getByRole('button', { name: 'Save and continue', exact: true });

async function heading(page: Page): Promise<string> {
  return (await page.locator('h1').first().innerText()).trim();
}

/** Clicks Continue and waits for the next page (or CCD's check your answers) to replace the current one. */
async function continueToNextPage(page: Page): Promise<void> {
  const current = await heading(page);
  await continueButton(page).click();
  await expect
    .poll(async () => (await saveAndContinueButton(page).isVisible()) || (await heading(page)) !== current, {
      timeout: VERY_LONG_TIMEOUT,
    })
    .toBe(true);
}

/** Continues, leaving the answers as they are, until the page with this heading. */
async function continueUntilPage(page: Page, pageHeading: string): Promise<void> {
  for (let step = 0; step < 15; step++) {
    if ((await heading(page)) === pageHeading) {
      return;
    }
    if (await saveAndContinueButton(page).isVisible()) {
      break;
    }
    await continueToNextPage(page);
  }
  throw new Error(`"${pageHeading}" was not among the event's pages`);
}

/** Continues, leaving the remaining answers as they are, to CCD's check your answers. */
export async function continueToCheckYourAnswers(page: Page): Promise<void> {
  for (let step = 0; step < 15; step++) {
    if (await saveAndContinueButton(page).isVisible()) {
      return;
    }
    await continueToNextPage(page);
  }
  throw new Error('Check your answers was not reached');
}

/** From the event's first page: past the information page, then answers "Can you attend the eviction on this date?". */
export async function answerAttendance(page: Page, canAttend: boolean): Promise<void> {
  await continueButton(page).click();
  const answer = page.locator(`#claimantCanAttendEviction_${canAttend ? 'Yes' : 'No'}`);
  await expect(answer).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  await answer.check();
  await continueToNextPage(page);
}

function radio(page: Page, question: string, option: string) {
  return page.getByRole('group', { name: question }).getByRole('radio', { name: option, exact: true });
}

/**
 * Changes one risk answer to "Yes" on the reused warrant pages, answering the follow-up questions that then
 * appear, and leaves the browser on the page after them.
 */
export async function changeRiskAnswer(page: Page, question: string, answer: string): Promise<void> {
  if (answer !== 'Yes') {
    throw new Error(`Only changing an answer to "Yes" is supported, not "${answer}"`);
  }
  switch (question) {
    case riskQuestions.anyRiskToBailiff:
      await continueUntilPage(page, pageHeadings.livingInTheProperty);
      await radio(page, question, answer).check();
      await continueToNextPage(page);
      await page.getByRole('checkbox', { name: followUps.riskCategory, exact: true }).check();
      await continueToNextPage(page);
      await page.getByLabel('How have they been violent or aggressive?').fill(followUps.violentDetails);
      await continueToNextPage(page);
      return;
    case riskQuestions.vulnerablePeople:
      await continueUntilPage(page, pageHeadings.vulnerablePeople);
      await radio(page, question, answer).check();
      await radio(
        page,
        'Confirm if the vulnerable people in the property are adults, children, or both adults and children',
        followUps.vulnerableCategory
      ).check();
      await page.getByLabel('How are they vulnerable?').fill(followUps.vulnerableReason);
      await continueToNextPage(page);
      return;
    case riskQuestions.difficultToAccess:
      await continueUntilPage(page, pageHeadings.propertyAccess);
      await radio(page, question, answer).check();
      await page.getByLabel('Explain why it’s difficult to access the property').fill(followUps.accessDifficulty);
      await continueToNextPage(page);
      return;
    default:
      throw new Error(`Changing "${question}" is not supported`);
  }
}

/** Saves and continues from check your answers and waits for CCD to confirm the event was submitted. */
export async function submitConfirmEviction(page: Page): Promise<void> {
  await saveAndContinueButton(page).click();
  await expectConfirmEvictionSubmitted(page);
}

export async function expectConfirmEvictionSubmitted(page: Page): Promise<void> {
  await expect(page.getByText(/has been updated with event: Confirm the eviction details/)).toBeVisible({
    timeout: VERY_LONG_TIMEOUT,
  });
}

/** One question on the open "Risk assessment" tab, its row labels mapped to their values. */
export type ClaimantRiskAnswerRow = Record<string, string>;

/** Every question on the open "Risk assessment" tab, in the order the tab lists them. */
export async function claimantRiskAnswerRows(page: Page): Promise<ClaimantRiskAnswerRow[]> {
  const panels = page
    .locator('.mat-tab-body-active ccd-read-complex-field-table .complex-panel')
    .filter({ has: page.locator('th', { hasText: claimantRiskAssessmentFields.question }) });
  await expect.poll(async () => panels.count(), { timeout: VERY_LONG_TIMEOUT }).toBeGreaterThan(0);

  const rows: ClaimantRiskAnswerRow[] = [];
  for (let index = 0; index < (await panels.count()); index++) {
    const fields = panels.nth(index).locator('tr.complex-panel-simple-field');
    const row: ClaimantRiskAnswerRow = {};
    for (let field = 0; field < (await fields.count()); field++) {
      const label = (await fields.nth(field).locator('th').innerText()).trim();
      row[label] = (await fields.nth(field).locator('td').innerText()).trim();
    }
    rows.push(row);
  }
  return rows;
}

/** The open tab's row for one question. Fails if the tab does not list it. */
export async function claimantRiskAnswerRow(page: Page, question: string): Promise<ClaimantRiskAnswerRow> {
  const row = (await claimantRiskAnswerRows(page)).find(
    candidate => candidate[claimantRiskAssessmentFields.question] === question
  );
  expect(row, `"${question}" on the "Risk assessment" tab`).toBeDefined();
  return row as ClaimantRiskAnswerRow;
}

/** Today in the UK as the tab writes dates, e.g. "6 March 2026". */
export function todayAsTabDate(): string {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'Europe/London',
  }).format(new Date());
}
