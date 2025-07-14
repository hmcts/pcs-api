// actions/click.action.ts
import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickAction implements IAction {
  async execute(page: Page, fieldName: string): Promise<void> {
    const locator = page.locator(`button:has-text("${fieldName}"),
           [value="${fieldName}"],
           [aria-label="${fieldName}"],
           [name="${fieldName}"],
           label:has-text("${fieldName}") + button,
           label:has-text("${fieldName}") ~ button,
           a:has-text("${fieldName}")`);
    await locator.click();
  }
}
