import { userStore } from '../data/user-store';
import * as idamHelper from './idam-helpers/idam.helper';
import { buildUserDataWithRole } from './idam-helpers/testConfig';

export async function createTempUser(
  key: string,
  roles: string[]
): Promise<void> {
  const password = process.env.PCS_FRONTEND_IDAM_USER_TEMP_PASSWORD as string;

  const userData = buildUserDataWithRole(roles, password);

  await idamHelper.createAccount(userData);

  userStore[key] = {
    email: userData.user.email,
    password,
    temp: true,
    roles
  };

  console.log(`Created temp user "${userData.user.email}" with roles: ${roles.join(', ')}`);
}
