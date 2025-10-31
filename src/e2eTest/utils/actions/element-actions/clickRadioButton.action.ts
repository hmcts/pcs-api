import { Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';

// export class ClickRadioButton implements IAction {
//   async execute(page: Page, action: string, params: string | actionRecord): Promise<void> {
//     const radioButton = typeof params === 'string'
//       ? page.locator(`input[type="radio"] + label:has-text("${params}")`)
//       : page.locator(`legend:has-text("${params.question}")`)
//             .locator('..')
//             .getByRole('radio', { name: params.option as string, exact: true });
//     await radioButton.click();
//   }
// }

export class ClickRadioButton implements IAction {
  async execute(page: Page, action: string, params: string | actionRecord): Promise<void> {
    if (typeof params === 'string') {
      const radioButton = page.locator(`input[type="radio"] + label:has-text("${params}")`);
      await radioButton.click();
      return;
    }

    // Ensure params is typed as actionRecord
    const { question, option, index } = params as actionRecord;

    // Default to 0 if index is not provided
    const idx = index !== undefined ? Number(index) : 0;

    const questionLocators = page.locator(`legend:has-text("${question}")`);
    const targetQuestion = questionLocators.nth(idx);

    const radioButton = targetQuestion
      .locator('..')
      .getByRole('radio', { name: option as string, exact: true });

    await radioButton.click();
  }
}
