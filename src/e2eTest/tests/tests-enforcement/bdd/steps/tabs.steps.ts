// Enforcement status tab, risk assessment tab, case file view documents and defendant visibility (features 12-14).
import { DataTable } from 'playwright-bdd';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import {
  createCaseWithWarrant,
  createCaseWithWrit,
  expectCaseTab,
  openCaseSummary,
  requireCaseReference,
  signInAs,
  startEvent,
} from '../support/enforcement-journeys';
import {
  ENFORCEMENT_STATUS_TAB,
  activeTab,
  enforcementSections,
  expectEnforcementSection,
  openCaseTab,
  tabSections,
  todayTabDate,
} from '../support/enforcement-status-tab';
import { RISK_ASSESSMENT_TAB } from '../support/case-tabs';
import {
  AWAITING_BAILIFF_ALLOCATION,
  approveApplicationAsCaseworker,
  restoreClaimantSession,
  scheduleEvictionOnApprovedWarrant,
  setUpWarrantAtStatus,
} from '../support/approve-warrant';
import { ADD_BAILIFF_NOTE, SET_RISK_LEVEL, addBailiffNote, chooseRiskLevel, continueButton } from '../support/bailiff-events';
import { CLAIMANT_LEGAL_REPRESENTATIVE } from '../support/session';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { fixturePropertyAddress, isLocalStack } from '@config/local-stack.config';
import { markNoticeServedAsBailiff } from '../support/notice-served';
import {
  applyForWarrantOfRestitution,
  closeWarrantWithReturn,
  generateWarrantOfRestitution,
} from '../support/repeat-enforcement';
import {
  CITIZEN_DEFENDANT,
  DEFENDANT_LEGAL_REPRESENTATIVE,
  canSetUpDefendants,
  defendantViews,
  expectEnforcementTab,
} from '../support/defendant-visibility';

/** Whether a scenario opened the case as one of the defendants. */
const isDefendant = (user?: string): boolean => user === CITIZEN_DEFENDANT || user === DEFENDANT_LEGAL_REPRESENTATIVE;

/** The two tabs the enforcement work adds; any other tab name is still someone else's feature. */
const ENFORCEMENT_TABS = [ENFORCEMENT_STATUS_TAB, RISK_ASSESSMENT_TAB];

/** The internal sections of the "Risk assessment" tab. */
const RISK_INTERNAL_SECTIONS = ['Bailiff note', 'Risk assessment level'];
const CLAIMANT_RISK_SECTION = 'Claimant risk level';

function tabNotBuilt(tabName: string) {
  return { summary: `the "${tabName}" tab` };
}

/** What the scenario produced for a value the feature quotes illustratively, such as a warrant number. */
const actual = (world: EnforcementWorld, quoted: string): string => world.aliases[quoted] ?? quoted;

/** An approved warrant, so a generated one: the tabs only appear once a warrant has been generated. */
Given('the case has a warrant of possession', async ({ page, context, world }) => {
  await setUpWarrantAtStatus(page, context, world, AWAITING_BAILIFF_ALLOCATION);
});

Given('the case has a warrant of possession {string}', async ({ page, context, world }, number: string) => {
  await setUpWarrantAtStatus(page, context, world, AWAITING_BAILIFF_ALLOCATION);
  world.aliases[number] = world.warrantNumber as string;
});

Given('the case has no generated warrant or writ', async ({ page, context, world }) => {
  // An application awaiting review: submitted and paid for, but not approved, so nothing is generated.
  await setUpWarrantAtStatus(page, context, world, 'Awaiting review');
});

Given('the claimant\'s warrant application is {string}', async ({ page, context, world }, status: string) => {
  await setUpWarrantAtStatus(page, context, world, status);
});

Given('a warrant has been generated', async ({ page, context, world }) => {
  await setUpWarrantAtStatus(page, context, world, AWAITING_BAILIFF_ALLOCATION);
});

const WARRANT_TYPES = ['warrant of possession', 'warrant of restitution'];

// pcs-api only offers a warrant of restitution once the case has had a warrant, so the restitution is applied
// for after an earlier warrant closed with a successful eviction. That warrant was generated too, so the
// "Risk assessment" tab is already on the case before the restitution is (RiskAssessmentTabView keeps it once
// the warrant closes), and the tab then moves on to the restitution.
Given('the claimant applied for a {string}', async ({ page, context, world, $tags }, type: string) => {
  if (!WARRANT_TYPES.includes(type)) {
    return markPending({ ticket: 'HDPI-7419', summary: `seeding a ${type} application` }, $tags);
  }
  if (type === 'warrant of possession') {
    await createCaseWithWarrant(world);
    return;
  }
  approximatePrecondition('HDPI-7350', 'an earlier warrant, closed with a successful eviction, already shows the "Risk assessment" tab');
  await closeWarrantWithReturn(page, context, world, 'Eviction successful');
  await applyForWarrantOfRestitution(page, world);
});

