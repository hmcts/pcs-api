import { expect, Page } from '@playwright/test';

export class AssertionHelper {
  private static currentPage: Page | null = null;

  static initialize(page: Page): void {
    AssertionHelper.currentPage = page;
  }

  private static getActivePage(): Page {
    if (!AssertionHelper.currentPage) {
      throw new Error('AssertionHelper not initialized. Call initAssertionHelper(page) before using assertions.');
    }
    return AssertionHelper.currentPage;
  }

  static async assertAlertMessageMatches(pattern: RegExp): Promise<void> {
    const locator = AssertionHelper.getActivePage().locator('div.alert-message');
    const text = await locator.textContent();
    if (!text || !text.trim()) {
      throw new Error('Alert message not found or empty.');
    }
    expect(text.trim()).toMatch(pattern);
  }
}

export const initAssertionHelper = AssertionHelper.initialize;
export const assertAlertMessageMatches = AssertionHelper.assertAlertMessageMatches;
