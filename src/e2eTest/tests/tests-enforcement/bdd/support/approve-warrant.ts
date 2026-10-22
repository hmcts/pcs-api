import { Page, expect } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition } from '../fixtures/pending';
import {
  allocateBailiffViaTestingSupport,
  idamUserId,
  localUserName,
  localUsers,
} from '@config/local-stack.config';
import { createCaseWithWarrant, openCaseSummary, requireCaseReference, startEvent } from './enforcement-journeys';
import { backgroundSetUp } from './demo-continuity';
import { recordEnforcementFeePaid } from './decide-enforcement';
import { ccdDateTime, confirmationValue, slashedDate, submitEventAs } from './event-api';
import {
  CLAIMANT_LEGAL_REPRESENTATIVE,
  expectSignedInAs,
  expectedSignedInRole,
  requireEmailFor,
  signInAs,
} from './session';
import { RISK_ANSWER_FIELDS, riskAnswerChange, tabQuestion } from './claimant-risk-assessment';
import { recordWarrantReturnAs } from './warrant-return';

const HEARING_CENTRE_ADMIN = 'hearing-centre-admin';
const BAILIFF = 'bailiff';

export async function approveWarrantAsCaseworker(
  world: EnforcementWorld,
  options: { feeOutstanding?: boolean } = {}
): Promise<void> {
  world.warrantNumber = await approveApplicationAsCaseworker(world, 'Warrant', options);
  world.warrantApproved = true;
}

export async function approveApplicationAsCaseworker(
  world: EnforcementWorld,
  kind: 'Warrant' | 'Writ',
  { feeOutstanding = true }: { feeOutstanding?: boolean } = {}
): Promise<string> {
  if (feeOutstanding) {
    await recordEnforcementFeePaid();
  }
  const confirmation = await submitEventAs(requireEmailFor(HEARING_CENTRE_ADMIN), 'decideEnforcement', {
    enforcementDecision: 'ACCEPT',
  });
  // The kind reads "Warrant approved" in the panel body and "Warrant of possession approved" in its header.
  expect(`${confirmation.header ?? ''} ${confirmation.body ?? ''}`, 'the "Decide enforcement" confirmation')
    .toMatch(new RegExp(`${kind}\\b[^<]*approved`));
  const enforcementNumber = confirmationValue(confirmation, `${kind} number:`);
  world.warrantNumber = enforcementNumber;
  world.currentEvent = undefined;
  expectSignedInAs(HEARING_CENTRE_ADMIN);
  return enforcementNumber;
}

export async function scheduleEvictionAsBailiff(
  world: EnforcementWorld,
  daysAhead = 14,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  await submitEventAs(requireEmailFor(BAILIFF), 'scheduleEviction', {
    evictionDateTime: ccdDateTime(daysAhead, '10:00'),
    evictionDispenseWithService: dispenseWithService,
  });
  recordEvictionScheduled(world, slashedDate(daysAhead), dispenseWithService);
  expectSignedInAs(BAILIFF);
}

