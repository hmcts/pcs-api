// Claimant legal representative applies for enforcement through "Enforce the order" (features 01 and 15).
import { Given, When } from '../fixtures/enforcement.fixtures';
import type { EnforcementType, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import type { PendingScope } from '../fixtures/pending';
import {
  answerApplicationQuestions,
  applyForEnforcement,
  chooseApplicationType,
  requireCaseReference,
  signStatementOfTruth,
  submitApplication,
} from '../support/enforcement-journeys';
import { recordEnforcementFeePaid } from '../support/decide-enforcement';
import { Page } from '@playwright/test';

const enforcementTypes: EnforcementType[] = ['warrant of possession', 'writ of possession', 'warrant of restitution', 'writ of restitution'];

function asEnforcementType(type: string): EnforcementType {
  if (!enforcementTypes.includes(type as EnforcementType)) {
    throw new Error(`Unknown enforcement type "${type}". Expected one of: ${enforcementTypes.join(', ')}`);
  }
  return type as EnforcementType;
}

// pcs-api offers a writ of restitution once the claim has a writ, which these helpers have no set-up for.
const writOfRestitutionJourneyNotDriven: PendingScope = {
  summary: 'these helpers cannot drive the writ of restitution journey',
};

/** Whole "Enforce the order" journey from the case summary to the submitted banner. */
async function applyFor(type: EnforcementType, world: EnforcementWorld, page: Page, tags: string[]): Promise<void> {
  if (type === 'writ of restitution') {
    return markPending(writOfRestitutionJourneyNotDriven, tags);
  }
  world.askedToPayFee = await applyForEnforcement(type, world, page);
}

When('I choose to apply for a {string}', async ({ page, world, $tags }, type: string) => {
  const enforcementType = asEnforcementType(type);
  if (enforcementType === 'writ of restitution') {
    return markPending(writOfRestitutionJourneyNotDriven, $tags);
  }
  await chooseApplicationType(enforcementType, world, page);
});

When('I answer the {string} application questions', async ({ world }, type: string) => {
  await answerApplicationQuestions(asEnforcementType(type), world);
});

When('I sign the statement of truth', async ({ world }) => {
  const type = world.applicationType;
  if (!type) {
    throw new Error('No application type has been chosen: start with "When I choose to apply for a ..."');
  }
  await signStatementOfTruth(type);
});

When('I submit the application', async ({ page, world }) => {
  world.askedToPayFee = await submitApplication(page);
});

Given('I have submitted an application for a {string}', async ({ page, world, $tags }, type: string) => {
  await applyFor(asEnforcementType(type), world, page, $tags);
});

When('I submit an application for a {string}', async ({ page, world, $tags }, type: string) => {
  await applyFor(asEnforcementType(type), world, page, $tags);
});

/**
 * A local stack has no Payments service to take the enforcement fee (HDPI-2877), so the fee is paid through pcs-api
 * testing support, which sends a paid callback through pcs-api's enforcement payment handler and so moves the
 * application from "Awaiting payment" to "Awaiting review" as paying the service request would.
 */
async function payEnforcementFee(): Promise<void> {
  approximatePrecondition('HDPI-2877', 'the enforcement fee is paid through testing support, not from the Service Request tab');
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

pendingSteps({ ticket: 'HDPI-8424', summary: 'Further enforcement on the same case' }, [
  'I apply for a {string} explaining how the defendants returned to the property',
  'I upload evidence',
  'bailiff events other than {string} are not available',
  'the possession order was made 6 years ago',
  'I can apply for a warrant of possession',
  'the defendant\'s application to set aside the order is listed for a hearing',
  'the warrant resumes at {string} if the application is dismissed',
  'the warrant closes with return name {string} if the order is set aside',
]);
