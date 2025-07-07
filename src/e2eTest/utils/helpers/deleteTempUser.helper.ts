
import * as idamHelper from '../helpers/idam-helpers/idam.helper';
import { getAllUsers, deleteTempUser } from './test-accounts';

export async function cleanupTempUsers(): Promise<void> {
  const all = getAllUsers();
  for (const [key, creds] of Object.entries(all)) {
    if (creds.temp) {
      try {
        await idamHelper.deleteAccount(creds.email);
        console.log(`Deleted temp user ${creds.email}`);
        deleteTempUser(key);
      } catch (err) {
        console.warn(`Could not delete temp user ${creds.email}`, err);
      }
    }
  }
}
