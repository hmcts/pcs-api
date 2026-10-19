/**
 * What defendants can see of enforcement (feature 14).
 *
 * pcs-api decides it per case view (`DefendantEnforcementView`): until a warrant's notice of eviction is
 * served, and while a warrant of restitution is live, a defendant gets no enforcement field and no
 * enforcement document. The checks therefore look at two things: the XUI case tabs for the defendant's legal
 * representative, and the case data CCD returns to each defendant, which is what pcs-frontend renders for a
 * citizen and what XUI's Case File View lists.
 *
 * Both defendants are given their case role through pcs-api testing support, which preview and AAT have too.
 * The legal representative is the permanent defendant solicitor everywhere; only a local stack has a citizen
 * user (the IDAM simulator's), so outside it the steps that need the citizen stay pending (HDPI-7539).
 */
import { BrowserContext, Page } from '@playwright/test';
import { VERY_LONG_TIMEOUT } from 'playwright.config';
import { expect } from '../fixtures/enforcement.fixtures';
import { approximatePrecondition } from '../fixtures/pending';
import type { DefendantView, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { assignDefendantCaseRole, caseDataAs, isLocalStack, localUsers } from '@config/local-stack.config';
import { user } from '@data/user-data';
import { expectCaseTab, openCaseSummary, requireCaseReference, signInAs } from './enforcement-journeys';
import { backgroundSetUp } from './demo-continuity';

export const DEFENDANT_LEGAL_REPRESENTATIVE = 'defendant legal representative';
export const CITIZEN_DEFENDANT = 'citizen defendant';

/** The Case File View folder enforcement documents are filed in (`FeatureCaseFileCategory.ENFORCEMENT`). */
export const ENFORCEMENT_CATEGORY_ID = 'enforcement';

/**
 * The case fields behind each enforcement tab. A citizen has no XUI tabs, so for them "does not see the tab"
 * means CCD gives them none of these fields.
 */
const TAB_FIELDS: Record<string, string[]> = {
  'Enforcement status': ['enforcementStatusTabMarkdown', 'enforcements', 'enforcementStatus'],
  'Risk assessment': [
    'showRiskAssessmentTab',
    'riskAssessmentPrintMarkdown',
    'riskAssessmentInternalMarkdown',
    'riskAssessmentClaimantMarkdown',
    'bailiffNotes',
    'currentRiskLevel',
    'currentRiskLevelReason',
  ],
};

/**
 * Every enforcement field a case view can carry that is not specific to one tab. None of them may reach a
 * defendant; `enforcements` only may once a warrant is visible to them.
 */
const OTHER_ENFORCEMENT_FIELDS = [
  'defendantsCanSeeEnforcement',
  'canRescheduleEviction',
  'canMarkNoticeOfEvictionServed',
  'showConfirmEvictionJourney',
  'formattedEvictionDate',
  'confirmEvictionSummaryMarkup',
  'hasUnsubmittedEnforcementData',
];

/** Whether there is a citizen user to link as a defendant: only the local stack's IDAM simulator has one. */
export function canLinkCitizenDefendant(): boolean {
  return isLocalStack();
}

/** Gives the defendant solicitor the [DEFENDANTSOLICITOR] case role on the scenario's case. */
export async function linkDefendantLegalRepresentative(world: EnforcementWorld): Promise<void> {
  await backgroundSetUp(async () => {
    const email = isLocalStack() ? localUsers.defendantSolicitor : user.defendantSolicitor.email;
    await assignDefendantCaseRole(requireCaseReference(world), email, '[DEFENDANTSOLICITOR]');
    world.defendantLegalRepresentative = email;
  });
}

/** Gives the local citizen the [DEFENDANT] case role on the scenario's case, as their access code would. */
export async function linkCitizenDefendant(world: EnforcementWorld): Promise<void> {
  await backgroundSetUp(async () => {
    await assignDefendantCaseRole(requireCaseReference(world), localUsers.citizenDefendant, '[DEFENDANT]');
    world.citizenDefendant = localUsers.citizenDefendant;
  });
}

/**
 * Has each linked defendant view the case: the legal representative opens it in XUI and stays signed in, so
 * the tab checks that follow see their page; the citizen's view is the case data CCD gives them.
 */
export async function viewAsEachDefendant(page: Page, context: BrowserContext, world: EnforcementWorld): Promise<DefendantView[]> {
  const caseReference = requireCaseReference(world);
  const views: DefendantView[] = [];
  if (world.citizenDefendant) {
    views.push({ who: CITIZEN_DEFENDANT, inXui: false, caseData: await caseDataAs(caseReference, world.citizenDefendant) });
  }
  if (world.defendantLegalRepresentative) {
    views.push({
      who: DEFENDANT_LEGAL_REPRESENTATIVE,
      inXui: true,
      caseData: await caseDataAs(caseReference, world.defendantLegalRepresentative),
    });
    await signInAs(DEFENDANT_LEGAL_REPRESENTATIVE, page, context);
    await openCaseSummary(page);
  }
  if (views.length === 0) {
    throw new Error('No defendant is linked to the case: start with the defendant set-up steps in the Background');
  }
  world.defendantViews = views;
  return views;
}

/**
 * Has each linked defendant view the case once the enforcement is theirs to see. The event that opens it up is
 * submitted through the API, so the case data can still be the one from before it: the first defendant's view
 * is re-read until it carries the "Enforcement status" tab.
 */
export async function viewAsEachDefendantOnceEnforcementVisible(
  page: Page,
  context: BrowserContext,
  world: EnforcementWorld
): Promise<DefendantView[]> {
  const caseReference = requireCaseReference(world);
  const defendant = world.citizenDefendant ?? world.defendantLegalRepresentative;
  if (defendant) {
    await expect
      .poll(async () => Boolean((await caseDataAs(caseReference, defendant)).enforcementStatusTabMarkdown), {
        timeout: VERY_LONG_TIMEOUT,
        intervals: [1_000],
      })
      .toBe(true);
  }
  return viewAsEachDefendant(page, context, world);
}

/** The views the last "defendant views the case" step captured, or fresh ones if there are none. */
export async function defendantViews(page: Page, context: BrowserContext, world: EnforcementWorld): Promise<DefendantView[]> {
  return world.defendantViews ?? viewAsEachDefendant(page, context, world);
}

/** Forgets the captured views, so the next check looks again after the case has changed. */
export function caseChangedForDefendants(world: EnforcementWorld): void {
  world.defendantViews = undefined;
}

export async function expectEnforcementTab(page: Page, views: DefendantView[], tabName: string, shown: boolean): Promise<void> {
  const fields = TAB_FIELDS[tabName];
  if (!fields) {
    throw new Error(`No enforcement tab known as "${tabName}". Expected one of: ${Object.keys(TAB_FIELDS).join(', ')}`);
  }
  for (const view of views) {
    if (view.inXui) {
      await expectCaseTab(page, tabName, shown);
    }
    if (shown) {
      expect(view.caseData.enforcementStatusTabMarkdown, `the tab the ${view.who} gets`).toBeTruthy();
      expect(String(view.caseData.enforcementStatusTabMarkdown)).not.toContain('Claimant eviction confirmation');
    } else {
      for (const field of fields) {
        expect(view.caseData[field], `${field} in the case data the ${view.who} gets`).toBeUndefined();
      }
    }
  }
}

/** No enforcement field reaches any defendant, whichever tab it belongs to. */
export function expectNoEnforcementFields(views: DefendantView[]): void {
  const every = [...Object.values(TAB_FIELDS).flat(), ...OTHER_ENFORCEMENT_FIELDS];
  for (const view of views) {
    for (const field of every) {
      expect(view.caseData[field], `${field} in the case data the ${view.who} gets`).toBeUndefined();
    }
  }
}

type CaseDocument = { id?: string; value?: { category_id?: string; document_filename?: string } };

export function enforcementDocuments(view: DefendantView): CaseDocument[] {
  const all = (view.caseData.allDocuments ?? []) as CaseDocument[];
  return all.filter(document => document.value?.category_id === ENFORCEMENT_CATEGORY_ID);
}

/**
 * No defendant gets an enforcement document. CCD categories cannot be restricted by role, so XUI still draws
 * the Enforcement folder, empty (OPEN-QUESTIONS 50); what pcs-api controls is that nothing is in it.
 */
export function expectNoEnforcementDocuments(views: DefendantView[]): void {
  for (const view of views) {
    expect(enforcementDocuments(view), `enforcement documents the ${view.who} gets`).toEqual([]);
  }
}

/**
 * Every enforcement document a defendant gets is a notice of eviction, the only one pcs-api lets them see. pcs-api
 * names each generated document "<title> - <number>" (`EnforcementOutput.title`).
 */
export function expectOnlyNoticesOfEviction(views: DefendantView[]): void {
  for (const view of views) {
    const filenames = enforcementDocuments(view).map(document => document.value?.document_filename ?? '');
    expect(filenames.filter(filename => !filename.startsWith('Notice of eviction - ')),
      `enforcement documents other than the notice of eviction the ${view.who} gets`).toEqual([]);
  }
}

/**
 * Links both defendants if the scenario has not: a defendant legal representative and a citizen, given their
 * case roles through testing support. Feature 08 talks about "defendants" without setting any up.
 */
export async function ensureDefendantsLinked(world: EnforcementWorld): Promise<void> {
  if (world.defendantLegalRepresentative || world.citizenDefendant) {
    return;
  }
  approximatePrecondition(
    'HDPI-7539',
    canLinkCitizenDefendant()
      ? 'the defendant legal representative and a citizen defendant are given their case roles through testing support'
      : 'the defendant legal representative is given their case role through testing support; there is no citizen user outside a local stack'
  );
  await linkDefendantLegalRepresentative(world);
  if (canLinkCitizenDefendant()) {
    await linkCitizenDefendant(world);
  }
  caseChangedForDefendants(world);
}

/** The linked defendants, by who they are and their email. */
function linkedDefendants(world: EnforcementWorld): { who: string; email: string }[] {
  return [
    ...(world.defendantLegalRepresentative ? [{ who: DEFENDANT_LEGAL_REPRESENTATIVE, email: world.defendantLegalRepresentative }] : []),
    ...(world.citizenDefendant ? [{ who: CITIZEN_DEFENDANT, email: world.citizenDefendant }] : []),
  ];
}

/**
 * Waits until the case data CCD gives the defendant carries a notice of eviction in the Enforcement folder.
 * pcs-api generates the notice after the schedule commits, so it can arrive after the notice is served.
 */
export async function expectNoticeOfEvictionVisibleTo(world: EnforcementWorld, who?: string): Promise<void> {
  const caseReference = requireCaseReference(world);
  const defendants = linkedDefendants(world).filter(defendant => !who || defendant.who === who);
  expect(defendants, `a linked ${who ?? 'defendant'}`).not.toHaveLength(0);
  for (const defendant of defendants) {
    await expect
      .poll(async () => {
        const view: DefendantView = { who: defendant.who, inXui: false, caseData: await caseDataAs(caseReference, defendant.email) };
        return enforcementDocuments(view).map(document => document.value?.document_filename ?? '')
          .some(filename => filename.startsWith('Notice of eviction - '));
      }, { message: `a notice of eviction the ${defendant.who} gets`, timeout: 120_000, intervals: [2_000, 5_000] })
      .toBe(true);
  }
}
