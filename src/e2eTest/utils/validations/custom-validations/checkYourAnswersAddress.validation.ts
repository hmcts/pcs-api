import { Page, test } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaAddressData } from '@utils/actions/custom-actions/collectCYAData.action';
import { extractSimpleQAFromPage } from '@utils/cya/cya-validation-utils';
import { buildCYAErrorMessage, hasValidationErrors, validateCYAData } from '@utils/cya/cya-validation-helpers';

export class CheckYourAnswersAddressValidation implements IValidation {
  async validate(page: Page, _validation: string, _fieldName?: string, _data?: any): Promise<void> {
    const collectedQA = cyaAddressData.collectedQAPairs || [];
    if (collectedQA.length === 0) {
      throw new Error('Address CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
    }

    const pageQA = await test.step('Extract Q&A pairs from Address CYA page', async () => {
      return await extractSimpleQAFromPage(page);
    });

    const results = await test.step(`Validate ${collectedQA.length} collected Q&A pairs against Address CYA page`, async () => {
      return validateCYAData(collectedQA, pageQA, false);
    });

    if (hasValidationErrors(results)) {
      throw new Error(buildCYAErrorMessage(results, 'Address'));
    }
  }
}
