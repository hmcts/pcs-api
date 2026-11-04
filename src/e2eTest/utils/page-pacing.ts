import { Locator, Page } from '@playwright/test';
import { installHumanActions } from '@utils/demo-actions';

// Optional holds and human-speed actions for a recorded run. Off unless a test turns it on, and nothing here
// may fail a test: errors are swallowed.
let holdMs = 0;
let pacedPage: Page | undefined;
let lastHeldUrl: string | undefined;
let lastHoldEndedAt = 0;
let suspended = 0;

/** How long an emphasised element is shown before the action on it. */
const EMPHASIS_MS = 400;

/** Pages that are never held: the sign-in pages, the OAuth hand-back XUI redirects through, and a context with nothing loaded yet. */
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

/** Runs `action` at full speed while the pacing is on, for plumbing such as signing in. `lastHeldUrl` is left alone, so the next screen reached is still held. */
export async function withoutPagePacing<T>(action: () => Promise<T>): Promise<T> {
  suspended++;
  try {
    return await action();
  } finally {
    suspended--;
  }
}

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

/** Counts any hold that has only just finished, so a page-change pause directly followed by this one does not double the wait. */
export async function holdPage(page: Page): Promise<void> {
  if (!activePage() || page.isClosed() || !(await worthHolding(page))) {
    return;
  }
  await wait(page, holdMs - (Date.now() - lastHoldEndedAt));
}

/** A page with nothing rendered yet is a blank white document, which reads as dead air in the video. */
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
    await page.waitForTimeout(ms).catch(() => undefined);
  }
  lastHoldEndedAt = Date.now();
}
