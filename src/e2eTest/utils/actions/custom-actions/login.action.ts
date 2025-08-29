import { IdamUtils } from '@hmcts/playwright-common';
import { Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';

import { performAction } from '../../controller';
import { IAction, actionData } from '../../interfaces/action.interface';

type UserInfo = {
  email: string;
  password: string;
  forename: string;
  surname: string;
};
export class LoginAction implements IAction {
  async execute(page: Page, action: string, loginData?: actionData): Promise<void> {
    if (Array.isArray(loginData)) {
      await this.createUser(loginData);
    }
    await performAction('inputText', 'Email address', process.env.IDAM_PCS_USER_EMAIL);
    await performAction('inputText', 'Password', process.env.IDAM_PCS_USER_PASSWORD);
    await performAction('clickButton', 'Sign in');
  }

  public async createUser(roles: string[]): Promise<UserInfo> {
    const token = process.env.CREATE_USER_BEARER_TOKEN as string;
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;
    const uniqueId = uuidv4();
    const email = process.env.IDAM_PCS_USER_EMAIL = `TEST_PCS_USER.${uniqueId}@test.test`;
    const forename = 'fn_' + uniqueId.split('-')[0];
    const surname = 'sn_' + uniqueId.split('-')[1];
    const user = await new IdamUtils().createUser({
      bearerToken: token,
      password,
      user: {
        email,
        forename,
        surname,
        roleNames: roles
      },
    });
    return {
      email: user.email,
      password: user.password,
      forename,
      surname
    };
  }
}
