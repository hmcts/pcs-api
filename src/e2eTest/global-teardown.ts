import { cleanupTempUsers } from 'utils/helpers/deleteTempUser.helper';

async function globalTeardown() {
  console.log('Cleaning up temp users...');
  await cleanupTempUsers();
}

export default globalTeardown;
