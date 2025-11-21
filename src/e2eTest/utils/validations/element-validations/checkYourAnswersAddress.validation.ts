import { Page } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaAddressData } from '@utils/data/cya-address-data';
import { CYAValidationBase } from './cya-validation-base';
import { propertyDetails } from '@data/page-data';

/**
 * Validation class for Address Check Your Answers (CYA) page
 * Validates that all questions and answers displayed on the Address CYA page match the data collected during the address journey
 */
export class CheckYourAnswersAddressValidation extends CYAValidationBase implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await this.validateQAPairs(page, cyaAddressData.collectedQAPairs || [], 'Address CYA');
  }
  protected async findQuestionOnPage(page: Page, questionText: string): Promise<{
    found: boolean;
    question: string;
    answer: string;
  }> {
    const tables = page.locator('table.form-table, table.complex-panel-table');
    const tableCount = await tables.count();

    for (let i = 0; i < tableCount; i++) {
      const table = tables.nth(i);
      const rows = table.locator('tr');
      const rowCount = await rows.count();

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        const questionCell = row.locator('th').first();
        const answerCell = row.locator('td.form-cell, td.case-field-content').first();
        const question = await questionCell.textContent({ timeout: 1000 }).catch(() => null);
        if (!question) continue;

        const complexField = answerCell.locator('ccd-read-complex-field-table table.complex-panel-table');
        const hasComplexField = await complexField.count() > 0;

        if (hasComplexField && this.match(question, 'Property address')) {
          const complexRows = complexField.locator('tr.complex-panel-simple-field');
          const complexRowCount = await complexRows.count();

          for (let k = 0; k < complexRowCount; k++) {
            const complexRow = complexRows.nth(k);
            const complexQuestion = await complexRow.locator('th').textContent({ timeout: 500 }).catch(() => null);
            if (!complexQuestion) continue;

            const complexQuestionTrimmed = complexQuestion.trim();
            const questionTextTrimmed = questionText.trim();
            const exactMatch = complexQuestionTrimmed.toLowerCase() === questionTextTrimmed.toLowerCase();
            const hasNumber = /\d+/.test(complexQuestionTrimmed) || /\d+/.test(questionTextTrimmed);

            if (hasNumber && !exactMatch) continue;
            if (!exactMatch && !this.match(complexQuestionTrimmed, questionTextTrimmed)) continue;

            const valueLocator = complexRow.locator(`th#complex-panel-simple-field-label > span.text-16:has-text("${complexQuestionTrimmed}")`)
              .locator('xpath=../..')
              .locator('td span.text-16:not(:has(ccd-field-read-label))');

            let answer = await valueLocator.textContent({ timeout: 500 }).catch(() => null);
            if (!answer?.trim()) {
              answer = await complexRow.locator('td').textContent({ timeout: 500 }).catch(() => null);
            }

            if (answer?.trim()) {
              return { found: true, question: complexQuestionTrimmed, answer: answer.trim() };
            }
          }
        } else if (!hasComplexField && this.match(question, questionText)) {
          const answer = await this.extractAnswerFromCell(answerCell);
          return { found: true, question: question.trim(), answer: answer || '' };
        }
      }
    }

    return { found: false, question: questionText, answer: '' };
  }

  /**
   * Extract answer from cell for simple fields
   * Complex fields are handled in findQuestionOnPage
   */
  private async extractAnswerFromCell(cell: any): Promise<string | null> {
    const textField = cell.locator('ccd-read-text-field span.text-16, ccd-field-read span.text-16').first();
    const answer = await textField.textContent({ timeout: 1000 }).catch(() => null);
    return answer?.trim() || null;
  }
}

