// Notifications (GOV.UK Notify emails, dashboard notifications) and work allocation tasks (feature 18 and others).
import { Given, Then, When, expect, test } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import { isLocalStack } from '@config/local-stack.config';
import { openCaseSummary, requireCaseReference, startEvent, xuiEvents } from '../support/enforcement-journeys';
import { decideEnforcement } from '../support/decide-enforcement';
import { currentEnforcement } from '../support/enforcement-status-tab';
import { resolveEvictionDate } from '../support/schedule-eviction';
import {
  WARRANT_RETURN_EMAIL,
  devTemplateId,
  emailEvictionDateTime,
  expectClaimantEmail,
  expectNoClaimantEmail,
} from '../support/notify-emails';

// Nothing creates enforcement work allocation tasks yet (HDPI-8627). A step that only states a task exists
// is recorded as an approximated precondition so the scenario carries on: the enforcement journeys are all
// reachable from the next-steps drop down as well as from a task. Steps that *interact* with a task stay
// pending, because there is nothing to click.
const noTasksYet = (description: string) => approximatePrecondition('HDPI-8627', description);

Given('an {string} task exists on the case', async ({ world }, task: string) => {
  world.journey = task;
  noTasksYet(`the "${task}" task is not created`);
});

Given('a {string} task is assigned to me', async ({ world }, task: string) => {
  world.journey = task;
  noTasksYet(`the "${task}" task is not created or assigned`);
});

/**
 * Where the scenario is about the journey rather than the task, the journey is started from the next-steps
 * drop down instead. A task with no event of the same name stays pending.
 */
When('I select the {string} task from the {string} tab', async ({ page, world, $tags }, task: string, _tab: string) => {
  if (!xuiEvents[task]) {
    return markPending({ ticket: 'HDPI-8627', summary: `selecting the "${task}" task` }, $tags);
  }
  noTasksYet(`the "${task}" task is not created; the journey is started from the next-steps drop down`);
  await openCaseSummary(page);
  await startEvent(task);
  world.currentEvent = task;
});

Given('a {string} task exists', async ({}, task: string) => noTasksYet(`the "${task}" task is not created`));

Given('{string} is the allocated bailiff with an open {string} task', async ({}, bailiff: string, task: string) =>
  noTasksYet(`no bailiff is allocated and the "${task}" task is not created`));

Given('the enforcement has open {string} and {string} tasks', async ({}, first: string, second: string) =>
  noTasksYet(`the "${first}" and "${second}" tasks are not created`));

// Closing a task is approximated the same way, so a scenario about the event submit is still filmed. In the work
// allocation feature (18) the task is the point of the scenario, so there it stays pending.
Then('the {string} task is completed', async ({ $tags }, task: string) => {
  if ($tags.includes('@work-allocation')) {
    return markPending({ ticket: 'HDPI-8627', summary: `completing the "${task}" task` }, $tags);
  }
  noTasksYet(`the "${task}" task is not created, so it cannot be completed`);
});

// Nothing creates enforcement tasks, so the absence of one proves nothing. Approximated like the steps
// above rather than pending, so the scenario it belongs to — the claimant going through "Confirm the
// eviction date" without changing a risk answer — is still run and filmed.
Then('no {string} task is created', async ({ $tags }, task: string) => {
  if ($tags.includes('@work-allocation')) {
    return markPending({ ticket: 'HDPI-8627', summary: `checking that no "${task}" task is created` }, $tags);
  }
  noTasksYet(`no tasks are created at all, so the absence of a "${task}" task is not evidence`);
});

Then('open bailiff tasks for the enforcement are cancelled', async () =>
  noTasksYet('no bailiff tasks are created, so none are cancelled'));

Then('open bailiff tasks are cancelled', async () =>
  noTasksYet('no bailiff tasks are created, so none are cancelled'));

// Enforcement emails (HDPI-7538). Locally pcs-api sends them to wiremock's Notify stub with the dev profile's
// stand-in template ids, so each one is checked in the request journal (support/notify-emails.ts). Nowhere else
// can the suite see what Notify was sent, so off the local stack the email is recorded as an approximated
// precondition and the scenario carries on.
const emailTicket = 'HDPI-7538';

/** What else the scenario says the email does, which is the template's wording and so cannot be checked. */
function noteTemplateContent(name: string, content: string): void {
  test.info().annotations.push({
    type: 'not checked',
    description: `${emailTicket}: that the "${name}" email ${content} is the Notify template's wording, which has not been written yet`,
  });
}

/** "Thursday 25 June 2026 at 10:00am" for the eviction date the scenario scheduled, or any such date. */
function expectedEvictionDateTime(world: EnforcementWorld): unknown {
  if (!world.evictionDate) {
    return expect.stringMatching(/^[A-Z][a-z]+day \d{1,2} [A-Z][a-z]+ \d{4} at \d{2}:\d{2}[ap]m$/);
  }
  const entered = world.evictionDate;
  const date = resolveEvictionDate(entered.slice(0, entered.lastIndexOf(' ')));
  const time = entered.slice(entered.lastIndexOf(' ') + 1);
  expect(date, `the eviction date "${entered}"`).toBeDefined();
  const { day, month, year } = date as { day: string; month: string; year: string };
  return emailEvictionDateTime(Number(day), Number(month), Number(year), time);
}

