/**
 * Demo project only: consecutive scenarios of one feature whose Background text is identical run against the
 * case it set up and the session it signed in, so the video reads as one pass. CI's `enforcement-bdd` project
 * is untouched, because there a cascading failure costs more than a slow run.
 *
 * It cannot film the wrong user: a restored session is kept only once XUI, asked whose the cookies are, names
 * the user being signed in as, and the page is then opened on XUI again rather than left as whoever rendered it.
 *
 * It cannot film the wrong state: the case is handed on only when the Background text is identical, no event
 * was submitted on it after the Background finished, its state and enforcements re-read from the data store are
 * unchanged, and the scenario did not fail. Anything else makes the next scenario set up its own case. Set-up
 * outside a {@link backgroundSetUp} helper cannot be skipped, so such a Background is never reused.
 */
import { BrowserContext, Cookie, Page, TestInfo, test } from '@playwright/test';
import Axios from 'axios';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { performAction } from '@utils/controller-enforcement';
import { DEMO_PROJECT } from './demo-pacing';

export type BddStep = { textWithKeyword?: string; isBg?: boolean };

type HeldCase = {
  key: string;
  caseReference: string;
  world: Record<string, unknown>;
  caseNumber?: string;
  caseDetails: { id: string; fid: string; state: string };
  annotations: { type: string; description?: string }[];
  fingerprint: string;
  changed: boolean;
};

let enabled = false;
let scope: string | undefined;
let held: HeldCase | undefined;
const notReusable = new Set<string>();
const sessions = new Map<string, Cookie[]>();
let currentRole = '';

let started = false;
let key: string | undefined;
let reusing = false;
let inBackground = false;
let setUpDepth = 0;
let unskippableSetUp = false;
const backgroundCases = new Set<string>();

export async function beginScenario(testInfo: TestInfo): Promise<void> {
  enabled = testInfo.project.name === DEMO_PROJECT;
  started = false;
  key = undefined;
  reusing = false;
  inBackground = false;
  setUpDepth = 0;
  unskippableSetUp = false;
  backgroundCases.clear();
  if (!enabled) {
    return;
  }
  if (scope !== testInfo.file) {
    await releaseHeldCase();
    notReusable.clear();
    sessions.clear();
    scope = testInfo.file;
  }
}

export async function enteringStep(steps: BddStep[], stepIndex: number, world: object): Promise<void> {
  if (!enabled) {
    return;
  }
  if (!started) {
    started = true;
    key = backgroundKey(steps);
    reusing = key !== undefined && (await claimHeldCase(key, world));
  }
  const backgroundStep = steps[stepIndex]?.isBg === true;
  if (inBackground && !backgroundStep) {
    await holdCase(world);
  }
  inBackground = backgroundStep;
}

export async function endScenario(testInfo: TestInfo, world: { caseReference?: string }, context: BrowserContext): Promise<boolean> {
  if (!enabled) {
    return false;
  }
  inBackground = false;
  await rememberSession(context, currentRole);
  // A scenario that stopped at a pending step keeps its case: anything it did would show in the fingerprint.
  const failed = testInfo.status === 'failed' || testInfo.status === 'timedOut';
  if (held && (held.changed || failed)) {
    await releaseHeldCase();
  }
  return held !== undefined && held.caseReference === world.caseReference;
}

export async function backgroundSetUp(setUp: () => Promise<void>): Promise<void> {
  if (!enabled || !inBackground) {
    await setUp();
    return;
  }
  if (reusing) {
    return;
  }
  setUpDepth++;
  try {
    await setUp();
  } finally {
    setUpDepth--;
    if (setUpDepth === 0 && process.env.CASE_NUMBER) {
      backgroundCases.add(process.env.CASE_NUMBER);
    }
  }
}

export function recordCaseChange(caseReference: string | undefined): void {
  if (!enabled || !caseReference) {
    return;
  }
  if (inBackground) {
    if (setUpDepth > 0) {
      return;
    }
    unskippableSetUp = true;
  }
  if (held?.caseReference === caseReference.replace(/-/g, '')) {
    held.changed = true;
  }
}

const CASE_EVENT_SUBMIT = /\/cases\/(\d{16})\/events(?:[/?]|$)/;

export function recordBrowserRequest(method: string, url: string): void {
  if (!enabled || method === 'GET') {
    return;
  }
  const submitted = CASE_EVENT_SUBMIT.exec(url);
  if (submitted) {
    recordCaseChange(submitted[1]);
  }
}

export async function rememberSession(context: BrowserContext, role: string): Promise<void> {
  if (!enabled) {
    return;
  }
  try {
    sessions.set(role, await context.cookies());
  } catch {
    // The context is already closing: the next scenario signs in instead.
  }
}

