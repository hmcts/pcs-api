import { test as baseTest, expect as baseExpect, Page } from '@playwright/test';
import { loginHelper } from '@helpers/idam-helpers/login.helper';
import { LoginPage } from '@pages/login.page';
import Config from "config/config";
import * as idamHelper from '@helpers/idam-helpers/idam.helper';


type MyFixtures = {
  loggedInPage: Page;
};

export const test = baseTest.extend<MyFixtures>({
  loggedInPage: async ({ page }, use) => {
    const login = new LoginPage(page);

    let userCreated = false;

    const { userData, password } = await (async () => {
      if (Config.manageCasesBaseURL.includes(Config.localHost.env)) {
        return {
          userData: { user: { email: Config.localHost.username } },
          password: Config.localHost.password
        };
      } else {
        userCreated = true;
        return await loginHelper.createUser(Config.iDam.roles);
      }
    })();

    await login.login(userData.user.email, password);
    await use(page);

    //tear down
    if (userCreated) {
      try {
        await idamHelper.deleteAccount(userData.user.email);
      } catch (err) {
        console.warn(`Teardown failed for user ${userData.user.email}:`, err);
      }
    }

  }
});

export const expect = baseExpect;
