import { test as baseTest, expect as baseExpect, Page } from '@playwright/test';
import { loginHelper } from '../helpers/idam-helpers/login.helper';
import { LoginPage } from '@pages/login.page';
import Config from "config/config";

type MyFixtures = {
  loggedInPage: Page;
};

export const test = baseTest.extend<MyFixtures>({
  loggedInPage: async ({ page }, use) => {
    const login = new LoginPage(page);
    const { userData, password } = await loginHelper.createUser(Config.e2e.roles);

    if (Config.manageCasesBaseURL.includes('localhost')) {
      userData.user.email = 'caseworker@pcs.com';
    }

    await login.login(userData.user.email, password);
    await use(page);
  }
});

export const expect = baseExpect;
