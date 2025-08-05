import { Page, expect } from '@playwright/test';
import { IValidation} from '../../interfaces/validation.interface';

export class MainHeaderValidation implements IValidation {
  async validate(page: Page, fieldName: string): Promise<void> {

    const locator = page.locator('h1,h1.govuk-heading-xl, h1.govuk-heading-l');

    await expect(locator).toHaveText(fieldName, { timeout: 10000 });
  }
}
