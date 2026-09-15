import { Page } from '@playwright/test';
import { actionData, actionRecord, IAction } from '../../interfaces/action.interface';


export class InputDateAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: actionData): Promise<void> {

    if (typeof fieldParams !== 'string') {

      throw new Error('Date field requires a string label and string[] value');

    }

    const locator = page.locator(`//span[text()="${fieldParams}"]/ancestor::div[1]/descendant::input[not(@disabled)]`);

    const locCount = await locator.count();
    const dateVal = String(value).split('/');
    if (dateVal.length > locCount) {
      throw new Error(
        `Date fields are not in the correct format. ` +
        `Found ${locCount} inputs but received ${dateVal.length} values.`
      );
    }

    for (let i = 0; i < dateVal.length; i++) {
      await locator.nth(i).fill(dateVal[i]);
    }
  }
}
