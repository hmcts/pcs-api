/**
 * Runs the enforcement BDD suite in two passes, because pcs-api's test clock is a process-wide offset.
 *
 * The first pass runs every scenario that leaves the clock alone, on the configured number of workers. The
 * second runs the `@clock` scenarios on a single worker, so that nothing measuring against the real date is
 * running while the clock is shifted (tests-enforcement/bdd/support/test-clock.ts).
 *
 * The clock endpoint only exists where pcs-api is run with `ENABLE_TESTING_SUPPORT_CLOCK`: the local cftlib
 * stack (build.gradle) and a per-PR preview (charts/pcs-api/values.preview.template.yaml). AAT and the other
 * shared environments keep the real clock, so there the second pass is not run at all.
 *
 * Each pass is a Playwright run of its own, so each does its own global set-up and tear-down (the IdAM
 * sign-in and the fixture documents). Both write into the same `allure-results`, which the calling script
 * turns into one report.
 */
import { spawnSync } from 'node:child_process';

/** Scenarios the suite never runs: a journey that does not exist yet, or a question for the business. */
const NOT_RUN = '@pending|@open-question';

/** Scenarios that move pcs-api's clock. */
const CLOCK = '@clock';

/** The environments whose pcs-api is deployed with `ENABLE_TESTING_SUPPORT_CLOCK`. */
const CLOCK_ENVIRONMENTS = new Set(['local', 'preview']);

function playwright(...args: string[]): number {
  const result = spawnSync('yarn', ['playwright', 'test', '--project', 'enforcement-bdd', ...args], {
    stdio: 'inherit',
  });
  if (result.error) {
    throw result.error;
  }
  return result.status ?? 1;
}

const environment = (process.env.ENVIRONMENT ?? '').toLowerCase();

let status = playwright('--grep-invert', `${NOT_RUN}|${CLOCK}`);

if (CLOCK_ENVIRONMENTS.has(environment)) {
  const clockStatus = playwright('--grep', CLOCK, '--grep-invert', NOT_RUN, '--workers', '1');
  status = status || clockStatus;
} else {
  console.log(
    `\nNot running the ${CLOCK} scenarios: pcs-api on "${environment || 'this environment'}" keeps the real `
      + 'clock (ENABLE_TESTING_SUPPORT_CLOCK is only set for local and preview).\n'
  );
}

process.exit(status);
