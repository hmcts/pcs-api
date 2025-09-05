// actions/check.action.ts
import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class CheckAction implements IAction {
  async execute(page: Page, action: string, fieldName: string): Promise<void> {
    const locator = page.getByRole('checkbox', { name: fieldName });
    await locator.check();
  }
}
