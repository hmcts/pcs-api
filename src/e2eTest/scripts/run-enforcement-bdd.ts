// pcs-api's test clock is a process-wide offset, so the `@clock` scenarios run on their own, on one worker, in
// a second pass. The endpoint only exists where pcs-api runs with `ENABLE_TESTING_SUPPORT_CLOCK`.
import { spawnSync } from 'node:child_process';

const NOT_RUN = '@pending|@open-question';

const CLOCK = '@clock';

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
