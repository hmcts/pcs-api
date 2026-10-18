// Find a case by warrant or writ number (feature 16).
//
// pcs-api indexes every warrant and writ number on the case as a global search "Other reference"
// (`EnforcementNumberSearchReferences`, HDPI-7677). The numbers are generated on approval, so the steps get
// real ones by applying through "Enforce the order" and approving through "Decide enforcement", then map the
// feature file's illustrative numbers ("WP-0001") onto them.
//
// Searching needs a user with the GS_profile access profile, which `SearchCriteria` is restricted to. The
// local bailiff users have it through their `hmcts-admin` organisation role (cftlib-am-role-assignments.json);
// the hearing centre admin who approves does not.
import { BrowserContext, Page } from '@playwright/test';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import {
  answerApplicationQuestions,
  canSignInAs,
  chooseApplicationType,
  createIssuedCase,
  openCaseSummary,
  requireCaseReference,
  signInAs,
  signStatementOfTruth,
  startEvent,
  submitApplication,
} from '../support/enforcement-journeys';
import { approveApplicationAsCaseworker, restoreClaimantSession } from '../support/approve-warrant';
import { currentSignedInRole } from '../support/session';
import { expectSearchFindsCase, searchByOtherReference } from '../support/global-search';

const OTHER_REFERENCE = 'Other reference';
const DEFENDANT_LEGAL_REPRESENTATIVE = 'defendant legal representative';

/** Applies for `type` as the claimant's legal representative and approves it; returns the number it was given. */
async function generateEnforcement(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld,
  type: Extract<EnforcementType, 'warrant of possession' | 'writ of possession'>
): Promise<string> {
  await restoreClaimantSession(page, context);
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType(type, world, page);
  await answerApplicationQuestions(type, world);
  await signStatementOfTruth(type);
  await submitApplication();
  return approveApplicationAsCaseworker(page, world, type === 'writ of possession' ? 'Writ' : 'Warrant');
}

/** The real number behind one of the feature file's illustrative numbers. */
function realNumber(world: EnforcementWorld, illustrative: string): string {
  const number = world.enforcementNumbers[illustrative];
  if (!number) {
    throw new Error(
      `No enforcement number was generated for "${illustrative}" in this scenario. `
        + `Generated: ${JSON.stringify(world.enforcementNumbers)}`
    );
  }
  return number;
}

Given('a PCS case {string} with warrants {string} and {string}', async ({ page, context, world }, _reference: string, first: string, second: string) => {
  approximatePrecondition(
    'HDPI-7677',
    `"${first}" is a writ of possession, not a warrant: an approved warrant stays live and blocks a second `
      + 'application until a warrant return closes it, while an approved writ is transferred to the High Court '
      + 'and is not live (OPEN-QUESTIONS 10)'
  );
  await createIssuedCase(world);
  try {
    world.enforcementNumbers[first] = await generateEnforcement(page, context, world, 'writ of possession');
    world.enforcementNumbers[second] = await generateEnforcement(page, context, world, 'warrant of possession');
    world.warrantNumber = world.enforcementNumbers[second];
    world.warrantApproved = true;
  } finally {
    await restoreClaimantSession(page, context);
  }
});

When('a new writ {string} is generated on the case', async ({ page, context, world }, illustrative: string) => {
  try {
    world.enforcementNumbers[illustrative] = await generateEnforcement(page, context, world, 'writ of possession');
  } finally {
    await signInAs('bailiff', page, context);
  }
});

When('I search in global search with {string} {string}', async ({ page, world }, field: string, illustrative: string) => {
  expect(field, 'the only global search field these steps drive').toBe(OTHER_REFERENCE);
  world.searchedReference = realNumber(world, illustrative);
  await searchByOtherReference(page, world.searchedReference);
});

Then('the results include case {string}', async ({ page, world }, _reference: string) => {
  expect(world.searchedReference, 'a global search earlier in the scenario').toBeDefined();
  await expectSearchFindsCase(page, world.searchedReference as string, requireCaseReference(world));
});

Then(
  'the case\'s global search {string} values include {string}, {string} and {string}',
  async ({ page, world }, field: string, first: string, second: string, third: string) => {
    expect(field).toBe(OTHER_REFERENCE);
    for (const illustrative of [first, second, third]) {
      await expectSearchFindsCase(page, realNumber(world, illustrative), requireCaseReference(world));
    }
  }
);

// A defendant search that finds nothing proves nothing if the case is not indexed yet, so before swapping
// users this checks that the bailiff signed in by the Background can find the case by every number.
Given('I am signed in as a defendant legal representative', async ({ page, context, world, $tags }) => {
  if (!canSignInAs(DEFENDANT_LEGAL_REPRESENTATIVE)) {
    return markPending(
      {
        ticket: 'HDPI-7539',
        summary: `signing in as a ${DEFENDANT_LEGAL_REPRESENTATIVE}: no ${DEFENDANT_LEGAL_REPRESENTATIVE} the suite can use in this environment`,
      },
      $tags
    );
  }
  expect(currentSignedInRole(), 'the Background\'s bailiff').toBe('bailiff');
  for (const number of Object.values(world.enforcementNumbers)) {
    await expectSearchFindsCase(page, number, requireCaseReference(world));
  }
  await signInAs(DEFENDANT_LEGAL_REPRESENTATIVE, page, context);
});

// XUI may not offer the search page to a professional user at all; that also returns nothing.
Then('no results are returned', async ({ page, world }) => {
  expect(world.searchedReference, 'a global search earlier in the scenario').toBeDefined();
  const outcome = await searchByOtherReference(page, world.searchedReference as string);
  if (outcome.kind === 'results') {
    expect(outcome.caseReferences, 'cases found by the search').not.toContain(requireCaseReference(world));
  }
});
