import { Page, test } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaData } from '@utils/actions/custom-actions/collectCYAData.action';
import { extractCCDTable } from '@utils/cya/cya-validation-utils';
import { buildCYAErrorMessage, hasValidationErrors, validateCYAData } from '@utils/cya/cya-validation-helpers';

export class CheckYourAnswersValidation implements IValidation {
  async validate(page: Page, _validation: string, _fieldName?: string, _data?: any): Promise<void> {
    const collectedQA = cyaData.collectedQAPairs || [];
    if (collectedQA.length === 0) {
      throw new Error('Final CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
    }

    const pageQA = await test.step('Extract Q&A pairs from CYA page', async () => {
      return await extractCCDTable(page, 'table.form-table');
    });

    const results = await test.step(`Validate ${collectedQA.length} collected Q&A pairs against CYA page`, async () => {
      return validateCYAData(collectedQA, pageQA, true);
    });

    if (hasValidationErrors(results)) {
      throw new Error(buildCYAErrorMessage(results, 'Final'));
    }
  }
}
