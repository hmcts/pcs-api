import { updateTestReadme } from '../update-testReadme';

async function globalTeardownConfig() {
  if (!process.env.CI) {
    await updateTestReadme();
  }}

export default globalTeardownConfig;
