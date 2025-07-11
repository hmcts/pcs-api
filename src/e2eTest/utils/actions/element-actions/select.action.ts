import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class SelectAction implements IAction {
  async execute(page: Page, fieldName: string, option?: string | number): Promise<void> {
    if (option === undefined) {
      throw new Error(`Option value is required for select action on '${fieldName}'`);
    }

    const locator = page.locator(
      `label:has-text("${fieldName}") + select,
       label:has-text("${fieldName}") ~ select,
       [aria-label="${fieldName}"],
       select[name="${fieldName}"]`
    ).first();

    if (typeof option === 'number') {
      await locator.selectOption({ index: option });
    } else {
      await locator.selectOption(option);
    }
  }
}
