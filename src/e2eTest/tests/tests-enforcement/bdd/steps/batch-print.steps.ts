// Feature 17: batch print enforcement documents across cases (HDPI-8012).
//
// Batch print is a pcs-api API rather than a CCD event (see support/batch-print.ts), so these steps call it
// as the bailiff. Choosing the cases from "My work" stays pending: XUI's task list cannot act on several
// tasks at once, and nothing creates the bailiff's tasks yet (HDPI-8627).
import { BrowserContext, Page } from '@playwright/test';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending } from '../fixtures/pending';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import {
  allocateBailiffViaTestingSupport,
  isLocalStack,
  localDocumentStoreIsUp,
  localIdamUserId,
  localUserName,
  localUsers,
} from '@config/local-stack.config';
import {
  AWAITING_BAILIFF_ALLOCATION,
  AWAITING_SCHEDULING,
  scheduleEvictionAsBailiff,
  setUpWarrantAtStatus,
} from '../support/approve-warrant';
import { requireCaseReference, signInAs } from '../support/enforcement-journeys';
import {
  PRINTED_KINDS,
  PRINT_ENFORCEMENT_DOCUMENTS,
  checkBatchPrint,
  fileEnforcementDocuments,
  filePlaceholderDocument,
  printBatch,
} from '../support/batch-print';

const NOT_ALLOCATED_REASON = 'You are not the allocated bailiff on this case';

/** Points the journeys, which work on one case at a time, at `caseReference`. */
function switchToCase(world: EnforcementWorld, caseReference: string): void {
  world.caseReference = caseReference;
  process.env.CASE_NUMBER = caseReference;
  caseInfo.id = caseReference;
  caseInfo.fid = caseReference.replace(/(.{4})(?=.)/g, '$1-');
}

/** The real case standing in for a reference quoted in the feature file. */
function realCase(world: EnforcementWorld, featureReference: string): string {
  const caseReference = world.batchPrintCaseFor[featureReference];
  if (!caseReference) {
    throw new Error(`Case "${featureReference}" was not set up in the background`);
  }
  return caseReference;
}

async function requireDocumentStore(tags: string[]): Promise<void> {
  if (!isLocalStack() || !(await localDocumentStoreIsUp())) {
    markPending(
      {
        ticket: 'HDPI-8012',
        summary: 'Batch print needs the cftlib bailiff users and dm-store: run against ENVIRONMENT=local with '
          + 'LOCAL_DOC_SERVICES=true',
      },
      tags
    );
  }
}

/** Allocates the case's warrant to another bailiff user, as `allocateBailiff` does for the default one. */
async function allocateTo(caseReference: string, email: string): Promise<void> {
  await allocateBailiffViaTestingSupport(caseReference, {
    userId: await localIdamUserId(email),
    name: localUserName(email),
    email,
  });
}

async function signInAsBailiff(page: Page, context: BrowserContext): Promise<void> {
  await signInAs('bailiff', page, context);
}

Given(
  'I am the allocated bailiff on cases {string}, {string} and {string}',
  async ({ page, context, world, $tags }, first: string, second: string, third: string) => {
    await requireDocumentStore($tags);
    for (const featureReference of [first, second, third]) {
      await setUpWarrantAtStatus(page, context, world, AWAITING_SCHEDULING);
      const caseReference = requireCaseReference(world);
      world.batchPrintCases.push(caseReference);
      world.batchPrintCaseFor[featureReference] = caseReference;
    }
    world.batchPrintSelection = [...world.batchPrintCases];
    await signInAsBailiff(page, context);
  }
);

Given('each case has an eviction scheduled this week', async ({ page, context, world }) => {
  for (const caseReference of world.batchPrintCases) {
    switchToCase(world, caseReference);
    await scheduleEvictionAsBailiff(page, context, world, 1);
    await fileEnforcementDocuments(caseReference);
  }
  approximatePrecondition(
    'HDPI-9566 / HDPI-7354',
    'the warrant and notice of eviction are not generated yet, and the risk assessment is not a document, so '
      + 'each case has placeholders filed in its Enforcement folder through pcs-api testing support'
  );
  switchToCase(world, world.batchPrintCases[0]);
});

