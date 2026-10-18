/**
 * Further enforcement on the same case (features 01 and 15, HDPI-8424).
 *
 * A closed warrant is reached by submitting the real events through the data store API: a seeded application,
 * "Decide enforcement", the allocate-bailiff testing-support shortcut and then a final return through "Record
 * warrant return", which a hearing centre admin can submit. Nothing else puts a warrant into "Closed". Further
 * applications made as set-up go through the API too; the "Enforce the order" journeys a scenario's When is about
 * stay in XUI.
 */
import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import {
  AWAITING_SCHEDULING,
  allocateBailiff,
  approveWarrantAsCaseworker,
  expectClaimantNext,
  recordReturnAsHearingCentreAdmin,
  setUpWarrantAtStatus,
} from './approve-warrant';
import {
  answerApplicationQuestions,
  chooseApplicationType,
  openCaseSummary,
  requireCaseReference,
  signStatementOfTruth,
  startEvent,
  submitApplication,
} from './enforcement-journeys';
import { EnforcementRow, enforcementRows } from './enforcement-status-tab';
import { chooseFromList, submitEventAs } from './event-api';
import { CLAIMANT_LEGAL_REPRESENTATIVE, expectSignedInAs, requireEmailFor } from './session';
import { statementOfTruth } from '@data/page-data-figma/page-data-enforcement-figma';

/**
 * Puts a closed warrant of possession on a new case: approved, allocated, then closed by a final return
 * named `returnName`. Records the warrant's number on the world as an earlier enforcement and expects the
 * claimant's legal representative next.
 */
export async function closeWarrantWithReturn(world: EnforcementWorld, returnName: string): Promise<void> {
  await setUpWarrantAtStatus(world, AWAITING_SCHEDULING);
  await closeApprovedWarrant(world, returnName);
}

/**
 * Closes the case's approved warrant with a final return named `returnName`. "Record warrant return" needs a
 * warrant in "Awaiting scheduling" or later, so a warrant nobody has been allocated is first allocated to the
 * local bailiff through testing support. Records the warrant's number on the world as an earlier enforcement
 * and expects the claimant's legal representative next.
 */
export async function closeApprovedWarrant(world: EnforcementWorld, returnName: string): Promise<void> {
  expect(world.warrantApproved, 'an approved warrant to close').toBe(true);
  try {
    if (!world.allocatedBailiff) {
      await allocateBailiff(world);
    }
    await recordReturnAsHearingCentreAdmin(returnName);
  } finally {
    expectClaimantNext();
  }
  expect(world.warrantNumber, 'a warrant number generated when the first warrant was approved').toBeDefined();
  world.earlierWarrantNumbers.push(world.warrantNumber as string);
  world.warrantNumber = undefined;
  world.warrantApproved = false;
  world.allocatedBailiff = undefined;
}

/**
 * The claimant applies for a further warrant of possession through "Enforce the order". Records on the
 * world whether pcs-api's confirmation asked for the fee.
 */
export async function applyForFurtherWarrant(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType('warrant of possession', world, page);
  await answerApplicationQuestions('warrant of possession', world);
  await signStatementOfTruth('warrant of possession');
  world.askedToPayFee = await submitApplication(page);
}

/**
 * The claimant applies for a warrant of restitution through "Enforce the order", which pcs-api only offers once
 * the case has had a warrant. The journey ends on the "Anything else that could help with the eviction"
 * placeholder, with no statement of truth (OPEN-QUESTIONS 30), so it goes from there to check your answers.
 * Restitution has no fee, so the application starts in "Awaiting review" (OPEN-QUESTIONS 60).
 */
export async function applyForWarrantOfRestitution(page: Page, world: EnforcementWorld): Promise<void> {
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType('warrant of restitution', world, page);
  await answerApplicationQuestions('warrant of restitution', world);
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Submit application', exact: true }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  world.askedToPayFee = await submitApplication(page);
}

/** The statement of truth `signStatementOfTruth` signs, as "Enforce the order" stores it for a warrant and a writ. */
function signedByLegalRepresentative(prefix: 'warrant' | 'writ'): Record<string, unknown> {
  return {
    [`${prefix}Certification`]: ['CERTIFY'],
    [`${prefix}CompletedBy`]: 'LEGAL_REPRESENTATIVE',
    [`${prefix}AgreementClaimantLegalRep`]: ['AGREED'],
    [`${prefix}FullNameLegalRep`]: statementOfTruth.fullNameHiddenTextInput,
    [`${prefix}FirmNameLegalRep`]: statementOfTruth.nameOfFirmHiddenTextInput,
    [`${prefix}PositionLegalRep`]: statementOfTruth.positionOrOfficeHeldHiddenTextInput,
  };
}

