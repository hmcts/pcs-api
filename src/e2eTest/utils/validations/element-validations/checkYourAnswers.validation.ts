import { Page, test } from '@playwright/test';
import { IValidation, validationData } from '@utils/interfaces';
import { cyaData, cyaAddressData } from '@utils/actions/custom-actions/collectCYAData.action';
import { extractCCDTable } from '@utils/cya/cya-extraction-utils';
import { buildCYAErrorMessage, hasValidationErrors, validateCYAData } from '@utils/cya/cya-validation-util';

/**
 * Validates "Check Your Answers" pages by comparing collected Q&A pairs
 * during the journey against what's displayed on the CYA page.
 * Supports both Final CYA and Address CYA validations.
 */
export class CheckYourAnswersValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: validationData): Promise<void> {
    const validationsMap = new Map<string, () => Promise<void>>([
      ['validateCheckYourAnswers', () => this.validateCYAPage(page, data, cyaData, 'Final', true)],
      ['validateCheckYourAnswersAddress', () => this.validateCYAPage(page, data, cyaAddressData, 'Address', false)]
    ]);

    const validationToPerform = validationsMap.get(validation);
    if (!validationToPerform) {
      throw new Error(`No validation found for '${validation}'. Available: ${Array.from(validationsMap.keys()).join(', ')}`);
    }

    await validationToPerform();
  }

  private async validateCYAPage(
    page: Page,
    data: validationData | undefined,
    dataStore: typeof cyaData | typeof cyaAddressData,
    pageType: 'Final' | 'Address',
    useWhitespaceNormalization: boolean
  ): Promise<void> {
    const collectedQA = dataStore.collectedQAPairs;
    const errorPrefix = `${pageType} CYA`;
    const stepDescription = pageType === 'Final' ? 'CYA page' : 'Address CYA page';
    
    if (collectedQA.length === 0) {
      throw new Error(`${errorPrefix}: No CYA data collected. Make sure to collect Q&A pairs during the journey.`);
    }

    const pageCYAQA = await test.step(`Extract Q&A pairs from ${stepDescription}`, async () => {
      return await extractCCDTable(page, 'table.form-table');
    });

    // Attach Q&A data to Allure report for debugging (when data parameter is provided)
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

    // Validate collected Q&A against page Q&A
    const results = await test.step(
      `Validate ${collectedQA.length} collected Q&A pairs against ${stepDescription}`,
      async () => {
        return validateCYAData(collectedQA, pageCYAQA, useWhitespaceNormalization);
      }
    );

    if (hasValidationErrors(results)) {
      throw new Error(buildCYAErrorMessage(results, pageType));
    }
  }

  private formatQAData(qaPairs: Array<{question?: string; answer?: string; step?: string}>): string {
    return JSON.stringify(qaPairs, null, 2);
  }
}
