/**
 * Who the browser is signed in as, within one scenario. Set-up never signs anyone in or out: it goes through the
 * APIs and at most says who the scenario expects next, so a video shows a sign-in only when the scenario's own
 * journey changes user. The pages that open XUI call `ensureSignedIn`, which signs in only if the browser is
 * someone else.
 *
 * Reset before each scenario by the `enforcementExecutor` fixture: the module outlives a test within a worker.
 */
import { BrowserContext, Page } from '@playwright/test';
import { user, users } from '@data/user-data';
import { staff } from '@data/user-data/staff.user.data';
import { dismissCookieBanner } from '@config/cookie-banner';
import { assignDefendantCaseRole, isLocalStack, localUsers, signInToIdamSimulator } from '@config/local-stack.config';
import { performAction as performCommonAction } from '@utils/controller';
import { performAction } from '@utils/controller-enforcement';
import { withoutPagePacing } from '@utils/page-pacing';
import { showDemoCaptionStatus } from './demo-captions';
import { noteSignedInRole, restoreSession } from './demo-continuity';
import { userId } from './event-api';

export const CLAIMANT_LEGAL_REPRESENTATIVE = 'claimant legal representative';
export const DEFENDANT_LEGAL_REPRESENTATIVE_ROLE = 'defendant legal representative';

/** On AAT there are still no bailiff users (HDPI-7539), so those entries are undefined and the scenarios stay pending. */
const signInUsers: Record<string, () => string | undefined> = {
  [CLAIMANT_LEGAL_REPRESENTATIVE]: () =>
    isLocalStack() ? localUsers.claimantSolicitor : (user.localAuthorityOrg1Usr1.email as string),
  'hearing-centre-admin': () => (isLocalStack() ? localUsers.hearingCentreAdmin : user.hearingCenterAdmin.email),
  'hearing-centre-team-leader': () =>
    isLocalStack() ? localUsers.hearingCentreTeamLeader : staff.pcs_hearing_centre_team_leader_email,
  judge: () => (isLocalStack() ? localUsers.judge : users.find(candidate => candidate.user === 'County Court Judge')?.email),
  // On a local stack this user is given the [DEFENDANTSOLICITOR] case role through pcs-api testing support,
  // because link-defendant-solicitor-to-party answers 500 "Failed to create CCD case" there.
  [DEFENDANT_LEGAL_REPRESENTATIVE_ROLE]: () =>
    isLocalStack() ? localUsers.defendantSolicitor : user.defendantSolicitor.email,
  bailiff: () => (isLocalStack() ? localUsers.bailiff : undefined),
  'bailiff-admin': () => (isLocalStack() ? localUsers.bailiffAdmin : undefined),
  'bailiff-manager': () => (isLocalStack() ? localUsers.bailiffManager : undefined),
};

const FEATURE_USER_ROLES: Record<string, string> = {
  'hearing centre admin': 'hearing-centre-admin',
  'hearing centre team leader': 'hearing-centre-team-leader',
  'bailiff admin': 'bailiff-admin',
  'bailiff manager': 'bailiff-manager',
};

export const signInRoleFor = (name: string): string => FEATURE_USER_ROLES[name] ?? name;

export const emailFor = (role: string): string | undefined => signInUsers[signInRoleFor(role)]?.();

export function canSignInAs(role: string): boolean {
  return Boolean(emailFor(role));
}

export function requireEmailFor(role: string): string {
  const email = emailFor(role);
  if (!email) {
    throw new Error(`No user to act as "${role}" in this environment`);
  }
  return email;
}

let signedInRole: string = CLAIMANT_LEGAL_REPRESENTATIVE;
let expectedRole: string = CLAIMANT_LEGAL_REPRESENTATIVE;
const defendantLinkedCases = new Set<string>();

export const currentSignedInRole = (): string => signedInRole;

export const expectedSignedInRole = (): string => expectedRole;

export function expectSignedInAs(name: string): void {
  expectedRole = signInRoleFor(name);
}

