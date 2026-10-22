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
import { applyForEnforcement, requireCaseReference } from './enforcement-journeys';
import { EnforcementRow, enforcementRows } from './enforcement-status-tab';
import { chooseFromList, submitEventAs } from './event-api';
import { CLAIMANT_LEGAL_REPRESENTATIVE, expectSignedInAs, requireEmailFor } from './session';
import { statementOfTruth } from '@data/page-data-figma/page-data-enforcement-figma';

export async function closeWarrantWithReturn(world: EnforcementWorld, returnName: string): Promise<void> {
  await setUpWarrantAtStatus(world, AWAITING_SCHEDULING);
  await closeApprovedWarrant(world, returnName);
}

/** "Record warrant return" needs a warrant in "Awaiting scheduling" or later, so one nobody has been allocated is allocated first. */
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

export async function applyForFurtherWarrant(page: Page, world: EnforcementWorld): Promise<void> {
  world.askedToPayFee = await applyForEnforcement('warrant of possession', world, page);
}

export async function applyForWarrantOfRestitution(page: Page, world: EnforcementWorld): Promise<void> {
  world.askedToPayFee = await applyForEnforcement('warrant of restitution', world, page);
}

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

export async function enforcementsOnTheCase(world: EnforcementWorld): Promise<EnforcementRow[]> {
  const caseReference = requireCaseReference(world);
  const expected = world.earlierWarrantNumbers.length + 1;
  await expect
    .poll(async () => (await enforcementRows(caseReference)).length, { timeout: VERY_LONG_TIMEOUT })
    .toBe(expected);
  return enforcementRows(caseReference);
}
