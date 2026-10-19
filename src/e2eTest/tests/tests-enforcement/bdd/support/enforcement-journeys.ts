/**
 * Thin wrappers that compose the existing enforcement actions (utils/registry/registry-enforcement) into the
 * journeys the feature files describe. The action arguments follow tests-enforcement/*.spec.ts.
 */
import { BrowserContext, Locator, Page } from '@playwright/test';
import { expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld, SubmitCasePayload } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition } from '../fixtures/pending';
import { LONG_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';
import { getCaseTypeId } from '@utils/common/caseType.utils';
import { performAction as performCommonAction } from '@utils/controller';
import { performAction, performValidation } from '@utils/controller-enforcement';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { defendantDetails } from '@utils/actions/custom-actions/custom-actions-enforcement/enforcement.action';
import { createCaseApiData, enforceWarrantApiData, submitCaseApiData } from '@data/api-data';
import { caseSummary } from '@data/page-data';
import { CYAStore } from '@utils/validations/custom-validations/CYA/cyaPage.validation';
import { backgroundSetUp } from './demo-continuity';
import { ensureSignedIn, signInAs as signInAsUser } from './session';
import {
  basicClaimFixtureClaim,
  createCaseFromFixture,
  isLocalStack,
  testCaseFixtures,
  type TestCaseFixture,
  waitForServiceRequest,
} from '@config/local-stack.config';
import {
  checkYourAnswers,
  evidenceUpload,
  provideEvidence,
  theNICEWillChoose,
  warrantOfRestitutionAnyoneAtPropertyRiskIntro,
} from '@data/page-data/page-data-enforcement';
import {
  additionalInformation,
  confirmDefendantsDOB,
  confirmEvictionDetails,
  confirmHCEOfficer,
  defendantInBreathingSpace,
  enforcementApplication,
  languageUsed,
  livingInTheProperty,
  missedPayments,
  nameAndAddressForEviction,
  peopleYouWantToEvict,
  propertyAccessDetails,
  shareEvidenceWithJudge,
  statementOfTruth,
  vulnerableAdultsChildren,
} from '@data/page-data-figma/page-data-enforcement-figma';
import { explainHowDefendantsReturned } from '@data/page-data-figma/page-data-enforcement-figma/explainHowDefendantsReturned.page.data';
import { decideEnforcement } from './decide-enforcement';
import { demoEmphasise, demoPause } from '@utils/page-pacing';


/** XUI events the steps can drive today, keyed by the name the feature files use. */
export const xuiEvents: Record<string, { label: string; firstPageHeader: string }> = {
  'Enforce the order': { label: caseSummary.enforceTheOrderEvent, firstPageHeader: enforcementApplication.mainHeader },
  // The requirements and the case summary markup call it "Confirm the eviction date";
  // pcs-api names the event "Confirm the eviction details".
  'Confirm the eviction date': { label: caseSummary.confirmEvictionEvent, firstPageHeader: confirmEvictionDetails.mainHeader },
  // A caseworker event: sign in as a hearing centre admin first.
  [decideEnforcement.eventName]: { label: decideEnforcement.eventName, firstPageHeader: decideEnforcement.mainHeader },
  // pcs-api AddBailiffNote: the event name and its one page's label.
  'Add a bailiff note': { label: 'Add a bailiff note', firstPageHeader: 'Add bailiff note' },
  // pcs-api AllocateBailiff and SetRiskLevel: the event names and their first page labels.
  'Allocate case to bailiff': { label: 'Allocate case to bailiff', firstPageHeader: 'Find the bailiff\'s name' },
  'Set risk level': { label: 'Set risk level', firstPageHeader: 'Set risk level' },
  // pcs-api ScheduleEviction, RescheduleEviction and MarkNoticeOfEvictionServed: event names and first page labels.
  // They are offered to bailiff staff only once the warrant reaches the right status (HDPI-7419), which the
  // suite cannot set up yet, and there are no bailiff test users (HDPI-7539), so features 05, 07 and 08 stay @pending.
  'Schedule eviction': { label: 'Schedule eviction', firstPageHeader: 'Schedule eviction' },
  'Reschedule eviction': { label: 'Reschedule eviction', firstPageHeader: 'Reschedule eviction' },
  // CCD caps event names at 30 characters, so pcs-api names this event "Mark eviction notice served".
  'Mark notice of eviction as served': {
    label: 'Mark eviction notice served',
    firstPageHeader: "Mark 'notice of eviction' as served",
  },
  // pcs-api RecordWarrantReturn: the event name and its one page's label.
  'Record warrant return': { label: 'Record warrant return', firstPageHeader: 'Record warrant return' },
};

/** Creates and submits a claim through the API, pays the issue fee if needed, and records the case on the world. */
export async function createIssuedCase(world: EnforcementWorld): Promise<void> {
  await backgroundSetUp(async () => {
    const submitPayload = isLocalStack() ? await createIssuedCaseLocally() : await createIssuedCaseFromClaimPayload();
    await performAction('getDefendantDetails', {
      defendant1NameKnown: submitPayload.defendant1.nameKnown,
      additionalDefendants: submitPayload.addAnotherDefendant,
      payLoad: submitPayload,
    });
    world.caseReference = caseInfo.id;
    world.submitPayload = submitPayload as SubmitCasePayload;
  });
}

async function createIssuedCaseFromClaimPayload(): Promise<SubmitCasePayload> {
  const submitPayload = submitCaseApiData.submitCasePayload();
  await performAction('createCaseAPI', { data: createCaseApiData.createCasePayload });
  await performAction('submitCaseAPI', { data: submitPayload });
  if (caseInfo.state !== 'CASE_ISSUED') {
    await performCommonAction('updatePaymentAPI');
  }
  return submitPayload;
}

/**
 * The claim payloads reference uploaded documents, and cftlib has no dm-store for CDAM to store them in,
 * so a local run builds the case from the `Create Case Make A Claim Basic Case` fixture and pays its issue
 * fee. The fixture's claim is simpler than `submitCasePayload()` — one defendant whose name is known.
 */
async function createIssuedCaseLocally(): Promise<SubmitCasePayload> {
  const created = await createCaseFromFixture(testCaseFixtures.basicClaim);
  process.env.CASE_NUMBER = created.id;
  caseInfo.id = created.id;
  caseInfo.fid = created.id.replace(/(.{4})(?=.)/g, '$1-');
  caseInfo.state = created.state;
  if (created.state !== 'CASE_ISSUED') {
    // The service request is created on a db-scheduler task, so it appears after the case does.
    await waitForServiceRequest(created.id);
    await performCommonAction('updatePaymentAPI');
    caseInfo.state = 'CASE_ISSUED';
  }
  return basicClaimFixtureClaim as unknown as SubmitCasePayload;
}

/**
 * Gives the scenario a warrant of possession application, as the warrant of restitution spec does: on preview
 * and AAT by submitting `enforceTheOrder` through the API on the scenario's case. A local run creates a new case
 * from the Create-Enforcement-Warrant fixture instead, which is the route the local suite has been run with.
 */
export async function seedWarrantApplication(world: EnforcementWorld): Promise<void> {
  if (isLocalStack()) {
    await createCaseWithWarrant(world);
    return;
  }
  await performAction('enforceCaseAPI', { data: enforceWarrantApiData.enforceCasePayloadNoJourney });
  world.enforcementSeeded = true;
}

/**
 * A case issued with a warrant of possession application already submitted, i.e. an enforcement in
 * "Awaiting payment". On a local stack this is one `createTestCase` submit; on AAT it is a claim followed
 * by an `enforceTheOrder` API call.
 */
export async function createCaseWithWarrant(world: EnforcementWorld): Promise<void> {
  await backgroundSetUp(async () => {
    if (isLocalStack()) {
      await createCaseFromEnforcementFixture(world, testCaseFixtures.enforcementWarrant);
      return;
    }
    await createIssuedCase(world);
    await seedWarrantApplication(world);
  });
}

/**
 * A case issued with a writ of possession application already submitted, awaiting payment, created from the
 * Create-Enforcement-Writ fixture through the `createTestCase` event. The event exists wherever testing support
 * is on (a local stack booted with ENABLE_TESTING_SUPPORT=true, preview and AAT); there is no API payload for a
 * writ application.
 */
export async function createCaseWithWrit(world: EnforcementWorld): Promise<void> {
  await backgroundSetUp(async () => createCaseFromEnforcementFixture(world, testCaseFixtures.enforcementWrit));
}

async function createCaseFromEnforcementFixture(world: EnforcementWorld, fixture: TestCaseFixture): Promise<void> {
  const created = await createCaseFromFixture(fixture);
  process.env.CASE_NUMBER = created.id;
  caseInfo.id = created.id;
  caseInfo.fid = created.id.replace(/(.{4})(?=.)/g, '$1-');
  caseInfo.state = created.state;
  world.caseReference = created.id;
  world.submitPayload = basicClaimFixtureClaim as unknown as SubmitCasePayload;
  await performAction('getDefendantDetails', {
    defendant1NameKnown: basicClaimFixtureClaim.defendant1.nameKnown,
    additionalDefendants: basicClaimFixtureClaim.addAnotherDefendant,
    payLoad: basicClaimFixtureClaim,
  });
  world.enforcementSeeded = true;
}

export async function openCaseSummary(page: Page): Promise<void> {
  await ensureSignedIn(page);
  await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}/cases/case-details/PCS/${getCaseTypeId()}/${process.env.CASE_NUMBER}#Summary`);
  await page
    .waitForURL(`${process.env.MANAGE_CASE_BASE_URL}/**/**/**/**/**#Summary`, { timeout: LONG_TIMEOUT })
    .catch(() => undefined);
  if (isOnCaseSummary(page)) {
    return;
  }
  // XUI sometimes opens a just-created case on its first tab and drops the #Summary fragment; select the tab.
  const summaryTab = page.getByRole('tab', { name: caseSummary.mainHeader, exact: true });
  await expect(summaryTab).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  await summaryTab.click();
  await expect.poll(() => isOnCaseSummary(page), { timeout: LONG_TIMEOUT }).toBe(true);
}

