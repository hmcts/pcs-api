import { mergeTests } from '@playwright/test';
import { createBdd, test as bddTest } from 'playwright-bdd';
import { test as consoleLogTest } from '@utils/test-fixtures';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction } from '@utils/controller-enforcement';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { submitCaseApiData } from '@data/api-data';
import { resetSignedInRole } from '../support/session';
import { demoHoldMs } from '../support/demo-pacing';
import { disablePagePacing, enablePagePacing } from '@utils/page-pacing';

export type EnforcementType =
  | 'warrant of possession'
  | 'writ of possession'
  | 'warrant of restitution'
  | 'writ of restitution';

export type SubmitCasePayload = ReturnType<typeof submitCaseApiData.submitCasePayload>;

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
  /** Whether the claimant said they could attend, once they have answered. */
  claimantConfirmedEviction?: boolean;
  /** The error a start callback gave when the scenario last tried to open an event, if any. */
  startEventError?: string;
  /**
   * Warrant numbers of the enforcements this scenario closed before applying again, oldest first. Once set,
   * the status steps read the current enforcement (the first the "Enforcement status" tab lists) rather
   * than expecting the case to have only one.
   */
  earlierWarrantNumbers: string[] = [];
  /** The illustrative warrant numbers in the feature files (e.g. "WP-0001"), mapped to the numbers pcs-api gave. */
  warrantAliases: Record<string, string> = {};
  /** Whether pcs-api's submit confirmation asked for the enforcement fee, once an application is submitted. */
  askedToPayFee?: boolean;
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
    async ({ page, world }, use, testInfo) => {
      initializeExecutor(page);
      initializeEnforcementExecutor(page);
      resetSignedInRole();
      defendantDetails.length = 0;
      moneyMap.clear();
      fieldsMap.clear();
      const holdMs = demoHoldMs(testInfo);
      if (holdMs > 0) {
        enablePagePacing(page, holdMs);
      }
      await use();
      disablePagePacing();
      defendantDetails.length = 0;
      moneyMap.clear();
      fieldsMap.clear();
      if (world.caseReference) {
        await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
      }
      PageContentValidation.finaliseTest();
    },
    { auto: true },
  ],
});

export { expect } from '@utils/test-fixtures';

export const { Given, When, Then, Step, Before, AfterStep } = createBdd(test);
