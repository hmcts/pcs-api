import { Page } from '@playwright/test';
import configData from "../../data/config.data";

import * as idamHelper from '../../helpers/idam-helpers/idam.helper';
import { buildUserDataWithRole } from '../../helpers/idam-helpers/testConfig';
import { IAction } from '../../interfaces/action.interface';
import { initializeExecutor, performAction } from '../../test-executor';

type UserCredentials = {
  email: string;
  password: string;
};

export class LoginAction implements IAction {
  async execute(page: Page, fieldName: string, value?: string | number | boolean | string[] | object): Promise<void> {
    initializeExecutor(page);
    await page.goto(configData.manageCasesBaseURL);
    console.log("URL:",configData.manageCasesBaseURL);
    let credentials: UserCredentials;
    let shouldCleanup = false;

    if (fieldName === 'newIdamUser') {
      const tempCreds = await this.generateTempCredentials();
      credentials = tempCreds;
      shouldCleanup = true;
    } else {
      credentials = this.validateCredentials(value);
    }

    await this.performLogin(credentials.email, credentials.password);

    if (shouldCleanup) {
      await idamHelper.deleteAccount(credentials.email);
    }
  }

  private async generateTempCredentials(): Promise<UserCredentials> {
    const password = process.env.PCS_FRONTEND_IDAM_USER_TEMP_PASSWORD as string;
    const userData = buildUserDataWithRole(configData.iDam.roles, password);
    await idamHelper.createAccount(userData);
    return {
      email: userData.user.email,
      password,
    };
  }

  private validateCredentials(value: unknown): UserCredentials {
    if (typeof value !== 'object' || value === null) {
      throw new Error('Credentials must be provided as an object');
    }
    const { email, password } = value as UserCredentials;
    if (!email || !password) {
      throw new Error('Credentials object must contain email and password');
    }
    return { email, password };
  }

  private async performLogin(email: string, password: string): Promise<void> {
    await performAction('fill', 'Email address', email);
    await performAction('fill', 'Password', password);
    await performAction('click', 'Sign in');
    console.log("User: "+ email + " has IDAM Roles: " + configData.iDam.roles);
  }
}