/**
 * The answers the XUI journeys in enforcement-journeys.ts and above give "Enforce the order", by application
 * type: no breathing space, missed payments, the name and address right, no dates of birth, no risk, nobody
 * vulnerable, easy access, nothing else to add, English; a writ is transferred to the High Court with no HCEO
 * hired; a warrant of restitution keeps the earlier warrant's "no" risk answers.
 */
const APPLICATION_ANSWERS: Record<'WARRANT' | 'WRIT' | 'WARRANT_OF_RESTITUTION', Record<string, unknown>> = {
  WARRANT: {
    warrantDefendantInBreathingSpace: 'NO',
    warrantDefendantMissedPayments: 'YES',
    warrantCorrectNameAndAddress: 'YES',
    warrantDefendantsDOBKnown: 'NO',
    warrantAnyRiskToBailiff: 'NO',
    vulnerablePeoplePresent: 'NO',
    warrantIsDifficultToAccessProperty: 'NO',
    warrantAdditionalInformationSelect: 'NO',
    warrantEnforcementLanguageUsed: 'ENGLISH',
    ...signedByLegalRepresentative('warrant'),
  },
  WRIT: {
    writHasClaimTransferredToHighCourt: 'Yes',
    writWasGeneralApplicationToTransferToHighCourtSuccessful: 'Yes',
    writCorrectNameAndAddress: 'YES',
    writDefendantInBreathingSpace: 'NO',
    writDefendantMissedPayments: 'YES',
    writHasHiredHighCourtEnforcementOfficer: 'NO',
    writEnforcementLanguageUsed: 'ENGLISH',
    ...signedByLegalRepresentative('writ'),
  },
  WARRANT_OF_RESTITUTION: {
    warrant_restHowDefendantsReturned: 'They returned to the property after the eviction',
    warrant_restAnyRiskToBailiff: 'NO',
    vulnerablePeoplePresentWarrantRest: 'NO',
    warrant_restIsDifficultToAccessProperty: 'NO',
  },
};

const APPLICATION_TYPES = {
  'warrant of possession': 'WARRANT',
  'writ of possession': 'WRIT',
  'warrant of restitution': 'WARRANT_OF_RESTITUTION',
} as const;

/**
 * The claimant's legal representative applies for `type` on the scenario's case through the data store API, with
 * the XUI journey's answers. Records the type, and whether the application owes a fee, on the world, and expects
 * the claimant's legal representative next.
 */
export async function applyAsClaimant(world: EnforcementWorld, type: keyof typeof APPLICATION_TYPES): Promise<void> {
  const code = APPLICATION_TYPES[type];
  await submitEventAs(requireEmailFor(CLAIMANT_LEGAL_REPRESENTATIVE), 'enforceTheOrder', startData => ({
    chooseEnforcementType: chooseFromList(startData, 'chooseEnforcementType', code),
    ...APPLICATION_ANSWERS[code],
  }));
  world.applicationType = type;
  world.enforcementSeeded = true;
  world.askedToPayFee = (await enforcementRows(requireCaseReference(world)))[0]?.status === 'Awaiting payment';
  expectSignedInAs(CLAIMANT_LEGAL_REPRESENTATIVE);
}

/**
 * Puts a live, approved warrant of restitution on the case: the case's approved warrant, if it has one, is
 * closed with a successful eviction, then the claimant applies for restitution and a hearing centre admin
 * approves it. Leaves the restitution's number on the world as the current warrant and expects the claimant's
 * legal representative next.
 */
export async function generateWarrantOfRestitution(world: EnforcementWorld): Promise<string> {
  if (world.warrantApproved) {
    await closeApprovedWarrant(world, 'Eviction successful');
  }
  await applyAsClaimant(world, 'warrant of restitution');
  try {
    await approveWarrantAsCaseworker(world, { feeOutstanding: false });
  } finally {
    expectClaimantNext();
  }
  return world.warrantNumber as string;
}

/** Every enforcement on the case as the case data lists them, current first. */
export async function enforcementsOnTheCase(world: EnforcementWorld): Promise<EnforcementRow[]> {
  const caseReference = requireCaseReference(world);
  const expected = world.earlierWarrantNumbers.length + 1;
  await expect
    .poll(async () => (await enforcementRows(caseReference)).length, { timeout: VERY_LONG_TIMEOUT })
    .toBe(expected);
  return enforcementRows(caseReference);
}
