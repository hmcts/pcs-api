import { Page, expect } from '@playwright/test';
import {IValidation, validationData} from "../../interfaces/validation.interface";

export class TextValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
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
      case 'paragraph':
        data.elementType = 'p';
        break;
      case 'inlineText':
        data.elementType = 'span';
        break;
      case 'listItem':
        data.elementType = 'li';
    }
    const locator = page.locator(`${data.elementType}:has-text("${data.text}")`).first()
    await expect(locator).toHaveText(String(data.text));
  }
}
