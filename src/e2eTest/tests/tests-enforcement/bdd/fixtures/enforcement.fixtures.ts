import { mergeTests } from '@playwright/test';
import { createBdd, test as bddTest } from 'playwright-bdd';
import { test as consoleLogTest } from '@utils/test-fixtures';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction } from '@utils/controller-enforcement';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { submitCaseApiData } from '@data/api-data';
import { CLAIMANT_LEGAL_REPRESENTATIVE, answerCookieQuestion, requireEmailFor, resetSession } from '../support/session';
import { beginScenario, endScenario, recordBrowserRequest } from '../support/demo-continuity';
import { resetClockIfMoved } from '../support/test-clock';
import { demoHoldMs } from '../support/demo-pacing';
import { disablePagePacing, enablePagePacing } from '@utils/page-pacing';
import type { BatchPrintFile, BatchPrintPlan } from '../support/batch-print';

export type EnforcementType =
  | 'warrant of possession'
  | 'writ of possession'
  | 'warrant of restitution'
  | 'writ of restitution';

export type SubmitCasePayload = ReturnType<typeof submitCaseApiData.submitCasePayload>;

/**
 * What one defendant saw of the case: the case data CCD gave them, and whether that came with an XUI page
 * (the legal representative) or not (a citizen, who uses pcs-frontend).
 */
export type DefendantView = { who: string; inXui: boolean; caseData: Record<string, unknown> };

/** Per-scenario state shared between steps. */
export class EnforcementWorld {
  /**
   * Case reference (16 digits, no dashes) of the case this scenario created. The reference quoted in the
   * feature files is illustrative: every scenario creates its own case.
   */
  caseReference?: string;
  /** The claim submitted for that case; the enforcement actions read defendant details from it. */
  submitPayload?: SubmitCasePayload;
  /** Set once a warrant application has been seeded on the case through the API. */
  enforcementSeeded = false;
  /** The enforcement type chosen in "Enforce the order". */
  applicationType?: EnforcementType;
  /** The XUI event this scenario is in, once started. */
  currentEvent?: string;
  /**
   * The event the scenario's journey is about, named by the task its background says exists. Lets a generic
   * step such as "I am on the "Check your answers" screen" know which event's screen it means.
   */
  journey?: string;
  /** The bailiff chosen in "Allocate case to bailiff", by email. */
  chosenBailiff?: string;
  /** Set once the warrant has been approved through "Decide enforcement", so it is live with a number. */
  warrantApproved = false;
  /** The warrant number generated on approval, e.g. WP-00000006. */
  warrantNumber?: string;
  /** The bailiff the scenario allocated the warrant to, if any. */
  allocatedBailiff?: string;
  /** The risk level the scenario chose in "Set risk level". */
  riskLevel?: string;
  /** The note text the scenario typed into "Add a bailiff note". */
  enteredNote?: string;
  /** The eviction date and time the scenario entered in "Schedule eviction". */
  evictionDate?: string;
  /** Set once an eviction has actually been scheduled on the warrant. */
  evictionScheduled = false;
  /** Whether the eviction was scheduled with the court's permission to dispense with service. */
  serviceDispensedWith?: boolean;
  /** Set once the bailiff has marked the notice of eviction as served. */
  noticeServed = false;
  /** The defendant legal representative and citizen defendant given a case role on this scenario's case. */
  defendantLegalRepresentative?: string;
  citizenDefendant?: string;
  /** What each defendant last saw of the case, captured by a "defendant views the case" step. */
  defendantViews?: DefendantView[];
  /** Who the last "a <user> opens the case" step opened it as. A citizen defendant has no XUI session. */
  openedAs?: string;
  /** Whether the claimant said they could attend, once they have answered. */
  claimantConfirmedEviction?: boolean;
  /** The error a start callback gave when the scenario last tried to open an event, if any. */
  startEventError?: string;
  /** The risk questions whose answers the claimant changed in "Confirm the eviction details", as the tab words them. */
  changedRiskQuestions?: string[];
  /** The risk question a scenario is asserting about, as the tab words it. */
  riskQuestion?: string;
  /** The answer the claimant changed `riskQuestion` to. */
  changedRiskAnswer?: string;
  /**
   * The warrant and writ numbers this scenario was given, keyed by the illustrative number the feature file
   * uses for each (e.g. "WP-0001"), in the order they were generated.
   */
  enforcementNumbers: Record<string, string> = {};
  /** The real reference the scenario last searched for in global search. */
  searchedReference?: string;
  /**
   * Values the feature files quote illustratively, mapped to what this scenario actually produced: a warrant
   * number such as "WP-0001" to the number generated on approval, or an order date to the approval date.
   */
  aliases: Record<string, string> = {};
  /** The warrant return the scenario recorded in "Record warrant return", if any. */
  recordedReturn?: { type: string; outcome: string; returnName: string; date: string; time: string };
  /** The status of the warrant just before the scenario last submitted a return. */
  statusBeforeReturn?: string;
  /** pcs-api's label for the return name, and the additional details, of the return the scenario last submitted. */
  submittedReturn?: { returnName: string; additionalDetails: string };
  /**
   * Warrant numbers of the enforcements this scenario closed before applying again, oldest first. Once set,
   * the status steps read the current enforcement (the first the "Enforcement status" tab lists) rather
   * than expecting the case to have only one.
   */
  earlierWarrantNumbers: string[] = [];
  /** Whether pcs-api's submit confirmation asked for the enforcement fee, once an application is submitted. */
  askedToPayFee?: boolean;
  /** The file name prefixes of the generated documents the scenario has found in the Enforcement folder. */
  checkedDocuments: string[] = [];
  /** The generated document the scenario's "the document" and "it" steps refer to. */
  currentDocument?: string;
  /** Batch print (feature 17): the cases set up as allocated to the bailiff, in the order the feature lists them. */
  batchPrintCases: string[] = [];
  /** The real case standing in for each reference the batch print feature quotes. */
  batchPrintCaseFor: Record<string, string> = {};
  /** The cases chosen for the next batch print, in print order. */
  batchPrintSelection: string[] = [];
  /** A chosen case allocated to another bailiff. */
  notAllocatedCase?: string;
  /** The file name of the latest notice of eviction filed on the case. */
  latestNoticeOfEviction?: string;
  /** What pcs-api said the last batch print would contain, and the file it then produced. */
  batchPrintPlan?: BatchPrintPlan;
  batchPrintFile?: BatchPrintFile;
}

