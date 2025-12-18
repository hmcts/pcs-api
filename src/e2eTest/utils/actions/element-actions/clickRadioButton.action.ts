import { Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: actionRecord): Promise<void> {
    const idx = params.index !== undefined ? Number(params.index) : 0;
    const question = params.question as string;
    const option = params.option as string;

    const patterns = [
      () => this.radioPattern1(page, question, option, idx),
      () => this.radioPattern2(page, question, option, idx),
      () => this.radioPattern3(page, question, option, idx)
    ];

    for (const getLocator of patterns) {
      const locator = getLocator();
      if (await this.clickWithRetry(locator)) {
        return;
      }
    }
  }

  private async clickWithRetry(locator: any): Promise<boolean> {
    if ((await locator.count()) !== 1) {
      return false;
    }

    let attempt = 0;
    let radioIsChecked = false;

    do {
      attempt++;
      await locator.click({ timeout: 2000, force: attempt > 1 });
      await new Promise(resolve => setTimeout(resolve, 500));
      radioIsChecked = await locator.isChecked();
    } while (!radioIsChecked && attempt < 3);

    return radioIsChecked;
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
