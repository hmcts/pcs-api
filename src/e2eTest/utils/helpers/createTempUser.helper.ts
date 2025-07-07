import * as idamHelper from './idam-helpers/idam.helper';
import { buildUserDataWithRole } from './idam-helpers/testConfig';

import { setTempUser } from './test-accounts';

export async function createTempUser(
  key: string,
  roles: string[]
): Promise<void> {
  const password = process.env.PCS_IDAM_USER_USER_PASSWORD || '';
  const userData = buildUserDataWithRole(roles, password);
  await idamHelper.createAccount(userData);

  setTempUser(key, {
    email: userData.user.email,
    password,
    temp: true,
    roles,
  });

  console.log(`Created temp user "${userData.user.email}" with roles: ${roles.join(', ')}`);
}


