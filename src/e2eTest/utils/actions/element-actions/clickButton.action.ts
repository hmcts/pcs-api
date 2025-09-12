import {Locator, Page} from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, option?: string): Promise<void> {
    const locator = page.locator(`button:text-is("${fieldName}"),
                                  [value="${fieldName}"],
                                  :has-text("${fieldName}") + button,
                                  :has-text("${fieldName}") ~ button,
                                  a:text-is("${fieldName}")`);
    await locator.first().click();
  }
}
