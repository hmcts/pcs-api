// Find a case by warrant or writ number (feature 16).
//
// pcs-api indexes every warrant and writ number on the case as a global search "Other reference"
// (`EnforcementNumberSearchReferences`, HDPI-7677). The numbers are generated on approval, so the steps get
// real ones by applying through "Enforce the order" and approving through "Decide enforcement", submitted through
// the data store API, then map the feature file's illustrative numbers ("WP-0001") onto them.
//
// Searching needs a user with the GS_profile access profile, which `SearchCriteria` is restricted to. The
// local bailiff users have it through their `hmcts-admin` organisation role (cftlib-am-role-assignments.json);
// the hearing centre admin who approves does not.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import { canSignInAs, createIssuedCase, requireCaseReference, signInAs } from '../support/enforcement-journeys';
import { approveApplicationAsCaseworker, expectClaimantNext } from '../support/approve-warrant';
import { expectSignedInAs, expectedSignedInRole } from '../support/session';
import { expectSearchFindsCase, searchByOtherReference } from '../support/global-search';
import { applyAsClaimant, closeApprovedWarrant } from '../support/repeat-enforcement';

const OTHER_REFERENCE = 'Other reference';
/** The final return that closes a live warrant so that another application can be made. */
const CLOSING_RETURN_NAME = 'Warrant withdrawn';
const DEFENDANT_LEGAL_REPRESENTATIVE = 'defendant legal representative';

/**
 * The claimant's legal representative applies for `type` and a hearing centre admin approves it, both through
 * the data store API; returns the number it was given.
 */
async function generateEnforcement(
  world: EnforcementWorld,
  type: Extract<EnforcementType, 'warrant of possession' | 'writ of possession'>
): Promise<string> {
  await applyAsClaimant(world, type);
  return approveApplicationAsCaseworker(world, type === 'writ of possession' ? 'Writ' : 'Warrant');
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

Given('a PCS case {string} with warrants {string} and {string}', async ({ world }, _reference: string, first: string, second: string) => {
  approximatePrecondition(
    'HDPI-7677',
    `"${first}" is a writ of possession, not a warrant: an approved warrant stays live and blocks a second `
      + 'application until a warrant return closes it, while an approved writ is transferred to the High Court '
      + 'and is not live (OPEN-QUESTIONS 25)'
  );
  await createIssuedCase(world);
  try {
    world.enforcementNumbers[first] = await generateEnforcement(world, 'writ of possession');
    world.enforcementNumbers[second] = await generateEnforcement(world, 'warrant of possession');
    world.warrantNumber = world.enforcementNumbers[second];
    world.warrantApproved = true;
  } finally {
    expectClaimantNext();
  }
});

// The Background leaves its warrant live, and "Enforce the order" refuses another application while it is, so
// the bailiff staff close it with a final return first.
When('a new writ {string} is generated on the case', async ({ world }, illustrative: string) => {
  if (world.warrantApproved) {
    approximatePrecondition(
      'HDPI-8424',
      'the live warrant is closed with a final return first, because only one enforcement can be live at a time'
    );
    await closeApprovedWarrant(world, CLOSING_RETURN_NAME);
  }
  try {
    world.enforcementNumbers[illustrative] = await generateEnforcement(world, 'writ of possession');
  } finally {
    expectSignedInAs('bailiff');
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
  expect(expectedSignedInRole(), 'the Background\'s bailiff').toBe('bailiff');
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
