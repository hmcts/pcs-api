import { IAction } from '../../interfaces/action.interface';
import {Page, test} from '@playwright/test';
import { performAction } from '../../controller';
import {getUser} from "@utils/helpers/idam-helpers/idam.helper";

export class LoginAction implements IAction {
  async execute(page: Page, userKey: string): Promise<void> {

    const userCreds = getUser(userKey);
    if (!userCreds) {
      throw new Error(`No credentials found for key: ${userKey}`);
    }

    await test.step(`Test User ${userCreds.email} logging in with roles ${userCreds.roles.join(', ')}`, async () => {
    await performAction('inputText', 'Email address', userCreds.email);
    await performAction('inputText', 'Password', userCreds.password);
    await performAction('clickButton', 'Sign in');
    });
  }
}