type EnforcementFixtures = {
  world: EnforcementWorld;
  enforcementExecutor: void;
};

// mergeTests keeps the browser console capture from utils/test-fixtures alongside the playwright-bdd fixtures.
const base = mergeTests(bddTest, consoleLogTest);

export const test = base.extend<EnforcementFixtures>({
  world: async ({}, use) => {
    await use(new EnforcementWorld());
  },
  // Same per-test set-up and tear-down as the tests-enforcement specs' beforeEach/afterEach.
  enforcementExecutor: [
    async ({ page, context, world }, use, testInfo) => {
      initializeExecutor(page);
      initializeEnforcementExecutor(page);
      resetSession();
      await beginScenario(testInfo);
      // An event submitted in XUI is a POST to the data store, which is how the demo project tells that a
      // case it is holding for the next scenario has been changed.
      context.on('request', request => recordBrowserRequest(request.method(), request.url()));
      // The storage state is the claimant's session; every other user's answer is added when they sign in.
      await answerCookieQuestion(context, requireEmailFor(CLAIMANT_LEGAL_REPRESENTATIVE));
      defendantDetails.length = 0;
      moneyMap.clear();
      fieldsMap.clear();
      const holdMs = demoHoldMs(testInfo);
      if (holdMs > 0) {
        enablePagePacing(page, holdMs);
      }
      await use();
      // A clock this scenario moved is left on the stack for everything that follows, so putting it back comes
      // first; the rest of the tear-down still has to run if that fails, and the failure is still reported.
      try {
        await resetClockIfMoved();
      } finally {
        disablePagePacing();
        defendantDetails.length = 0;
        moneyMap.clear();
        fieldsMap.clear();
        // The demo project keeps this scenario's case, and its case role, when the next scenario can film
        // against it (support/demo-continuity.ts).
        const caseHeldForNextScenario = await endScenario(testInfo, world, context);
        if (world.caseReference && !caseHeldForNextScenario) {
          await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
        }
        PageContentValidation.finaliseTest();
      }
    },
    { auto: true },
  ],
});

export { expect } from '@utils/test-fixtures';

export const { Given, When, Then, Step, Before, BeforeStep, AfterStep } = createBdd(test);
