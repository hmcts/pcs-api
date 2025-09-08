// import { Page } from '@playwright/test';
// import { IAction } from '../../interfaces/action.interface';
//
// export class InputTextAction implements IAction {
//   async execute(page: Page, action: string, fieldName: string, value?: string): Promise<void> {
//     if (!value) {
//       throw new Error('inputText action requires a value');
//     }
//     const locator = page.locator(`:has-text("${fieldName}") + input,
//            label:has-text("${fieldName}") + textarea,
//            label:has-text("${fieldName}") ~ input,
//            [aria-label="${fieldName}"],
//            label:has-text("${fieldName}")+ div input,
//            [placeholder="${fieldName}"]`)
//     await locator.fill(value);
//   }
// }
// // h2:has-text("${fieldName}") >> xpath=ancestor::ccd-label-field/following::ccd-field-write[1]//textarea`).first();
//

import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';

export class InputTextAction implements IAction {
  async execute(page: Page, action: string, fieldParams: string | actionRecord, value: string): Promise<void> {
    let locator;
    let fieldName: string;
    let index: number = 0;

    if (typeof fieldParams === 'string') {
      fieldName = fieldParams;
    } else {
      fieldName = fieldParams.title;
      if ('index' in fieldParams && typeof fieldParams.index === 'number') {
        index = fieldParams.index;
      }
    }
    locator = page.locator(`fieldset:has(h2:text("${fieldName}")) textarea:visible:enabled`);
    let count = await locator.count();

    if (count > 0) {
      if (count <= index) {
        throw new Error(
          `Requested index ${index} exceeds available ground textareas (${count}) for "${fieldName}"`
        );
      }
      await locator.nth(index).fill(value);
      return;
    }
    if (typeof fieldParams === 'string') {
      locator = page.locator(`
           :has-text("${fieldName}") + input,
           label:has-text("${fieldName}") + textarea,
           label:has-text("${fieldName}") ~ input,
           [aria-label="${fieldName}"],
           label:has-text("${fieldName}")+ div input,
           [placeholder="${fieldName}"]
      `);
    } else {
      locator = page
        .locator(`:has-text("${fieldParams.title}")`)
        .locator('..')
        .getByRole('textbox', { name: fieldParams.textbox });
    }
    count = await locator.count();
    if (count === 0) {
      throw new Error(`No match found for field "${fieldName}"`);
    }
    if (count <= index) {
      throw new Error(
        `Requested index ${index} exceeds available elements (${count}) for field "${fieldName}"`
      );
    }
    await locator.nth(index).fill(value);
  }
}
