import {Locator, Page} from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, fieldName: string, option?: string): Promise<void> {
    let locator: Locator;

    if (option) {
      // If a parent and option are provided, search inside the parent
      locator = page
        .locator(`fieldset:has-text("${fieldName}"),
                  div:has-text("${fieldName}"),
                  section:has-text("${fieldName}")`)
        .locator(`button:has-text("${option}"),
                  [value="${option}"],
                  [aria-label="${option}"],
                  [name="${option}"],
                  label:has-text("${option}") + button,
                  label:has-text("${option}") ~ button,
                  a:has-text("${option}")`);
    } else {
      // Original behavior for just a single fieldName
      locator = page.locator(`button:has-text("${fieldName}"),
                              [value="${fieldName}"],
                              [aria-label="${fieldName}"],
                              [name="${fieldName}"],
                              label:has-text("${fieldName}") + button,
                              label:has-text("${fieldName}") ~ button,
                              a:has-text("${fieldName}")`);
    }
    await locator.first().click();
  }
}
