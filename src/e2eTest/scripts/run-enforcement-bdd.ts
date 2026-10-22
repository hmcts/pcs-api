import { spawnSync } from 'node:child_process';

const NOT_RUN = '@pending|@open-question';

const result = spawnSync(
  'yarn',
  ['playwright', 'test', '--project', 'enforcement-bdd', '--grep-invert', NOT_RUN],
  { stdio: 'inherit' }
);

if (result.error) {
  throw result.error;
}

process.exit(result.status ?? 1);
