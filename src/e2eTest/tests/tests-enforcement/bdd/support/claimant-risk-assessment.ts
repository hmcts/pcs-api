import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { activeTab, reopenCurrentTab } from './enforcement-status-tab';

export const CONFIRM_EVICTION = 'Confirm the eviction date';

export const riskQuestions = {
  anyRiskToBailiff: 'Does anyone living at the property pose a risk to the bailiff?',
  vulnerablePeople: 'Is anyone living at the property vulnerable?',
  difficultToAccess: 'Is it difficult to access the property?',
};

const FEATURE_WORDINGS: Record<string, string> = {
  'Does anyone living at the property pose a risk to the bailiff?': riskQuestions.anyRiskToBailiff,
};

export function riskQuestion(featureWording: string): string {
  return FEATURE_WORDINGS[featureWording] ?? featureWording;
}

export const claimantRiskAssessmentFields = {
  question: 'Question',
  originalAnswer: 'Original answer',
  updatedAnswer: 'Updated answer',
};

export const CLAIMANT_RISK_SECTION = 'Claimant risk assessment';
export const UPDATE_BANNER = 'The claimant has updated their risk assessment.';

export const answerDates = {
  originalForInternal: (date: string) => `Information provided by the claimant on ${date}`,
  updatedForInternal: (date: string) => `The claimant updated this information on ${date}`,
  updatedForClaimant: (date: string) => `You updated this information on ${date}`,
};

/** The tab asks the claimant's questions in the same words as the pages that captured them. */
export function tabQuestion(question: string): string {
  return riskQuestion(question);
}

const followUps = {
  riskCategory: 'Violent or aggressive behaviour',
  violentDetails: 'Pushed a housing officer on the last visit',
  vulnerableCategory: 'Vulnerable adults',
  vulnerableReason: 'An elderly relative with dementia lives there',
  accessDifficulty: 'The entrance is a locked communal gate',
};

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

async function continueToNextPage(page: Page): Promise<void> {
  const current = await heading(page);
  await continueButton(page).click();
  await expect
    .poll(async () => (await saveAndContinueButton(page).isVisible()) || (await heading(page)) !== current, {
      timeout: VERY_LONG_TIMEOUT,
    })
    .toBe(true);
}

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

export async function continueToCheckYourAnswers(page: Page): Promise<void> {
  for (let step = 0; step < 15; step++) {
    if (await saveAndContinueButton(page).isVisible()) {
      return;
    }
    await continueToNextPage(page);
  }
  throw new Error('Check your answers was not reached');
}

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

export async function submitConfirmEviction(page: Page): Promise<void> {
  await saveAndContinueButton(page).click();
  await expectConfirmEvictionSubmitted(page);
}

export async function expectConfirmEvictionSubmitted(page: Page): Promise<void> {
  await expect(page.getByText(/has been updated with event: Confirm the eviction details/)).toBeVisible({
    timeout: VERY_LONG_TIMEOUT,
  });
}

export type ClaimantRiskAnswerRow = Record<string, string>;

/** The page is reloaded between tries: the change is set up through the API, so already-rendered markup would never gain it. */
export async function claimantRiskAnswerRow(page: Page, question: string): Promise<ClaimantRiskAnswerRow> {
  const rows = activeTab(page).locator('tr.govuk-table__row').filter({
    has: page.locator('th[scope="row"]', { hasText: question }),
  });
  await expect
    .poll(
      async () => {
        const count = await rows.count();
        if (count === 0) {
          await reopenCurrentTab(page);
        }
        return count;
      },
      { timeout: VERY_LONG_TIMEOUT, intervals: [2_000] }
    )
    .toBeGreaterThan(0);
  const row = rows.first();
  const cells = row.locator('td');
  return {
    [claimantRiskAssessmentFields.question]: (await row.locator('th').innerText()).trim(),
    [claimantRiskAssessmentFields.originalAnswer]: (await cells.nth(0).innerText()).trim(),
    [claimantRiskAssessmentFields.updatedAnswer]: (await cells.nth(1).innerText()).trim(),
  };
}

