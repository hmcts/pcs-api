import { IdamUtils } from '@hmcts/playwright-common';
import { expect, Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';
import { performAction } from '../../controller';
import { IAction, actionData, actionRecord } from '../../interfaces/action.interface';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import { LONG_TIMEOUT } from 'playwright.config';

export class LoginAction implements IAction {
  async execute(page: Page, action: string, userType: string | actionRecord, roles?: actionData): Promise<void> {
    const actionsMap = new Map<string, () => Promise<void>>([
      ['createUserAndLogin', () => this.createUserAndLogin(userType as string, roles as string[], page)],
      ['login', () => this.login(userType, page)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async login(user: string | actionRecord, page: Page) {
    const userEmail = typeof user === 'string' ? process.env.IDAM_PCS_USER_EMAIL : user.email;
    const userPassword = typeof user === 'string' ?  process.env.IDAM_PCS_USER_PASSWORD : user.password;
    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }
    try {
      await page.waitForSelector('#email', { timeout: LONG_TIMEOUT });
    } catch (error) {
      const heading = await page.locator('h1').first().innerText().catch(() => '<no heading>');
      const alreadySignedIn = await page.getByText('Sign out', { exact: true }).first()
        .isVisible().catch(() => false);
      console.warn(`[login] the email field never appeared for ${userEmail} — url ${page.url()}, `
        + `heading "${heading}", already signed in: ${alreadySignedIn}`);
      throw error;
    }
    await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
    await performAction('clickButton', signInOrCreateAnAccount.continueButton);
    const pwdHeader = page.getByLabel('Enter your password', { exact: true });
    await expect(pwdHeader).toBeVisible({ timeout: LONG_TIMEOUT });
    await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
    await performAction('clickButton', signInOrCreateAnAccount.continueButton);
    const signOut = page.getByText('Sign out', { exact: true }).first();
    try {
      await expect(signOut).toBeVisible({ timeout: LONG_TIMEOUT });
    } catch (error) {
      const heading = await page.locator('h1').first().innerText().catch(() => '<no heading>');
      const errorText = await page.locator('.govuk-error-summary, .error-summary, #errorSummary')
        .first().innerText().catch(() => '');
      console.warn(`[login] "Sign out" never appeared for ${userEmail} — url ${page.url()}, `
        + `heading "${heading}"${errorText ? `, error: ${errorText.replace(/\s+/g, ' ').slice(0, 300)}` : ''}`);
      throw error;
    }
  }

  private async createUserAndLogin(userType: string, roles: string[], page:Page): Promise<void> {
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
    await this.login(userType, page);
  }
}
