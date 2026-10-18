/**
 * The claimant's risk assessment in "Confirm the eviction details" (HDPI-7552), and the original and updated
 * answers it leaves on the "Risk assessment" tab.
 *
 * pcs-api reuses the warrant application's risk pages in the event, pre-filled with the claimant's current
 * answers, after the attendance question. With the seeded warrant (no risk, nobody vulnerable, no access
 * difficulty, nothing else to add) that is four pages: "Everyone living at the property", "Vulnerable adults
 * and children at the property", "Access to the property" and "Anything else that could help with the eviction".
 *
 * Once the claimant has changed an answer, the "Claimant risk level" section of the tab is a GOV.UK table: one
 * row per question, with the question as the row header, then the original answer and the updated answer. The
 * updated cell is empty when that answer did not change. The section's paragraphs give the dates. While the
 * change waits for review, the internal part of the tab starts with an "Important" banner.
 */
import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { activeTab } from './enforcement-status-tab';

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
};

export const CLAIMANT_RISK_SECTION = 'Claimant risk level';
export const UPDATE_BANNER = 'The claimant has updated their risk assessment.';

/** The tab's introductions to the original and the updated answers, for internal users and the claimant side. */
export const answerDates = {
  originalForInternal: (date: string) => `Information provided by the claimant on ${date}`,
  updatedForInternal: (date: string) => `The claimant updated this information on ${date}`,
  updatedForClaimant: (date: string) => `You updated this information on ${date}`,
};

/** The tab words some questions differently from the warrant pages. */
const TAB_WORDINGS: Record<string, string> = {
  [riskQuestions.anyRiskToBailiff]: 'Does anyone at the property pose a risk to the bailiff?',
};

/** A question as the tab words it, from its wording on the warrant pages or in a feature file. */
export function tabQuestion(question: string): string {
  const pageWording = riskQuestion(question);
  return TAB_WORDINGS[pageWording] ?? pageWording;
}

/** The answers a change gives to each question's follow-ups, which are mandatory once the answer is "Yes". */
const followUps = {
  riskCategory: 'Violent or aggressive behaviour',
  violentDetails: 'Pushed a housing officer on the last visit',
  vulnerableCategory: 'Vulnerable adults',
  vulnerableReason: 'An elderly relative with dementia lives there',
  accessDifficulty: 'The entrance is a locked communal gate',
};

/**
 * The risk answers "Confirm the eviction details" asks again and pre-fills (its fields in
 * `CaseEventToFields/confirmEviction.json`), which an API submit sends back.
 */
export const RISK_ANSWER_FIELDS = [
  'warrantAnyRiskToBailiff',
  'warrantEnforcementRiskCategories',
  'warrantEnforcementViolentDetails',
  'warrantEnforcementFirearmsDetails',
  'warrantEnforcementCriminalDetails',
  'warrantEnforcementVerbalOrWrittenThreatsDetails',
  'warrantEnforcementProtestGroupMemberDetails',
  'warrantEnforcementPoliceOrSocialServicesDetails',
  'warrantEnforcementDogsOrOtherAnimalsDetails',
  'vulnerablePeoplePresent',
  'vulnerableAdultsChildren',
  'warrantIsDifficultToAccessProperty',
  'warrantClarificationOnAccessDifficultyText',
  'warrantAdditionalInformationSelect',
  'warrantAdditionalInformationDetails',
];

/** The field values `changeRiskAnswer` leaves behind when it changes `question` to "Yes", for an API submit. */
export function riskAnswerChange(question: string): Record<string, unknown> {
  switch (riskQuestion(question)) {
    case riskQuestions.anyRiskToBailiff:
      return {
        warrantAnyRiskToBailiff: 'YES',
        warrantEnforcementRiskCategories: ['VIOLENT_OR_AGGRESSIVE'],
        warrantEnforcementViolentDetails: followUps.violentDetails,
      };
    case riskQuestions.vulnerablePeople:
      return {
        vulnerablePeoplePresent: 'YES',
        vulnerableAdultsChildren: { vulnerableCategory: 'VULNERABLE_ADULTS', vulnerableReasonText: followUps.vulnerableReason },
      };
    case riskQuestions.difficultToAccess:
      return {
        warrantIsDifficultToAccessProperty: 'YES',
        warrantClarificationOnAccessDifficultyText: followUps.accessDifficulty,
      };
    default:
      throw new Error(`Changing "${question}" is not supported`);
  }
}

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

/** One question on the open "Risk assessment" tab: the question and its original and updated answers. */
export type ClaimantRiskAnswerRow = Record<string, string>;

/**
 * The open tab's row for one question, in the table shown once the claimant has changed an answer. Fails if the
 * tab does not list it.
 */
export async function claimantRiskAnswerRow(page: Page, question: string): Promise<ClaimantRiskAnswerRow> {
  const rows = activeTab(page).locator('tr.govuk-table__row').filter({
    has: page.locator('th[scope="row"]', { hasText: question }),
  });
  await expect.poll(async () => rows.count(), { timeout: VERY_LONG_TIMEOUT }).toBeGreaterThan(0);
  const row = rows.first();
  const cells = row.locator('td');
  return {
    [claimantRiskAssessmentFields.question]: (await row.locator('th').innerText()).trim(),
    [claimantRiskAssessmentFields.originalAnswer]: (await cells.nth(0).innerText()).trim(),
    [claimantRiskAssessmentFields.updatedAnswer]: (await cells.nth(1).innerText()).trim(),
  };
}
