import { Page } from '@playwright/test';
import { IAction, actionRecord } from '../../interfaces/action.interface';
import { MEDIUM_TIMEOUT } from '../../../playwright.config';

export class SelectAction implements IAction {
  async execute(page: Page, action: string, fieldName: string | actionRecord, option: string | number): Promise<void> {
    const locator = typeof fieldName === 'string'
      ? page.locator(`:has-text("${fieldName}") + select,
                                  :has-text("${fieldName}") ~ select,
                                  select[name="${fieldName}"]`)
      : page.locator(`:has-text("${fieldName.dropdown}") + select,
                                  :has-text("${fieldName.dropdown}") ~ select,
                                  select[name="${fieldName.dropdown}"]`).nth(Number(fieldName.index));
    // .first() on both paths. selectOption is strict, and the `:has-text()` selectors above
    // match ancestors as well as siblings, so a page with more than one <select> resolved to
    // several elements and the index path threw a strict-mode violation rather than
    // selecting anything.
    const select = locator.first();
    if (typeof option === 'number') {
      await select.selectOption({ index: option });
      return;
    }
    // selectOption auto-waits for the <select> but NOT for the option to exist inside it.
    // These dropdowns are populated by CCD after the page loads, so asking for an option
    // that has not arrived yet failed instantly with 'did not find some options', which
    // reads like a missing feature rather than a race.
    //
    // Matches value OR label because that is what selectOption does with a bare string.
    // Waiting on the label alone would sit out the full timeout whenever a caller passes a
    // value, adding 10s to a call that was going to succeed.
    //
    // Swallows its own timeout: a genuinely absent option should be reported by
    // selectOption against the real dropdown, not as a count assertion on a locator.
    const wanted = select.locator(`option[value="${option}"]`)
      .or(select.getByRole('option', { name: option as string, exact: true }));
    await wanted.first()
      .waitFor({ state: 'attached', timeout: MEDIUM_TIMEOUT })
      .catch(() => undefined);
    await select.selectOption(option);
  }
}
