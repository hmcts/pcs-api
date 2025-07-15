// actions/clear.action.ts
import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClearAction implements IAction {
  async execute(page: Page, fieldName: string): Promise<void> {
    const locator = page.locator(`[data-testid="${fieldName}"]`);
    await locator.clear();
  }
}
