import { Page } from '@playwright/test';
import { IAction, actionRecord } from '../../interfaces/action.interface';

export class SelectAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, option: string | number): Promise<void> {
    const locator = typeof fieldName === 'string'
      ? page.locator(`:has-text("${fieldName}") + select,
                                  :has-text("${fieldName}") ~ select,
                                  select[name="${fieldName}"]`)
      : page.locator(`:has-text("${fieldName.dropdown}") + select,
                                  :has-text("${fieldName.dropdown}") ~ select,
                                  select[name="${fieldName.dropdown}"]`).nth(Number(fieldName.index));
    if (typeof option === 'number') {
      await locator.selectOption({ index: option });
    } else {
      await locator.selectOption(option);
    }
  }
}