export async function restoreSession(role: string, email: string, page: Page): Promise<boolean> {
  const context = page.context();
  const cookies = enabled ? sessions.get(role) : undefined;
  if (!cookies?.length) {
    return false;
  }
  await context.clearCookies();
  await context.addCookies(cookies);
  if ((await xuiSignedInAs(context)) === email.toLowerCase()) {
    // Swapping the cookies under the page leaves the user who rendered it on screen, and a goto differing only
    // by fragment would not reload it, so the page is opened on XUI again.
    await page.goto(String(process.env.MANAGE_CASE_BASE_URL), { waitUntil: 'domcontentloaded' });
    return true;
  }
  sessions.delete(role);
  await context.clearCookies();
  return false;
}

export function noteSignedInRole(role: string): void {
  currentRole = role;
}

/** XUI serves its Angular app to anyone, so a page that loads proves nothing; `/api/user/details` answers 401 without a session. */
async function xuiSignedInAs(context: BrowserContext): Promise<string | undefined> {
  const base = String(process.env.MANAGE_CASE_BASE_URL);
  try {
    const response = await context.request.get(`${base}/api/user/details`);
    if (!response.ok()) {
      return undefined;
    }
    const details = (await response.json()) as { userInfo?: { email?: string } };
    return details.userInfo?.email?.toLowerCase();
  } catch {
    return undefined;
  }
}

function backgroundKey(steps: BddStep[]): string | undefined {
  const background = steps.filter(step => step.isBg).map(step => step.textWithKeyword ?? '');
  return background.length ? `${scope}\n${background.join('\n')}` : undefined;
}

async function claimHeldCase(backgroundKey: string, world: object): Promise<boolean> {
  if (notReusable.has(backgroundKey) || !held || held.key !== backgroundKey || held.changed) {
    return false;
  }
  if ((await caseFingerprint(held.caseReference)) !== held.fingerprint) {
    await releaseHeldCase();
    return false;
  }
  Object.assign(world, plainCopy(held.world));
  process.env.CASE_NUMBER = held.caseNumber;
  Object.assign(caseInfo, held.caseDetails);
  await restoreDefendantDetails(held.world);
  for (const annotation of held.annotations) {
    testAnnotations().push({ ...annotation });
  }
  return true;
}

async function holdCase(world: { caseReference?: string }): Promise<void> {
  if (reusing || key === undefined) {
    return;
  }
  if (unskippableSetUp || backgroundCases.size > 1) {
    notReusable.add(key);
  }
  if (held?.key !== key) {
    await releaseHeldCase();
  }
  if (notReusable.has(key) || !world.caseReference) {
    return;
  }
  held = {
    key,
    caseReference: world.caseReference,
    world: plainCopy({ ...world }),
    caseNumber: process.env.CASE_NUMBER,
    caseDetails: { ...caseInfo },
    annotations: testAnnotations().map(annotation => ({ ...annotation })),
    fingerprint: await caseFingerprint(world.caseReference),
    changed: false,
  };
}

async function releaseHeldCase(): Promise<void> {
  const releasing = held;
  held = undefined;
  if (!releasing) {
    return;
  }
  const opened = caseInfo.id;
  caseInfo.id = releasing.caseReference;
  try {
    await performAction('deleteCaseRole', '[CLAIMANTSOLICITOR]');
  } finally {
    caseInfo.id = opened;
  }
}

/** Reads that could not be made, so that each one gives a fingerprint nothing can match. */
let unreadableCases = 0;

/** The case's state and every enforcement on it. Anything the enforcement journeys do changes one of them, so an identical fingerprint means an untouched case. */
async function caseFingerprint(caseReference: string): Promise<string> {
  try {
    const response = await Axios.get(`${process.env.DATA_STORE_URL_BASE}/cases/${caseReference}`, {
      headers: {
        Authorization: `Bearer ${process.env.BEARER_TOKEN}`,
        ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
        experimental: 'experimental',
        Accept: '*/*',
      },
    });
    return stableJson({ state: response.data?.state, enforcements: response.data?.data?.enforcements ?? [] });
  } catch {
    return `unreadable ${++unreadableCases}`;
  }
}

/** A copy, so that handing the world to the next scenario cannot share anything with the one that set it up. */
function plainCopy(world: Record<string, unknown>): Record<string, unknown> {
  return JSON.parse(JSON.stringify(world));
}

/** JSON with the object keys in a fixed order, so two reads of the same case give the same string. */
function stableJson(value: unknown): string {
  if (Array.isArray(value)) {
    return `[${value.map(stableJson).join(',')}]`;
  }
  if (value && typeof value === 'object') {
    return `{${Object.entries(value as Record<string, unknown>)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([name, item]) => `${JSON.stringify(name)}:${stableJson(item)}`)
      .join(',')}}`;
  }
  return JSON.stringify(value) ?? 'null';
}

async function restoreDefendantDetails(world: Record<string, unknown>): Promise<void> {
  const claim = world.submitPayload as { defendant1?: { nameKnown?: string }; addAnotherDefendant?: string } | undefined;
  if (!claim?.defendant1) {
    return;
  }
  await performAction('getDefendantDetails', {
    defendant1NameKnown: claim.defendant1.nameKnown,
    additionalDefendants: claim.addAnotherDefendant,
    payLoad: claim,
  });
}

function testAnnotations(): { type: string; description?: string }[] {
  return test.info().annotations;
}