export function resetSession(): void {
  signedInRole = CLAIMANT_LEGAL_REPRESENTATIVE;
  expectedRole = CLAIMANT_LEGAL_REPRESENTATIVE;
  noteSignedInRole(CLAIMANT_LEGAL_REPRESENTATIVE);
  defendantLinkedCases.clear();
}

export async function signInAs(name: string, page: Page): Promise<void> {
  expectSignedInAs(name);
  await ensureSignedIn(page);
}

/**
 * Signs the browser in as the expected user if it is someone else. In the demo project the session an earlier
 * scenario of the same feature left signed in as this user is carried into this one instead, but only when XUI
 * answers for it as this user (demo-continuity.ts); otherwise the sign-in runs.
 */
export async function ensureSignedIn(page: Page): Promise<void> {
  const role = expectedRole;
  const email = requireEmailFor(role);
  if (role === DEFENDANT_LEGAL_REPRESENTATIVE_ROLE) {
    await linkDefendantLegalRepresentative(email);
  }
  if (signedInRole === role) {
    return;
  }
  if (!(await restoreSession(role, email, page))) {
    await signInFresh(email, page);
  }
  signedInRole = role;
  noteSignedInRole(role);
}

async function linkDefendantLegalRepresentative(email: string): Promise<void> {
  const caseReference = String(process.env.CASE_NUMBER);
  if (defendantLinkedCases.has(caseReference)) {
    return;
  }
  if (isLocalStack()) {
    await assignDefendantCaseRole(caseReference, email, '[DEFENDANTSOLICITOR]');
  } else {
    await performCommonAction('getCaseAPIDynamic', { req: 'Link Solicitor', email, password: process.env.IDAM_PCS_USER_PASSWORD });
  }
  defendantLinkedCases.add(caseReference);
}

/** exui-common-lib's cookie banner only shows while there is no `hmcts-exui-cookies-<IDAM user id>-mc-accepted` cookie. */
export async function answerCookieQuestion(context: BrowserContext, email: string): Promise<void> {
  const domain = new URL(String(process.env.MANAGE_CASE_BASE_URL)).hostname;
  await context.addCookies([
    { name: `hmcts-exui-cookies-${await userId(email)}-mc-accepted`, value: 'false', domain, path: '/' },
  ]);
}

/** Signing in is plumbing rather than part of the journey, so it runs at full speed even in the demo project. */
async function signInFresh(email: string, page: Page): Promise<void> {
  await showDemoCaptionStatus(page, `Signing in as ${email}`);
  await withoutPagePacing(async () => {
    await clearBrowserSession(page);
    await answerCookieQuestion(page.context(), email);
    await performAction('navigateToUrl', `${process.env.MANAGE_CASE_BASE_URL}`);
    if (isLocalStack()) {
      await signInToIdamSimulator(page, email);
    } else {
      // AAT's IDAM has its own cookie banner on the sign-in page.
      await dismissCookieBanner(page, 'additional');
      await performAction('login', { email, password: process.env.IDAM_PCS_USER_PASSWORD });
    }
  });
  await showDemoCaptionStatus(page, undefined);
}

/**
 * Storage goes before the cookies: once the cookies are gone XUI's next request finds no session and redirects to
 * sign-in, and an evaluate racing that redirect fails. The page may also still be navigating after the previous
 * step, so a navigation that destroys the context is waited out and the clear retried.
 */
async function clearBrowserSession(page: Page): Promise<void> {
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      await page.evaluate(() => {
        try {
          localStorage.clear();
          sessionStorage.clear();
        } catch {
          // The page may not have an origin yet (about:blank).
        }
      });
      break;
    } catch (error) {
      if (!isNavigationRace(error)) {
        throw error;
      }
      await page.waitForLoadState('load', { timeout: 10_000 }).catch(() => undefined);
    }
  }
  await page.context().clearCookies();
}

function isNavigationRace(error: unknown): boolean {
  return error instanceof Error && /Execution context was destroyed|navigat/i.test(error.message);
}