/** The personalisation pcs-api's EnforcementEmail declares for the email, as this scenario should have set it. */
async function expectedPersonalisation(name: string, world: EnforcementWorld): Promise<Record<string, unknown>> {
  const caseReference = requireCaseReference(world);
  switch (name) {
    case 'Caseworker generates the warrant of possession':
    case 'Application for a writ successful': {
      const { number } = await currentEnforcement(caseReference);
      expect(number, 'the warrant or writ number on the enforcement').toBeTruthy();
      return { enforcementNumber: number };
    }
    case 'Caseworker rejects the warrant of possession application':
      return { rejectionReason: decideEnforcement.rejectionReasonInput };
    case 'Bailiff schedules the eviction date':
    case 'Bailiff reschedules the eviction date':
    case 'Claimant confirms attendance at the eviction date':
      return { evictionDateTime: expectedEvictionDateTime(world) };
    default:
      return {};
  }
}

/**
 * Checks the named email was sent to the claimant, or says why it cannot be. Returns whether it was checked.
 */
async function expectEmail(
  world: EnforcementWorld,
  tags: string[],
  name: string,
  personalisation?: Record<string, unknown>
): Promise<boolean> {
  if (!devTemplateId(name)) {
    markPending({ ticket: emailTicket, summary: `the "${name}" email has no content yet, so no template id is configured` }, tags);
    return false;
  }
  if (!isLocalStack()) {
    approximatePrecondition(emailTicket, `the "${name}" email cannot be observed off the local stack, where Notify is not wiremock`);
    return false;
  }
  await expectClaimantEmail(requireCaseReference(world), name, personalisation ?? (await expectedPersonalisation(name, world)));
  return true;
}

for (const recipient of ['the claimant receives', 'I receive']) {
  Then(`${recipient} the {string} email`, async ({ world, $tags }, name: string) => {
    await expectEmail(world, $tags, name);
  });
}

Then(
  'the claimant receives the {string} email telling them to give the writ to a High Court Enforcement Officer',
  async ({ world, $tags }, name: string) => {
    if (await expectEmail(world, $tags, name)) {
      noteTemplateContent(name, 'tells the claimant to give the writ to a High Court Enforcement Officer');
    }
  }
);

// pcs-api has no XUI base URL to pass, so the link is meant to be written into the template (EnforcementEmail).
Then('the claimant receives the {string} email with a link to confirm the date', async ({ world, $tags }, name: string) => {
  if (await expectEmail(world, $tags, name)) {
    noteTemplateContent(name, 'links to the case to confirm the date');
  }
});

// Every return name that notifies the claimant shares one template; the return name and body are personalisation.
Then('the claimant {string} an email', async ({ world, $tags }, notified: string) => {
  const submitted = world.submittedReturn;
  expect(submitted, 'a return submitted by this scenario').toBeDefined();
  const { returnName } = submitted as { returnName: string };
  if (notified === 'receives') {
    await expectEmail(world, $tags, WARRANT_RETURN_EMAIL, {
      returnName,
      warrantReturnBody: expect.stringMatching(/\S/),
    });
    return;
  }
  expect(notified, 'whether the claimant is notified').toBe('does not receive');
  if (!devTemplateId(WARRANT_RETURN_EMAIL)) {
    return markPending({ ticket: emailTicket, summary: 'the warrant return email has no template id configured' }, $tags);
  }
  if (!isLocalStack()) {
    return approximatePrecondition(emailTicket, `that no email is sent for "${returnName}" cannot be observed off the local stack`);
  }
  await expectNoClaimantEmail(requireCaseReference(world), WARRANT_RETURN_EMAIL);
});

Then('the claimant email says {string}', async ({ world, $tags }, text: string) => {
  expect(world.submittedReturn, 'a return submitted by this scenario').toBeDefined();
  await expectEmail(world, $tags, WARRANT_RETURN_EMAIL, {
    returnName: world.submittedReturn?.returnName,
    warrantReturnBody: expect.stringContaining(text),
  });
});

pendingSteps({ ticket: 'HDPI-7538', summary: 'Enforcement notifications' }, [
  // No enforcement email carries the notice of appointment: HDPI-7538's table has none for it.
  'it is sent to the claimant by email',
  // pcs-api has no claimant dashboard notifications for enforcement.
  'the claimant sees a {string} notification on their dashboard',
  // WarrantReturnNotificationService only emails the claimant (TODO HDPI-8424); the content is TBC.
  'each defendant is notified by their preferred method, email or post',
]);

pendingSteps({ ticket: 'HDPI-8627', summary: 'Enforcement work allocation tasks' }, [
  'a {string} work allocation task is created',
  'the task is due in 10 working days',
  'I select the {string} task from the case {string} tab',
  'an {string} task is created',
  'I select the {string} task',
  'a {string} task is created and assigned to {string}',
  'open bailiff tasks on the case are reassigned to {string}',
  '{string} receives the tasks for the case',
  '{string} can still open all of their allocated cases and tasks',
  'a {string} task {string}',
  'a {string} task is created for the allocated bailiff',
  'I have started the {string} journey from the task',
  'the scheduled task job runs',
  'a {string} task is created and assigned to me',
  'selecting the task opens the {string} journey',
  'I select the tasks for the three cases',
  'a {string} task is created for {string}',
  'the {string} event is submitted',
  'the bailiff records a return that closes the enforcement',
  'both tasks are cancelled',
  'the claimant withdraws the enforcement application',
  'the {string} task is cancelled',
  'the enforcement is reallocated to {string}',
  'the {string} task is assigned to {string}',
  'each task shows the case reference and property address',
  'no warrant number column is shown',
  'a {string} task is created',
  'it is due in 10 working days',
  'it has major priority 2 of 7',
]);
