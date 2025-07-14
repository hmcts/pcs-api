// validations/text.validation.ts
import {Page, expect, test} from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class TextValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    const textLocator = page.locator(`a[aria-label$="${data.content}"]`);
    const text = (await textLocator.innerText())?.trim();
    if (!text) {
      throw new Error('text is not present or empty.');
    }
    await test.step(`Found text: "${text}"`, async () => {
      if (!data) {
        throw new Error('data is required for TextValidation');
      }
      expect(text, `text is not matching with provided data`).toBe(data.content);
    });
  }
}
