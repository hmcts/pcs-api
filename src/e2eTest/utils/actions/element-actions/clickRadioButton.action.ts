import {Page} from '@playwright/test';
import {actionData, IAction} from '@utils/interfaces/action.interface';

export class ClickRadioButton implements IAction {
  async execute(
    page: Page,
    action: string,
    params: actionData
  ): Promise<void> {
    const radioButton = typeof params === 'string'
      ? page.locator(`input[type="radio"] + label:has-text("${params}")`)
      : page
        .locator(
          `div.form-group:has(legend label span.form-label:has-text("${(params as {question: string}).question}"))`
        )
        .locator(`label.form-label:has-text("${(params as {option: string}).option}")`);

    await radioButton.click();
  }
}
