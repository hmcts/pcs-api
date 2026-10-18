// Generated enforcement documents in the Enforcement folder of case file view (features 01, 02 and 05).
import { Then, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
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
  noteStandInTemplate,
} from '../support/case-documents';

/** Reading case file view as the claimant and as an internal user needs the cftlib users and the IDAM simulator. */
const localStackOnly: PendingScope = {
  summary: 'reading case file view as each kind of user is only wired up for the local stack',
};

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

async function expectVisibleToClaimantAndInternalUsers(world: EnforcementWorld, title: string): Promise<void> {
  const caseReference = requireCaseReference(world);
  await expectEnforcementDocument(caseReference, title, 'internal user');
  await expectEnforcementDocument(caseReference, title, 'claimant');
}

async function expectVisibleToInternalUsersOnly(world: EnforcementWorld, title: string): Promise<void> {
  await expectHiddenFrom(requireCaseReference(world), title, 'claimant');
}

Then('an {string} folder is created in case file view', async ({ world, $tags }, folder: string) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
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
    if (!isLocalStack()) {
      return markPending(localStackOnly, $tags);
    }
    requireEnforcementFolder(folder);
    await expectGenerated(world, name);
    noteStandInTemplate(`that the ${name} is unsealed`);
  }
);

Then('both documents are visible to the claimant and internal users', async ({ world, $tags }) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
  expect(world.checkedDocuments, 'the documents the scenario found').toHaveLength(2);
  for (const title of world.checkedDocuments) {
    await expectVisibleToClaimantAndInternalUsers(world, title);
  }
});

Then('no {string} document is generated', async ({ world, $tags }, name: string) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
  // Every application output is scheduled together, so once the application is filed the others would be too.
  const caseReference = requireCaseReference(world);
  await expectEnforcementDocument(caseReference, documentTitle('enforcement application', world));
  expect(await countEnforcementDocuments(caseReference, documentTitle(name, world)), `no ${name}`).toBe(0);
});

Then('a {string} document is generated with the county court seal', async ({ world, $tags }, name: string) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
  await expectGenerated(world, name);
  noteStandInTemplate('the county court seal');
});

Then('a {string} document is generated with the High Court seal', async ({ world, $tags }, name: string) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
  await expectGenerated(world, name);
  noteStandInTemplate('the High Court seal');
});

Then('a {string} document is generated with the {string} seal', async ({ world, $tags }, name: string, seal: string) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
  await expectGenerated(world, name);
  noteStandInTemplate(`the ${seal} seal`);
});

Then('a {string} document is generated without a seal', async ({ world, $tags }, name: string) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
  await expectGenerated(world, name);
  noteStandInTemplate(`that the ${name} is unsealed`);
});

for (const subject of ['the document', 'it']) {
  Then(`${subject} is stored in the {string} folder in case file view`, async ({ world, $tags }, folder: string) => {
    if (!isLocalStack()) {
      return markPending(localStackOnly, $tags);
    }
    requireEnforcementFolder(folder);
    await expectEnforcementDocument(requireCaseReference(world), requireCurrentDocument(world));
  });

  Then(`${subject} is visible to internal users only`, async ({ world, $tags }) => {
    if (!isLocalStack()) {
      return markPending(localStackOnly, $tags);
    }
    await expectVisibleToInternalUsersOnly(world, requireCurrentDocument(world));
  });

  Then(`${subject} is visible to the claimant and internal users`, async ({ world, $tags }) => {
    if (!isLocalStack()) {
      return markPending(localStackOnly, $tags);
    }
    await expectVisibleToClaimantAndInternalUsers(world, requireCurrentDocument(world));
  });
}

Then('it is visible to {string}', async ({ world, $tags }, viewers: string) => {
  if (!isLocalStack()) {
    return markPending(localStackOnly, $tags);
  }
  const title = requireCurrentDocument(world);
  if (viewers === 'internal users') {
    return expectVisibleToInternalUsersOnly(world, title);
  }
  if (viewers === 'claimant and internal users') {
    return expectVisibleToClaimantAndInternalUsers(world, title);
  }
  throw new Error(`Unknown viewers "${viewers}". Expected "internal users" or "claimant and internal users"`);
});