When('the {string} is generated', async ({ page, context, world, $tags }, type: string) => {
  if (!WARRANT_TYPES.includes(type)) {
    return markPending({ ticket: 'HDPI-7419', summary: `approving a ${type}` }, $tags);
  }
  try {
    // Restitution has no fee, so the application is already awaiting review.
    await approveApplicationAsCaseworker(page, world, 'Warrant', { feeOutstanding: type === 'warrant of possession' });
  } catch (error) {
    await restoreClaimantSession(page, context);
    throw error;
  }
});

async function setUpApprovedWrit(
  page: Parameters<typeof approveApplicationAsCaseworker>[0],
  world: EnforcementWorld,
  $tags: string[]
): Promise<boolean> {
  if (!(await createCaseWithWrit(world))) {
    markPending({ ticket: 'HDPI-7419', summary: 'seeding a writ application outside a local stack' }, $tags);
    return false;
  }
  await approveApplicationAsCaseworker(page, world, 'Writ');
  return true;
}

Given(
  'the case has a writ of possession {string} approved by a judge\'s order dated {string}',
  async ({ page, context, world, $tags }, number: string, orderDate: string) => {
    try {
      if (!(await setUpApprovedWrit(page, world, $tags))) {
        return;
      }
    } finally {
      await restoreClaimantSession(page, context);
    }
    world.aliases[number] = world.warrantNumber as string;
    const now = new Date();
    world.aliases[orderDate] = `${String(now.getDate()).padStart(2, '0')}/${String(now.getMonth() + 1).padStart(2, '0')}/${now.getFullYear()}`;
    approximatePrecondition(
      'HDPI-7353',
      `no judge's order date is recorded: the date of transfer is the day the caseworker approved the writ, `
        + `so "${orderDate}" is today`
    );
  }
);

Given('the case has only a writ of possession', async ({ page, context, world, $tags }) => {
  try {
    await setUpApprovedWrit(page, world, $tags);
  } finally {
    await restoreClaimantSession(page, context);
  }
});

Given('the notice of eviction {string}', async ({ page, context, world, $tags }, served: string) => {
  if (served === 'has not been served') {
    // True by construction: the scenario never marks the notice of eviction as served.
    return;
  }
  if (served !== 'has been served' || !canSetUpDefendants()) {
    return markPending({ ticket: 'HDPI-7539', summary: 'defendants seeing the enforcement once the notice is served' }, $tags);
  }
  await scheduleEvictionOnApprovedWarrant(page, context, world);
  await markNoticeServedAsBailiff(page, context, world);
});

Given('no risk level or bailiff note has been added', async ({ world }) => {
  // True by construction: the warrant was only just approved.
  expect(world.riskLevel, 'a risk level set by this scenario').toBeUndefined();
  expect(world.enteredNote, 'a bailiff note added by this scenario').toBeUndefined();
});

