// Case set-up, signing in and moving around the case in XUI. Shared by every feature.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import {
  canSignInAs,
  createCaseWithWarrant,
  createIssuedCase,
  nextStepOptions,
  openCaseSummary,
  signInAs,
  startEvent,
  startEventAllowingRefusal,
  xuiEvents,
} from '../support/enforcement-journeys';
import {
  DECIDE_ENFORCEMENT,
  chooseDecision,
  clickContinue,
  decideEnforcement,
  expectCaseDetailsHeader,
  expectCaseDetailsNotEditable,
  signInAsHearingCentreAdmin,
  submitDecision,
} from '../support/decide-enforcement';
import { AWAITING_BAILIFF_ALLOCATION, setUpWarrantAtStatus } from '../support/approve-warrant';
import { RESCHEDULE_EVICTION, SCHEDULE_EVICTION, scheduleEviction } from '../support/schedule-eviction';
import { MARK_NOTICE_SERVED, noticeServed } from '../support/notice-served';
import {
  ADD_BAILIFF_NOTE,
  ALLOCATE_BAILIFF,
  SET_RISK_LEVEL,
  addBailiffNote,
  changeAllocatedBailiff,
  chooseRiskLevel,
  findBailiff,
  goToAllocationCheckYourAnswers,
  setRiskLevel,
  standInBailiff,
} from '../support/bailiff-events';

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

Given('a PCS case {string} with a warrant of possession with status {string}', async ({ page, context, world }, _reference: string, status: string) => {
  await setUpWarrantAtStatus(page, context, world, status);
});

Given('a PCS case {string} with a warrant of possession', async ({ page, context, world }, _reference: string) => {
  await setUpWarrantAtStatus(page, context, world, AWAITING_BAILIFF_ALLOCATION);
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
    return markPending(
      {
        ticket: 'HDPI-7539',
        summary: `signing in as a ${role}: no ${role} the suite can use in this environment`,
      },
      $tags
    );
  }
  await signInAs(role, page, context);
});

When('I open the case', async ({ page }) => {
  await openCaseSummary(page);
});