function isOnCaseSummary(page: Page): boolean {
  const url = page.url();
  return url.includes('/cases/case-details/') && url.includes(String(process.env.CASE_NUMBER)) && url.endsWith('#Summary');
}

/**
 * Asserts whether a case tab is on the open case. Waits for the Summary tab first, so "not shown" is only
 * checked once XUI has rendered the tabs.
 *
 * A tab the case gains is waited for across reloads: set-up goes through the APIs, so the case can be opened
 * in the same moment the event that adds the tab is recorded, and the page already rendered never gains it.
 */
export async function expectCaseTab(page: Page, tabName: string, shown: boolean): Promise<void> {
  await expect(page.getByRole('tab', { name: caseSummary.mainHeader, exact: true })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  const tab = page.getByRole('tab', { name: tabName, exact: true });
  if (!shown) {
    await expect(tab).toHaveCount(0);
    return;
  }
  await expect
    .poll(
      async () => {
        const count = await tab.count();
        if (count === 0) {
          await page.reload({ waitUntil: 'domcontentloaded' }).catch(() => undefined);
          await expect(page.getByRole('tab', { name: caseSummary.mainHeader, exact: true }))
            .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
        }
        return count;
      },
      { timeout: VERY_LONG_TIMEOUT, intervals: [2_000] }
    )
    .toBeGreaterThan(0);
}

/** The case summary "Next step" drop down (same locator as the select action). */
function nextStepSelect(page: Page): Locator {
  const field = caseSummary.nextStepEventList;
  return page.locator(`:has-text("${field}") + select, :has-text("${field}") ~ select`).first();
}

/** Option labels in the case summary "Next step" drop down. */
export async function nextStepOptions(page: Page): Promise<string[]> {
  return (await nextStepSelect(page).locator('option').allTextContents()).map(option => option.trim());
}

/** How long the demo videos show the chosen event before Go is clicked. */
const DEMO_NEXT_STEP_PAUSE_MS = 800;

/** Chooses an event in the "Next step" drop down. The demo videos show the drop down being chosen from. */
async function selectNextStep(label: string): Promise<void> {
  await demoEmphasise(nextStepSelect);
  await performAction('select', caseSummary.nextStepEventList, label);
  await demoPause(DEMO_NEXT_STEP_PAUSE_MS);
}

export async function startEvent(eventName: string): Promise<void> {
  const event = xuiEvents[eventName];
  await selectNextStep(event.label);
  await performAction('clickButton', caseSummary.go);
  await performValidation('mainHeader', event.firstPageHeader);
}

/**
 * Selects an event and clicks Go without requiring its first page to open.
 *
 * The enforcement events are gated by start callbacks, not by hiding the event, so an event that is not
 * available is still offered in the drop down and refused once Go is clicked. Returns the callback error
 * if there was one.
 */
export async function startEventAllowingRefusal(page: Page, eventName: string): Promise<string | undefined> {
  const event = xuiEvents[eventName];
  await selectNextStep(event.label);
  await performAction('clickButton', caseSummary.go);

  const errors = page.locator('.govuk-error-summary, .error-summary, #errors');
  const heading = page.getByRole('heading', { name: event.firstPageHeader });
  await expect
    .poll(async () => (await errors.count()) > 0 || (await heading.count()) > 0, { timeout: VERY_LONG_TIMEOUT })
    .toBe(true);

  if (!(await errors.count())) {
    return undefined;
  }
  // CCD puts a generic message in the error summary and the callback's own message in a separate list, so
  // the whole page text is returned and the caller matches against it.
  return (await page.locator('body').innerText()).replace(/\s+/g, ' ').trim();
}

export async function chooseApplicationType(type: EnforcementType, world: EnforcementWorld, page: Page): Promise<void> {
  world.applicationType = type;
  // The check your answers validation compares the page with every answer recorded in the test, so a second
  // application in the same scenario would be checked against the first one's answers too.
  CYAStore.getInstance().clearAll();
  switch (type) {
    case 'warrant of possession':
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfPossessionRadioOption,
        nextPage: defendantInBreathingSpace.mainHeader,
      });
      await answerBreathingSpaceAndMissedPayments(nameAndAddressForEviction.mainHeader);
      return;
    case 'writ of possession':
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.writOfPossessionRadioOption,
        question1: enforcementApplication.claimTransferredToHighCourtQuestionHidden,
        question2: enforcementApplication.genAppSuccessfulQuestionHidden,
        option1: enforcementApplication.yesRadioOptionHidden,
        nextPage: nameAndAddressForEviction.mainHeader,
      });
      return;
    case 'warrant of restitution':
      // pcs-api only offers restitution once a warrant has been submitted on the case (EnforceTheOrder.start).
      if (!world.enforcementSeeded) {
        approximatePrecondition('HDPI-7419', 'warrant of restitution needs an earlier warrant; seeded a warrant application through the API');
        await seedWarrantApplication(world);
        await openCaseSummary(page);
        await startEvent('Enforce the order');
      }
      await performAction('selectApplicationType', {
        question: enforcementApplication.typeOfApplicationQuestion,
        option: enforcementApplication.warrantOfRestitutionRadioOptionDynamic,
        nextPage: peopleYouWantToEvict.mainHeaderWarrantOfRestitutionDynamic,
      });
      return;
    default:
      throw new Error(`No "Enforce the order" journey for ${type}`);
  }
}

