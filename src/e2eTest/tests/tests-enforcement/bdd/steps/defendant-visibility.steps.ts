// What defendants can see of enforcement (feature 14).
//
// Runs on a local stack, where the scenario can give a defendant solicitor and a citizen their case roles
// through pcs-api testing support. The citizen stands in for the feature's second defendant: the fixture
// claim has one defendant, and pcs-frontend is not part of the local stack, so the citizen's view is the case
// data CCD gives them, which is what pcs-frontend renders.
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import { BrowserContext, Page } from '@playwright/test';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import {
  approveApplicationAsCaseworker,
  approveWarrantAsCaseworker,
  expectClaimantNext,
  scheduleEvictionOnApprovedWarrant,
} from '../support/approve-warrant';
import { documentTitle, expectEnforcementDocument } from '../support/case-documents';
import { createCaseWithWrit, expectCaseTab, requireCaseReference, signInAs } from '../support/enforcement-journeys';
import {
  applyForWarrantOfRestitution,
  closeApprovedWarrant,
  generateWarrantOfRestitution,
} from '../support/repeat-enforcement';
import { ENFORCEMENT_STATUS_TAB, enforcementSections, openCaseTab } from '../support/enforcement-status-tab';
import { markNoticeServedAsBailiff, markNoticeServedInXui } from '../support/notice-served';
import {
  DEFENDANT_LEGAL_REPRESENTATIVE,
  caseChangedForDefendants,
  canSetUpDefendants,
  defendantViews,
  enforcementDocuments,
  expectEnforcementTab,
  expectNoEnforcementDocuments,
  expectOnlyNoticesOfEviction,
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

Given('the eviction has been scheduled', async ({ world }) => {
  await scheduleEvictionOnApprovedWarrant(world);
});

Given('the notice of eviction has not been served', async ({ world }) => {
  expect(world.evictionScheduled, 'an eviction scheduled without dispensing with service').toBe(true);
  expect(world.serviceDispensedWith).toBe(false);
  expect(world.noticeServed, 'the notice of eviction marked as served').toBe(false);
});

Given('the bailiff answered {string} to {string}', async ({ world, $tags }, answer: string, question: string) => {
  if (question !== DISPENSE_WITH_SERVICE_QUESTION || (answer !== 'Yes' && answer !== 'No')) {
    return markPending({ ticket: 'HDPI-7344', summary: `answering "${answer}" to "${question}"` }, $tags);
  }
  await scheduleEvictionOnApprovedWarrant(world, answer);
});

When('the bailiff marks the notice of eviction as served', async ({ page, world }) => {
  expect(world.evictionScheduled && world.serviceDispensedWith === false, 'a notice of eviction to serve').toBe(true);
  await markNoticeServedInXui(page, world);
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

// The only enforcement document a defendant can get is a served warrant's notice of eviction, which pcs-api
// generates after the schedule commits, so whether or not it has arrived yet, nothing else may be there.
Then('they do not see the risk assessment document', async ({ page, context, world }) => {
  expectOnlyNoticesOfEviction(await defendantViews(page, context, world));
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

pendingSteps({ ticket: 'HDPI-7354', summary: 'Polling each defendant view for the generated notice of eviction' }, [
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

/** Links both defendants to the scenario's current case, which a later set-up step may have replaced. */
async function linkBothDefendants(world: EnforcementWorld, $tags: string[]): Promise<boolean> {
  if (!canSetUpDefendants()) {
    markPending(noDefendantUsers, $tags);
    return false;
  }
  await linkDefendantLegalRepresentative(world);
  await linkCitizenDefendant(world);
  return true;
}

When('a defendant opens the case', async ({ page, context, world, $tags }) => {
  if (!(await linkBothDefendants(world, $tags))) {
    return;
  }
  world.openedAs = DEFENDANT_LEGAL_REPRESENTATIVE;
  await viewAsEachDefendant(page, context, world);
});

When('a defendant views case file view', async ({ page, context, world, $tags }) => {
  if (!(await linkBothDefendants(world, $tags))) {
    return;
  }
  await viewAsEachDefendant(page, context, world);
});

/**
 * Feature 14: the sealed warrant or writ is generated on approval. A warrant of possession has its notice of
 * eviction served as well, so that the defendants can see the warrant's enforcement and the check that the
 * warrant itself is still withheld means something; a writ and a live warrant of restitution are never
 * visible to defendants. Waits until an internal user can see the document.
 */
Given('a {string} has been generated', async ({ world, $tags }, type: string) => {
  if (type === 'warrant of possession') {
    expect(world.warrantApproved, 'the Background\'s approved warrant').toBe(true);
    await scheduleEvictionOnApprovedWarrant(world);
    await markNoticeServedAsBailiff(world);
  } else if (type === 'writ of possession') {
    if (!(await createCaseWithWrit(world))) {
      return markPending({ ticket: 'HDPI-7419', summary: 'seeding a writ application outside a local stack' }, $tags);
    }
    try {
      await approveApplicationAsCaseworker(world, 'Writ');
    } finally {
      expectClaimantNext();
    }
  } else if (type === 'warrant of restitution') {
    await generateWarrantOfRestitution(world);
  } else {
    return markPending({ ticket: 'HDPI-7419', summary: `generating a ${type}` }, $tags);
  }
  world.currentDocument = documentTitle(type, world);
  await expectEnforcementDocument(requireCaseReference(world), world.currentDocument);
  caseChangedForDefendants(world);
});

Then('they do not see the {string} document', async ({ page, context, world }, type: string) => {
  const title = documentTitle(type, world);
  for (const view of await defendantViews(page, context, world)) {
    const filenames = enforcementDocuments(view).map(document => document.value?.document_filename ?? '');
    expect(filenames.filter(filename => filename.startsWith(title)), `"${title}…" documents the ${view.who} gets`)
      .toEqual([]);
  }
});

/**
 * The warrant's notice of eviction is served before it closes, because a closed warrant only stays visible to
 * defendants if they could see it while it was live.
 */
Given('the previous warrant ended with {string}', async ({ world }, returnName: string) => {
  await scheduleEvictionOnApprovedWarrant(world);
  await markNoticeServedAsBailiff(world);
  await closeApprovedWarrant(world, returnName);
  caseChangedForDefendants(world);
});

Given('the defendants could see the enforcement for that warrant', async ({ page, context, world }) => {
  await expectEnforcementTab(page, await viewAsEachDefendant(page, context, world), ENFORCEMENT_STATUS_TAB, true);
});

When('the claimant applies for a warrant of restitution', async ({ page, world }) => {
  expectClaimantNext();
  await applyForWarrantOfRestitution(page, world);
  caseChangedForDefendants(world);
});

async function expectNoEnforcementInformation(page: Page, context: BrowserContext, world: EnforcementWorld): Promise<void> {
  const views = await viewAsEachDefendant(page, context, world);
  await expectEnforcementTab(page, views, ENFORCEMENT_STATUS_TAB, false);
  await expectEnforcementTab(page, views, 'Risk assessment', false);
  expectNoEnforcementFields(views);
  expectNoEnforcementDocuments(views);
}

Then('defendants can no longer see any enforcement information', async ({ page, context, world }) => {
  await expectNoEnforcementInformation(page, context, world);
});

Then('defendants cannot see any enforcement information', async ({ page, context, world }) => {
  await expectNoEnforcementInformation(page, context, world);
});

// Approving the restitution keeps it live and the enforcement hidden; its final return ends it, and the earlier
// warrant, whose notice was served, shows again (OPEN-QUESTIONS 105).
Then(
  'they stay unable to see it until the bailiff records a return on the warrant of restitution',
  async ({ page, context, world }) => {
    try {
      await approveWarrantAsCaseworker(world, { feeOutstanding: false });
    } finally {
      expectClaimantNext();
    }
    await expectNoEnforcementInformation(page, context, world);

    await closeApprovedWarrant(world, 'Warrant withdrawn');
    await expectEnforcementTab(page, await viewAsEachDefendant(page, context, world), ENFORCEMENT_STATUS_TAB, true);
  }
);
