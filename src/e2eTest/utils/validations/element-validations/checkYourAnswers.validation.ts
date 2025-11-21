import { Page } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaData } from '@utils/data/cya-data';
import { CYAValidationBase } from './cya-validation-base';

/**
 * Validation class for Final Check Your Answers (CYA) page
 * Validates that all questions and answers displayed on the Final CYA page match the data collected during the journey
 */
export class CheckYourAnswersValidation extends CYAValidationBase implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await this.validateQAPairs(page, cyaData.collectedQAPairs || [], 'Final CYA');
  }

  /**
   * Find question on Final CYA page
   */
  protected async findQuestionOnPage(page: Page, questionText: string): Promise<{
    found: boolean;
    question: string;
    answer: string;
  }> {
    // Final CYA uses table.form-table and table.complex-panel-table
    const tables = page.locator('table.form-table, table.complex-panel-table');
    const tableCount = await tables.count();

    for (let i = 0; i < tableCount; i++) {
      const table = tables.nth(i);
      const rows = table.locator('tr');
      const rowCount = await rows.count();

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        const questionCell = row.locator('th').first();
        // Get the answer cell - select the content cell, not the "Change" cell
        // The structure is: <th>Question</th> <td class="form-cell">Answer</td> <td class="change">Change</td>
        const answerCell = row.locator('td.form-cell, td.case-field-content').first();

        const question = await questionCell.textContent({ timeout: 1000 }).catch(() => null);
        if (!question) continue;

        // Check if question matches
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

    return { found: false, question: questionText, answer: '' };
  }

  /**
   * Extract answer from cell (handles various field types)
   * Excludes "Change" links and extracts from proper field components
   */
  private async extractAnswerFromCell(cell: any): Promise<string | null> {
    // For radio/checkbox fields - extract first (most common)
    const radioField = cell.locator('ccd-read-fixed-radio-list-field, ccd-read-multi-select-list-field');
    const radioCount = await radioField.count();
    if (radioCount > 0) {
      const radioText = await radioField.textContent({ timeout: 500 }).catch(() => null);
      if (radioText && radioText.trim()) return radioText.trim();
    }

    // For text fields
    const textField = cell.locator('ccd-read-text-field, ccd-read-text-area-field');
    const textCount = await textField.count();
    if (textCount > 0) {
      const textValue = await textField.textContent({ timeout: 500 }).catch(() => null);
      if (textValue && textValue.trim()) return textValue.trim();
    }

    // For complex fields (like address), extract from nested structure
    const complexField = cell.locator('ccd-read-complex-field-table table.complex-panel-table');
    const complexCount = await complexField.count();
    if (complexCount > 0) {
      const parts: string[] = [];
      const complexRows = complexField.locator('tr');
      const complexRowCount = await complexRows.count();

      for (let i = 0; i < complexRowCount; i++) {
        const row = complexRows.nth(i);
        const label = await row.locator('th').textContent({ timeout: 500 }).catch(() => null);
        const value = await row.locator('td').textContent({ timeout: 500 }).catch(() => null);
        if (label && value && value.trim()) {
          parts.push(`${label.trim()}: ${value.trim()}`);
        }
      }

      if (parts.length > 0) {
        return parts.join(', ');
      }
    }

    // For money fields
    const moneyField = cell.locator('ccd-read-money-gbp-field');
    const moneyCount = await moneyField.count();
    if (moneyCount > 0) {
      const moneyValue = await moneyField.textContent({ timeout: 500 }).catch(() => null);
      if (moneyValue && moneyValue.trim()) return moneyValue.trim();
    }

    // For date fields
    const dateField = cell.locator('ccd-read-date-field');
    const dateCount = await dateField.count();
    if (dateCount > 0) {
      const dateValue = await dateField.textContent({ timeout: 500 }).catch(() => null);
      if (dateValue && dateValue.trim()) return dateValue.trim();
    }

    // For document fields
    const docField = cell.locator('ccd-read-document-field');
    const docCount = await docField.count();
    if (docCount > 0) {
      const docLinks = docField.locator('a');
      const docLinkCount = await docLinks.count();
      const docNames: string[] = [];
      for (let i = 0; i < docLinkCount; i++) {
        const docName = await docLinks.nth(i).textContent({ timeout: 500 }).catch(() => null);
        if (docName && docName.trim()) docNames.push(docName.trim());
      }
      if (docNames.length > 0) return docNames.join(', ');
    }

    // For email fields
    const emailField = cell.locator('ccd-read-email-field');
    const emailCount = await emailField.count();
    if (emailCount > 0) {
      const emailValue = await emailField.textContent({ timeout: 500 }).catch(() => null);
      if (emailValue && emailValue.trim()) return emailValue.trim();
    }

    // Last resort: get text from span.text-16 (common pattern) but exclude links
    const textSpan = cell.locator('span.text-16:not(:has(a))').first();
    let answer = await textSpan.textContent({ timeout: 1000 }).catch(() => null);
    
    // If still no answer, try getting all text but exclude "Change" and links
    if (!answer || !answer.trim()) {
      // Get text but exclude any links (like "Change")
      const allText = await cell.locator('*:not(a):not(button)').allTextContents().catch(() => []);
      if (allText.length > 0) {
        answer = allText.join(' ').replace(/Change/gi, '').replace(/\s+/g, ' ').trim();
      }
    }

    return answer && answer.trim() ? answer.trim() : null;
  }
}
