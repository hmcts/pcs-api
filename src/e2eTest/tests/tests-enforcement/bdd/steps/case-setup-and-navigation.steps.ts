// Case set-up, signing in and moving around the case in XUI. Shared by every feature.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import {
  canSignInAs,
  createIssuedCase,
  nextStepOptions,
  openCaseSummary,
  seedWarrantApplication,
  signInAs,
  startEvent,
  xuiEvents,
} from '../support/enforcement-journeys';

/** Tickets that deliver the enforcement events the steps cannot drive yet. */
const eventTickets: Record<string, string> = {
  'Decide enforcement': 'HDPI-8656',
  'Allocate case to bailiff': 'HDPI-7356',
  'Set risk level': 'HDPI-7343',
  'Schedule eviction': 'HDPI-7344',
  'Reschedule eviction': 'HDPI-7549',
  'Mark notice of eviction as served': 'HDPI-7346',
  'Add a bailiff note': 'HDPI-7593',
  'Record warrant return': 'HDPI-7347',
};

function eventNotBuilt(eventName: string) {
  return { ticket: eventTickets[eventName], summary: `the "${eventName}" event` };
}

// The case reference in the feature files is illustrative: each scenario creates its own case.
Given('a PCS case {string}', async ({ world }, _reference: string) => {
  await createIssuedCase(world);
});

Given('a PCS case {string} with a possession order made', async ({ world }, _reference: string) => {
  approximatePrecondition('HDPI-7342', 'no possession order is made; "Enforce the order" is available once the case is issued (README open question 1)');
  await createIssuedCase(world);
});

Given('a PCS case {string} with a warrant of possession with status {string}', async ({ world }, _reference: string, status: string) => {
  await createIssuedCase(world);
  await seedWarrantApplication(world);
  approximatePrecondition('HDPI-7419', `enforcement status "${status}" is not modelled yet; the case has a submitted warrant application`);
});

// The claimant legal representative is the storage-state user from config/global-setup.config.ts, who also
// creates the case through the API. Opening the case shows the session is valid and the user can see it.
Given('I am signed in to XUI as the claimant\'s legal representative', async ({ page }) => {
  await openCaseSummary(page);
});

Given('I am signed in to XUI as the claimant\'s legal representative with the {string} case role', async ({ page, $tags }, caseRole: string) => {
  if (caseRole !== '[CLAIMANTSOLICITOR]') {
    return markPending({ ticket: 'HDPI-7539', summary: `signing in with the ${caseRole} case role` }, $tags);
  }
  await openCaseSummary(page);
});

Given('there is no live enforcement on the case', async ({ world }) => {
  // True by construction: the scenario's case is new and nothing has been enforced on it yet.
  expect(world.caseReference, 'a case created by this scenario').toBeDefined();
  expect(world.enforcementSeeded, 'an enforcement seeded on the case').toBe(false);
});

// Internal users and the defendant's legal representative sign in in place of the storage-state user.
Given('I am signed in to XUI as a {string}', async ({ page, context, $tags }, role: string) => {
  if (!canSignInAs(role)) {
    return markPending({ ticket: 'HDPI-7539', summary: `signing in as a ${role}: there is no ${role} test user yet` }, $tags);
  }
  await signInAs(role, page, context);
});

When('I open the case', async ({ page }) => {
  await openCaseSummary(page);
});

Then('the {string} event is available in the next steps drop down', async ({ page, $tags }, eventName: string) => {
  const event = xuiEvents[eventName];
  if (!event) {
    return markPending(eventNotBuilt(eventName), $tags);
  }
  await expect.poll(() => nextStepOptions(page)).toContain(event.label);
});

Then('the {string} event is not available', async ({ page, $tags }, eventName: string) => {
  const event = xuiEvents[eventName];
  if (!event) {
    return markPending(eventNotBuilt(eventName), $tags);
  }
  // Wait for the drop down to load with the user's other events before checking this one is missing.
  await expect.poll(async () => (await nextStepOptions(page)).length).toBeGreaterThan(0);
  expect(await nextStepOptions(page)).not.toContain(event.label);
});

When('I start the {string} event', async ({ $tags }, eventName: string) => {
  if (!xuiEvents[eventName]) {
    return markPending(eventNotBuilt(eventName), $tags);
  }
  await startEvent(eventName);
});

Then('the case reference is unchanged', async ({ page, world }) => {
  expect(world.caseReference, 'a case created by this scenario').toBeDefined();
  expect(page.url()).toContain(String(world.caseReference));
});

pendingSteps({ ticket: 'HDPI-7419', summary: 'Enforcement events gated on the live enforcement' }, [
  'I see the error {string}',
]);

pendingSteps({ ticket: 'HDPI-7539', summary: 'Signing in as a role other than the claimant legal representative' }, [
  'I am signed in to XUI as a hearing centre admin',
  'I am signed in to XUI as a bailiff manager',
  'I am signed in to XUI as a bailiff',
  'I am signed in to XUI as the allocated bailiff',
  'I am signed in to XUI as bailiff {string}',
  'I am signed in as a defendant legal representative',
  'I am signed in to XUI as a bailiff with open enforcement tasks',
]);

pendingSteps({ summary: 'Generic XUI step for an enforcement event that does not exist yet' }, [
  'the {string} journey opens',
  'I see the case number, the possession property address and {string}',
  'none of these are editable',
  'the {string} button is disabled',
  'I am on the {string} screen',
  'I select {string}',
  'I return to the case summary',
  'I submit the {string} event',
  'the {string} screen opens',
  'the warrant number is not shown in the header',
  'I see the {string} confirmation with the case number and warrant number',
  'I submit the event',
  'I start the {string} event from the next steps drop down',
  'I see {string}',
  'the question {string} is mandatory',
  'the answer defaults to {string}',
  'I see the confirmation {string} with the case number and warrant number',
  'today is {string} and the time is {string}',
  'the {string} screen shows my answers with {string} links',
  'I open {string}',
  '{string}',
]);

pendingSteps({ ticket: 'HDPI-7539', summary: 'Opening the case as a role other than the claimant legal representative' }, [
  'an internal user opens the case',
  'a {string} opens the case',
  'a bailiff opens the case',
  'a hearing centre admin opens the case',
]);
