import { Page, expect } from '@playwright/test';
import {IValidation, validationRecord} from "@utils/interfaces";

function escapeForRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export class TextValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationRecord): Promise<void> {
    let elementType = data.elementType;
    switch (data.elementType) {
      case 'link':
        elementType = 'a';
        break;
      case 'paragraphLink':
        elementType = 'p > a';
        break;
      case 'heading':
        elementType = 'h1.govuk-heading-l';
        break;
      case 'subHeader':
        elementType = 'h3';
        break;
      case 'paragraph':
        elementType = 'p';
        break;
      case 'inlineText':
        elementType = 'span';
        break;
      case 'listItem':
        elementType = 'li';
    }
    const text = String(data.text);
    const regexForText = (t: string, allowDashVariants = false, allowFlexibleWhitespace = false) => {
      let src = escapeForRegex(t);
      if (allowDashVariants) src = src.replace(/\\-/g, '[\\-\\u2013\\u2014]');
      if (allowFlexibleWhitespace) src = src.replace(/ /g, '\\s+');
      return new RegExp('^\\s*' + src + '\\s*$');
    };
    const locator = elementType === 'p'
      ? page.getByText(text, { exact: true }).first()
      : elementType === 'a'
        ? page.locator('xpath=//a[not(ancestor::*[@hidden])]').filter({ hasText: regexForText(text) }).first()
        : elementType === 'li'
          ? page.locator('xpath=//li[not(ancestor::*[@hidden])]').filter({ hasText: regexForText(text, true, true) }).first()
          : page.locator(`${elementType}:text-is("${data.text}")`).first();
    await locator.scrollIntoViewIfNeeded();
    await locator.waitFor({ state: 'visible' });
    const actual = await locator.innerText();
    const normalized = (s: string) => (s ?? '').replace(/\s+/g, ' ').replace(/[\u2013\u2014]/g, '-').trim();
    expect(normalized(actual ?? ''), `Expected text "${text}"`).toBe(normalized(text));
  }
}
