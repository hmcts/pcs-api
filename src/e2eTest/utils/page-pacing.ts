import { Locator, Page } from '@playwright/test';
import { installHumanActions } from '@utils/demo-actions';

/**
 * An optional pause each time the page arrives at a new URL, so that a recorded run goes at a pace a viewer
 * can follow. Off unless a test turns it on: the enforcement BDD fixtures do, in the `enforcement-bdd-demo`
 * project only. The controllers call {@link holdIfPageChanged} after every action and validation. Turning it on
 * also makes every browser action happen at human speed, with a visible pointer and typing (utils/demo-actions.ts).
 *
 * Everything here is for the recording only, so none of it may fail a test: errors are swallowed.
 */
let holdMs = 0;
let pacedPage: Page | undefined;
let lastHeldUrl: string | undefined;
let lastHoldEndedAt = 0;
let suspended = 0;

/** How long an emphasised element is shown before the action on it. */
const EMPHASIS_MS = 400;

/**
 * Pages that are not part of the story and so are never held: the sign-in pages (the local IDAM simulator,
 * IDAM web), the OAuth hand-back XUI redirects through, and a context with nothing loaded yet.
 */
const UNINTERESTING_URL = /^about:|:5062\/|idam-web|\/login\b|\/sign-in\b|\/oauth2\/callback|\/receiver\b/;

export function enablePagePacing(page: Page, ms: number): void {
  holdMs = ms;
  pacedPage = page;
  lastHeldUrl = page.url();
  lastHoldEndedAt = 0;
  installHumanActions(page, activePage);
}

export function disablePagePacing(): void {
  holdMs = 0;
  pacedPage = undefined;
  lastHeldUrl = undefined;
  lastHoldEndedAt = 0;
  suspended = 0;
}

export function pagePacingEnabled(): boolean {
  return holdMs > 0;
}

/**
 * Runs `action` at full speed even while the demo pacing is on: no holds, no pointer, no typed-out text.
 * For the parts of a run that are plumbing rather than story — signing in, above all, which otherwise spends
 * a quarter of a minute typing a username and a password into the IDAM simulator's bare form. The step
 * caption still shows, so the video still says who is being signed in as.
 *
 * `lastHeldUrl` is deliberately left alone, so the first screen reached afterwards is still held.
 */
export async function withoutPagePacing<T>(action: () => Promise<T>): Promise<T> {
  suspended++;
  try {
    return await action();
  } finally {
    suspended--;
  }
}

/**
 * On the first action or validation that sees a new URL: scrolls to the top, so the case header and the
 * "Next step" drop down are in view, then pauses. The rest of the actions on that URL do not.
 */
export async function holdIfPageChanged(page: Page): Promise<void> {
  if (!activePage() || page.isClosed() || page.url() === lastHeldUrl) {
    return;
  }
  await scrollToTop(page);
  if (!(await worthHolding(page))) {
    lastHeldUrl = page.url();
    return;
  }
  await wait(page, holdMs);
  await scrollToTop(page);
}

/**
 * Keeps the current screen up for the hold time, counting any hold that has only just finished, so that a
 * page-change pause directly followed by this one does not double the wait.
 */
export async function holdPage(page: Page): Promise<void> {
  if (!activePage() || page.isClosed() || !(await worthHolding(page))) {
    return;
  }
  await wait(page, holdMs - (Date.now() - lastHoldEndedAt));
}

/**
 * Whether the screen is one a viewer would want time to read. A sign-in or redirect URL never is, and neither
 * is a page with nothing rendered yet: XUI's redirects between sign-in steps show a blank white document for
 * as long as they are held, which reads as dead air in the video.
 */
async function worthHolding(page: Page): Promise<boolean> {
  if (UNINTERESTING_URL.test(page.url())) {
    return false;
  }
  return page
    .evaluate(() => {
      const main = document.querySelector('main, h1, .govuk-heading-l, .govuk-heading-xl');
      return Boolean(main && (main as HTMLElement).innerText?.trim());
    })
    .catch(() => true);
}

/**
 * Brings an element the next action uses into view and outlines it, then pauses, so the viewer sees what is
 * about to be chosen. Does nothing outside the demo project.
 */
export async function demoEmphasise(target: (page: Page) => Locator): Promise<void> {
  const page = activePage();
  if (!page) {
    return;
  }
  try {
    await target(page)
      .first()
      .evaluate(
        (element: HTMLElement) => {
          element.scrollIntoView({ block: 'center' });
          element.style.outline = '5px solid #ffdd00';
          element.style.outlineOffset = '3px';
          element.style.boxShadow = '0 0 0 11px #0b0c0c';
        },
        undefined,
        { timeout: 2_000 },
      );
    await page.waitForTimeout(EMPHASIS_MS);
  } catch {
    // Only for the recording.
  }
}

/** A pause in the demo project only, e.g. to show a choice before it is submitted. */
export async function demoPause(ms: number): Promise<void> {
  const page = activePage();
  if (page) {
    await page.waitForTimeout(ms).catch(() => undefined);
  }
}

function activePage(): Page | undefined {
  return holdMs && !suspended && pacedPage && !pacedPage.isClosed() ? pacedPage : undefined;
}

async function scrollToTop(page: Page): Promise<void> {
  await page.evaluate(() => window.scrollTo(0, 0)).catch(() => undefined);
}

async function wait(page: Page, ms: number): Promise<void> {
  lastHeldUrl = page.url();
  if (ms > 0) {
    // A pause is only for the recording: a page closed or torn down meanwhile must not fail the test.
    await page.waitForTimeout(ms).catch(() => undefined);
  }
  lastHoldEndedAt = Date.now();
}
