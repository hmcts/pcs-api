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
   */
  protected async findQuestionOnPage(page: Page, questionText: string): Promise<{
    found: boolean;
    question: string;
    answer: string;
  }> {
    // Address CYA uses table.form-table and table.complex-panel-table
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
   * Extract answer from cell (handles complex fields like address)
   * Uses the same logic as formLabelValue validation for reliability
   */
  private async extractAnswerFromCell(cell: any): Promise<string | null> {
    // For complex fields (like address), use formLabelValue locator strategy
    // This matches the approach used in createCase.saveResume.spec.ts
    const complexField = cell.locator('ccd-read-complex-field-table table.complex-panel-table');
    const complexCount = await complexField.count();
    if (complexCount > 0) {
      const addressParts: { [key: string]: string } = {};
      
      // Use the same locator strategy as formLabelValue validation
      // Match the labels used in createCase.saveResume.spec.ts (propertyDetails)
      const addressFields = [
        { label: propertyDetails.buildingAndStreetLabel, key: 'building' },
        { label: propertyDetails.townOrCityLabel, key: 'town' },
        { label: propertyDetails.postcodeZipcodeLabel, key: 'postcode' },
        { label: 'Postcode', key: 'postcode' } // Fallback if Postcode/Zipcode doesn't match
      ];

      for (const field of addressFields) {
        // Skip if we already have this part (e.g., postcode might match both labels)
        if (addressParts[field.key]) continue;
        
        // Try the complex panel locator (same as formLabelValue)
        const valueLocator = cell.locator(`th#complex-panel-simple-field-label > span.text-16:has-text("${field.label}")`)
          .locator('xpath=../..')
          .locator('td span.text-16:not(:has(ccd-field-read-label))');
        
        const count = await valueLocator.count().catch(() => 0);
        if (count > 0) {
          const value = await valueLocator.textContent({ timeout: 500 }).catch(() => null);
          if (value && value.trim()) {
            addressParts[field.key] = value.trim();
          }
        }
      }

      // Build address string in the format: "Building, Town, Postcode"
      const parts: string[] = [];
      if (addressParts.building) parts.push(addressParts.building);
      if (addressParts.town) parts.push(addressParts.town);
      if (addressParts.postcode) parts.push(addressParts.postcode);
      
      if (parts.length > 0) {
        return parts.join(', ');
      }
    }

    // For simple text fields, use formLabelValue locator strategy
    const textField = cell.locator('ccd-read-text-field span.text-16').first();
    let answer = await textField.textContent({ timeout: 1000 }).catch(() => null);
    
    // If still no answer, try other text field locators
    if (!answer || !answer.trim()) {
      const altTextField = cell.locator('ccd-field-read span.text-16').first();
      answer = await altTextField.textContent({ timeout: 1000 }).catch(() => null);
    }

    return answer && answer.trim() ? answer.trim() : null;
  }
}

