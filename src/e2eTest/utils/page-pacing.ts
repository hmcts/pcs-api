import { Page } from '@playwright/test';

/**
 * An optional pause each time the page arrives at a new URL, so that a recorded run goes at a pace a viewer
 * can follow. Off unless a test turns it on: the enforcement BDD fixtures do, in the `enforcement-bdd-demo`
 * project only. The controllers call {@link holdIfPageChanged} after every action and validation.
 */
let holdMs = 0;
let lastHeldUrl: string | undefined;
let lastHoldEndedAt = 0;

export function enablePagePacing(page: Page, ms: number): void {
  holdMs = ms;
  lastHeldUrl = page.url();
  lastHoldEndedAt = 0;
}

export function disablePagePacing(): void {
  holdMs = 0;
  lastHeldUrl = undefined;
  lastHoldEndedAt = 0;
}

export function pagePacingEnabled(): boolean {
  return holdMs > 0;
}

/** Pauses once per URL: the first action or validation that sees a new URL waits, the rest do not. */
export async function holdIfPageChanged(page: Page): Promise<void> {
  if (!holdMs || page.isClosed() || page.url() === lastHeldUrl) {
    return;
  }
  await wait(page, holdMs);
}

/**
 * Keeps the current screen up for the hold time, counting any hold that has only just finished, so that a
 * page-change pause directly followed by this one does not double the wait.
 */
export async function holdPage(page: Page): Promise<void> {
  if (!holdMs || page.isClosed()) {
    return;
  }
  await wait(page, holdMs - (Date.now() - lastHoldEndedAt));
}

async function wait(page: Page, ms: number): Promise<void> {
  lastHeldUrl = page.url();
  if (ms > 0) {
    // A pause is only for the recording: a page closed or torn down meanwhile must not fail the test.
    await page.waitForTimeout(ms).catch(() => undefined);
  }
  lastHoldEndedAt = Date.now();
}
