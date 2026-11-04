import { IdamUtils } from '@hmcts/playwright-common';
import { expect, Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';
import { performAction } from '../../controller';
import { IAction, actionData, actionRecord } from '../../interfaces/action.interface';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import { LONG_TIMEOUT } from 'playwright.config';

export class LoginAction implements IAction {
  async execute(page: Page, action: string, userType: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createUserAndLogin', () => this.createUserAndLogin(userType as string, roles as string[], page)],
      ['createUser', () => this.createUser(userType as string, roles as string[])],
      ['login', () => this.login(userType, page)],
      ['generateCitizenAccessToken', () => this.generateCitizenAccessToken()]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async login(user: string | actionRecord, page: Page) {
    const userEmail = !user || typeof user === 'string' ? process.env.IDAM_PCS_USER_EMAIL : user.email;
    const userPassword = !user || typeof user === 'string' ? process.env.IDAM_PCS_USER_PASSWORD : user.password;
    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }
    await page.waitForSelector('#email', { timeout: LONG_TIMEOUT });
    await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
    await performAction('clickButton', signInOrCreateAnAccount.continueButton);
    const pwdHeader = page.getByLabel('Enter your password', { exact: true });
    await expect(pwdHeader).toBeVisible({ timeout: LONG_TIMEOUT });
    await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
    await performAction('clickButton', signInOrCreateAnAccount.continueButton);
    const signOut = page.getByText('Sign out', { exact: true });
    await expect(signOut).toBeVisible({ timeout: LONG_TIMEOUT });
  }

  private async createUserAndLogin(userType: string, roles: string[], page:Page): Promise<void> {
    const token = await this.getTemporaryUserCreationToken();
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
    await this.login(userType, page);
  }

  private async createUser(userType: string, roles: string[]): Promise<void> {
    const token = await this.getTemporaryUserCreationToken();
    const password = process.env.IDAM_PCS_USER_PASSWORD as string;
    const uniqueId = uuidv4();
    process.env.IDAM_PCS_USER_EMAIL = `TEST_PCS_USER.${userType}.${uniqueId}@test.test`;
    await new IdamUtils().createUser({
      bearerToken: token,
      password,
      user: {
        email: process.env.IDAM_PCS_USER_EMAIL,
        forename: `fn_${uniqueId.split('-')[0]}`,
        surname: `sn_${uniqueId.split('-')[1]}`,
        roleNames: roles,
      },
    });
  }

  private async generateCitizenAccessToken(): Promise<void> {
    process.env.CITIZEN_ACCESS_TOKEN = await new IdamUtils().generateIdamToken({
      username: process.env.IDAM_PCS_USER_EMAIL as string,
      password: process.env.IDAM_PCS_USER_PASSWORD as string,
      grantType: 'password',
      clientId: 'pcs-frontend',
      clientSecret: process.env.PCS_FRONTEND_IDAM_SECRET as string,
      scope: 'profile openid roles',
    });
  }

  private async getTemporaryUserCreationToken(): Promise<string> {
    if (process.env.CREATE_USER_BEARER_TOKEN) {
      return process.env.CREATE_USER_BEARER_TOKEN;
    }

    const systemPassword = process.env.IDAM_SYSTEM_PASSWORD ?? process.env.IDAM_SYSTEM_USER_PASSWORD;
    if (!process.env.IDAM_SYSTEM_USERNAME || !systemPassword) {
      throw new Error(
        'Temporary user creation requires IDAM_SYSTEM_USERNAME and IDAM_SYSTEM_PASSWORD (or IDAM_SYSTEM_USER_PASSWORD)'
      );
    }

    return new IdamUtils().generateIdamToken({
      username: process.env.IDAM_SYSTEM_USERNAME,
      password: systemPassword,
      grantType: 'password',
      clientId: 'pcs-frontend',
      clientSecret: process.env.PCS_FRONTEND_IDAM_SECRET as string,
      scope: 'profile openid roles',
    });
  }
}
