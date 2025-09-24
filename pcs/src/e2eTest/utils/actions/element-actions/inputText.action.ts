import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: string): Promise<void> {
    const locator = typeof fieldParams === 'string'
      ? await this.getStringFieldLocator(page, fieldParams)
      : page.locator(`:has-text("${fieldParams.text}") ~ input,
                      fieldset:has(h2:text-is("${fieldParams.text}")) textarea:visible:enabled`).nth(Number(fieldParams.index));
    await locator.fill(value);
  }

  private async getStringFieldLocator(page: Page, fieldParams: string) {
    const roleLocator = page.getByRole('textbox', { name: fieldParams, exact: true });
    return (await roleLocator.count() > 0)
      ? roleLocator
      : page.locator(`:has-text("${fieldParams}") ~ input,
                      label:has-text("${fieldParams}") ~ textarea,
                      label:has-text("${fieldParams}") + div input`);
  }
}
