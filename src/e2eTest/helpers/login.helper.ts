import { Page } from '@playwright/test';
import ConfigData from "@data/config.data";

import { initActionHelper, performAction } from '../helpers';
import * as idamHelper from './idam-helpers/idam.helper';
import { buildUserDataWithRole } from './idam-helpers/testConfig';

export class loginHelper {
  static async login(page: Page): Promise<String> {
    initActionHelper(page);
    const password = process.env.PCS_FRONTEND_IDAM_USER_TEMP_PASSWORD as string;
    const userData = buildUserDataWithRole(ConfigData.iDam.roles, password);

    await idamHelper.createAccount(userData);

    await performAction('fill', 'Email address', userData.user.email);
    await performAction('fill', 'Password', password);
    await performAction('click', 'Sign in');
    console.log("User "+ userData.user.email + " has IDAM Roles:", userData.user.roleNames);
    return userData.user.email;
  }
}
