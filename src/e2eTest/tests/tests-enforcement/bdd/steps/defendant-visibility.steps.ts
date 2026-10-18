// What defendants can see of enforcement (feature 14).
//
// Runs on a local stack, where the scenario can give a defendant solicitor and a citizen their case roles
// through pcs-api testing support. The citizen stands in for the feature's second defendant: the fixture
// claim has one defendant, and pcs-frontend is not part of the local stack, so the citizen's view is the case
// data CCD gives them, which is what pcs-frontend renders. No notice of eviction document can be generated
// yet (HDPI-7354, no Docmosis template), so the steps about seeing it stay pending.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import { scheduleEvictionOnApprovedWarrant } from '../support/approve-warrant';
import { expectCaseTab, requireCaseReference, signInAs } from '../support/enforcement-journeys';
import { ENFORCEMENT_STATUS_TAB, enforcementSections, openCaseTab } from '../support/enforcement-status-tab';
import { markNoticeServedAsBailiff } from '../support/notice-served';
import {
  DEFENDANT_LEGAL_REPRESENTATIVE,
  caseChangedForDefendants,
  canSetUpDefendants,
  defendantViews,
  expectEnforcementTab,
  expectNoEnforcementDocuments,
  expectNoEnforcementFields,
  linkCitizenDefendant,
  linkDefendantLegalRepresentative,
  viewAsEachDefendant,
} from '../support/defendant-visibility';

const DISPENSE_WITH_SERVICE_QUESTION = 'Has the court given permission to dispense with service?';

const noDefendantUsers = {
  ticket: 'HDPI-7539',
  summary: 'a defendant linked to the case: only a local stack has users that testing support can link',
};

Given('the defendant is represented in XUI by a legal representative', async ({ world, $tags }) => {
  if (!canSetUpDefendants()) {
    return markPending(noDefendantUsers, $tags);
  }
  await linkDefendantLegalRepresentative(world);
});

Given('a second defendant is a citizen using pcs-frontend', async ({ world, $tags }) => {
  if (!canSetUpDefendants()) {
    return markPending(noDefendantUsers, $tags);
  }
  approximatePrecondition(
    'HDPI-7539',
    'the citizen is given the [DEFENDANT] case role directly, in place of an access code, on a claim whose one '
      + 'defendant is also represented; pcs-frontend is not run, so the citizen\'s view is the case data CCD gives them'
  );
  await linkCitizenDefendant(world);
});

Given('the eviction has been scheduled', async ({ page, context, world }) => {
  await scheduleEvictionOnApprovedWarrant(page, context, world);
});

Given('the notice of eviction has not been served', async ({ world }) => {
  expect(world.evictionScheduled, 'an eviction scheduled without dispensing with service').toBe(true);
  expect(world.serviceDispensedWith).toBe(false);
  expect(world.noticeServed, 'the notice of eviction marked as served').toBe(false);
});

Given('the bailiff answered {string} to {string}', async ({ page, context, world, $tags }, answer: string, question: string) => {
  if (question !== DISPENSE_WITH_SERVICE_QUESTION || (answer !== 'Yes' && answer !== 'No')) {
    return markPending({ ticket: 'HDPI-7344', summary: `answering "${answer}" to "${question}"` }, $tags);
  }
  await scheduleEvictionOnApprovedWarrant(page, context, world, answer);
});

When('the bailiff marks the notice of eviction as served', async ({ page, context, world }) => {
  expect(world.evictionScheduled && world.serviceDispensedWith === false, 'a notice of eviction to serve').toBe(true);
  await markNoticeServedAsBailiff(page, context, world);
  caseChangedForDefendants(world);
});

// Nothing records the eviction itself until the bailiff records the warrant return (HDPI-7347), and defendant
// visibility does not change at the eviction, so the checks are made against the scheduled eviction.
When('the eviction takes place', async ({ world }) => {
  expect(world.evictionScheduled, 'a scheduled eviction').toBe(true);
  approximatePrecondition('HDPI-7347', 'the eviction is not recorded; the checks are made against the scheduled eviction');
});

When('each defendant views the case', async ({ page, context, world }) => {
  await viewAsEachDefendant(page, context, world);
});

