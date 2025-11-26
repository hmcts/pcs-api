import { Page, test } from '@playwright/test';
import {IValidation, validationData} from '@utils/interfaces';
import { cyaAddressData } from '@utils/actions/custom-actions/collectCYAData.action';
import { extractSimpleQAFromPage } from '@utils/cya/cya-extraction-utils';
import { buildCYAErrorMessage, hasValidationErrors, validateCYAData } from '@utils/cya/cya-validation-util';

export class CheckYourAnswersAddressValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: validationData): Promise<void> {
    const collectedQA = cyaAddressData.collectedQAPairs || [];
    if (collectedQA.length === 0) {
      throw new Error('Address CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
    }

    const pageCYAQA = await test.step('Extract Q&A pairs from Address CYA page', async () => {
      return await extractSimpleQAFromPage(page);
    });

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

    const results = await test.step(`Validate ${collectedQA.length} collected Q&A pairs against Address CYA page`, async () => {
      return validateCYAData(collectedQA, pageCYAQA, false);
    });

    if (hasValidationErrors(results)) {
      throw new Error(buildCYAErrorMessage(results, 'Address'));
    }
  }

  private formatQAData(qaPairs: Array<{question?: string; answer?: string; step?: string}>): string {
    return JSON.stringify(qaPairs, null, 2);
  }
}