export async function answerApplicationQuestions(type: EnforcementType, world: EnforcementWorld): Promise<void> {
  switch (type) {
    case 'warrant of possession':
      return answerWarrantQuestions(world);
    case 'writ of possession':
      return answerWritQuestions(world);
    case 'warrant of restitution':
      return answerWarrantOfRestitutionQuestions();
    default:
      throw new Error(`No "Enforce the order" journey for ${type}`);
  }
}

/** Warrant of possession, no risk to the bailiff (enforcement.spec.ts "risk to Bailiff [No]"). */
async function answerWarrantQuestions(world: EnforcementWorld): Promise<void> {
  await performAction('selectNameAndAddressForEviction', {
    question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
    option: nameAndAddressForEviction.yesRadioOption,
    defendant1NameKnown: requireSubmitPayload(world).defendant1.nameKnown,
    nextPage: confirmDefendantsDOB.mainHeader,
  });
  await performAction('confirmDefendantsDOB', {
    question: confirmDefendantsDOB.defendantsDOBQuestion,
    option: confirmDefendantsDOB.noRadioOption,
    nextPage: livingInTheProperty.mainHeader,
  });
  await performAction('selectEveryoneLivingAtTheProperty', {
    question: livingInTheProperty.riskToBailiffQuestion,
    option: livingInTheProperty.noRadioOption,
    nextPage: vulnerableAdultsChildren.mainHeader,
  });
  await performAction('selectVulnerablePeopleInTheProperty', {
    question: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion,
    option: vulnerableAdultsChildren.noRadioOption,
    confirm: vulnerableAdultsChildren.confirmVulnerablePeopleHiddenQuestion,
    peopleOption: vulnerableAdultsChildren.vulnerableAdultsHiddenRadioOption,
    label: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextLabel,
    input: vulnerableAdultsChildren.howAreTheyVulnerableHiddenTextInput,
    nextPage: propertyAccessDetails.mainHeader,
  });
  await performAction('provideDetailsBasedOnRadioOptionSelection', {
    question: propertyAccessDetails.accessToThePropertyQuestion,
    option: propertyAccessDetails.noRadioOption,
    nextPage: additionalInformation.mainHeader,
  });
  await performAction('provideDetailsBasedOnRadioOptionSelection', {
    question: additionalInformation.anythingElseQuestion,
    option: additionalInformation.noRadioOption,
    nextPage: languageUsed.mainHeader,
  });
  await performAction('selectLanguageUsed', {
    question: languageUsed.whichLanguageUsedQuestion,
    option: languageUsed.englishRadioOption,
    nextPage: statementOfTruth.mainHeader,
  });
}

