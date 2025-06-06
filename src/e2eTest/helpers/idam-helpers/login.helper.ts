// login.helper.ts
import { buildUserDataWithRole, UserData } from './testConfig';
import * as idamHelper from './idam.helper';

export class loginHelper {
  static async createUser(role: string): Promise<{ userData: UserData; password: string }> {
    const password = process.env.PCS_FRONTEND_IDAM_USER_TEMP_PASSWORD as string;
    const userData = buildUserDataWithRole(role, password);
    await idamHelper.createAccount(userData);
    return { userData, password };
  }
}

