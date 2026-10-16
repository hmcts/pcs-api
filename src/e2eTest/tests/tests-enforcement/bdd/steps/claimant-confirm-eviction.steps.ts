// Claimant confirms the eviction appointment (feature 06). "Confirm the eviction details" exists in pcs-api
// with two information pages; the answers, notifications and tasks the feature describes do not exist yet.
import { Given } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, pendingSteps } from '../fixtures/pending';

// "Confirm the eviction details" does not read an eviction date yet, so the scenario can carry on without one.
Given('the eviction is scheduled for {string} at {string}', async ({}, date: string, time: string) => {
  approximatePrecondition('HDPI-7344', `eviction on ${date} at ${time} not scheduled: "Schedule eviction" does not exist yet`);
});

pendingSteps({ ticket: 'HDPI-7547', summary: 'Claimant confirms the eviction' }, [
  'I answer that I can attend on {string} at {string}',
  'I provide a contact in my organisation and a contact for the day of the eviction',
  'I confirm I will arrange for someone to gain access to, and secure, the property',
  'I answer that I cannot attend',
  'I give the dates I am not available',
  'I change my answer to {string} to {string}',
  'the updated answer is stored alongside my original answer with today\'s date',
  'I do not change any risk assessment answers',
  'I have updated my risk assessment',
  'I see my original answers alongside my updated answers',
  'I do not see the risk level or bailiff notes',
]);
