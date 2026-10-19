/**
 * One continuous pass per feature, for the demo videos.
 *
 * Every scenario re-runs its feature's Background, so a feature whose scenarios share one Background was
 * filmed as the same preamble over and over: set a case up through the APIs, sign in, open the case, once per
 * scenario. In the `enforcement-bdd-demo` project consecutive scenarios that share an identical Background
 * instead run against the same case and the same signed-in session, so the video reads as one journey. CI's
 * `enforcement-bdd` project is untouched — there each scenario still sets up its own case and signs in for
 * itself, because a cascading failure costs far more than a slow run.
 *
 * Two things are carried from one scenario to the next:
 *
 *   - the case the Background set up, keyed on the feature file and the resolved text of every Background
 *     step. Two scenarios share a case only when both are character-identical, so a Background asking for a
 *     different status, a different enforcement type or with an extra step gets a case of its own.
 *   - the signed-in session, as the browser context's cookies. Playwright gives every test a fresh context
 *     and will not reuse one at all while it is recording video, so the session cannot simply be left open;
 *     the cookies are put into the new context instead, and only after XUI has confirmed it still knows them.
 *
 * Why this cannot film the wrong state. The case is handed to the next scenario only when all of:
 *
 *   - the Background text is identical, so the set-up asked for is the same;
 *   - no event was submitted on it after its Background finished. Every event submitted through the data
 *     store API (event-api.ts) and every event submitted in XUI (the browser's POST to
 *     `/cases/<reference>/events`) is recorded against the case it was submitted on;
 *   - the case's state and enforcements, re-read from the data store, are identical to what they were when
 *     the Background finished. That is a positive check against the real case, not a record of what the suite
 *     believes it did;
 *   - the scenario did not fail. One that failed may have been part way through a submit. One that stopped at
 *     a pending step keeps its case: it did nothing after its Background, and the fingerprint would show it if
 *     it had.
 *
 * Anything else — a different Background, a submitted event, a changed case, a failure — makes the next
 * scenario set up its own case, which is what every scenario did before.
 *
 * Set-up this file cannot skip would re-run against the reused case, so a Background whose set-up does not go
 * through a helper wrapped in {@link backgroundSetUp} is never reused: an event submitted by a Background step
 * outside those helpers is noticed the first time the Background runs, and the case is then offered to nobody
 * (feature 16 builds its two warrants that way). A Background that sets up more than one case is not offered
 * either, because only one case is held.
 *
 * The case role the held case keeps is dropped when it is released. The last case a worker holds keeps it,
 * which costs the claimant's legal representative one extra case in their case list per feature filmed.
 */
import { BrowserContext, Cookie, TestInfo, test } from '@playwright/test';
import Axios from 'axios';
import { caseInfo } from '@utils/actions/custom-actions/createCaseAPI.action';
import { performAction } from '@utils/controller-enforcement';
import { DEMO_PROJECT } from './demo-pacing';

/** The fields of playwright-bdd's step data this file reads. */
export type BddStep = { textWithKeyword?: string; isBg?: boolean };

/** What the Background left behind, kept for the next scenario with the same Background. */
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
/** The spec file the held case and the sessions belong to: a different feature starts again. */
let scope: string | undefined;
let held: HeldCase | undefined;
/** Backgrounds whose set-up this file cannot skip, so their case is never offered to a second scenario. */
const notReusable = new Set<string>();
/** The cookies of the last session signed in as each role. */
const sessions = new Map<string, Cookie[]>();
/** Who the browser is signed in as now, as session.ts last reported it. */
let currentRole = '';

let started = false;
let key: string | undefined;
let reusing = false;
let inBackground = false;
let setUpDepth = 0;
/** Whether a Background step submitted an event outside a {@link backgroundSetUp} helper. */
let unskippableSetUp = false;
/** The cases this scenario's Background set up. */
const backgroundCases = new Set<string>();

/** Starts a scenario, and a new feature's cache when the spec file changes. */
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

/**
 * Before each step: claims the feature's case at the scenario's first step, and captures it once the
 * Background has finished.
 */
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

