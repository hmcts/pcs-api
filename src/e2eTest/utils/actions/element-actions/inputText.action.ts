import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: string): Promise<void> {

    let locator;
    if (typeof fieldParams !== 'string' && fieldParams.index !== null) {
      locator = page.locator(`//span[text()="${fieldParams.text}"]/parent::label/following-sibling::*[self::textarea or self::input][not(@disabled)]`)

      locator = (await locator.count()) > 1
        ? locator.nth(Number(fieldParams.index))
        : locator.first();

    } else {
      locator = typeof fieldParams === 'string'
        ? await this.getStringFieldLocator(page, fieldParams)
        : page.locator(`fieldset:has(h2:has-text("${fieldParams.text}")) textarea:visible:enabled,
      :has-text("${fieldParams.text}") ~ input:visible:enabled,
      label:has-text("${fieldParams.text}") ~ textarea,
      :has-text("${fieldParams.text}") ~ textarea:visible:enabled`).first();
    }
    await locator.fill(value);
  }

  private async getStringFieldLocator(page: Page, fieldParams: string) {
    const roleLocator = page.getByRole('textbox', { name: fieldParams, exact: true });
    return (await roleLocator.count() > 0)
      ? roleLocator
      : page.locator(`:has-text("${fieldParams}") ~ input:visible:enabled,
                      label:has-text("${fieldParams}") ~ textarea,
                      label:has-text("${fieldParams}") + div input`);
  }
}