Given('a warrant has been generated with a risk level and a bailiff note', async ({ page, context, world }) => {
  await setUpWarrantAtStatus(page, context, world, AWAITING_BAILIFF_ALLOCATION);
  try {
    await signInAs('bailiff', page, context);
    await openCaseSummary(page);
    await startEvent(SET_RISK_LEVEL);
    await chooseRiskLevel(page, 'Medium');
    await continueButton(page).click();
    await expect(page.getByRole('heading', { name: 'Risk level set to Medium' })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
    world.riskLevel = 'Medium';

    await openCaseSummary(page);
    await startEvent(ADD_BAILIFF_NOTE);
    await page.getByLabel(addBailiffNote.noteLabel).fill('Rear gate is locked');
    await continueButton(page).click();
    await expect(page.getByRole('heading', { name: 'Bailiff note added' })).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
    world.enteredNote = 'Rear gate is locked';
  } finally {
    await restoreClaimantSession(page, context);
  }
});

When('a bailiff opens the {string} tab', async ({ page, context, world }, tabName: string) => {
  await signInAs('bailiff', page, context);
  await openCaseTab(page, requireCaseReference(world), tabName);
});

When('a caseworker opens the {string} tab', async ({ page, context, world }, tabName: string) => {
  await signInAs('hearing-centre-admin', page, context);
  await openCaseTab(page, requireCaseReference(world), tabName);
});

When('the claimant legal representative opens the {string} tab', async ({ page, context, world }, tabName: string) => {
  await signInAs(CLAIMANT_LEGAL_REPRESENTATIVE, page, context);
  await openCaseTab(page, requireCaseReference(world), tabName);
});

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

Then('the {string} tab is {string}', async ({ page, context, world, $tags }, tabName: string, visibility: string) => {
  if (!ENFORCEMENT_TABS.includes(tabName)) {
    return markPending(tabNotBuilt(tabName), $tags);
  }
  if (visibility !== 'shown' && visibility !== 'hidden') {
    throw new Error(`Unknown tab visibility "${visibility}". Expected "shown" or "hidden"`);
  }
  if (isDefendant(world.openedAs)) {
    // Checks the case data CCD gives the defendant as well as, for a legal representative, the XUI tab.
    await expectEnforcementTab(page, await defendantViews(page, context, world), tabName, visibility === 'shown');
    return;
  }
  await expectCaseTab(page, tabName, visibility === 'shown');
});

Then('the {string} tab is shown', async ({ page }, tabName: string) => {
  await openCaseSummary(page);
  await expectCaseTab(page, tabName, true);
});

Then('there is no {string} tab', async ({ page, context, world, $tags }, tabName: string) => {
  if (!ENFORCEMENT_TABS.includes(tabName)) {
    return markPending(tabNotBuilt(tabName), $tags);
  }
  if (isDefendant(world.openedAs)) {
    await expectEnforcementTab(page, await defendantViews(page, context, world), tabName, false);
    return;
  }
  await expectCaseTab(page, tabName, false);
});

Then('I see {string} as a heading', async ({ page, world }, heading: string) => {
  await expect(activeTab(page).getByRole('heading', { level: 2, name: actual(world, heading), exact: true }))
    .toBeVisible({ timeout: VERY_LONG_TIMEOUT });
});

Then(
  'I see the sections {string}, {string}, {string} and {string}',
  async ({ page }, first: string, second: string, third: string, fourth: string) => {
    const [enforcement] = await enforcementSections(page);
    expect(Object.keys(enforcement.sections)).toEqual([first, second, third, fourth]);
  }
);

// A row in the summary under the first warrant or writ on the "Enforcement status" tab.
Then('I see {string} as {string}', async ({ page, world }, label: string, value: string) => {
  const [enforcement] = await enforcementSections(page);
  expect(enforcement.summary[label], label).toBe(actual(world, value));
});

/**
 * A section of the open tab shows a placeholder or an introduction. The claimant's application is dated the
 * day the scenario submitted it, so a date in the quoted text stands for today.
 */
Then('{string} shows {string}', async ({ page }, section: string, text: string) => {
  const expected = text.replace(/\bon \d{1,2} [A-Z][a-z]+ \d{4}\b/, `on ${todayTabDate()}`);
  if (expected !== text) {
    approximatePrecondition('HDPI-7350', `the application was submitted today, so "${text}" is dated ${todayTabDate()}`);
  }
  await expect.poll(async () => (await tabSections(page))[section]?.text, { timeout: VERY_LONG_TIMEOUT })
    .toBe(expected);
});

Then('I see the green print button between the tabs and the {string} section', async ({ page }, section: string) => {
  const tab = activeTab(page);
  // govuk-button is the GOV.UK green button.
  const print = tab.locator('a.govuk-button', { hasText: 'Print' });
  await expect(print).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  await expect(tab.getByRole('heading', { name: section, exact: true })).toBeVisible();
  // The button and the section headings in page order.
  const order = await tab.locator('a.govuk-button, h2').evaluateAll(elements =>
    elements.map(element => (element.textContent ?? '').trim()));
  expect(order.indexOf('Print'), 'the print button on the tab').toBeGreaterThanOrEqual(0);
  expect(order.indexOf('Print'), `the print button above the "${section}" section`).toBeLessThan(order.indexOf(section));
});

Then('I see the claimant\'s answers to:', async ({ page }, table: DataTable) => {
  const answers = (await tabSections(page))[CLAIMANT_RISK_SECTION];
  expect(answers, `the "${CLAIMANT_RISK_SECTION}" section`).toBeDefined();
  for (const [question] of table.raw()) {
    expect(Object.keys(answers.rows), 'the claimant\'s answers').toContain(question);
  }
});

Then('the risk level and bailiff notes are {string}', async ({ page, context, world }, visibility: string) => {
  if (isDefendant(world.openedAs)) {
    expect(visibility, 'defendants never see the risk level or bailiff notes').toBe('hidden');
    await expectEnforcementTab(page, await defendantViews(page, context, world), RISK_ASSESSMENT_TAB, false);
    return;
  }
  const tab = page.getByRole('tab', { name: RISK_ASSESSMENT_TAB, exact: true });
  if (!(await tab.count())) {
    expect(visibility, 'no "Risk assessment" tab, so nothing on it is shown').toBe('hidden');
    return;
  }
  await openCaseTab(page, requireCaseReference(world), RISK_ASSESSMENT_TAB);
  const headings = Object.keys(await tabSections(page));
  if (visibility === 'shown') {
    expect(headings).toEqual(expect.arrayContaining(RISK_INTERNAL_SECTIONS));
  } else {
    expect(headings).toContain(CLAIMANT_RISK_SECTION);
    expect(headings).not.toEqual(expect.arrayContaining([RISK_INTERNAL_SECTIONS[0]]));
    expect(headings).not.toEqual(expect.arrayContaining([RISK_INTERNAL_SECTIONS[1]]));
  }
});

Then('the claimant risk assessment section says {string} instead of {string}', async ({ page }, you: string, claimant: string) => {
  const text = (await tabSections(page))[CLAIMANT_RISK_SECTION]?.text ?? '';
  expect(text).toMatch(new RegExp(`\\b${you}\\b`, 'i'));
  expect(text).not.toContain(claimant);
});

/** Feature 06: the claimant's answer, under "Claimant eviction confirmation" on the status tab. */
Then('the {string} tab shows my answers under {string}', async ({ page, world }, tabName: string, section: string) => {
  expect(tabName).toBe(ENFORCEMENT_STATUS_TAB);
  await expectEnforcementSection(page, requireCaseReference(world), section, block => {
    const answers = Object.entries(block?.rows ?? {}).filter(([question]) => question.startsWith('Can you'));
    expect(answers, 'the attendance answer').toHaveLength(1);
    if (world.claimantConfirmedEviction !== undefined) {
      expect(answers[0][1]).toBe(world.claimantConfirmedEviction ? 'Yes' : 'No');
    }
  });
});

Then('the case header shows the case number and property address', async ({ page, world }) => {
  const header = page.locator('ccd-case-header');
  // XUI may show the reference with or without dashes.
  const reference = requireCaseReference(world);
  const dashed = reference.replace(/(.{4})(?=.)/g, '$1-');
  await expect(header).toContainText(new RegExp(`${reference}|${dashed}`), { timeout: VERY_LONG_TIMEOUT });
  await expect(header).toContainText('Property address');
  if (isLocalStack()) {
    await expect(header).toContainText(fixturePropertyAddress.AddressLine1);
  }
});

Then('the case header does not show {string}', async ({ page, world }, number: string) => {
  await expect(page.locator('ccd-case-header')).not.toContainText(actual(world, number));
});

// The generation dates are illustrative: both are generated today, the closed warrant first.
Given(
  'the case has a closed warrant {string} generated on {string}',
  async ({ page, context, world }, number: string, _generatedOn: string) => {
    await closeWarrantWithReturn(page, context, world, 'Eviction successful');
    world.aliases[number] = world.earlierWarrantNumbers.at(-1) as string;
  }
);

Given(
  'a live warrant of restitution {string} generated on {string}',
  async ({ page, context, world }, number: string, _generatedOn: string) => {
    world.aliases[number] = await generateWarrantOfRestitution(page, context, world);
  }
);

Then('{string} is listed above {string}', async ({ page, world }, upper: string, lower: string) => {
  const expected = world.earlierWarrantNumbers.length + (world.warrantNumber ? 1 : 0);
  await expect
    .poll(async () => (await enforcementSections(page)).length, { timeout: VERY_LONG_TIMEOUT })
    .toBe(expected);
  const headings = (await enforcementSections(page)).map(section => section.heading);
  const upperIndex = headings.indexOf(actual(world, upper));
  const lowerIndex = headings.indexOf(actual(world, lower));
  expect(upperIndex, `${upper} is listed`).toBeGreaterThanOrEqual(0);
  expect(lowerIndex, `${lower} is listed`).toBeGreaterThanOrEqual(0);
  expect(upperIndex, `${upper} is listed above ${lower}`).toBeLessThan(lowerIndex);
});

Given('the case has a writ of possession', async ({ page, context, world, $tags }) => {
  try {
    await setUpApprovedWrit(page, world, $tags);
  } finally {
    await restoreClaimantSession(page, context);
  }
});

pendingSteps({ ticket: 'HDPI-7547', summary: 'Confirm the eviction only asks whether the claimant can attend' }, [
  'the claimant has completed {string}',
  '{string} shows:',
]);

pendingSteps({ ticket: 'HDPI-7350', summary: 'XUI sanitises case markdown, so the tab cannot open the print dialogue' }, [
  'I select the print button',
  'the browser print dialogue opens with the risk assessment details',
]);

pendingSteps({ ticket: 'HDPI-7351', summary: 'Enforcement status tab' }, [
  'the case {string} tab shows the event that changed it, who submitted it and when',
]);
