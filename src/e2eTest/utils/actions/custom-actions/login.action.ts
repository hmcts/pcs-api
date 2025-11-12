import { IdamUtils } from '@hmcts/playwright-common';
import { Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';
import { performAction, performValidation } from '../../controller';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import { SessionManager } from '../../session-manager';

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
    const userEmail = typeof user === 'string' ? process.env.IDAM_PCS_USER_EMAIL : user.email;
    const userPassword = typeof user === 'string' ?  process.env.IDAM_PCS_USER_PASSWORD : user.password;
    if (!userEmail || !userPassword || typeof userEmail !== 'string' || typeof userPassword !== 'string') {
      throw new Error('Login failed: missing credentials');
    }

    // Check if already authenticated (storageState should handle this, but verify)
    const currentUrl = page.url();
    if (!currentUrl.includes('/login') && !currentUrl.includes('/sign-in')) {
      // Already logged in, skip login
      console.log('Already authenticated, skipping login');
      return;
    }

    // If we reach here, we need to login (shouldn't happen if globalSetup worked correctly)
    console.warn('Login action called but user not authenticated. This should be handled by globalSetup.');
    await performValidation('mainHeader', signInOrCreateAnAccount.mainHeader)
    await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
    await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
    await performAction('clickButton', signInOrCreateAnAccount.signInButton);

    // Wait for navigation after login to ensure cookies are set
    await page.waitForURL('**/cases', { timeout: 30000 }).catch(() => {
      // If URL doesn't match, wait for any navigation
      return page.waitForLoadState('networkidle');
    });

    // Save storage state after successful login (Playwright's native format)
    await SessionManager.saveStorageState(page);
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
