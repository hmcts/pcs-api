import * as fs from 'fs';
import * as path from 'path';
import { test } from '@playwright/test';

type Cookie = {
  name: string;
  value: string;
  domain: string;
  path: string;
  expires?: number;
  httpOnly?: boolean;
  secure?: boolean;
  sameSite?: 'Strict' | 'Lax' | 'None';
};

// Get the e2eTest directory (parent of utils)
const E2E_TEST_DIR = path.resolve(__dirname, '../..');
const USER_COOKIES_DIR = path.join(E2E_TEST_DIR, 'fixtures/.user-cookies');

export default class CookiesHelper {
  private static getCookiePath(userKey: string): string {
    // Ensure directory exists
    if (!fs.existsSync(USER_COOKIES_DIR)) {
      fs.mkdirSync(USER_COOKIES_DIR, { recursive: true });
    }
    return path.join(USER_COOKIES_DIR, `${userKey}.json`);
  }

  static async getCookies(userKey: string, isTeardown = false): Promise<Cookie[]> {
    const cookiePath = this.getCookiePath(userKey);
    try {
      const data = fs.readFileSync(cookiePath, 'utf-8');
      return JSON.parse(data);
    } catch (error) {
      if (isTeardown) {
        test.skip(true, (error as Error).message);
      }
      throw new Error(`Cookies path: ${cookiePath} does not exist for user: ${userKey}`);
    }
  }

  static async cookiesExist(userKey: string): Promise<boolean> {
    const cookiePath = this.getCookiePath(userKey);
    return fs.existsSync(cookiePath);
  }

  static async writeCookies(cookies: Cookie[], userKey: string): Promise<void> {
    const cookiePath = this.getCookiePath(userKey);
    fs.writeFileSync(cookiePath, JSON.stringify(cookies, null, 2), 'utf-8');
    console.log(`Cookies saved to: ${cookiePath}`);
  }

  static deleteAllCookies(): void {
    if (fs.existsSync(USER_COOKIES_DIR)) {
      fs.rmSync(USER_COOKIES_DIR, { recursive: true, force: true });
    }
  }
}

