// validations/text.validation.ts
import {Page, expect, test} from '@playwright/test';
import {IValidation, ValidationData} from "../../interfaces/validation.interface";

export class TextValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: ValidationData): Promise<void> {
    const textLocator = page.locator(`a[aria-label$="${data.content}"]`);
    const text = (await textLocator.innerText())?.trim();
    if (!text) {
      throw new Error('Table column is not present or empty.');
    }
    await test.step(`Found number: "${text}"`, async () => {
      if (!data) {
        throw new Error('table column requires case number');
      }
      console.log(`Table column ${data.content}`);
      expect(text, `Table column should exactly match`).toBe(data.content);
    });
  }
}
