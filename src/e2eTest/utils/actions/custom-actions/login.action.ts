import { IdamUtils, IdamPage, SessionUtils } from '@hmcts/playwright-common';
import { Page } from '@playwright/test';
import { v4 as uuidv4 } from 'uuid';
import { IAction, actionData, actionRecord } from '@utils/interfaces';
import * as path from 'path';
import * as fs from 'fs';

// Session configuration
const SESSION_DIR = path.join(process.cwd(), '.auth');
const STORAGE_STATE_FILE = 'storage-state.json';
const SESSION_COOKIE_NAME = 'Idam.Session';

function getStorageStatePath(): string {
  if (!fs.existsSync(SESSION_DIR)) {
    fs.mkdirSync(SESSION_DIR, { recursive: true });
  }
  return path.join(SESSION_DIR, STORAGE_STATE_FILE);
}

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
    const userPassword = typeof user === 'string' ? process.env.IDAM_PCS_USER_PASSWORD : user.password;
    if (!userEmail || !userPassword || typeof userEmail !== 'string' || typeof userPassword !== 'string') {
      throw new Error('Login failed: missing credentials');
    }

    // Skip login if already authenticated with valid session
    const storageStatePath = getStorageStatePath();
    try {
      if (SessionUtils.isSessionValid(storageStatePath, SESSION_COOKIE_NAME)) {
        console.log('Already authenticated with valid session, skipping login');
        return;
      }
    } catch {
      // Session check failed, proceed with login
    }

    // Perform login using IdamPage from @hmcts/playwright-common
    const idamPage = new IdamPage(page);
    await idamPage.login({
      username: userEmail,
      password: userPassword,
      sessionFile: storageStatePath,
    });
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
