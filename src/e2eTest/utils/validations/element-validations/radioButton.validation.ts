import { Page, expect } from '@playwright/test';
import { IValidation, validationData } from '../../interfaces/validation.interface';

export class RadioButtonValidation implements IValidation {
  async validate(page: Page, fieldName: string, data: validationData): Promise<void> {
    const radio = page.getByRole('radio', { name: String(data.option) });
    if (data.shouldBeChecked) {
      await expect(radio).toBeChecked();
    } else {
      await expect(radio).not.toBeChecked();
    }
  }
}
