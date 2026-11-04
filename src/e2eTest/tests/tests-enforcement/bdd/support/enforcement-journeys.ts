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

export const xuiEvents: Record<string, { label: string; firstPageHeader: string }> = {
  'Enforce the order': { label: caseSummary.enforceTheOrderEvent, firstPageHeader: enforcementApplication.mainHeader },
  // The case summary markup calls it "Confirm the eviction date"; pcs-api names the event "Confirm the eviction details".
  'Confirm the eviction date': { label: caseSummary.confirmEvictionEvent, firstPageHeader: confirmEvictionDetails.mainHeader },
  [decideEnforcement.eventName]: { label: decideEnforcement.eventName, firstPageHeader: decideEnforcement.mainHeader },
  'Add a bailiff note': { label: 'Add a bailiff note', firstPageHeader: 'Add bailiff note' },
  'Allocate case to bailiff': { label: 'Allocate case to bailiff', firstPageHeader: 'Find the bailiff\'s name' },
  'Set risk level': { label: 'Set risk level', firstPageHeader: 'Set risk level' },
  'Schedule eviction': { label: 'Schedule eviction', firstPageHeader: 'Schedule eviction' },
  'Reschedule eviction': { label: 'Reschedule eviction', firstPageHeader: 'Reschedule eviction' },
  // CCD caps event names at 30 characters, so pcs-api names this event "Mark eviction notice served".
  'Mark notice of eviction as served': {
    label: 'Mark eviction notice served',
    firstPageHeader: "Mark 'notice of eviction' as served",
  },
  'Record warrant return': { label: 'Record warrant return', firstPageHeader: 'Record warrant return' },
};

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

export async function seedWarrantApplication(world: EnforcementWorld): Promise<void> {
  if (isLocalStack()) {
    await createCaseWithWarrant(world);
    return;
  }
  await performAction('enforceCaseAPI', { data: enforceWarrantApiData.enforceCasePayloadNoJourney });
  world.enforcementSeeded = true;
}

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

/** Waits for the Summary tab first, so "not shown" is only checked once XUI has rendered the tabs, and reloads between tries. */
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

function nextStepSelect(page: Page): Locator {
  const field = caseSummary.nextStepEventList;
  return page.locator(`:has-text("${field}") + select, :has-text("${field}") ~ select`).first();
}

export async function nextStepOptions(page: Page): Promise<string[]> {
  return (await nextStepSelect(page).locator('option').allTextContents()).map(option => option.trim());
}

const DEMO_NEXT_STEP_PAUSE_MS = 800;

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
  // CCD puts a generic message in the error summary and the callback's own in a separate list, so the whole page text is returned.
  return (await page.locator('body').innerText()).replace(/\s+/g, ' ').trim();
}

export async function chooseApplicationType(type: EnforcementType, world: EnforcementWorld, page: Page): Promise<void> {
  world.applicationType = type;
  // Check your answers is validated against every answer recorded in the test, so a second application would be checked against the first's.
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

/** Answering "yes" to missed payments is what matters: "no" gates the "you need permission from a judge" page. */
async function answerBreathingSpaceAndMissedPayments(nextPage: string): Promise<void> {
  await performAction('isDefendantInBreathingSpace', {
    option: defendantInBreathingSpace.noRadioOption,
    nextPage: missedPayments.mainHeader,
  });
  await performAction('missedPayments', { option: missedPayments.yesRadioOption, nextPage });
}

const HOW_THE_DEFENDANTS_RETURNED =
  'The defendants broke the lock on the back door and moved back in on 3 February 2026. '
  + 'A neighbour saw them carrying furniture into the property and has given a witness statement.';
const WITNESS_STATEMENT_DESCRIPTION = "Neighbour's witness statement";

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
  await performAction('reTryOnCallBackError', additionalInformation.continueButton, languageUsed.mainHeader);
  await performAction('selectLanguageUsed', {
    question: languageUsed.whichLanguageUsedQuestion,
    option: languageUsed.englishRadioOption,
    nextPage: statementOfTruth.mainHeader,
  });
}

/** The warrant and writ statement-of-truth pages have the same fields, but the two actions take them under different keys. */
export async function signStatementOfTruth(type: EnforcementType): Promise<void> {
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
  if (type === 'warrant of possession' || type === 'warrant of restitution') {
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

export const PAY_PANEL_TITLE = /Pay .+ for HM Courts and Tribunals Service/;

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

export async function applyForEnforcement(type: EnforcementType, world: EnforcementWorld, page: Page): Promise<boolean> {
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType(type, world, page);
  await answerApplicationQuestions(type, world);
  await signStatementOfTruth(type);
  return submitApplication(page);
}

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

export async function signInAs(user: string, page: Page, _context?: BrowserContext): Promise<void> {
  await signInAsUser(user, page);
}
