import { Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: actionRecord): Promise<void> {
    const idx = params.index !== undefined ? Number(params.index) : 0;
    const question = params.question as string;
    const option = params.option as string;

    if (await this.clickWithRetry(page, this.radioPattern1(page, question, option, idx))) return;
    if (await this.clickWithRetry(page, this.radioPattern2(page, question, option, idx))) return;
    if (await this.clickWithRetry(page, this.radioPattern3(page, question, option, idx))) return;
  }

  private async clickWithRetry(page: Page, locator: any): Promise<boolean> {
    if ((await locator.count()) !== 1) {
      return false;
    }
    for (let attempt = 0; attempt < 3; attempt++) {
      const clicked = await locator.click({
        timeout: 2000,
        force: attempt > 0
      }).then(() => true).catch(() => false);

      if (clicked) {
        await page.waitForTimeout(300);
        const isChecked = await locator.isChecked().then(() => true).catch(() => false);
        if (isChecked) {
          return true;
        }
      }

      if (attempt < 2) {
        await page.waitForTimeout(500);
      }
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
