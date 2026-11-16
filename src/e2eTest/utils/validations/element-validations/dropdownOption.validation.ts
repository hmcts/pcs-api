import { Page, expect } from '@playwright/test';
import { IValidation, validationRecord } from '@utils/interfaces';

export class DropdownOptionValidation implements IValidation {
  async validate(
    page: Page,
    validation: string,
    fieldName: string,
    data: validationRecord
  ): Promise<void> {
    const dropdown = page.getByLabel(fieldName);

    const option = dropdown.getByRole('option', {
      name: String(data.text),
      exact: true
    });

    if (data.shouldBeAvailable) {
      await expect(option).toHaveCount(1);
    } else {
      await expect(option).toHaveCount(0);
    }
  }
}
