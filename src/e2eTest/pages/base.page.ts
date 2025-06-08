import { Page, Locator } from '@playwright/test';

export class BasePage {
  constructor(protected page: Page) {}

  async goto(url: string): Promise<void> {
    await this.page.goto(url);
  }

  getElement(selector: string): Locator {
    return this.page.locator(selector);
  }

  async waitForText(text: string): Promise<void> {
    await this.page.waitForSelector(`text=${text}`, { state: 'visible' });
  }

  async waitForUrlToContain(partial: string): Promise<void> {
    await this.page.waitForURL(`**/${partial}`);
  }
}