/** Ends a scenario. Returns whether its case is being held, and so should keep its case role. */
export async function endScenario(testInfo: TestInfo, world: { caseReference?: string }, context: BrowserContext): Promise<boolean> {
  if (!enabled) {
    return false;
  }
  inBackground = false;
  await rememberSession(context, currentRole);
  // A scenario that stopped at a pending step keeps its case: it did nothing, and anything it did do would
  // show in the fingerprint the next scenario checks. One that failed may have been part way through a submit.
  const failed = testInfo.status === 'failed' || testInfo.status === 'timedOut';
  if (held && (held.changed || failed)) {
    await releaseHeldCase();
  }
  return held !== undefined && held.caseReference === world.caseReference;
}

/**
 * Set-up a scenario that is reusing its feature's case can skip, and whose changes to the case are expected
 * while the Background runs. Runs `setUp` whenever there is nothing to reuse.
 */
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

/** Records that an event has been submitted on `caseReference`, whoever submitted it. */
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

/** The data store event submit, which is what XUI posts when an event is submitted in the browser. */
const CASE_EVENT_SUBMIT = /\/cases\/(\d{16})\/events(?:[/?]|$)/;

/** Records an event submitted in the browser. Attached to the browser context by the fixtures. */
export function recordBrowserRequest(method: string, url: string): void {
  if (!enabled || method === 'GET') {
    return;
  }
  const submitted = CASE_EVENT_SUBMIT.exec(url);
  if (submitted) {
    recordCaseChange(submitted[1]);
  }
}

/** Keeps the cookies of the session signed in as `role`, so the next scenario wanting them can carry on. */
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

/**
 * Puts the session last signed in as `role` into this scenario's browser context, so it does not have to sign
 * in again. Answers false unless XUI still knows that session, in which case the caller signs in as usual.
 */
export async function restoreSession(role: string, context: BrowserContext): Promise<boolean> {
  const cookies = enabled ? sessions.get(role) : undefined;
  if (!cookies?.length) {
    return false;
  }
  await context.clearCookies();
  await context.addCookies(cookies);
  if (await xuiKnowsSession(context)) {
    return true;
  }
  sessions.delete(role);
  await context.clearCookies();
  return false;
}

/** Told by session.ts, so that the session saved at the end of a scenario is saved under the right user. */
export function noteSignedInRole(role: string): void {
  currentRole = role;
}

/**
 * Whether XUI still has the session the context's cookies belong to: it answers a signed-in-only page itself
 * rather than redirecting to IDAM. `context.request` shares the context's cookies.
 */
async function xuiKnowsSession(context: BrowserContext): Promise<boolean> {
  const base = String(process.env.MANAGE_CASE_BASE_URL);
  try {
    const response = await context.request.get(`${base}/cases`);
    return response.ok() && new URL(response.url()).origin === new URL(base).origin;
  } catch {
    return false;
  }
}

/** The feature file and the resolved text of every Background step, or undefined when there is no Background. */
function backgroundKey(steps: BddStep[]): string | undefined {
  const background = steps.filter(step => step.isBg).map(step => step.textWithKeyword ?? '');
  return background.length ? `${scope}\n${background.join('\n')}` : undefined;
}

/** Gives this scenario the case held for `backgroundKey`, if it may still have it. */
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

/** Holds the case this scenario's Background set up, for the next scenario with the same Background. */
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

/** Drops the held case, giving up the case role the scenario that set it up kept for it. */
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

/**
 * The case as the Background left it: its state and every enforcement on it, from the data store. Anything the
 * enforcement journeys do changes one of them, so an identical fingerprint means an untouched case.
 *
 * A case that cannot be read is given a fingerprint of its own, which no later read can match: the scenario
 * carries on and sets up a case for itself rather than failing over a check that is only there to save time.
 */
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

/**
 * A copy of the world's data, so that handing it to the next scenario cannot share anything with the one that
 * set it up. The world holds only the strings, booleans, arrays and records the steps put on it.
 */
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

/**
 * The defendant names the enforcement journeys read, which the fixtures clear before each scenario. The action
 * only reads the claim, so the names are worked out again rather than carried over.
 */
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

/** The running test's annotations, so the Background's approximated preconditions follow its case. */
function testAnnotations(): { type: string; description?: string }[] {
  return test.info().annotations;
}
