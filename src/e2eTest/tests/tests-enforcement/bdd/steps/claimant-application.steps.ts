// Claimant legal representative applies for enforcement through "Enforce the order" (features 01 and 15).
import { Given, When } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { markPending, pendingSteps } from '../fixtures/pending';
import type { PendingScope } from '../fixtures/pending';
import {
  answerApplicationQuestions,
  chooseApplicationType,
  openCaseSummary,
  signStatementOfTruth,
  startEvent,
  submitApplication,
} from '../support/enforcement-journeys';
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

pendingSteps({ summary: 'Paying the enforcement fee by service request' }, [
  'I pay the {string} fee using a service request',
  'I submit and pay for a {string} application',
]);

pendingSteps({ ticket: 'HDPI-8424', summary: 'Repeat warrant fee rules' }, [
  'the case has a closed warrant with return name {string}',
  'that warrant was generated less than 12 calendar months ago',
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
