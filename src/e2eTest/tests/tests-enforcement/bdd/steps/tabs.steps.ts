// Enforcement status tab, risk assessment tab, case file view documents and defendant visibility (features 12-14).
import { Given, Then, When } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import { expectCaseTab, requireCaseReference, seedWarrantApplication } from '../support/enforcement-journeys';
import { ENFORCEMENT_STATUS_TAB, openCaseTab } from '../support/enforcement-status-tab';
import { RISK_ASSESSMENT_TAB } from '../support/case-tabs';

/** Tabs the steps can check today, with the ticket that delivers each of the others. */
const builtTabs = ['Enforcement status', 'Risk assessment'];
const tabTickets: Record<string, string> = { 'Risk assessment': 'HDPI-7350' };

function tabNotBuilt(tabName: string) {
  return { ticket: tabTickets[tabName], summary: `the "${tabName}" tab` };
}

Given('the case has a warrant of possession', async ({ world }) => {
  approximatePrecondition('HDPI-8656', 'the warrant is a submitted application: "Decide enforcement" does not approve and generate it yet');
  await seedWarrantApplication(world);
});

Given('the notice of eviction {string}', async ({ $tags }, served: string) => {
  if (served !== 'has not been served') {
    return markPending({ ticket: 'HDPI-7346', summary: 'the "Mark notice of eviction as served" event' }, $tags);
  }
  // True by construction: nothing can mark the notice of eviction as served yet.
});

Then('the {string} tab is {string}', async ({ page, $tags }, tabName: string, visibility: string) => {
  if (!builtTabs.includes(tabName)) {
    return markPending(tabNotBuilt(tabName), $tags);
  }
  if (visibility !== 'shown' && visibility !== 'hidden') {
    throw new Error(`Unknown tab visibility "${visibility}". Expected "shown" or "hidden"`);
  }
  await expectCaseTab(page, tabName, visibility === 'shown');
});

/** The two tabs the enforcement work adds; any other tab name is still someone else's feature. */
const ENFORCEMENT_TABS = [ENFORCEMENT_STATUS_TAB, RISK_ASSESSMENT_TAB];

When('I open the {string} tab', async ({ page, world, $tags }, tabName: string) => {
  if (!ENFORCEMENT_TABS.includes(tabName)) {
    return markPending(tabNotBuilt(tabName), $tags);
  }
  await openCaseTab(page, requireCaseReference(world), tabName);
});

When('the {string} tab is opened', async ({ page, world, $tags }, tabName: string) => {
  if (!ENFORCEMENT_TABS.includes(tabName)) {
    return markPending(tabNotBuilt(tabName), $tags);
  }
  await openCaseTab(page, requireCaseReference(world), tabName);
});

Given('I am on the {string} tab', async ({ page, world, $tags }, tabName: string) => {
  if (!ENFORCEMENT_TABS.includes(tabName)) {
    return markPending(tabNotBuilt(tabName), $tags);
  }
  await openCaseTab(page, requireCaseReference(world), tabName);
});

Then('there is no {string} tab', async ({ page, $tags }, tabName: string) => {
  if (!builtTabs.includes(tabName)) {
    return markPending(tabNotBuilt(tabName), $tags);
  }
  await expectCaseTab(page, tabName, false);
});

pendingSteps({ ticket: 'HDPI-8442', summary: 'Enforcement documents in case file view' }, [
  'an {string} folder is created in case file view',
  'an unsealed {string} document is stored in the {string} folder',
  'both documents are visible to the claimant and internal users',
  'no {string} document is generated',
  'the {string} document contains {string}',
  'a {string} document is generated with the county court seal',
]);

pendingSteps({ ticket: 'HDPI-7351', summary: 'Enforcement status tab' }, [
  'the {string} tab shows my answers under {string}',
  'the case has no generated warrant or writ',
  'the case has a warrant of possession {string}',
  'a bailiff opens the {string} tab',
  'I see {string} as a heading',
  'I see the sections {string}, {string}, {string} and {string}',
  'the claimant has completed {string}',
  '{string} shows:',
  'the case has a writ of possession {string} approved by a judge\'s order dated {string}',
  'a caseworker opens the {string} tab',
  'I see {string} as {string}',
  'the case has a closed warrant {string} generated on {string}',
  'a live warrant of restitution {string} generated on {string}',
  '{string} is listed above {string}',
  'the case has a writ of possession',
  'the case header shows the case number and property address',
  'the case header does not show {string}',
  'the claimant applied for a {string}',
  'the {string} is generated',
  'the {string} tab is shown',
  'the case has only a writ of possession',
  'the claimant\'s warrant application is {string}',
  'a warrant has been generated',
  '{string} shows {string}',
  'I see the claimant\'s answers to:',
  'the claimant legal representative opens the {string} tab',
  'the eviction has been scheduled',
  'the notice of eviction has not been served',
  'the bailiff marks the notice of eviction as served',
  'the bailiff answered {string} to {string}',
  'the eviction takes place',
  'a {string} has been generated',
  'the previous warrant ended with {string}',
  'the claimant applies for a warrant of restitution',
  'the case {string} tab shows the event that changed it, who submitted it and when',
]);

pendingSteps({ ticket: 'HDPI-7539', summary: 'Defendant visibility of enforcement' }, [
  'I see the {string} table with claimant type, claimant name and defendant names',
  'a defendant opens the {string} tab',
  'they see the warrant number, {string}, {string} and {string}',
  'they do not see {string}',
  'a defendant opens the case',
  'the defendant is represented in XUI by a legal representative',
  'a second defendant is a citizen using pcs-frontend',
  'each defendant views the case',
  'they do not see the {string} tab',
  'they do not see the {string} folder in case file view',
  'they do not see any enforcement events in the case {string} tab',
  'the defendant legal representative sees the {string} tab',
  'the defendant legal representative sees the {string} folder with the {string}',
  'the citizen defendant sees the {string} under {string} in pcs-frontend',
  'defendants do not see the {string} tab',
  'defendants do not see the {string} folder',
  'a defendant views the case',
  'they do not see the risk assessment document',
  'they do not see bailiff notes',
  'a defendant views case file view',
  'they do not see the {string} document',
  'the defendants could see the enforcement for that warrant',
  'defendants can no longer see any enforcement information',
  'they stay unable to see it until the bailiff records a return on the warrant of restitution',
  'defendants cannot see any enforcement information',
]);

pendingSteps({ ticket: 'HDPI-7350', summary: 'Risk assessment tab' }, [
  'no risk level or bailiff note has been added',
  'I see the green print button between the tabs and the {string} section',
  'I select the print button',
  'the browser print dialogue opens with the risk assessment details',
  'a warrant has been generated with a risk level and a bailiff note',
  'the risk level and bailiff notes are {string}',
  'the claimant risk assessment section says {string} instead of {string}',
]);
