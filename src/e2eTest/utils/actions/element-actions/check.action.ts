// actions/check.action.ts
import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class CheckAction implements IAction {
  async execute(page: Page, action: string, fieldName: string): Promise<void> {
    const locator = page.locator(`input[type="checkbox"][aria-label="${fieldName}"],
           input[type="checkbox"][name="${fieldName}"],
           label:has-text("${fieldName}") input[type="checkbox"]`);
    await locator.check();
  }
}
