import { IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { performAction } from '../../controller';
import configData from "../../../config/test.config";
import {getUser} from "@utils/helpers/idam-helpers/idam.helper";

export class LoginAction implements IAction {
  async execute(page: Page, userKey: string): Promise<void> {

    const userCreds = getUser(userKey);
    if (!userCreds) {
      throw new Error(`No credentials found for key: ${userKey}`);
    }
    await page.goto(configData.manageCasesBaseURL);
    console.log("Test URL:",configData.manageCasesBaseURL);

    await performAction('fill', 'Email address', userCreds.email);
    await performAction('fill', 'Password', userCreds.password);
    await performAction('click', 'Sign in');

    console.log(`Test User: ${userCreds.email} with Roles: ${userCreds.roles.join(', ')}`);
  }
}
