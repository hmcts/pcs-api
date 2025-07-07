import { IAction } from '../../interfaces/action.interface';
import { Page } from '@playwright/test';
import { performAction } from '../../test-executor';
import { userStore } from '../../data/user-store';
import configData from "../../data/config.data";

export class LogInNewAction implements IAction {
  async execute(page: Page, fieldName?: string): Promise<void> {
    const userCreds = fieldName && userStore[fieldName];
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
