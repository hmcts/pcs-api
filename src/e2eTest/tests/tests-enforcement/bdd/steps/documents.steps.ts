// Generated enforcement documents in the Enforcement folder of case file view (features 01, 02 and 05).
import { Then, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { markPending } from '../fixtures/pending';
import type { PendingScope } from '../fixtures/pending';
import { isLocalStack } from '@config/local-stack.config';
import { requireCaseReference } from '../support/enforcement-journeys';
import {
  ENFORCEMENT_FOLDER,
  countEnforcementDocuments,
  documentTitle,
  enforcementDocuments,
  expectEnforcementDocument,
  expectHiddenFrom,
} from '../support/case-documents';
import { docmosisRender, noteCheckedOnRenderData } from '../support/docmosis-renders';

/**
 * Locally Docmosis is wiremock, whose request journal keeps what pcs-api asked it to render. Preview and AAT render
 * with the real Docmosis, which keeps nothing the suite can read, so the checks made on the render data (seal,
 * date stamp, answers) only run locally. Case file view itself is checked everywhere.
 */
const docmosisJournalOnly: PendingScope = {
  summary: 'the seal, date stamp and answers are checked in the request journal of the local stack\'s wiremock '
    + 'Docmosis stub; preview and AAT render with the real Docmosis, which keeps no journal',
};

/** Marks the scenario pending outside a local stack, where there is no Docmosis request journal to read. */
function requireDocmosisJournal(tags: string[]): void {
  if (!isLocalStack()) {
    markPending(docmosisJournalOnly, tags);
  }
}

function requireEnforcementFolder(folder: string): void {
  expect(folder, 'the folder enforcement documents are filed in').toBe(ENFORCEMENT_FOLDER);
}

function requireCurrentDocument(world: EnforcementWorld): string {
  if (!world.currentDocument) {
    throw new Error('The scenario has not generated a document yet');
  }
  return world.currentDocument;
}

/** Waits for the named document and makes it the one "the document" and "it" refer to. */
async function expectGenerated(world: EnforcementWorld, name: string): Promise<string> {
  const title = documentTitle(name, world);
  await expectEnforcementDocument(requireCaseReference(world), title);
  world.currentDocument = title;
  world.checkedDocuments.push(title);
  return title;
}

/**
 * The seal pcs-api asked Docmosis to put on the document: "County Court", "High Court", or none. The stub's
 * placeholder PDF carries no seal, so the request is what can be checked.
 */
async function expectSeal(world: EnforcementWorld, title: string, seal: string | undefined, tags: string[]): Promise<void> {
  requireDocmosisJournal(tags);
  const render = await docmosisRender(requireCaseReference(world), title);
  expect(render.data.seal?.toLowerCase() ?? undefined, `the seal on "${title}…"`).toBe(seal?.toLowerCase());
  noteCheckedOnRenderData(seal ? `the ${seal} seal` : 'that the document is unsealed');
}

async function expectVisibleToClaimantAndInternalUsers(world: EnforcementWorld, title: string): Promise<void> {
  const caseReference = requireCaseReference(world);
  await expectEnforcementDocument(caseReference, title, 'internal user');
  await expectEnforcementDocument(caseReference, title, 'claimant');
}

async function expectVisibleToInternalUsersOnly(world: EnforcementWorld, title: string): Promise<void> {
  await expectHiddenFrom(requireCaseReference(world), title, 'claimant');
}

Then('an {string} folder is created in case file view', async ({ world }, folder: string) => {
  requireEnforcementFolder(folder);
  const caseReference = requireCaseReference(world);
  await expect
    .poll(async () => (await enforcementDocuments(caseReference, 'internal user')).length, {
      message: `a document in the ${ENFORCEMENT_FOLDER} folder`,
      timeout: 120_000,
    })
    .toBeGreaterThan(0);
});

Then(
  'an unsealed {string} document is stored in the {string} folder',
  async ({ world, $tags }, name: string, folder: string) => {
    requireEnforcementFolder(folder);
    await expectSeal(world, await expectGenerated(world, name), undefined, $tags);
  }
);

Then('both documents are visible to the claimant and internal users', async ({ world }) => {
  expect(world.checkedDocuments, 'the documents the scenario found').toHaveLength(2);
  for (const title of world.checkedDocuments) {
    await expectVisibleToClaimantAndInternalUsers(world, title);
  }
});

Then('no {string} document is generated', async ({ world }, name: string) => {
  // Every application output is scheduled together, so once the application is filed the others would be too.
  const caseReference = requireCaseReference(world);
  await expectEnforcementDocument(caseReference, documentTitle('enforcement application', world));
  expect(await countEnforcementDocuments(caseReference, documentTitle(name, world)), `no ${name}`).toBe(0);
});

/** How long pcs-api could take to schedule and file a warrant after the decision: two db-scheduler polls. */
const DECISION_SETTLE_MS = 25_000;

// A rejected application gets no warrant. The application documents were generated when the fee was paid, so
// once they are in the folder and the decision has had time to schedule one, there is no warrant beside them.
Then('no warrant is generated', async ({ world }) => {
  const caseReference = requireCaseReference(world);
  await expectEnforcementDocument(caseReference, documentTitle('enforcement application', world));
  await new Promise(resolve => setTimeout(resolve, DECISION_SETTLE_MS));
  const warrant = documentTitle('warrant of possession', world);
  expect(await countEnforcementDocuments(caseReference, warrant), `no "${warrant}…" document`).toBe(0);
});

Then('a {string} document is generated with the county court seal', async ({ world, $tags }, name: string) => {
  await expectSeal(world, await expectGenerated(world, name), 'County Court', $tags);
});

Then('a {string} document is generated with the High Court seal', async ({ world, $tags }, name: string) => {
  await expectSeal(world, await expectGenerated(world, name), 'High Court', $tags);
});

Then('a {string} document is generated with the {string} seal', async ({ world, $tags }, name: string, seal: string) => {
  await expectSeal(world, await expectGenerated(world, name), seal, $tags);
});

Then('a {string} document is generated without a seal', async ({ world, $tags }, name: string) => {
  await expectSeal(world, await expectGenerated(world, name), undefined, $tags);
});

for (const subject of ['the document', 'it']) {
  Then(`${subject} is stored in the {string} folder in case file view`, async ({ world }, folder: string) => {
    requireEnforcementFolder(folder);
    await expectEnforcementDocument(requireCaseReference(world), requireCurrentDocument(world));
  });

  Then(`${subject} is visible to internal users only`, async ({ world }) => {
    await expectVisibleToInternalUsersOnly(world, requireCurrentDocument(world));
  });

  Then(`${subject} is visible to the claimant and internal users`, async ({ world }) => {
    await expectVisibleToClaimantAndInternalUsers(world, requireCurrentDocument(world));
  });
}

Then('it is visible to {string}', async ({ world }, viewers: string) => {
  const title = requireCurrentDocument(world);
  if (viewers === 'internal users') {
    return expectVisibleToInternalUsersOnly(world, title);
  }
  if (viewers === 'claimant and internal users') {
    return expectVisibleToClaimantAndInternalUsers(world, title);
  }
  throw new Error(`Unknown viewers "${viewers}". Expected "internal users" or "claimant and internal users"`);
});

// The answers the "Enforce the order" journeys in support/enforcement-journeys.ts give, by the question the
// feature names and the field of the enforcement order pcs-api renders the application from.
type ExpectedAnswers = Record<string, Record<string, unknown>>;

const statementOfTruth = (prefix: string) => ({
  [`${prefix}Certification`]: ['CERTIFY'],
  [`${prefix}CompletedBy`]: 'LEGAL_REPRESENTATIVE',
  [`${prefix}FullNameLegalRep`]: expect.any(String),
});

const applicationAnswers: Partial<Record<EnforcementType, ExpectedAnswers>> = {
  'warrant of possession': {
    'what do you want to apply for': { chooseEnforcementType: expect.objectContaining({ valueCode: 'WARRANT' }) },
    'breathing space': { warrantDefendantInBreathingSpace: 'NO' },
    'missed payments': { warrantDefendantMissedPayments: 'YES' },
    'correct name and address': { warrantCorrectNameAndAddress: 'YES' },
    "defendants' dates of birth": { warrantDefendantsDOBKnown: 'NO' },
    'language used': { warrantEnforcementLanguageUsed: 'ENGLISH' },
    'statement of truth': statementOfTruth('warrant'),
  },
  'writ of possession': {
    'what do you want to apply for': { chooseEnforcementType: expect.objectContaining({ valueCode: 'WRIT' }) },
    'correct name and address': { writCorrectNameAndAddress: 'YES' },
    'breathing space': { writDefendantInBreathingSpace: 'NO' },
    'missed payments': { writDefendantMissedPayments: 'YES' },
    'High Court Enforcement Officer hired': { writHasHiredHighCourtEnforcementOfficer: 'NO' },
    // Only asked when an officer was hired; the journey answers that none was.
    'who you hired': { writHceoDetails: undefined },
    'language used': { writEnforcementLanguageUsed: 'ENGLISH' },
    'statement of truth': statementOfTruth('writ'),
  },
};

Then('the {string} document contains {string}', async ({ world, $tags }, name: string, questions: string) => {
  const type = world.applicationType as EnforcementType;
  const answers = applicationAnswers[type];
  if (name !== 'enforcement application' || !answers) {
    return markPending({ summary: `reading the ${name} for a ${type}` }, $tags);
  }
  const title = await expectGenerated(world, name);
  requireDocmosisJournal($tags);
  const order = (await docmosisRender(requireCaseReference(world), title)).data.enforcementOrder ?? {};
  for (const question of questions.split(',').map(item => item.trim())) {
    const expected = answers[question];
    expect(expected, `a known answer for "${question}"`).toBeDefined();
    for (const [field, value] of Object.entries(expected as Record<string, unknown>)) {
      expect(order[field], `"${question}" (${field}) in the ${name}`).toEqual(value);
    }
  }
  noteCheckedOnRenderData(`each answer in the ${name}`);
});

Then('the document is date and time stamped in {string} format', async ({ world, $tags }, format: string) => {
  expect(format).toBe('DD/MM/YYYY HH:MM AM/PM');
  requireDocmosisJournal($tags);
  const render = await docmosisRender(requireCaseReference(world), requireCurrentDocument(world));
  expect(render.data.generatedAt, 'the generation time stamp').toMatch(/^\d{2}\/\d{2}\/\d{4} (0\d|1[0-2]):[0-5]\d (AM|PM)$/);
  noteCheckedOnRenderData('the date and time stamp');
});