/** Writ of possession, no HCEO hired, nothing repaid (enforcement.writJourney.spec.ts). */
async function answerWritQuestions(world: EnforcementWorld): Promise<void> {
  await performAction('selectNameAndAddressForEviction', {
    question: nameAndAddressForEviction.nameAndAddressPageForEvictionQuestion,
    option: nameAndAddressForEviction.yesRadioOption,
    defendant1NameKnown: requireSubmitPayload(world).defendant1.nameKnown,
    nextPage: defendantInBreathingSpace.mainHeader,
  });
  await answerBreathingSpaceAndMissedPayments(confirmHCEOfficer.mainHeader);
  await performAction('selectHaveHiredHCEO', {
    question: confirmHCEOfficer.haveYouHiredHCEOQuestion,
    option: confirmHCEOfficer.noRadioOption,
    nextPage: theNICEWillChoose.mainHeader,
  });
  await performAction('clickButton', theNICEWillChoose.continueButton);
  await performValidation('mainHeader', languageUsed.mainHeader);
  await performAction('selectLanguageUsed', {
    question: languageUsed.whichLanguageUsedQuestion,
    option: languageUsed.englishRadioOption,
    nextPage: statementOfTruth.mainHeader,
  });
}

/**
 * "Is the defendant currently in a breathing space?" then "Missed payments": two pages both the warrant and
 * the writ journeys pass through. Answering yes to missed payments is what matters — answering no is the
 * only thing that gates the "you need permission from a judge" information page
 * (CannotApplyForWarrantInfoPage). The breathing-space answer has no show condition.
 */
