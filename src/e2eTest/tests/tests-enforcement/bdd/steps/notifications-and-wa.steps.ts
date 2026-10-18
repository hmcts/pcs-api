// Notifications (GOV.UK Notify emails, dashboard notifications) and work allocation tasks (feature 18 and others).
import { Given } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, pendingSteps } from '../fixtures/pending';

// Nothing creates enforcement work allocation tasks yet (HDPI-8627). A step that only states a task exists
// is recorded as an approximated precondition so the scenario carries on: the enforcement journeys are all
// reachable from the next-steps drop down as well as from a task. Steps that *interact* with a task stay
// pending, because there is nothing to click.
const noTasksYet = (description: string) => approximatePrecondition('HDPI-8627', description);

Given('an {string} task exists on the case', async ({}, task: string) => noTasksYet(`the "${task}" task is not created`));

Given('a {string} task is assigned to me', async ({}, task: string) => noTasksYet(`the "${task}" task is not created or assigned`));

Given('a {string} task exists', async ({}, task: string) => noTasksYet(`the "${task}" task is not created`));

Given('{string} is the allocated bailiff with an open {string} task', async ({}, bailiff: string, task: string) =>
  noTasksYet(`no bailiff is allocated and the "${task}" task is not created`));

Given('the enforcement has open {string} and {string} tasks', async ({}, first: string, second: string) =>
  noTasksYet(`the "${first}" and "${second}" tasks are not created`));

pendingSteps({ ticket: 'HDPI-7538', summary: 'Enforcement notifications' }, [
  'the claimant receives the {string} email',
  'the claimant receives the {string} email telling them to give the writ to a High Court Enforcement Officer',
  'it is sent to the claimant by email',
  'the claimant receives the {string} email with a link to confirm the date',
  'the claimant sees a {string} notification on their dashboard',
  'I receive the {string} email',
  'the claimant {string} an email',
  'the claimant email says {string}',
  'each defendant is notified by their preferred method, email or post',
]);

pendingSteps({ ticket: 'HDPI-8627', summary: 'Enforcement work allocation tasks' }, [
  'a {string} work allocation task is created',
  'the task is due in 10 working days',
  'I select the {string} task from the case {string} tab',
  'the {string} task is completed',
  'an {string} task is created',
  'I select the {string} task',
  'a {string} task is created and assigned to {string}',
  'open bailiff tasks on the case are reassigned to {string}',
  '{string} receives the tasks for the case',
  '{string} can still open all of their allocated cases and tasks',
  'a {string} task {string}',
  'a {string} task is created for the allocated bailiff',
  'no {string} task is created',
  'I select the {string} task from the {string} tab',
  'I have started the {string} journey from the task',
  'the scheduled task job runs',
  'a {string} task is created and assigned to me',
  'selecting the task opens the {string} journey',
  'open bailiff tasks for the enforcement are cancelled',
  'open bailiff tasks are cancelled',
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
