import { Page } from '@playwright/test';

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

export default class PageCookiesManager {
  constructor(private page: Page) {}

  async getCookies(): Promise<Cookie[]> {
    return await this.page.context().cookies();
  }

  async cookiesLogin(cookies: Cookie[]): Promise<void> {
    console.log(`Authenticating by setting ${cookies.length} cookies`);
    await this.page.context().addCookies(cookies);
  }

  async cookiesSignOut(): Promise<void> {
    await this.page.context().clearCookies();
  }
}