async function answerBreathingSpaceAndMissedPayments(nextPage: string): Promise<void> {
  await performAction('isDefendantInBreathingSpace', {
    option: defendantInBreathingSpace.noRadioOption,
    nextPage: missedPayments.mainHeader,
  });
  await performAction('missedPayments', { option: missedPayments.yesRadioOption, nextPage });
}

/**
 * What the claimant writes about the defendants' return, and the description of the evidence they file.
 * The character-limit checks that fill these fields with a string one over the limit live in
 * enforcement.warrantOfRestitution.spec.ts; the journey the demo videos film answers them as a claimant
 * would.
 */
const HOW_THE_DEFENDANTS_RETURNED =
  'The defendants broke the lock on the back door and moved back in on 3 February 2026. '
  + 'A neighbour saw them carrying furniture into the property and has given a witness statement.';
const WITNESS_STATEMENT_DESCRIPTION = "Neighbour's witness statement";

/**
 * Warrant of restitution with the seeded warrant's answers left as they are
 * (enforcement.warrantOfRestitution.spec.ts "all NO selection"). The journey ends on placeholder pages today.
 */
async function answerWarrantOfRestitutionQuestions(): Promise<void> {
  const seeded = enforceWarrantApiData.enforceCasePayloadNoJourney;
  await performAction('reTryOnCallBackError', peopleYouWantToEvict.continueButton, shareEvidenceWithJudge.mainHeader);
  await performAction('reTryOnCallBackError', shareEvidenceWithJudge.continueButton, explainHowDefendantsReturned.mainHeader);
  await performAction('provideHowDefendantReturnToProperty', {
    label: explainHowDefendantsReturned.howDidTheDefendantsReturnToThePropertyTextLabel,
    input: HOW_THE_DEFENDANTS_RETURNED,
    nextPage: provideEvidence.mainHeader,
  });
  await performAction('uploadEvidenceThatDefendantsAreAtProperty', {
    documents: [
      {
        type: evidenceUpload.witnessStatementDropDownInput,
        fileName: 'witnessStatement.pdf',
        description: WITNESS_STATEMENT_DESCRIPTION,
        docType: evidenceUpload.typeOfDocumentHiddenTextLabel,
        label: evidenceUpload.shortDescriptionHiddenTextLabel,
      },
    ],
    nextPage: warrantOfRestitutionAnyoneAtPropertyRiskIntro.mainHeader,
  });
  await performAction('reTryOnCallBackError', warrantOfRestitutionAnyoneAtPropertyRiskIntro.continueButton, livingInTheProperty.mainHeader);
  await performAction('validatePrePopulatedData', {
    testPage: livingInTheProperty.mainHeader,
    inputData: [{ type: 'radio', inputRadioQuestion: livingInTheProperty.riskToBailiffQuestion, expectedAnswer: seeded.warrantAnyRiskToBailiff }],
  });
  await performAction('reTryOnCallBackError', livingInTheProperty.continueButton, vulnerableAdultsChildren.mainHeader);
  await performAction('validatePrePopulatedData', {
    testPage: vulnerableAdultsChildren.mainHeader,
    inputData: [{ type: 'radio', inputRadioQuestion: vulnerableAdultsChildren.IsAnyOneLivingAtThePropertyQuestion, expectedAnswer: seeded.vulnerablePeoplePresent }],
  });
  await performAction('reTryOnCallBackError', vulnerableAdultsChildren.continueButton, propertyAccessDetails.mainHeader);
  await performAction('validatePrePopulatedData', {
    testPage: propertyAccessDetails.mainHeader,
    inputData: [{ type: 'radio', inputRadioQuestion: propertyAccessDetails.accessToThePropertyQuestion, expectedAnswer: seeded.warrantIsDifficultToAccessProperty }],
  });
  await performAction('reTryOnCallBackError', propertyAccessDetails.continueButton, additionalInformation.mainHeader);
}

