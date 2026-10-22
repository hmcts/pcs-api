// Demo project only: how long each new page and each When / Then step is held on screen (`DEMO_HOLD_MS`).
import { TestInfo } from '@playwright/test';

export const DEMO_PROJECT = 'enforcement-bdd-demo';

const DEFAULT_DEMO_HOLD_MS = 1200;

export function demoHoldMs(testInfo: TestInfo): number {
  if (testInfo.project.name !== DEMO_PROJECT) {
    return 0;
  }
  const configured = Number(process.env.DEMO_HOLD_MS?.trim());
  return Number.isFinite(configured) && configured >= 0 ? configured : DEFAULT_DEMO_HOLD_MS;
}
