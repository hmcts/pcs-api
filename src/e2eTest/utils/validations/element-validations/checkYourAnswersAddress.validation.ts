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
  /**
   * Find question on Address CYA page
   * Handles both simple questions and complex field sub-questions (like Building and Street within Property address)
   */
  protected async findQuestionOnPage(page: Page, questionText: string): Promise<{
    found: boolean;
    question: string;
    answer: string;
  }> {
    // First, try to find as a simple question in the main table
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

        // Check if this is a complex field (Property address)
        const complexField = answerCell.locator('ccd-read-complex-field-table table.complex-panel-table');
        const hasComplexField = await complexField.count() > 0;

        if (hasComplexField && this.match(question, 'Property address')) {
          // This is the Property address complex field - look for the sub-question within it
          const complexRows = complexField.locator('tr.complex-panel-simple-field');
          const complexRowCount = await complexRows.count();

          for (let k = 0; k < complexRowCount; k++) {
            const complexRow = complexRows.nth(k);
            const complexQuestionCell = complexRow.locator('th');
            const complexAnswerCell = complexRow.locator('td');

            const complexQuestion = await complexQuestionCell.textContent({ timeout: 500 }).catch(() => null);
            if (!complexQuestion) continue;

            const complexQuestionTrimmed = complexQuestion.trim();
            const questionTextTrimmed = questionText.trim();

            // Use exact match first for better precision (especially for "Address Line 2" vs "Address Line 3")
            const exactMatch = complexQuestionTrimmed.toLowerCase() === questionTextTrimmed.toLowerCase();

            // For questions with numbers, require exact match to avoid confusion
            const hasNumber = /\d+/.test(complexQuestionTrimmed) || /\d+/.test(questionTextTrimmed);
            if (hasNumber && !exactMatch) {
              continue; // Skip if numbers don't match exactly
            }

            // Only use fuzzy match if no numbers involved
            const fuzzyMatch = !hasNumber && !exactMatch && this.match(complexQuestionTrimmed, questionTextTrimmed);

            if (exactMatch || fuzzyMatch) {
              // Extract answer from the complex field sub-row using formLabelValue pattern
              const valueLocator = complexRow.locator(`th#complex-panel-simple-field-label > span.text-16:has-text("${complexQuestionTrimmed}")`)
                .locator('xpath=../..')
                .locator('td span.text-16:not(:has(ccd-field-read-label))');

              let answer = await valueLocator.textContent({ timeout: 500 }).catch(() => null);
              if (!answer || !answer.trim()) {
                // Fallback: get text directly from the td
                answer = await complexAnswerCell.textContent({ timeout: 500 }).catch(() => null);
              }

              if (answer && answer.trim()) {
                return {
                  found: true,
                  question: complexQuestionTrimmed,
                  answer: answer.trim()
                };
              }
            }
          }
        } else if (!hasComplexField) {
          // Simple question - check if it matches
          if (this.match(question, questionText)) {
            const answer = await this.extractAnswerFromCell(answerCell);
            return {
              found: true,
              question: question.trim(),
              answer: answer || ''
            };
          }
        }
      }
    }

    return { found: false, question: questionText, answer: '' };
  }

  /**
   * Extract answer from cell (handles complex fields like address)
   * Uses the same logic as formLabelValue validation for reliability
   */
  private async extractAnswerFromCell(cell: any): Promise<string | null> {
    // For complex fields (like address), extract individual field values
    // This matches the structure on Address CYA where each field is a separate row
    const complexField = cell.locator('ccd-read-complex-field-table table.complex-panel-table');
    const complexCount = await complexField.count();
    if (complexCount > 0) {
      // When question is a specific field label (e.g., "Building and Street"),
      // extract just that field's value from the complex structure
      // This will be handled by the question matching in findQuestionOnPage
      // For now, return null to let it fall through to simple field extraction
    }

    // For simple text fields, use formLabelValue locator strategy
    // This works for both simple fields and individual fields within complex structures
    const textField = cell.locator('ccd-read-text-field span.text-16').first();
    let answer = await textField.textContent({ timeout: 1000 }).catch(() => null);

    // If still no answer, try other text field locators
    if (!answer || !answer.trim()) {
      const altTextField = cell.locator('ccd-field-read span.text-16').first();
      answer = await altTextField.textContent({ timeout: 1000 }).catch(() => null);
    }

    // For complex field sub-fields, try the formLabelValue locator pattern
    if (!answer || !answer.trim()) {
      // Try extracting from complex panel structure using the question text
      // This will be handled in findQuestionOnPage when it matches the question
    }

    return answer && answer.trim() ? answer.trim() : null;
  }
}