/**
 * Statement of truth signed by the claimant's legal representative; lands on check your answers.
 *
 * The warrant and writ pages (pcs-api StatementOfTruthPage, StatementOfTruthWritPage) have the same fields:
 * the "I certify that:" box, "Completed by", the legal representative's agreement box, then full name, firm
 * and position. The two actions take those under different keys: `selectStatementOfTruth` reads the
 * certification from `selectCheckbox` and the agreement from `option2`, `selectStatementOfTruthWrit` reads
 * them from `option1` and `claimantBelieveCheckBox`.
 */
export async function signStatementOfTruth(type: 'warrant of possession' | 'writ of possession'): Promise<void> {
  await performAction('validateAmountToRePayTable', { headerName: statementOfTruth.mainHeader });
  const signedByLegalRepresentative = {
    question: statementOfTruth.completedByParagraph,
    option: statementOfTruth.claimantLegalRepresentativeRadioOption,
    label: statementOfTruth.fullNameHiddenTextLabel,
    input: statementOfTruth.fullNameHiddenTextInput,
    label1: statementOfTruth.nameOfFirmHiddenTextLabel,
    input1: statementOfTruth.nameOfFirmHiddenTextInput,
    label2: statementOfTruth.positionOrOfficeHeldHiddenTextLabel,
    input2: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
    nextPage: checkYourAnswers.mainHeader,
  };
  if (type === 'warrant of possession') {
    await performAction('selectStatementOfTruth', {
      ...signedByLegalRepresentative,
      selectCheckbox: statementOfTruth.iCertifyCheckboxDynamic,
      option2: statementOfTruth.signThisStatementHiddenCheckbox,
    });
  } else {
    await performAction('selectStatementOfTruthWrit', {
      ...signedByLegalRepresentative,
      option1: statementOfTruth.iCertifyCheckboxDynamic,
      claimantBelieveCheckBox: statementOfTruth.theClaimantBelievesHiddenCheckBox,
    });
  }
}

