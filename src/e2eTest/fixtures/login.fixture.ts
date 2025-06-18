import { test as baseTest, expect as baseExpect, Page } from '@playwright/test';
import {ConfigData} from "@data/config.data";
import * as actions from "@helpers/actions.helper";
import * as idamHelper from '@helpers/idam-helpers/idam.helper';

let Username: string | undefined;

type MyFixtures = {
  loggedInPage: Page;
  username?: string;
  password?: string;
};

export const test = baseTest.extend<MyFixtures>({
  username: [undefined, { option: true }],
  password: [undefined, { option: true }],

  loggedInPage: async ({ page, username, password }, use) => {

    let userCreated = false;
    let Username = username;
    let Password = password;

    // if credentials are provided then do not create new user
    if (!Username || !Password) {
        const { userData, password: generatedPassword } = await idamHelper.createUser(ConfigData.iDam.roles);
        Username = userData.user.email;
        Password = generatedPassword;
    }

    await page.goto(ConfigData.manageCasesBaseURL);

    await actions.fillInput(page, 'Email address', Username);
    await actions.fillInput(page, 'Password', Password);
    await actions.clickButton(page, 'Sign in');

    await use(page);
  }
});
export { Username };
export const expect = baseExpect;
