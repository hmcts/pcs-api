import { IdamUtils } from '@hmcts/playwright-common';
import { Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';
import { performAction, performValidation } from '../../controller';
import { IAction, actionData, actionRecord } from '../../interfaces/action.interface';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';

export class LoginAction implements IAction {
  async execute(page: Page, action: string, userType: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createUserAndLogin', () => this.createUserAndLogin(userType as string, roles as string[])],
      ['login', () => this.login(userType)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async login(user: string | actionRecord) {
    const userEmail = typeof user === 'string' ? process.env.IDAM_PCS_USER_EMAIL : user.email;
    const userPassword = typeof user === 'string' ?  process.env.IDAM_PCS_USER_PASSWORD : user.password;
    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }
    await performValidation('mainHeader', signInOrCreateAnAccount.mainHeader)
    await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
    await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
    await performAction('clickButton', signInOrCreateAnAccount.signInButton);
  }

  private async createUserAndLogin(userType: string, roles: string[]): Promise<void> {
    const token = process.env.CREATE_USER_BEARER_TOKEN as string;
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;
    const uniqueId = uuidv4();
    const email = process.env.IDAM_PCS_USER_EMAIL = `TEST_PCS_USER.${userType}.${uniqueId}@test.test`;
    const forename = 'fn_' + uniqueId.split('-')[0];
    const surname = 'sn_' + uniqueId.split('-')[1];
    await new IdamUtils().createUser({
      bearerToken: token,
      password,
      user: {
        email,
        forename,
        surname,
        roleNames: roles
      }
    });
    await this.login(userType);
  }

  private async createUser(userType: string, roles: string[]): Promise<void> {
    const token = process.env.BEARER_TOKEN as string;
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;
    const random7Digit = Math.floor(1000000 + Math.random() * 9000000);
    const email = (process.env.IDAM_PCS_USER_EMAIL = `TEST_PCS_USER.${userType}.${random7Digit}@test.test`);
    const forename = 'fn_' + random7Digit;
    const surname = 'sn_' + random7Digit;
    const { IdamUtils } = await import('@hmcts/playwright-common');
    await new IdamUtils().createUser({
      bearerToken: token,
      password,
      user: {
        email,
        forename,
        surname,
        roleNames: roles,
      },
    });
    await this.generateSolicitorAccessToken();
  }

  private async generateSolicitorAccessToken(): Promise<void> {
    const { IdamUtils } = await import('@hmcts/playwright-common');
    process.env.CITIZEN_ACCESS_TOKEN = await new IdamUtils().generateIdamToken({
      username: process.env.IDAM_PCS_USER_EMAIL,
      password: process.env.IDAM_PCS_USER_PASSWORD as string,
      grantType: 'password',
      clientId: 'pcs-frontend',
      clientSecret: process.env.PCS_FRONTEND_IDAM_SECRET as string,
      scope: 'profile openid roles',
    });
  }
}
