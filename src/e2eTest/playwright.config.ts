import * as process from 'node:process';
import * as path from 'path';
import * as fs from 'fs';

import {defineConfig, devices} from '@playwright/test';
import {defineBddConfig} from 'playwright-bdd';

const DEFAULT_VIEWPORT = {width: 1920, height: 1080};
export const VERY_SHORT_TIMEOUT = 1000;
export const SHORT_TIMEOUT = 5000;
export const MEDIUM_TIMEOUT = 10000;
export const LONG_TIMEOUT = 30000;
export const VERY_LONG_TIMEOUT = 60000;
export const actionRetries = 5;
export const waitForPageRedirectionTimeout = SHORT_TIMEOUT;
const STORAGE_STATE_PATH = path.join(__dirname, '.auth/storage-state.json');
const storageStateConfig = fs.existsSync(STORAGE_STATE_PATH) ? { storageState: STORAGE_STATE_PATH } : {};

// Nightly (Jenkins): E2E_TEST_SCOPE = tag grep; E2E_SPEC = comma/semicolon path keywords → testMatch globs.
const e2eSpecKeys = (process.env.E2E_SPEC ?? '')
  .split(/[,;]/)
  .map(s => s.trim().replace(/[^\w.-]/g, ''))
  .filter(Boolean);
const e2eTestMatch = e2eSpecKeys.length ? e2eSpecKeys.map(k => `**/*${k}*.spec.ts`) : undefined;
const e2eScope = process.env.E2E_TEST_SCOPE?.trim();
const e2eGrep = e2eScope ? new RegExp(e2eScope) : undefined;

// Preview defaults lower than AAT because each PR release has its own single-replica CCD stack.
// The actual ceiling is unmeasured; use E2E_WORKERS to tune it without a code change.
function resolveWorkers(): number {
  const environmentDefault = process.env.ENVIRONMENT === 'preview' ? 2 : 4;
  const parsed = Number(process.env.E2E_WORKERS?.trim());
  return Number.isInteger(parsed) && parsed >= 1 ? parsed : environmentDefault;
}

function demoSlowMo(): number {
  const parsed = Number(process.env.DEMO_SLOW_MO?.trim());
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 350;
}

// Enforcement BDD: `yarn bddgen` turns the .feature files into specs under .features-gen/ (gitignored).
// Only the enforcement-bdd project reads them; projects using testDir 'tests/' never pick them up.
const ENFORCEMENT_BDD_DIR = 'tests/tests-enforcement/bdd';
const enforcementBddTestDir = defineBddConfig({
  features: `${ENFORCEMENT_BDD_DIR}/features/**/*.feature`,
  steps: [`${ENFORCEMENT_BDD_DIR}/fixtures/*.ts`, `${ENFORCEMENT_BDD_DIR}/steps/**/*.ts`],
  featuresRoot: `${ENFORCEMENT_BDD_DIR}/features`,
  outputDir: '.features-gen/enforcement',
});

export default defineConfig({
  testDir: 'tests/',
  ...(e2eTestMatch?.length ? { testMatch: e2eTestMatch } : {}),
  ...(e2eGrep ? { grep: e2eGrep } : {}),
  /* Run tests in files in parallel */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  workers: resolveWorkers(),
  timeout: 600 * 1000,
  expect: { timeout: 30 * 1000 },
  use: { actionTimeout: 40 * 1000,  navigationTimeout: 40 * 1000, ...storageStateConfig },
  /* Report slow tests if they take longer than 5 mins */
  reportSlowTests: { max: 15, threshold: 5 * 60 * 1000 },
  globalSetup: require.resolve('./config/global-setup.config'),
  globalTeardown: require.resolve('./config/global-teardown.config'),
  reporter: [
    ['list'],
    [
      'allure-playwright',
      {
        resultsDir: 'allure-results',
        suiteTitle: false,
        environmentInfo: {
          os_version: process.version,
        },
      },
    ],
  ],
  projects: [
  
    {
      name: 'chrome',
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
        headless: !!process.env.CI,
      },
    },
    {
      name: 'enforcement-bdd',
      testDir: enforcementBddTestDir,
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'on-first-retry',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
        headless: !!process.env.CI,
      },
    },
    // Demo recording: one video per scenario, headed-size viewport, no retries hiding a re-run.
    // A separate project so CI's `enforcement-bdd` keeps `video: 'retain-on-failure'`; nothing selects
    // this one unless you ask for it by name. It runs at human speed: `slowMo` (DEMO_SLOW_MO, ms) on every
    // browser action, and the BDD fixtures hold each new page and each When/Then step for DEMO_HOLD_MS, caption
    // every step, and show every click, selection and piece of typing with a visible pointer
    // (tests-enforcement/bdd/support/demo-pacing.ts). scripts/enforcement-demo-videos.sh stitches the videos.
    {
      name: 'enforcement-bdd-demo',
      testDir: enforcementBddTestDir,
      // Unhappy and edge cases are tagged @not-filmed and kept out of the demo; enforcement-bdd still runs them.
      grepInvert: /@not-filmed/,
      retries: 0,
      // Human-speed actions (a visible pointer, typed text) make a long journey take several times longer.
      timeout: 1800 * 1000,
      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        launchOptions: { slowMo: demoSlowMo() },
        screenshot: 'on',
        video: { mode: 'on', size: DEFAULT_VIEWPORT },
        trace: 'on',
        javaScriptEnabled: true,
        viewport: DEFAULT_VIEWPORT,
        headless: !!process.env.CI,
      },
    },
    ...(process.env.CI
      ? [
          {
            name: 'firefox',
            use: {
              ...devices['Desktop Firefox'],
              channel: 'firefox',
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              viewport: DEFAULT_VIEWPORT,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'webkit',
            use: {
              ...devices['Desktop Safari'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              viewport: DEFAULT_VIEWPORT,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'edge',
            use: {
              ...devices['Desktop Edge'],
              channel: 'msedge',
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              viewport: DEFAULT_VIEWPORT,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'mobile-android',
            use: {
              ...devices['Pixel 5'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'mobile-ios',
            use: {
              ...devices['iPhone 12'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              headless: !!process.env.CI,
            },
          },
          {
            name: 'mobile-ipad',            
            use: {
              ...devices['iPad Pro 11'],
              screenshot: 'only-on-failure' as const,
              video: 'retain-on-failure' as const,
              trace: 'on-first-retry' as const,
              javaScriptEnabled: true,
              headless: !!process.env.CI,
            },
          },
        ]
      : []),
  ],
});