// Only the claimant legal representative can sign in today: it is the storage-state user from
// config/global-setup.config.ts. Other users wait on HDPI-7539.
When('a {string} opens the case', async ({ page, $tags }, user: string) => {
  if (user !== 'claimant legal representative') {
    return markPending({ ticket: 'HDPI-7539', summary: `opening the case as a ${user}` }, $tags);
  }
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

When('I start the {string} event', async ({ page, world, $tags }, eventName: string) => {
  if (!xuiEvents[eventName]) {
    return markPending(eventNotBuilt(eventName), $tags);
  }
  // A scenario that signed in as another role is on the XUI landing page, not the case.
  await openCaseSummary(page);
  // A start callback can refuse the event; the scenario then asserts the error it gave.
  world.startEventError = await startEventAllowingRefusal(page, eventName);
  if (!world.startEventError) {
    world.currentEvent = eventName;
  }
});

/**
 * The message a start callback refused the event with. CCD puts a generic "Unable to proceed because there
 * are one or more callback Errors or Warnings" in the error summary and the real message in a separate
 * "Errors" list, so the specific text is matched on the page.
 */
Then('I see the error {string}', async ({ world }, message: string) => {
  expect(world.startEventError, 'the event to have been refused by its start callback').toBeDefined();
  expect(world.startEventError).toContain(message);
});

When('I start the {string} event from the next steps drop down', async ({ page, world, $tags }, eventName: string) => {
  if (!xuiEvents[eventName]) {
    return markPending(eventNotBuilt(eventName), $tags);
  }
  await openCaseSummary(page);
  await startEvent(eventName);
  world.currentEvent = eventName;
});

Then('the case reference is unchanged', async ({ page, world }) => {
  expect(world.caseReference, 'a case created by this scenario').toBeDefined();
  expect(page.url()).toContain(String(world.caseReference));
});

pendingSteps({ ticket: 'HDPI-7419', summary: 'Enforcement events gated on the live enforcement' }, [
]);

// The cftlib bailiff users (CftlibConfig + cftlib-am-role-assignments.json) make these sign-ins real.
// "the allocated bailiff" is approximated: no bailiff can be allocated (HDPI-7356 / HDPI-8189), but a
// bailiff reaches the warrant through their `bailiff` organisation role.
Given('I am signed in to XUI as a bailiff', async ({ page, context }) => {
  await signInAs('bailiff', page, context);
});

Given('I am signed in to XUI as a bailiff manager', async ({ page, context }) => {
  await signInAs('bailiff manager', page, context);
});

Given('I am signed in to XUI as a bailiff admin', async ({ page, context }) => {
  await signInAs('bailiff admin', page, context);
});

Given('I am signed in to XUI as the allocated bailiff', async ({ page, context }) => {
  approximatePrecondition('HDPI-8189', 'signed in as a bailiff; no allocated-bailiff case role is granted yet');
  await signInAs('bailiff', page, context);
});

pendingSteps({ ticket: 'HDPI-7539', summary: 'Signing in as a specific named bailiff needs staff reference data' }, [
  'I am signed in to XUI as bailiff {string}',
]);

pendingSteps({ ticket: 'HDPI-8627', summary: 'A bailiff with open enforcement work allocation tasks' }, [
  'I am signed in to XUI as a bailiff with open enforcement tasks',
]);

pendingSteps({ summary: 'Generic XUI step for an enforcement event that does not exist yet' }, [
  'the {string} screen opens',
  'the warrant number is not shown in the header',
  'I see the {string} confirmation with the case number and warrant number',
  'today is {string} and the time is {string}',
  'the {string} screen shows my answers with {string} links',
  'I open {string}',
  '{string}',
]);

pendingSteps({ ticket: 'HDPI-7539', summary: 'Opening the case as a role other than the claimant legal representative' }, [
  'an internal user opens the case',
  'a bailiff opens the case',
  'a hearing centre admin opens the case',
]);

// Generic XUI steps. Only "Decide enforcement" drives them today; any other event stays pending.
const genericStepPending = { summary: 'Generic XUI step for an enforcement event that does not exist yet' };

function inDecideEnforcement(world: EnforcementWorld): boolean {
  return world.currentEvent === DECIDE_ENFORCEMENT;
}

Given('I am signed in to XUI as a hearing centre admin', async ({ page }) => {
  await signInAsHearingCentreAdmin(page);
  await openCaseSummary(page);
});

Then('the {string} journey opens', async ({ page, world, $tags }, eventName: string) => {
  if (eventName !== DECIDE_ENFORCEMENT || !inDecideEnforcement(world)) {
    return markPending(genericStepPending, $tags);
  }
  await expect(page.getByRole('heading', { name: decideEnforcement.mainHeader })).toBeVisible();
});

/**
 * The screens the suite can open, keyed by the name the feature files use. Each is the first (and, for the
 * bailiff events, only) page of a built event, so "I am on the X screen" just starts that event.
 */
const screenEvents: Record<string, string> = {
  [DECIDE_ENFORCEMENT]: DECIDE_ENFORCEMENT,
  [setRiskLevel.pageLabel]: SET_RISK_LEVEL,
  [addBailiffNote.pageLabel]: ADD_BAILIFF_NOTE,
  [findBailiff.pageLabel]: ALLOCATE_BAILIFF,
  [scheduleEviction.pageLabel]: SCHEDULE_EVICTION,
  [noticeServed.pageLabel]: MARK_NOTICE_SERVED,
  [RESCHEDULE_EVICTION]: RESCHEDULE_EVICTION,
};

Given('I am on the {string} screen', async ({ page, world, $tags }, screen: string) => {
  // "Allocate case to bailiff" is the only built bailiff event with a check your answers screen. Reaching it
  // means choosing a bailiff, so it is the feature's main bailiff.
  if (screen === findBailiff.checkYourAnswers && world.journey === ALLOCATE_BAILIFF) {
    return goToAllocationCheckYourAnswers(page, world, standInBailiff('Sam Jones') as string);
  }
  const event = screenEvents[screen];
  if (!event) {
    return markPending(genericStepPending, $tags);
  }
  await openCaseSummary(page);
  await startEvent(event);
  world.currentEvent = event;
});

// Every built event's first page carries the same read-only case-details summary list.
Then('I see the case number, the possession property address and {string}', async ({ page, world, $tags }, partyNames: string) => {
  if (!world.currentEvent) {
    return markPending(genericStepPending, $tags);
  }
  await expectCaseDetailsHeader(page, world, partyNames);
});

Then('none of these are editable', async ({ page, world, $tags }) => {
  if (!world.currentEvent) {
    return markPending(genericStepPending, $tags);
  }
  await expectCaseDetailsNotEditable(page);
});

Then('the {string} button is disabled', async ({ page, world, $tags }, button: string) => {
  if (!world.currentEvent) {
    return markPending(genericStepPending, $tags);
  }
  await expect(page.getByRole('button', { name: button, exact: true })).toBeDisabled();
});

When('I select {string}', async ({ page, world, $tags }, option: string) => {
  // Continue and Cancel are on every enforcement event page, so they do not need to know which event
  // the scenario is in.
  if (option === decideEnforcement.cancelLink) {
    return page.getByText(decideEnforcement.cancelLink, { exact: true }).click();
  }
  if (option === decideEnforcement.continueButton) {
    return clickContinue(page);
  }
  if (!world.currentEvent) {
    return markPending(genericStepPending, $tags);
  }
  if (world.currentEvent === ALLOCATE_BAILIFF && option === findBailiff.changeLink) {
    return changeAllocatedBailiff(page);
  }
  if (world.currentEvent === SET_RISK_LEVEL && (option === setRiskLevel.high || option === setRiskLevel.medium)) {
    return chooseRiskLevel(page, option);
  }
  if (
    world.currentEvent === DECIDE_ENFORCEMENT
    && (option === decideEnforcement.acceptRadioOption || option === decideEnforcement.rejectRadioOption)
  ) {
    return chooseDecision(page, option);
  }
  return markPending(genericStepPending, $tags);
});

Then('I return to the case summary', async ({ page, world, $tags }) => {
  if (!world.currentEvent) {
    return markPending(genericStepPending, $tags);
  }
  const eventPageHeadings: Record<string, string> = {
    [SET_RISK_LEVEL]: setRiskLevel.pageLabel,
    [ALLOCATE_BAILIFF]: findBailiff.pageLabel,
  };
  const eventPageHeading = eventPageHeadings[world.currentEvent] ?? decideEnforcement.mainHeader;
  await expect(page).toHaveURL(new RegExp(`/cases/case-details/.*${world.caseReference}(#.*)?$`));
  await expect(page.getByRole('heading', { name: eventPageHeading })).toBeHidden();
  world.currentEvent = undefined;
});

When('I submit the {string} event', async ({ page, world, $tags }, eventName: string) => {
  if (eventName === DECIDE_ENFORCEMENT && inDecideEnforcement(world)) {
    return submitDecision(page);
  }
  // The eviction events use CCD's own check-your-answers, so submitting is "Save and continue". Only
  // possible when the scenario actually started the event and answered its page; several scenarios submit
  // without any set-up because they are really about what the submit produces (documents, notifications),
  // which does not exist yet.
  if (eventName === SCHEDULE_EVICTION || eventName === RESCHEDULE_EVICTION) {
    if (world.currentEvent !== eventName) {
      return markPending(genericStepPending, $tags);
    }
    await page.getByRole('button', { name: scheduleEviction.saveAndContinue, exact: true }).click();
    world.currentEvent = undefined;
    return;
  }
  return markPending(genericStepPending, $tags);
});
