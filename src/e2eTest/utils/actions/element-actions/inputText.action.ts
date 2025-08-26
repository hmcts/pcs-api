import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, value?: string): Promise<void> {
    if (!value) {
      throw new Error('inputText action requires a value');
    }
    if (/\d{2}-\d{2}-\d{4}/.test(value)) {
      const [day, month, year] = value.split("-");

      await page
        .locator(`label:has-text("${fieldName}") ~ input[id$="-day"]`)
        .fill(day);
      await page
        .locator(`label:has-text("${fieldName}") ~ input[id$="-month"]`)
        .fill(month);
      await page
        .locator(`label:has-text("${fieldName}") ~ input[id$="-year"]`)
        .fill(year);

      return; // exit after filling date
    }
    const locator = page.locator(`label:has-text("${fieldName}") + input,
           label:has-text("${fieldName}") + textarea,
           label:has-text("${fieldName}") ~ input,
           [aria-label="${fieldName}"],
           [placeholder="${fieldName}"]`);
    await locator.fill(value);
  }
}
