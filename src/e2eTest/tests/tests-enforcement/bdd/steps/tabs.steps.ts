import { DataTable } from 'playwright-bdd';
import { Given, Then, When, expect } from '../fixtures/enforcement.fixtures';
import type { EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition, markPending, pendingSteps } from '../fixtures/pending';
import {
  canSignInAs,
  createCaseWithWarrant,
  createCaseWithWrit,
  expectCaseTab,
  openCaseSummary,
  requireCaseReference,
  signInAs,
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
  expectClaimantNext,
  scheduleEvictionOnApprovedWarrant,
  setUpWarrantAtStatus,
} from '../support/approve-warrant';
import { addBailiffNoteAsBailiff, setRiskLevelAsBailiff } from '../support/bailiff-events';
import { CLAIMANT_LEGAL_REPRESENTATIVE } from '../support/session';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { fixturePropertyAddress, isLocalStack } from '@config/local-stack.config';
import { markNoticeServedAsBailiff } from '../support/notice-served';
import {
  applyAsClaimant,
  closeWarrantWithReturn,
  generateWarrantOfRestitution,
} from '../support/repeat-enforcement';
import {
  CITIZEN_DEFENDANT,
  DEFENDANT_LEGAL_REPRESENTATIVE,
  defendantViews,
  expectEnforcementTab,
} from '../support/defendant-visibility';

const isDefendant = (user?: string): boolean => user === CITIZEN_DEFENDANT || user === DEFENDANT_LEGAL_REPRESENTATIVE;

const ENFORCEMENT_TABS = [ENFORCEMENT_STATUS_TAB, RISK_ASSESSMENT_TAB];

const RISK_INTERNAL_SECTIONS = ['Bailiff note', 'Risk assessment level'];
const CLAIMANT_RISK_SECTION = 'Claimant risk assessment';

function tabNotBuilt(tabName: string) {
  return { summary: `the "${tabName}" tab` };
}

const actual = (world: EnforcementWorld, quoted: string): string => world.aliases[quoted] ?? quoted;

Given('the case has a warrant of possession', async ({ world }) => {
  await setUpWarrantAtStatus(world, AWAITING_BAILIFF_ALLOCATION);
});

Given('the case has a warrant of possession {string}', async ({ world }, number: string) => {
  await setUpWarrantAtStatus(world, AWAITING_BAILIFF_ALLOCATION);
  world.aliases[number] = world.warrantNumber as string;
});

Given('the case has no generated warrant or writ', async ({ world }) => {
  await setUpWarrantAtStatus(world, 'Awaiting review');
});

Given('the claimant\'s warrant application is {string}', async ({ world }, status: string) => {
  await setUpWarrantAtStatus(world, status);
});

Given('a warrant has been generated', async ({ world }) => {
  await setUpWarrantAtStatus(world, AWAITING_BAILIFF_ALLOCATION);
});

const WARRANT_TYPES = ['warrant of possession', 'warrant of restitution'];

Given('the claimant applied for a {string}', async ({ world, $tags }, type: string) => {
  if (!WARRANT_TYPES.includes(type)) {
    return markPending({ ticket: 'HDPI-7419', summary: `seeding a ${type} application` }, $tags);
  }
  if (type === 'warrant of possession') {
    await createCaseWithWarrant(world);
    return;
  }
  approximatePrecondition('HDPI-7350', 'an earlier warrant, closed with a successful eviction, already shows the "Risk assessment" tab');
  await closeWarrantWithReturn(world, 'Eviction successful');
  await applyAsClaimant(world, 'warrant of restitution');
});

When('the {string} is generated', async ({ world, $tags }, type: string) => {
  if (!WARRANT_TYPES.includes(type)) {
    return markPending({ ticket: 'HDPI-7419', summary: `approving a ${type}` }, $tags);
  }
  await approveApplicationAsCaseworker(world, 'Warrant', { feeOutstanding: type === 'warrant of possession' });
});

async function setUpApprovedWrit(world: EnforcementWorld): Promise<void> {
  await createCaseWithWrit(world);
  await approveApplicationAsCaseworker(world, 'Writ');
}

Given(
  'the case has a writ of possession {string} approved by a judge\'s order dated {string}',
  async ({ world }, number: string, orderDate: string) => {
    try {
      await setUpApprovedWrit(world);
    } finally {
      expectClaimantNext();
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

Given('the case has only a writ of possession', async ({ world }) => {
  try {
    await setUpApprovedWrit(world);
  } finally {
    expectClaimantNext();
  }
});

Given('the notice of eviction {string}', async ({ world, $tags }, served: string) => {
  if (served === 'has not been served') {
    return;
  }
  if (served !== 'has been served' || !canSignInAs('bailiff')) {
    return markPending({ ticket: 'HDPI-7539', summary: 'a bailiff user to schedule the eviction and mark the notice served' }, $tags);
  }
  await scheduleEvictionOnApprovedWarrant(world);
  await markNoticeServedAsBailiff(world);
});

Given('no risk level or bailiff note has been added', async ({ world }) => {
  expect(world.riskLevel, 'a risk level set by this scenario').toBeUndefined();
  expect(world.enteredNote, 'a bailiff note added by this scenario').toBeUndefined();
});

Given('a warrant has been generated with a risk level and a bailiff note', async ({ world }) => {
  await setUpWarrantAtStatus(world, AWAITING_BAILIFF_ALLOCATION);
  try {
    await setRiskLevelAsBailiff('Medium');
    world.riskLevel = 'Medium';
    await addBailiffNoteAsBailiff('Rear gate is locked');
    world.enteredNote = 'Rear gate is locked';
  } finally {
    expectClaimantNext();
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

Then('I see {string} as {string}', async ({ page, world }, label: string, value: string) => {
  const [enforcement] = await enforcementSections(page);
  expect(enforcement.summary[label], label).toBe(actual(world, value));
});

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
  const print = tab.locator('a.govuk-button', { hasText: 'Print' });
  await expect(print).toBeVisible({ timeout: VERY_LONG_TIMEOUT });
  await expect(tab.getByRole('heading', { name: section, exact: true })).toBeVisible();
  const order = await tab.locator('a.govuk-button, h2').evaluateAll(elements =>
    elements.map(element => (element.textContent ?? '').trim()));
  const printIndex = order.findIndex(text => text.startsWith('Print'));
  expect(printIndex, 'the print button on the tab').toBeGreaterThanOrEqual(0);
  expect(printIndex, `the print button above the "${section}" section`).toBeLessThan(order.indexOf(section));
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

Given(
  'the case has a closed warrant {string} generated on {string}',
  async ({ world }, number: string, _generatedOn: string) => {
    await closeWarrantWithReturn(world, 'Eviction successful');
    world.aliases[number] = world.earlierWarrantNumbers.at(-1) as string;
  }
);

Given(
  'a live warrant of restitution {string} generated on {string}',
  async ({ world }, number: string, _generatedOn: string) => {
    world.aliases[number] = await generateWarrantOfRestitution(world);
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

Given('the case has a writ of possession', async ({ world }) => {
  try {
    await setUpApprovedWrit(world);
  } finally {
    expectClaimantNext();
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
