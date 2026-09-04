import { Page, expect } from '@playwright/test';
import { IValidation} from '../../interfaces/validation.interface';
import { pageHeading } from '@utils/common/locator.utils';

export class MainHeaderValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName: string): Promise<void> {
    await expect(pageHeading(page, fieldName)).toHaveText(fieldName);
  }
}


