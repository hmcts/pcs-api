// import { Page } from '@playwright/test';
// import { actionRecord, IAction } from '../../interfaces/action.interface';
//
// export class InputTextAction implements IAction {
//   async execute(
//     page: Page,
//     action: string,
//     fieldParams: string | actionRecord,
//     value: string
//   ): Promise<void> {
//     const locator =
//       typeof fieldParams === 'string'
//         ? page.locator(`
//             :has-text("${fieldParams}") ~ input,
//             label:has-text("${fieldParams}") + textarea,
//             label:has-text("${fieldParams}") + div input
//           `)
//         : page
//           .locator(`:has-text("${fieldParams.title}")`)
//           .locator('..')
//           .getByRole('textbox', { name: fieldParams.textbox });
//
//     try {
//       await locator.fill(value);
//     } catch (error) {
//       // Use nth(index) in catch; default to 0 if index not provided
//       // @ts-ignore
//       await locator.nth(<number><unknown>typeof fieldParams === 'string' ? 0 : fieldParams.index ?? 0).fill(value);
//     }
//   }
// }

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
        :has-text("${fieldParams}") ~ input,
        :has-text("${fieldParams}") + input,
        label:has-text("${fieldParams}") + textarea,
        label:has-text("${fieldParams}") + div input
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
