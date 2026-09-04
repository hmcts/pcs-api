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
    // .first() on the index path too. `:has-text()` matches ancestors as well as siblings, so
    // on a page with more than one <select> this locator resolves to several elements and
    // selectOption - being strict - waits out its whole timeout and then fails, reported as
    // "locator.selectOption: Timeout 5000ms exceeded" rather than as an ambiguous selector.
    // Verified locally: two selects on a page, index path times out; with .first() it selects.
    // The string path already had .first(); only the { index } path was missing it.
    const select = locator.first();
    if (typeof option === 'number') {
      await select.selectOption({ index: option });
      return;
    }
    await select.selectOption(option);
  }
}
