import { IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { performAction } from '../../test-executor';
import {getUser} from '../../helpers/test-accounts';
import configData from "../../data/config.data";

export class LogInAction implements IAction {
  async execute(page: Page, fieldName?: string): Promise<void> {

    const userCreds = getUser('caseworker');
    if (!userCreds) {
      throw new Error(`No credentials found for key: ${fieldName}`);
    }
    await page.goto(configData.manageCasesBaseURL);
    console.log("URL:",configData.manageCasesBaseURL);

    await performAction('fill', 'Email address', userCreds.email);
    await performAction('fill', 'Password', userCreds.password);
    await performAction('click', 'Sign in');
    console.log(`Logged in user: ${userCreds.email}`);
  }
}
