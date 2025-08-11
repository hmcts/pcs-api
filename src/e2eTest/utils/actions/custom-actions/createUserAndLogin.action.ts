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
  sessionFile?: string;
};
export class CreateUserAndLoginAction implements IAction {
  async execute(page: Page, roles: actionData): Promise<void> {
    const userCreds = await this.createUser(roles as string[]);
    await performAction('inputText', 'Email address', userCreds.email);
    await performAction('inputText', 'Password', userCreds.password);
    await performAction('clickButton', 'Sign in');
  }

  public async createUser(roles: string[]): Promise<UserInfo> {
    const token = process.env.CREATE_USER_BEARER_TOKEN as string;
    const password = process.env.IDAM_CITIZEN_USER_PASSWORD as string;
    const uniqueId = uuidv4();
    const email = `TEST_PCS_USER_citizen.${uniqueId}@test.test`;
    const forename = 'fn_' + uniqueId.split('-')[0];
    const surname = 'sn_' + uniqueId.split('-')[1];
    const user = await new IdamUtils().createUser({
      bearerToken: token,
      password,
      user: {
        email,
        forename,
        surname,
        roleNames: roles,
      },
    });
    return {
      email: user.email,
      password: user.password,
      forename,
      surname,
    };
  }
}
