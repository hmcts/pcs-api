/**
 * Pacing for the demo videos (`scripts/enforcement-demo-videos.sh`). In the `enforcement-bdd-demo` project
 * every new page and every When / Then step is held on screen for `DEMO_HOLD_MS` (default 1500ms), on top of
 * the project's `slowMo`, so that a viewer can read each screen. Every other project, including CI's
 * `enforcement-bdd`, runs at full speed.
 */
import { TestInfo } from '@playwright/test';

export const DEMO_PROJECT = 'enforcement-bdd-demo';

const DEFAULT_DEMO_HOLD_MS = 1500;

/** How long to hold each screen in this test: zero outside the demo project. */
export function demoHoldMs(testInfo: TestInfo): number {
  if (testInfo.project.name !== DEMO_PROJECT) {
    return 0;
  }
  const configured = Number(process.env.DEMO_HOLD_MS?.trim());
  return Number.isFinite(configured) && configured >= 0 ? configured : DEFAULT_DEMO_HOLD_MS;
}
