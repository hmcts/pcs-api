// Claimant legal representative applies for enforcement through "Enforce the order" (features 01 and 15).
import { Given, When } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import type { PendingScope } from '../fixtures/pending';
import {
  answerApplicationQuestions,
  chooseApplicationType,
  openCaseSummary,
  requireCaseReference,
  signStatementOfTruth,
  startEvent,
  submitApplication,
} from '../support/enforcement-journeys';
import { recordEnforcementFeePaid } from '../support/decide-enforcement';
import { backdateEnforcement } from '../support/backdate-enforcement';
import { Page } from '@playwright/test';

const enforcementTypes: EnforcementType[] = ['warrant of possession', 'writ of possession', 'warrant of restitution', 'writ of restitution'];

function asEnforcementType(type: string): EnforcementType {
  if (!enforcementTypes.includes(type as EnforcementType)) {
    throw new Error(`Unknown enforcement type "${type}". Expected one of: ${enforcementTypes.join(', ')}`);
  }
  return type as EnforcementType;
}

const writOfRestitutionNotOffered: PendingScope = { summary: '"Enforce the order" does not offer a writ of restitution' };
const noStatementOfTruthForRestitution: PendingScope = {
  summary: 'the warrant of restitution journey ends on placeholder pages, with no statement of truth',
};

/** Whole "Enforce the order" journey from the case summary to the submitted banner. */
async function applyFor(type: EnforcementType, world: EnforcementWorld, page: Page, tags: string[]): Promise<void> {
  if (type === 'writ of restitution') {
    return markPending(writOfRestitutionNotOffered, tags);
  }
  if (type === 'warrant of restitution') {
    return markPending(noStatementOfTruthForRestitution, tags);
  }
  await openCaseSummary(page);
  await startEvent('Enforce the order');
  await chooseApplicationType(type, world, page);
  await answerApplicationQuestions(type, world);
  await signStatementOfTruth(type);
  await submitApplication();
}

When('I choose to apply for a {string}', async ({ page, world, $tags }, type: string) => {
  const enforcementType = asEnforcementType(type);
  if (enforcementType === 'writ of restitution') {
    return markPending(writOfRestitutionNotOffered, $tags);
  }
  await chooseApplicationType(enforcementType, world, page);
});

When('I answer the {string} application questions', async ({ world }, type: string) => {
  await answerApplicationQuestions(asEnforcementType(type), world);
});

When('I sign the statement of truth', async ({ world, $tags }) => {
  const type = world.applicationType;
  if (type !== 'warrant of possession' && type !== 'writ of possession') {
    return markPending(noStatementOfTruthForRestitution, $tags);
  }
  await signStatementOfTruth(type);
});

When('I submit the application', async () => {
  await submitApplication();
});

Given('I have submitted an application for a {string}', async ({ page, world, $tags }, type: string) => {
  await applyFor(asEnforcementType(type), world, page, $tags);
});

When('I submit an application for a {string}', async ({ page, world, $tags }, type: string) => {
  await applyFor(asEnforcementType(type), world, page, $tags);
});

/**
 * pcs-api cannot take an enforcement fee yet (HDPI-2877): the fee is recorded as paid through pcs-api
 * testing support, which moves the application from "Awaiting payment" to "Awaiting review" as payment would.
 */
async function payEnforcementFee(): Promise<void> {
  approximatePrecondition('HDPI-2877', 'the enforcement fee is recorded as paid through testing support, not paid by service request');
  await recordEnforcementFeePaid();
}

When('I pay the {string} fee using a service request', async ({ world }, _feeCode: string) => {
  requireCaseReference(world);
  await payEnforcementFee();
});

When('I submit and pay for a {string} application', async ({ page, world, $tags }, type: string) => {
  await applyFor(asEnforcementType(type), world, page, $tags);
  await payEnforcementFee();
});

// "Less than 12 calendar months" is measured from when the warrant was generated (approved): 11 months back
// keeps it inside the free period, which a warrant generated moments ago trivially is.
Given('that warrant was generated less than 12 calendar months ago', async ({ world }) => {
  await backdateEnforcement(requireCaseReference(world), { period: 'P11M', enforcementNumber: world.warrantNumber });
});

pendingSteps({ ticket: 'HDPI-8424', summary: 'Repeat warrant fee rules' }, [
  'the case has a closed warrant with return name {string}',
  'I am not asked to pay a fee',
]);

pendingSteps({ ticket: 'HDPI-8424', summary: 'Further enforcement on the same case' }, [
  'the case has a warrant {string} closed with return name {string}',
  'I apply and pay for a new {string}',
  'the caseworker approves it',
  'the case has two warrants, {string} closed and {string} live',
  'the case has a closed warrant with return name {string} generated {string} ago',
  'I apply for a new {string}',
  'I am {string} the warrant fee',
  'the case has a warrant closed with return name {string}',
  'I apply for a {string} explaining how the defendants returned to the property',
  'I upload evidence',
  'bailiff events other than {string} are not available',
  'the possession order was made 6 years ago',
  'I can apply for a warrant of possession',
  'the defendant\'s application to set aside the order is listed for a hearing',
  'the warrant resumes at {string} if the application is dismissed',
  'the warrant closes with return name {string} if the order is set aside',
]);
