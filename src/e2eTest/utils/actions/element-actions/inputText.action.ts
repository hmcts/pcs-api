import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, value: string): Promise<void> {
    const roleLocator = page.getByRole('textbox', { name: fieldName, exact: true });
    const locator = (await roleLocator.count()) > 0
      ? roleLocator
      : page.locator(`:has-text("${fieldName}") ~ input:visible:enabled,
                       label:has-text("${fieldName}") ~ textarea,
                       label:has-text("${fieldName}") + div input`);
    if (await locator.count() === 0) {
      throw new Error(`Input field "${fieldName}" not found on the page.`);
    }
    await locator.fill(value);
  }
}