export async function scheduleEvictionInXui(
  page: Page,
  world: EnforcementWorld,
  daysAhead = 14,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  const {
    SCHEDULE_EVICTION,
    dispenseWithServiceRadio,
    enterEvictionDateTime,
    expectCheckYourAnswers,
    scheduleEviction,
  } = await import('./schedule-eviction');

  await signInAs(BAILIFF, page);
  await openCaseSummary(page);
  await startEvent(SCHEDULE_EVICTION);

  const date = slashedDate(daysAhead);
  await enterEvictionDateTime(page, date, '10:00');
  await dispenseWithServiceRadio(page, dispenseWithService).check();
  await page.getByRole('button', { name: 'Continue', exact: true }).click();
  await expectCheckYourAnswers(page);
  await page.getByRole('button', { name: scheduleEviction.saveAndContinue, exact: true }).click();
  await expect(page.getByRole('heading', { name: /Eviction date scheduled for/ }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  recordEvictionScheduled(world, date, dispenseWithService);
}

function recordEvictionScheduled(world: EnforcementWorld, date: string, dispenseWithService: 'Yes' | 'No'): void {
  world.evictionDate = `${date} 10:00`;
  world.evictionScheduled = true;
  world.serviceDispensedWith = dispenseWithService === 'Yes';
}

export async function rescheduleEvictionAsBailiff(
  world: EnforcementWorld,
  daysAhead: number,
  dispenseWithService: 'Yes' | 'No'
): Promise<void> {
  await submitEventAs(requireEmailFor(BAILIFF), 'rescheduleEviction', {
    evictionDateTime: ccdDateTime(daysAhead, '10:00'),
    evictionDispenseWithService: dispenseWithService,
  });
  recordEvictionScheduled(world, slashedDate(daysAhead), dispenseWithService);
  expectSignedInAs(BAILIFF);
}

export async function confirmEvictionAsClaimant(
  world: EnforcementWorld,
  canAttend: boolean,
  changeRiskAnswer?: string
): Promise<void> {
  await submitEventAs(requireEmailFor(CLAIMANT_LEGAL_REPRESENTATIVE), 'confirmEviction', startData => {
    const prefilled: Record<string, unknown> = {};
    for (const field of RISK_ANSWER_FIELDS) {
      if (startData[field] !== undefined && startData[field] !== null) {
        prefilled[field] = startData[field];
      }
    }
    return {
      ...prefilled,
      ...(changeRiskAnswer ? riskAnswerChange(changeRiskAnswer) : {}),
      claimantCanAttendEviction: canAttend ? 'Yes' : 'No',
    };
  });
  if (changeRiskAnswer) {
    world.changedRiskQuestions = [...(world.changedRiskQuestions ?? []), tabQuestion(changeRiskAnswer)];
  }
  world.claimantConfirmedEviction = canAttend;
  expectSignedInAs(CLAIMANT_LEGAL_REPRESENTATIVE);
}

export function expectClaimantNext(): void {
  expectSignedInAs(CLAIMANT_LEGAL_REPRESENTATIVE);
}

export const AWAITING_PAYMENT = 'Awaiting payment';
export const AWAITING_REVIEW = 'Awaiting review';
export const AWAITING_BAILIFF_ALLOCATION = 'Awaiting bailiff allocation';
export const AWAITING_SCHEDULING = 'Awaiting scheduling';
export const AWAITING_CLAIMANT_CONFIRMATION = 'Awaiting claimant confirmation';
export const CLAIMANT_CONFIRMED = 'Claimant confirmed';
export const CLAIMANT_UNABLE_TO_ATTEND = 'Claimant unable to attend on this date';
export const BREATHING_SPACE = 'Breathing space';
export const CLOSED = 'Closed';

const SETTABLE_STATUSES = [
  AWAITING_PAYMENT,
  AWAITING_REVIEW,
  AWAITING_BAILIFF_ALLOCATION,
  AWAITING_SCHEDULING,
  AWAITING_CLAIMANT_CONFIRMATION,
  CLAIMANT_CONFIRMED,
  CLAIMANT_UNABLE_TO_ATTEND,
  BREATHING_SPACE,
  CLOSED,
];

export const canSetUpStatus = (status: string): boolean => SETTABLE_STATUSES.includes(status);

const RETURNS_REACHING: Record<string, string> = {
  [CLOSED]: 'Eviction successful',
  [BREATHING_SPACE]: 'Claimant advised Defendant in Breathing Space',
};

const RETURN_RECORDABLE_STATUSES = [
  AWAITING_SCHEDULING,
  AWAITING_CLAIMANT_CONFIRMATION,
  CLAIMANT_CONFIRMED,
  CLAIMANT_UNABLE_TO_ATTEND,
  BREATHING_SPACE,
];

export async function recordReturnAsHearingCentreAdmin(returnName: string): Promise<string> {
  const label = await recordWarrantReturnAs(requireEmailFor(HEARING_CENTRE_ADMIN), returnName);
  expectSignedInAs(HEARING_CENTRE_ADMIN);
  return label;
}

export async function bringWarrantToStatus(world: EnforcementWorld, status: string): Promise<void> {
  const { currentEnforcement, expectEnforcementStatus } = await import('./enforcement-status-tab');
  const expected = expectedSignedInRole();
  const current = world.caseReference
    ? (await currentEnforcement(world.caseReference, world.earlierWarrantNumbers.length)).status
    : undefined;
  if (current === status) {
    return;
  }
  if (current && RETURNS_REACHING[status] && RETURN_RECORDABLE_STATUSES.includes(current)) {
    await recordReturnAsHearingCentreAdmin(RETURNS_REACHING[status]);
  } else {
    world.allocatedBailiff = undefined;
    world.evictionScheduled = false;
    await setUpWarrantAtStatus(world, status);
  }
  await expectEnforcementStatus(undefined, requireCaseReference(world), status, world.earlierWarrantNumbers.length);
  expectSignedInAs(expected);
}

export async function allocateBailiff(world: EnforcementWorld, email: string = localUsers.bailiff): Promise<void> {
  const userId = await idamUserId(email);
  await allocateBailiffViaTestingSupport(requireCaseReference(world), {
    userId,
    name: localUserName(email),
    email,
  });
  world.allocatedBailiff = email;
}

export async function setUpWarrantAtStatus(world: EnforcementWorld, status: string): Promise<void> {
  try {
    await backgroundSetUp(async () => driveWarrantToStatus(world, status));
  } finally {
    expectClaimantNext();
  }
}

async function driveWarrantToStatus(world: EnforcementWorld, status: string): Promise<void> {
  await createCaseWithWarrant(world);

  if (status === AWAITING_PAYMENT) {
    return;
  }
  if (status === AWAITING_REVIEW) {
    await recordEnforcementFeePaid();
    return;
  }

  await approveWarrantAsCaseworker(world);
  if (status === AWAITING_BAILIFF_ALLOCATION) {
    return;
  }

  // Everything past here needs a bailiff allocated. The event's staff type-ahead needs reference data cftlib
  // does not run, so the allocation goes through testing support, which calls the same service.
  await allocateBailiff(world);
  approximatePrecondition(
    'HDPI-7356',
    'the bailiff was allocated through pcs-api testing support rather than the allocateBailiff event, whose '
      + 'staff type-ahead needs reference data cftlib does not run'
  );
  if (status === AWAITING_SCHEDULING) {
    return;
  }
  if (status === CLOSED || status === BREATHING_SPACE) {
    await recordReturnAsHearingCentreAdmin(RETURNS_REACHING[status]);
    return;
  }

  await scheduleEvictionAsBailiff(world);
  if (status === AWAITING_CLAIMANT_CONFIRMATION) {
    return;
  }

  if (status === CLAIMANT_CONFIRMED || status === CLAIMANT_UNABLE_TO_ATTEND) {
    await confirmEvictionAsClaimant(world, status === CLAIMANT_CONFIRMED);
    return;
  }

  approximatePrecondition(
    'HDPI-7419',
    `"${status}" cannot be set up: no event or testing-support route reaches it. The warrant is in `
      + '"Awaiting claimant confirmation".'
  );
}

export async function scheduleEvictionOnApprovedWarrant(
  world: EnforcementWorld,
  dispenseWithService: 'Yes' | 'No' = 'No'
): Promise<void> {
  expect(world.warrantApproved, 'an approved warrant to schedule the eviction on').toBe(true);
  try {
    await backgroundSetUp(async () => {
      if (!world.allocatedBailiff) {
        await allocateBailiff(world);
        approximatePrecondition('HDPI-7356', 'the bailiff was allocated through pcs-api testing support');
      }
      await scheduleEvictionAsBailiff(world, 14, dispenseWithService);
    });
  } finally {
    expectClaimantNext();
  }
  world.defendantViews = undefined;
}
