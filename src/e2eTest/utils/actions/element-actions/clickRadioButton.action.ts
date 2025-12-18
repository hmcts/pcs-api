import { Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: actionRecord): Promise<void> {
    const idx = params.index !== undefined ? Number(params.index) : 0;
    const question = params.question as string;
    const option = params.option as string;

    const patterns = [
      () => {
        const questionSpan = page.locator(`span.form-label:has-text("${question}")`).nth(idx);
        return questionSpan.locator('xpath=ancestor::div[contains(@class, "form-group")][1]')
          .locator('div.multiple-choice')
          .filter({ has: page.locator('label.form-label').filter({ hasText: new RegExp(`^${this.escapeRegex(option)}$`) }) })
          .locator('input[type="radio"]');
      },
      () => {
        const escapedOption = option.replace(/'/g, "', \"'\", '");
        return page.locator(`span.form-label:has-text("${question}")`)
          .nth(idx)
          .locator(`xpath=ancestor::fieldset[1]//div.multiple-choice[.//label.form-label[normalize-space()='${escapedOption}']]/input[@type='radio']`);
      },
      () => {
        return page.locator(`legend:has-text("${question}")`)
          .nth(idx)
          .locator('xpath=ancestor::fieldset[1]')
          .locator('div.multiple-choice')
          .filter({ has: page.locator('label.form-label').filter({ hasText: option }) })
          .locator('input[type="radio"]');
      },
      () => {
        return page.locator(`legend:has-text("${question}")`)
          .nth(idx)
          .locator('..')
          .getByRole('radio', { name: option as string, exact: true });
      }
    ];

    for (const getPattern of patterns) {
      const locator = getPattern();
      if ((await locator.count()) === 1 && await locator.isVisible()) {
        await locator.check();
        return;
      }
    }
  }

  private escapeRegex(text: string): string {
    return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
}