Given(
  'case {string} has an original and a rescheduled notice of eviction',
  async ({ world }, featureReference: string) => {
    const caseReference = realCase(world, featureReference);
    await filePlaceholderDocument(caseReference, 'NOTICE_OF_EVICTION', `Original notice of eviction ${caseReference}`);
    world.latestNoticeOfEviction = await filePlaceholderDocument(
      caseReference, 'NOTICE_OF_EVICTION', `Rescheduled notice of eviction ${caseReference}`);
    world.batchPrintSelection = [caseReference];
    approximatePrecondition(
      'HDPI-7549 / HDPI-7354',
      'rescheduling does not generate a new notice of eviction yet, so both notices are placeholders filed '
        + 'through pcs-api testing support'
    );
  }
);

Given(
  'I select a case where I am not the allocated bailiff',
  async ({ page, context, world }) => {
    await setUpWarrantAtStatus(page, context, world, AWAITING_BAILIFF_ALLOCATION);
    const caseReference = requireCaseReference(world);
    await allocateTo(caseReference, localUsers.bailiffAdmin);
    await fileEnforcementDocuments(caseReference);
    world.notAllocatedCase = caseReference;
    world.batchPrintSelection = [...world.batchPrintCases, caseReference];
    await signInAsBailiff(page, context);
  }
);

async function batchPrintSelection(world: EnforcementWorld): Promise<void> {
  expect(world.batchPrintSelection.length, 'no cases have been chosen').toBeGreaterThan(0);
  world.batchPrintPlan = await checkBatchPrint(world.batchPrintSelection);
  world.batchPrintFile = await printBatch(world.batchPrintSelection);
}

When('I choose {string}', async ({ world }, action: string) => {
  expect(action).toBe(PRINT_ENFORCEMENT_DOCUMENTS);
  await batchPrintSelection(world);
});

When('I batch print that case', async ({ world }) => {
  await batchPrintSelection(world);
});

function expectAPdf(world: EnforcementWorld): void {
  const file = world.batchPrintFile;
  expect(file?.status).toBe(200);
  expect(file?.contentType).toContain('application/pdf');
  expect(file?.signature).toBe('%PDF');
}

Then(
  'I am given one file containing, for each case in turn, the warrant, the risk assessment and the notice of eviction',
  async ({ world }) => {
    expectAPdf(world);
    expect(world.batchPrintFile?.includedCases).toEqual(world.batchPrintSelection);
    const plan = world.batchPrintPlan;
    expect(plan?.included.map(printed => printed.caseReference)).toEqual(world.batchPrintSelection);
    for (const printed of plan?.included ?? []) {
      expect(printed.documents.map(document => document.kind), `documents for ${printed.caseReference}`)
        .toEqual(PRINTED_KINDS);
    }
  }
);

Then('only the latest notice of eviction is included', async ({ world }) => {
  expectAPdf(world);
  const printed = world.batchPrintPlan?.included ?? [];
  expect(printed).toHaveLength(1);
  const notices = printed[0].documents.filter(document => document.kind === 'NOTICE_OF_EVICTION');
  expect(notices.map(notice => notice.fileName)).toEqual([world.latestNoticeOfEviction]);
});

Then('that case is left out and I am told why', async ({ world }) => {
  const notMine = world.notAllocatedCase;
  expect(notMine, 'no unallocated case was chosen').toBeDefined();
  expect(world.batchPrintPlan?.excluded).toEqual([
    { caseReference: notMine, code: 'NOT_ALLOCATED', reason: NOT_ALLOCATED_REASON },
  ]);
  expect(world.batchPrintPlan?.included.map(printed => printed.caseReference)).toEqual(world.batchPrintCases);
  expectAPdf(world);
  expect(world.batchPrintFile?.includedCases).toEqual(world.batchPrintCases);
  expect(world.batchPrintFile?.excludedCases).toEqual([`${notMine}=NOT_ALLOCATED`]);
});
