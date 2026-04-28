import { expect, Page } from '@playwright/test';
import { actionRecord, IAction } from '@utils/interfaces/action.interface';
import { actionRetries } from '../../../playwright.config';

export class ClickRadioButtonAction implements IAction {
  async execute(page: Page, action: string, params: actionRecord): Promise<void> {
    const idx = params.index !== undefined ? Number(params.index) : 0;
    const question = params.question as string;
    const option = params.option as string;

    const patterns = [
      () => this.radioPatternOptionOnly(page, question, option, idx),
      () => this.radioPattern1(page, question, option, idx),
      () => this.radioPattern2(page, question, option, idx),
      () => this.radioPattern3(page, question, option, idx),
    ];

    for (const getLocator of patterns) {
      const locator = getLocator();
      if (!locator) {
        continue;
      }
      if (await this.clickWithRetry(locator)) {
        return;
      }
    }
    const questionPart = question != null && question !== '' ? `question: "${question}", ` : '';
    throw new Error(`The radio button with ${questionPart}option: "${option}" is not found`);
  }

  private async clickWithRetry(locator: any): Promise<boolean> {
    if (!locator) {
      return false;
    }
    try {
      await locator.waitFor({ state: 'visible' });
    } catch {
      return false;
    }
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
    } while (!radioIsChecked && attempt < actionRetries);
    expect(radioIsChecked, radioIsChecked
      ? `Radio was checked after ${attempt} ${attempt === 1 ? 'attempt' : 'attempts'}`
      : `Radio was not checked after ${actionRetries} attempts`).toBe(true);
    return radioIsChecked;
  }

  /** Option-only (no `question`), e.g. fee/pay: `label >> text=${option}`. */
  private radioPatternOptionOnly(page: Page, question: string, option: string, idx: number) {
    if (question != null && question !== '') {
      return null;
    }
    return page.locator(`label >> text=${option}`).nth(idx);
  }

  private radioPattern1(page: Page, question: string, option: string, idx: number) {
    if (question == null || question === '') {
      return null;
    }
    return page.locator(`legend:has-text("${question}")`)
      .nth(idx)
      .locator('..')
      .getByRole('radio', { name: option, exact: true });
  }

  private radioPattern2(page: Page, question: string, option: string, idx: number) {
    if (question == null || question === '') {
      return null;
    }
    return page.locator(`//span[text()="${question}"]/ancestor::fieldset[1]//child::label[text()="${option}"]/preceding-sibling::input[@type='radio']`);
  }

  private radioPattern3(page: Page, question: string, option: string, idx: number) {
    return page.locator(`label >> text=${option}`);
  } 
}

 
