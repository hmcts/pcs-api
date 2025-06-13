import { test as baseTest, expect as baseExpect, Page } from '@playwright/test';
import {ConfigData} from "@data/config.data";
import * as actions from "@helpers/actions.helper";
import * as idamHelper from '@helpers/idam-helpers/idam.helper';

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
    let finalUsername = username;
    let finalPassword = password;

    // if credentials are provided then do not create new user
    if (!finalUsername || !finalPassword) {
      if (ConfigData.manageCasesBaseURL.includes(ConfigData.localHost.env)) {
        finalUsername = ConfigData.localHost.username;
        finalPassword = ConfigData.localHost.password;
      } else {
        userCreated = true;
        const { userData, password: generatedPassword } = await idamHelper.createUser(ConfigData.iDam.roles);
        finalUsername = userData.user.email;
        finalPassword = generatedPassword;
      }
    }

    await page.goto(ConfigData.manageCasesBaseURL);

    await actions.fillInputById(page, 'username', finalUsername);
    await actions.fillInputById(page, 'password', finalPassword);
    await actions.clickLoginButton(page);

    await use(page);

    // Delete if we created the user
    if (userCreated && finalUsername) {
      try {
        await idamHelper.deleteAccount(finalUsername);
      } catch (err) {
        console.warn(`Teardown failed for user ${finalUsername}:`, err);
      }
    }
  }
});

export const expect = baseExpect;
