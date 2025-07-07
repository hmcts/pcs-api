import { userStore } from '../data/user-store';
import * as idamHelper from '../helpers/idam-helpers/idam.helper';

export async function cleanupTempUsers(): Promise<void> {
  const keysToDelete: string[] = [];

  for (const [key, creds] of Object.entries(userStore)) {
    if (creds.temp) {
      try {
        await idamHelper.deleteAccount(creds.email);
        console.log(`Deleted temp user: ${creds.email}`);
        keysToDelete.push(key);
      } catch (err) {
        console.warn(`Failed to delete temp user ${creds.email}:`, err);
      }
    }
  }

  for (const key of keysToDelete) {
    delete userStore[key];
  }
}
