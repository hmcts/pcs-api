import { Page, expect } from '@playwright/test';
import { IValidation, validationRecord } from '@utils/interfaces';

export class TextValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string, data: validationRecord): Promise<void> {
    const validationsMap = new Map<string, () => Promise<void>>([
      ['link', () => this.linkValidation(page, data)],
      ['text', () => this.textValidation(page, data)],
    ]);
    const validationToPerform = validationsMap.get(validation);
    if (!validationToPerform) throw new Error(`No action found for '${validation}'`);
    await validationToPerform();
  }

  private async textValidation(page: Page, data: validationRecord): Promise<void> {
    switch (data.elementType) {
      case 'heading':
        data.elementType = 'h1.govuk-heading-l';
        break;
      case 'subHeader':
        data.elementType = 'h3';
        break;
      case 'subHeading':
        data.elementType = 'h2';
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
      ? page.getByText(text, { exact: true }).filter({ visible: true }).first()
      : page.locator(`${data.elementType}:text-is("${data.text}")`).filter({ visible: true }).first();
    await locator.waitFor({ state: 'visible' });
    const actual = await locator.innerText();
    const normalized = (s: string) => (s ?? '').replace(/\s+/g, ' ').trim();
    expect(normalized(actual ?? ''), `Expected text "${text}"`).toBe(normalized(text));
  }

  private async linkValidation(page: Page, data: validationRecord): Promise<void> {
    const text = data?.text != null ? String(data.text) : '';
    if (!text) throw new Error('Link validation requires data: { text: "link text" }');

    const rowLocator = page.getByRole('row', { name: new RegExp(text) }).first();
    const linkLocator = page.getByRole('link', { name: text, exact: true }).filter({ visible: true }).first();
    const textLocator = page.getByText(text, { exact: true }).filter({ visible: true }).first();

    try {
      await rowLocator.waitFor({ state: 'visible', timeout: 40000 });
      return;
    } catch {
      try {
        await linkLocator.waitFor({ state: 'visible', timeout: 40000 });
        return;
      } catch {
        await textLocator.waitFor({ state: 'visible', timeout: 40000 });
      }
    }
  }
}
