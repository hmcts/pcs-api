import {expect, Locator, Page} from '@playwright/test';
import { BasePage } from './base.page';

export class HomePage extends BasePage {
  readonly manageCasesLink: Locator;

  constructor(page: Page) {
    super(page);
    this.manageCasesLink = page.locator('a', { hasText: 'Manage cases' });
  }

  async expectManageCasesLinkVisible() {
    await expect(this.manageCasesLink).toBeVisible();
  }

  async clickManageCasesLink() {
    await this.manageCasesLink.click();
  }
}
