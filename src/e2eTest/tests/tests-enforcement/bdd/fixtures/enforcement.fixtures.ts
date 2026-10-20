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

export type DefendantView = { who: string; inXui: boolean; caseData: Record<string, unknown> };

export class EnforcementWorld {
  caseReference?: string;
  submitPayload?: SubmitCasePayload;
  enforcementSeeded = false;
  applicationType?: EnforcementType;
  currentEvent?: string;
  journey?: string;
  chosenBailiff?: string;
  warrantApproved = false;
  warrantNumber?: string;
  allocatedBailiff?: string;
  riskLevel?: string;
  enteredNote?: string;
  evictionDate?: string;
  evictionScheduled = false;
  serviceDispensedWith?: boolean;
  noticeServed = false;
  defendantLegalRepresentative?: string;
  citizenDefendant?: string;
  defendantViews?: DefendantView[];
  openedAs?: string;
  claimantConfirmedEviction?: boolean;
  startEventError?: string;
  changedRiskQuestions?: string[];
  riskQuestion?: string;
  changedRiskAnswer?: string;
  enforcementNumbers: Record<string, string> = {};
  searchedReference?: string;
  aliases: Record<string, string> = {};
  recordedReturn?: { type: string; outcome: string; returnName: string; date: string; time: string };
  statusBeforeReturn?: string;
  submittedReturn?: { returnName: string; additionalDetails: string };
  earlierWarrantNumbers: string[] = [];
  askedToPayFee?: boolean;
  checkedDocuments: string[] = [];
  currentDocument?: string;
  batchPrintCases: string[] = [];
  batchPrintCaseFor: Record<string, string> = {};
  batchPrintSelection: string[] = [];
  notAllocatedCase?: string;
  latestNoticeOfEviction?: string;
  batchPrintPlan?: BatchPrintPlan;
  batchPrintFile?: BatchPrintFile;
}

type EnforcementFixtures = {
  world: EnforcementWorld;
  enforcementExecutor: void;
};

const base = mergeTests(bddTest, consoleLogTest);

export const test = base.extend<EnforcementFixtures>({
  world: async ({}, use) => {
    await use(new EnforcementWorld());
  },
  enforcementExecutor: [
    async ({ page, context, world }, use, testInfo) => {
      initializeExecutor(page);
      initializeEnforcementExecutor(page);
      resetSession();
      await beginScenario(testInfo);
      context.on('request', request => recordBrowserRequest(request.method(), request.url()));
      await answerCookieQuestion(context, requireEmailFor(CLAIMANT_LEGAL_REPRESENTATIVE));
      defendantDetails.length = 0;
      moneyMap.clear();
      fieldsMap.clear();
      const holdMs = demoHoldMs(testInfo);
      if (holdMs > 0) {
        enablePagePacing(page, holdMs);
      }
      await use();
      // A clock this scenario moved outlives it, so it goes back first, and the rest of the tear-down still runs.
      try {
        await resetClockIfMoved();
      } finally {
        disablePagePacing();
        defendantDetails.length = 0;
        moneyMap.clear();
        fieldsMap.clear();
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
