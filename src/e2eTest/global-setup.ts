import { createTempUser } from 'utils/helpers/createTempUser.helper';

async function globalSetup() {
  console.log('Creating temp user...');
  await createTempUser('caseworker', ['caseworker', 'caseworker-pcs']);
}

export default globalSetup;
