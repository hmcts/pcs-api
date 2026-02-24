import { Page, expect } from '@playwright/test';
import {IValidation, validationRecord} from "@utils/interfaces";

export class TextValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationRecord): Promise<void> {
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
        data.elementType = 'h3';
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
    const text = String(data.text);
    const locator = data.elementType === 'p'
      ? page.getByText(text, { exact: true }).first()
      : page.locator(`${data.elementType}:text-is("${data.text}")`).first();
    await locator.waitFor({ state: 'visible' });
    const actual = await locator.innerText();
    const normalized = (s: string) => (s ?? '').replace(/\s+/g, ' ').trim();
    expect(normalized(actual ?? ''), `Expected text "${text}"`).toBe(normalized(text));
  }
}
