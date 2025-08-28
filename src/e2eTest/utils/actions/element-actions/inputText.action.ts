import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, value?: string): Promise<void> {
    if (!value) {
      throw new Error('inputText action requires a value');
    }
    const locator = page.locator(`:has-text("${fieldName}") + input,
    :has-text("${fieldName}") + textarea`);
    await locator.fill(value);
  }
}
