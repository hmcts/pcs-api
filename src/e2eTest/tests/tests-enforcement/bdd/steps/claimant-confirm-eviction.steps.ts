import { Given, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, pendingSteps } from '../fixtures/pending';
import { CONFIRM_EVICTION, answerAttendance, continueToCheckYourAnswers } from '../support/claimant-risk-assessment';

Given('the eviction is scheduled for {string} at {string}', async ({ world }, date: string, time: string) => {
  expect(world.evictionScheduled, 'an eviction scheduled by the bailiff').toBe(true);
  approximatePrecondition('HDPI-7344', `the eviction was scheduled for ${world.evictionDate} rather than ${date} at ${time}`);
});

When('I answer that I can attend on {string} at {string}', async ({ page, world }, _date: string, _time: string) => {
  expect(world.currentEvent).toBe(CONFIRM_EVICTION);
  await answerAttendance(page, true);
  await continueToCheckYourAnswers(page);
});

When('I answer that I cannot attend', async ({ page, world }) => {
  expect(world.currentEvent).toBe(CONFIRM_EVICTION);
  await answerAttendance(page, false);
  await continueToCheckYourAnswers(page);
});

pendingSteps({ ticket: 'HDPI-7547', summary: 'Confirm the eviction only asks whether the claimant can attend (OPEN-QUESTIONS 29)' }, [
  'I provide a contact in my organisation and a contact for the day of the eviction',
  'I confirm I will arrange for someone to gain access to, and secure, the property',
  'I give the dates I am not available',
]);
