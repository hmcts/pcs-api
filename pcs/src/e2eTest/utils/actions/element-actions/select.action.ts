import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class SelectAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, option: string | number): Promise<void> {
    const locator = page.locator(`:has-text("${fieldName}") + select,
                                  :has-text("${fieldName}") ~ select,
                                  select[name="${fieldName}"]`);
    if (typeof option === 'number') {
      await locator.selectOption({ index: option });
    } else {
      await locator.selectOption(option);
    }
  }
}
