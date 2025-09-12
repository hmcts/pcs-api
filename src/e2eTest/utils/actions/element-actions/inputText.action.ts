import {Page} from '@playwright/test';
import {IAction} from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, value: string): Promise<void> {
    const locator = page.locator(`:has-text("${fieldName}") + input,
                                   label:has-text("${fieldName}") ~ textarea,
                                   label:has-text("${fieldName}") ~ input,
                                   [aria-label="${fieldName}"],
                                   label:has-text("${fieldName}")+ div input,
                                   [placeholder="${fieldName}"]`);
    await locator.fill(value);
  }
}
