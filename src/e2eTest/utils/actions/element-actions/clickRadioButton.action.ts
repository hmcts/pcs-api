import { expect, Page } from '@playwright/test';
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

    const radioPattern1 = page.locator(`legend:has-text("${question}")`)
      .nth(idx)
      .locator('..')
      .getByRole('radio', { name: option as string, exact: true });

    try {
      await radioPattern1.waitFor({ state: 'visible', timeout: 1000 });
      await radioPattern1.click();
    } catch {
      const radioPattern2 = page.locator(`//span[text()="${question}"]/ancestor::fieldset[1]//child::label[text()="${option}"]/preceding-sibling::input[@type='radio']`);
      const count = await radioPattern2.count();
      if (count > 1) {
        await radioPattern2.nth(count - 1).click();
      } else {
        await radioPattern2.click();
      }
    }
  }
}
