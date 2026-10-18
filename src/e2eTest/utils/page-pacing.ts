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

/** How long an emphasised element is shown before the action on it. */
const EMPHASIS_MS = 400;

/** Sign-in pages (the local IDAM simulator, IDAM web) are not part of the story: never held. */
const SIGN_IN_URL = /:5062\/|idam-web|\/login\b|\/sign-in\b/;

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
}

export function pagePacingEnabled(): boolean {
  return holdMs > 0;
}

/**
 * On the first action or validation that sees a new URL: scrolls to the top, so the case header and the
 * "Next step" drop down are in view, then pauses. The rest of the actions on that URL do not.
 */
export async function holdIfPageChanged(page: Page): Promise<void> {
  if (!holdMs || page.isClosed() || page.url() === lastHeldUrl) {
    return;
  }
  await scrollToTop(page);
  if (SIGN_IN_URL.test(page.url())) {
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
  if (!holdMs || page.isClosed() || SIGN_IN_URL.test(page.url())) {
    return;
  }
  await wait(page, holdMs - (Date.now() - lastHoldEndedAt));
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
  return holdMs && pacedPage && !pacedPage.isClosed() ? pacedPage : undefined;
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
