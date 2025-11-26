import { Page, test } from '@playwright/test';
import {IValidation, validationData} from '@utils/interfaces';
import { cyaData } from '@utils/actions/custom-actions/collectCYAData.action';
import { extractCCDTable } from '@utils/cya/cya-extraction-utils';
import { buildCYAErrorMessage, hasValidationErrors, validateCYAData } from '@utils/cya/cya-validation-util';

export class CheckYourAnswersValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: validationData): Promise<void> {
    const collectedQA = cyaData.collectedQAPairs;
    if (collectedQA.length === 0) {
      throw new Error('Final CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
    }

    const pageCYAQA = await test.step('Extract Q&A pairs from CYA page', async () => {
      return await extractCCDTable(page, 'table.form-table');
    });

    //For allure report logging
    if (data !== undefined) {
      await test.step('Attach Q&A data to Allure report', async () => {
        const collectedQAText = this.formatQAData(collectedQA);
        const pageCYAQAText = this.formatQAData(pageCYAQA);

        await test.info().attach('Collected Q&A during the journey', {
          body: collectedQAText,
          contentType: 'text/plain'
        });

        await test.info().attach('Extracted Q&A from CYA Page', {
          body: pageCYAQAText,
          contentType: 'text/plain'
        });
      });
    }

    const results = await test.step(`Validate ${collectedQA.length} collected Q&A pairs against CYA page`, async () => {
      return validateCYAData(collectedQA, pageCYAQA, true);
    });

    if (hasValidationErrors(results)) {
      throw new Error(buildCYAErrorMessage(results, 'Final'));
    }
  }

  private formatQAData(qaPairs: Array<{question?: string; answer?: string; step?: string}>): string {
    return JSON.stringify(qaPairs, null, 2);
  }
}
