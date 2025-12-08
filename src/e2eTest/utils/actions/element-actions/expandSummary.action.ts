import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ExpandSummaryAction implements IAction {
  async execute(page: Page, action: string, summaryDetails: string): Promise<void> {
    const locator = page.locator(`//summary[@class='govuk-details__summary']/child::span[contains(text(),"${summaryDetails}")]`);
    await locator.waitFor({ state: 'visible' });
    await locator.click();
  }
}