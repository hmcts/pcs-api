import { Page, expect } from '@playwright/test';
import {IValidation, validationData} from "../../interfaces/validation.interface";

export class TextValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
    let locator;
    switch (data.elementType) {
      case 'link':
        data.elementType = 'a';
        break;
      case 'paragraphLink':
        data.elementType = 'p > a';
        break;
      case 'heading':
        data.elementType = 'h1.govuk-heading-l';
        break;
      case 'subHeader':
        locator = page
          .locator('h2, h2.govuk-heading-l, h2.govuk-heading-m')
          .filter({ hasText: String(data.text) });
        break;
      case 'paragraph':
        data.elementType = 'p';
        break;
      case 'inlineText':
        data.elementType = 'span';
        break;
      case 'listItem':
        data.elementType = 'li';
    }
    if (!locator) {
      locator = page.locator(`${data.elementType}:has-text("${data.text}")`).first();
    }

    await expect(locator).toHaveText(String(data.text));
  }
}
