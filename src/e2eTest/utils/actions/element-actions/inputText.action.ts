import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, value?: string): Promise<void> {
    if (!value) {
      throw new Error('inputText action requires a value');
    }
    const locator = page.locator(`label:has-text("${fieldName}") + input,
           label:has-text("${fieldName}") + textarea,
           label:has-text("${fieldName}") ~ input,
           [aria-label="${fieldName}"],
           label:has-text("${fieldName}")+ div input,
           [placeholder="${fieldName}"],
           fieldset:has(h2:text("${fieldName}")) textarea,
           fieldset:has-text("${fieldName}") textarea`);
    await locator.fill(value);
  }
}
