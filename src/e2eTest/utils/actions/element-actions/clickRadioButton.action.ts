import { Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: string | actionRecord): Promise<void> {
    if (typeof params === 'string') {
      const radioButton = page.locator(`input[type="radio"] + label:has-text("${params}")`);
      await radioButton.click();
      return;
    }
    const { question, option, index } = params as actionRecord;
    const idx = index !== undefined ? Number(index) : 0;
    const questionLocators = page.locator(`legend:has-text("${question}")`);
    const targetQuestion = questionLocators.nth(idx);
    const radioButton = targetQuestion
      .locator('..')
      .getByRole('radio', { name: option as string, exact: true });
    await radioButton.click();
  }
}
