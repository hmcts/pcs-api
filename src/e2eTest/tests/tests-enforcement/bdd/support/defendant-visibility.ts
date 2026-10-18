/**
 * What defendants can see of enforcement (feature 14).
 *
 * pcs-api decides it per case view (`DefendantEnforcementView`): until a warrant's notice of eviction is
 * served, and while a warrant of restitution is live, a defendant gets no enforcement field and no
 * enforcement document. The checks therefore look at two things: the XUI case tabs for the defendant's legal
 * representative, and the case data CCD returns to each defendant, which is what pcs-frontend renders for a
 * citizen and what XUI's Case File View lists.
 *
 * Both defendants are given their case role through pcs-api testing support, which only a local stack has
 * users for, so outside a local stack these steps stay pending.
 */
import { BrowserContext, Page } from '@playwright/test';
import { expect } from '../fixtures/enforcement.fixtures';
import type { DefendantView, EnforcementWorld } from '../fixtures/enforcement.fixtures';
import { assignDefendantCaseRole, caseDataAs, isLocalStack, localUsers } from '@config/local-stack.config';
import { expectCaseTab, openCaseSummary, requireCaseReference, signInAs } from './enforcement-journeys';

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

export function canSetUpDefendants(): boolean {
  return isLocalStack();
}

/** Gives the local defendant solicitor the [DEFENDANTSOLICITOR] case role on the scenario's case. */
export async function linkDefendantLegalRepresentative(world: EnforcementWorld): Promise<void> {
  await assignDefendantCaseRole(requireCaseReference(world), localUsers.defendantSolicitor, '[DEFENDANTSOLICITOR]');
  world.defendantLegalRepresentative = localUsers.defendantSolicitor;
}

/** Gives the local citizen the [DEFENDANT] case role on the scenario's case, as their access code would. */
export async function linkCitizenDefendant(world: EnforcementWorld): Promise<void> {
  await assignDefendantCaseRole(requireCaseReference(world), localUsers.citizenDefendant, '[DEFENDANT]');
  world.citizenDefendant = localUsers.citizenDefendant;
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
 * the Enforcement folder, empty (OPEN-QUESTIONS 46); what pcs-api controls is that nothing is in it.
 */
export function expectNoEnforcementDocuments(views: DefendantView[]): void {
  for (const view of views) {
    expect(enforcementDocuments(view), `enforcement documents the ${view.who} gets`).toEqual([]);
  }
}
