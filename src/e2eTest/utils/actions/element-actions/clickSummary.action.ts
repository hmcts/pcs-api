import { Page } from '@playwright/test';

import { IAction } from '../../interfaces';

export class ClickSummaryAction implements IAction {
  async execute(page: Page, action: string, summaryText: string): Promise<void> {
    const summary = page.locator(`summary:has-text("${summaryText}")`);
    await summary.click();
  }
}
