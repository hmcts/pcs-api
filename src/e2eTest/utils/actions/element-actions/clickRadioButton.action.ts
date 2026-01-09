import { Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: actionRecord): Promise<void> {
    const idx = params.index !== undefined ? Number(params.index) : 0;
    const question = params.question as string;
    const option = params.option as string;

    if (await this.clickRadioButton(this.radioPattern1(page, question, option, idx))) return;
    if (await this.clickRadioButton(this.radioPattern2(page, question, option, idx))) return;
    if (await this.clickRadioButton(this.radioPattern3(page, question, option, idx))) return;
  }

  private async clickRadioButton(locator: any): Promise<boolean> {
    const count = await locator.count();
    if (count === 1 && await locator.isVisible()) {
      await locator.click();
      return true;
    }
    return false;
  }

  private radioPattern1(page: Page, question: string, option: string, idx: number) {
    const questionSpan = page.locator(`span.form-label:has-text("${question}")`).nth(idx);
    return questionSpan.locator('xpath=ancestor::div[contains(@class, "form-group")][1]')
      .locator('div.multiple-choice')
      .filter({ has: page.locator('label.form-label').filter({ hasText: new RegExp(`^${this.escapeRegex(option)}$`) }) })
      .locator('input[type="radio"]');
  }

  private radioPattern2(page: Page, question: string, option: string, idx: number) {
    const escapedOption = option.replace(/'/g, "', \"'\", '");
    return page.locator(`span.form-label:has-text("${question}")`)
      .nth(idx)
      .locator(`xpath=ancestor::fieldset[1]//div.multiple-choice[.//label.form-label[normalize-space()='${escapedOption}']]/input[@type='radio']`);
  }

  private radioPattern3(page: Page, question: string, option: string, idx: number) {
    return page.locator(`legend:has-text("${question}")`)
      .nth(idx)
      .locator('xpath=ancestor::fieldset[1]')
      .locator('div.multiple-choice')
      .filter({ has: page.locator('label.form-label').filter({ hasText: option }) })
      .locator('input[type="radio"]');
  }

  private escapeRegex(text: string): string {
    return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
}
