import { IdamUtils } from '@hmcts/playwright-common';
import { Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';
import { performAction, performValidation } from '../../controller';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import CookiesHelper from '../../helpers/cookies-helper';
import PageCookiesManager from '../../helpers/page-cookies-manager';

const USER_KEY = 'permanent-user'; // Single permanent user key
const runSetup = process.env.PLAYWRIGHT_RUN_SETUP === 'true';

export class LoginAction implements IAction {
  async execute(page: Page, action: string, userType: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createUserAndLogin', () => this.createUserAndLogin(page, userType as string, roles as string[])],
      ['login', () => this.login(page, userType)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async login(page: Page, user: string | actionRecord) {
    const cookiesManager = new PageCookiesManager(page);

    // Sign out first to clear any existing session
    await cookiesManager.cookiesSignOut();

    // Check if cookies exist
    const cookiesExist = await CookiesHelper.cookiesExist(USER_KEY);

    if (!cookiesExist) {
      // Perform full login via IDAM (cookies don't exist yet)
      const userEmail = typeof user === 'string' ? process.env.IDAM_PCS_USER_EMAIL : user.email;
      const userPassword = typeof user === 'string' ?  process.env.IDAM_PCS_USER_PASSWORD : user.password;
      if (!userEmail || !userPassword) {
        throw new Error('Login failed: missing credentials');
      }
      console.log(`Performing full login for user: ${userEmail}`);
      await performValidation('mainHeader', signInOrCreateAnAccount.mainHeader)
      await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
      await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
      await performAction('clickButton', signInOrCreateAnAccount.signInButton);

      // Wait for login to complete and save cookies
      await page.waitForURL(/.*\/cases.*/, { timeout: 30000 }).catch(() => {
        // If URL doesn't match, try waiting for a different indicator
        return page.waitForTimeout(2000);
      });

      // Save cookies after successful login (always save for future use)
      const cookies = await cookiesManager.getCookies();
      await CookiesHelper.writeCookies(cookies, USER_KEY);
      console.log(`Cookies saved for user: ${USER_KEY}`);
    } else {
      // Load cookies from file (cookies exist, use them for faster login)
      console.log(`Loading cookies for user: ${USER_KEY}`);
      const cookies = await CookiesHelper.getCookies(USER_KEY);
      await cookiesManager.cookiesLogin(cookies);

      // Navigate to a page to verify login
      await page.goto(process.env.MANAGE_CASE_BASE_URL || '');
      // Wait a bit for the page to load and verify we're logged in
      await page.waitForTimeout(2000);
    }
  }

  private async createUserAndLogin(page: Page, userType: string, roles: string[]): Promise<void> {
    const token = process.env.CREATE_USER_BEARER_TOKEN as string;
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
    await this.login(page, userType);
  }
}
