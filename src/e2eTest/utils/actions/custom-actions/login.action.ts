import { IdamUtils } from '@hmcts/playwright-common';
import { expect, Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';
import { performAction } from '../../controller';
import { IAction, actionData, actionRecord } from '../../interfaces/action.interface';
import { signInOrCreateAnAccount } from '@data/page-data/signInOrCreateAnAccount.page.data';
import { LONG_TIMEOUT, VERY_SHORT_TIMEOUT } from 'playwright.config';

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

  /**
   * Retries the whole sign-in sequence once.
   *
   * Login runs in `beforeEach` at 47 call sites and had no retry anywhere, so a single
   * hiccup in IDAM — a shared AAT service — failed the test before it asserted anything.
   * That is the most expensive kind of flake to diagnose, because the reported failure is
   * whatever setup step happened to be in progress rather than anything the test covers.
   *
   * A failed attempt can leave the page part-way through the flow (email accepted, password
   * page never rendered), so the retry re-navigates first to get back to a known state
   * rather than typing into whatever is currently on screen.
   *
   * Two attempts, not more: each attempt can spend up to ~90s in its three LONG_TIMEOUT
   * waits, and this sits inside a 600s test timeout alongside case-creation setup. Three
   * attempts would reach ~270s of login alone — the same arithmetic that made a 7-retry
   * upload backoff unshippable earlier.
   */
  private async login(user: string | actionRecord, page: Page) {
    const userEmail = typeof user === 'string' ? process.env.IDAM_PCS_USER_EMAIL : user.email;
    const userPassword = typeof user === 'string' ?  process.env.IDAM_PCS_USER_PASSWORD : user.password;
    // Checked before the try: absent credentials are a configuration error, and retrying
    // would only turn one clear failure into a slower, more confusing one.
    if (!userEmail || !userPassword) {
      throw new Error('Login failed: missing credentials');
    }
    try {
      await this.attemptLogin(userEmail, userPassword, page);
      return;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      console.warn(`[login] first attempt failed, retrying once: ${message}`);
    }
    // Reset to a known state: the failed attempt may have left the page part-way through,
    // and it may in fact have succeeded late, so check for a signed-in session before
    // typing into a form that will never appear.
    await page.goto(process.env.MANAGE_CASE_BASE_URL as string);
    const alreadySignedIn = await page.getByText('Sign out', { exact: true }).first()
      .waitFor({ state: 'visible', timeout: VERY_SHORT_TIMEOUT })
      .then(() => true)
      .catch(() => false);
    if (alreadySignedIn) {
      return;
    }
    await this.attemptLogin(userEmail, userPassword, page);
  }

  private async attemptLogin(userEmail: string, userPassword: string, page: Page) {
    await page.waitForSelector('#email', { timeout: LONG_TIMEOUT });
    await performAction('inputText', signInOrCreateAnAccount.emailAddressLabel, userEmail);
    await performAction('clickButton', signInOrCreateAnAccount.continueButton);
    const pwdHeader = page.getByLabel('Enter your password', { exact: true });
    await expect(pwdHeader).toBeVisible({ timeout: LONG_TIMEOUT });
    await performAction('inputText', signInOrCreateAnAccount.passwordLabel, userPassword);
    await performAction('clickButton', signInOrCreateAnAccount.continueButton);
    const signOut = page.getByText('Sign out', { exact: true }).first();
    await expect(signOut).toBeVisible({ timeout: LONG_TIMEOUT });
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