When('a defendant views the case', async ({ page, context, world }) => {
  await viewAsEachDefendant(page, context, world);
});

Then('they do not see the {string} tab', async ({ page, context, world }, tabName: string) => {
  await expectEnforcementTab(page, await defendantViews(page, context, world), tabName, false);
});

Then('defendants do not see the {string} tab', async ({ page, context, world }, tabName: string) => {
  const views = await defendantViews(page, context, world);
  await expectEnforcementTab(page, views, tabName, false);
  expectNoEnforcementFields(views);
});

Then('they do not see the {string} folder in case file view', async ({ page, context, world }, folder: string) => {
  expect(folder).toBe('Enforcement');
  expectNoEnforcementDocuments(await defendantViews(page, context, world));
});

Then('defendants do not see the {string} folder', async ({ page, context, world }, folder: string) => {
  expect(folder).toBe('Enforcement');
  expectNoEnforcementDocuments(await defendantViews(page, context, world));
});

// History is an internal tab, so a defendant's legal representative sees no events at all, and pcs-frontend
// shows citizens no case history.
Then('they do not see any enforcement events in the case {string} tab', async ({ page, context, world }, tabName: string) => {
  expect(tabName).toBe('History');
  const views = await defendantViews(page, context, world);
  expect(views.some(view => view.inXui), 'a defendant who views the case in XUI').toBe(true);
  await expectCaseTab(page, tabName, false);
});

// No risk assessment document is generated yet (HDPI-7350), so this checks the rule that covers it: the only
// enforcement document a defendant can get is a served notice of eviction, and none has been generated.
Then('they do not see the risk assessment document', async ({ page, context, world }) => {
  expectNoEnforcementDocuments(await defendantViews(page, context, world));
});

Then('they do not see bailiff notes', async ({ page, context, world }) => {
  expect(world.enteredNote, 'a bailiff note on the warrant').toBeDefined();
  const views = await defendantViews(page, context, world);
  for (const view of views) {
    expect(view.caseData.bailiffNotes, `bailiff notes the ${view.who} gets`).toBeUndefined();
  }
  await expectCaseTab(page, 'Risk assessment', false);
});

Then('the defendant legal representative sees the {string} tab', async ({ page, context, world }, tabName: string) => {
  expect(tabName).toBe('Enforcement status');
  const views = await viewAsEachDefendant(page, context, world);
  const legalRepresentative = views.filter(view => view.who === DEFENDANT_LEGAL_REPRESENTATIVE);
  await expectEnforcementTab(page, legalRepresentative, tabName, true);
});

pendingSteps({ ticket: 'HDPI-7354', summary: 'A generated notice of eviction: there is no Docmosis template for it yet' }, [
  'the defendant legal representative sees the {string} folder with the {string}',
  'the citizen defendant sees the {string} under {string} in pcs-frontend',
]);

// Feature 12: what a defendant sees on the "Enforcement status" tab once the notice is served.
When('a defendant opens the {string} tab', async ({ page, context, world, $tags }, tabName: string) => {
  if (!canSetUpDefendants()) {
    return markPending(noDefendantUsers, $tags);
  }
  expect(tabName).toBe(ENFORCEMENT_STATUS_TAB);
  world.openedAs = DEFENDANT_LEGAL_REPRESENTATIVE;
  await signInAs(DEFENDANT_LEGAL_REPRESENTATIVE, page, context);
  await openCaseTab(page, requireCaseReference(world), tabName);
});

Then(
  'they see the warrant number, {string}, {string} and {string}',
  async ({ page, world }, first: string, second: string, third: string) => {
    const sections = await enforcementSections(page);
    expect(sections, 'the one warrant the defendant can see').toHaveLength(1);
    expect(sections[0].heading).toBe(world.warrantNumber);
    expect(Object.keys(sections[0].sections)).toEqual(expect.arrayContaining([first, second, third]));
  }
);

Then('they do not see {string}', async ({ page }, section: string) => {
  for (const warrant of await enforcementSections(page)) {
    expect(Object.keys(warrant.sections), `the sections of ${warrant.heading}`).not.toContain(section);
  }
});
