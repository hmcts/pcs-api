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

    const radioButton1 = page.locator(`legend:has-text("${question}")`).nth(idx).locator('..').getByRole('radio', { name: option as string, exact: true });

    try {
      await radioButton1.waitFor({ state: 'visible', timeout: 1000 });
      await radioButton1.click();
    } catch {
      const radioButton2 = page.locator(`fieldset:has-text("${question}")`).nth(idx).locator(`div.multiple-choice:has-text("${option}") input[type="radio"]`);
      await radioButton2.click();
    }
  }
}