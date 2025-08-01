
import { Page, expect,test } from '@playwright/test';
import {IValidation, validationData} from "../../interfaces/validation.interface";

export class TextValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
    switch (data.elementType) {
      case 'link':
        data.elementType = 'a';
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
    }
    const locator = page.locator(`${data.elementType}:has-text("${data.text}")`)

    await test.step(`Found locator message: "${data.expected}"`, async () => {
      if ('text' in data) {
        await expect(locator).toHaveText(String(data.text));
      } else {
        throw new Error('TextValidation requires "expected" property in data');
      }
    });
  }
}