/** The heading pcs-api's submit confirmation shows when the application still owes its fee. */
export const PAY_PANEL_TITLE = /Pay .+ for HM Courts and Tribunals Service/;

/**
 * Submits from check your answers and returns to the case. pcs-api shows a confirmation page asking for the
 * fee when the application owes one, and none for a free application, which goes straight back to the case.
 * Returns whether the confirmation asked for the fee.
 */
export async function submitApplication(page: Page): Promise<boolean> {
  await performAction('clickButton', checkYourAnswers.submitApplicationButton);
  const closeConfirmation = page.getByRole('button', { name: 'Close and Return to case details' });
  const updatedBanner = page.locator('div.alert-message');
  await expect(closeConfirmation.or(updatedBanner).first()).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  let askedToPayFee = false;
  if (await closeConfirmation.isVisible()) {
    askedToPayFee = await page.getByText(PAY_PANEL_TITLE).first().isVisible();
    await closeConfirmation.click();
  }
  await performValidation('bannerAlert', 'Case #.* has been updated with event: Enforce the order');
  return askedToPayFee;
}

/** The case this scenario created. Fails with a clear message if no "Given a PCS case ..." step ran. */
export function requireCaseReference(world: EnforcementWorld): string {
  if (!world.caseReference) {
    throw new Error('No case has been created in this scenario: start with "Given a PCS case ..."');
  }
  return world.caseReference;
}

function requireSubmitPayload(world: EnforcementWorld): SubmitCasePayload {
  if (!world.submitPayload) {
    throw new Error('No case has been created in this scenario: start with "Given a PCS case ..."');
  }
  return world.submitPayload;
}

export { CLAIMANT_LEGAL_REPRESENTATIVE, canSignInAs, signInRoleFor } from './session';

/**
 * Signs in to XUI as `user`, a sign-in role or the feature files' name for one, unless the browser already is
 * that user, and expects them for the steps that follow (see session.ts).
 */
export async function signInAs(user: string, page: Page, _context?: BrowserContext): Promise<void> {
  await signInAsUser(user, page);
}
