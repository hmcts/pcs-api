import {Page} from '@playwright/test';

import {IAction} from '../../interfaces/action.interface';

export class ClickRadioButton implements IAction {
  async execute(
    page: Page,
    action: string,
    params: string | { question?: string; option: string }
  ): Promise<void> {
    let question: string | undefined;
    let option: string;
    if (typeof params === 'string') {
      option = params;
    } else {
      question = params.question;
      option = params.option;
    }
    let radioLabel;
    if (question) {
      const section = page.locator(
        `div.form-group:has(legend label span.form-label:has-text("${question}"))`
      );
      radioLabel = section.locator(`label.form-label:has-text("${option}")`);
    } else {
      radioLabel = page.locator(`label.form-label:has-text("${option}")`);
    }
    await radioLabel.click();
  }
}
