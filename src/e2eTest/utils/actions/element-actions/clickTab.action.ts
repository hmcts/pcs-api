import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickTabAction implements IAction {
  async execute(page: Page, action: string, tabName: string): Promise<void> {
    const locator = page.locator(`div.mat-tab-label .mat-tab-label-content:has-text("${tabName}"),
                                  a:text-is("${tabName}")`);
    await locator.waitFor({ state: 'visible' });
    await locator.click();
  }
}