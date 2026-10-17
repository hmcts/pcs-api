import { mergeTests } from '@playwright/test';
import { createBdd, test as bddTest } from 'playwright-bdd';
import { test as consoleLogTest } from '@utils/test-fixtures';
import { initializeExecutor } from '@utils/controller';
import { initializeEnforcementExecutor, performAction } from '@utils/controller-enforcement';
import { defendantDetails, fieldsMap, moneyMap } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { PageContentValidation } from '@utils/validations/element-validations/pageContent.validation';
import { submitCaseApiData } from '@data/api-data';

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
    async ({ page, world }, use) => {
      initializeExecutor(page);
      initializeEnforcementExecutor(page);
      defendantDetails.length = 0;
      moneyMap.clear();
      fieldsMap.clear();
      await use();
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

export const { Given, When, Then, Step, Before } = createBdd(test);
