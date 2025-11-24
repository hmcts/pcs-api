import { Page } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaAddressData } from '@utils/data/cya-address-data';

export class CheckYourAnswersAddressValidation implements IValidation {
  private static validationInProgress = false;

  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    if (CheckYourAnswersAddressValidation.validationInProgress) {
      console.log(`⚠️  Validation already in progress, skipping duplicate call`);
      return;
    }

    CheckYourAnswersAddressValidation.validationInProgress = true;
    try {
      await Promise.race([
        page.waitForLoadState('networkidle'),
        page.waitForSelector('table.form-table', { timeout: 10000 })
      ]).catch(() => {});
      await page.waitForTimeout(300);

      const collectedQA = cyaAddressData.collectedQAPairs || [];
      if (collectedQA.length === 0) {
        throw new Error('Address CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
      }

      console.log(`\n${'='.repeat(80)}`);
      console.log(`🔍 [Address CYA] Starting Validation - ${collectedQA.length} collected Q&A pairs`);
      console.log(`${'='.repeat(80)}\n`);

      const pageQA = await this.extractAllQAFromPage(page);
      console.log(`📄 Questions on CYA Page: ${pageQA.length}`);
      console.log(`📋 Collected Q&A Pairs: ${collectedQA.length}\n`);

      const errors: string[] = [];

      // Check 1: All collected Q&A pairs appear on CYA page
      for (const collected of collectedQA) {
        if (!collected.question || !collected.answer) continue;

        const found = pageQA.find(page => this.match(page.question, collected.question!));
        if (!found) {
          errors.push(`Question not found: "${collected.question}"`);
          continue;
        }

        if (found.answer.trim() !== collected.answer.trim()) {
          errors.push(`Mismatch for "${found.question}": Expected "${collected.answer}", Found "${found.answer}"`);
        }
      }

      // Check 2: All questions on CYA page were collected
      for (const page of pageQA) {
        const wasCollected = collectedQA.some(collected => {
          if (!collected.question) return false;
          return this.match(page.question, collected.question);
        });

        if (!wasCollected) {
          errors.push(`Question on CYA page not collected: "${page.question}"`);
        }
      }

      console.log(`${'='.repeat(80)}`);
      if (errors.length > 0) {
        console.log(`❌ [Address CYA] FAILED - ${errors.length} error(s)`);
        errors.forEach((err, i) => console.log(`  ${i + 1}. ${err}`));
        console.log(`${'='.repeat(80)}\n`);
        throw new Error(`Address CYA validation failed:\n${errors.join('\n')}`);
      } else {
        console.log(`✅ [Address CYA] PASSED - All ${collectedQA.length} pairs matched`);
        console.log(`${'='.repeat(80)}\n`);
      }
    } finally {
      CheckYourAnswersAddressValidation.validationInProgress = false;
    }
  }

  /**
   * Extract all Q&A pairs from Address CYA page
   * Simple structure: Address fields in nested table + sometimes a simple question
   */
  private async extractAllQAFromPage(page: Page): Promise<Array<{question: string; answer: string}>> {
    const qaPairs: Array<{question: string; answer: string}> = [];

    // Find the Property address nested table
    const complexTable = page.locator('table.complex-panel-table').first();
    const complexRowCount = await complexTable.locator('tr.complex-panel-simple-field').count();

    for (let i = 0; i < complexRowCount; i++) {
      const row = complexTable.locator('tr.complex-panel-simple-field').nth(i);
      const questionCell = row.locator('th#complex-panel-simple-field-label');
      const answerCell = row.locator('td');

      const question = await questionCell.textContent({ timeout: 500 }).catch(() => null);
      if (!question || !question.trim()) continue;

      // Simple extraction: get text from span.text-16 in the answer cell
      const answer = await answerCell.locator('span.text-16').first().textContent({ timeout: 500 }).catch(() => null);

      qaPairs.push({
        question: question.trim(),
        answer: (answer || '').trim()
      });
    }

    // Also check for simple questions outside the nested table (e.g., "Is the property located in England or Wales?")
    const mainTable = page.locator('table.form-table').first();
    const mainRows = mainTable.locator('tr');
    const mainRowCount = await mainRows.count();

    for (let i = 0; i < mainRowCount; i++) {
      const row = mainRows.nth(i);
      const questionCell = row.locator('th').first();
      const answerCell = row.locator('td.form-cell, td.case-field-content').first();

      const question = await questionCell.textContent({ timeout: 500 }).catch(() => null);
      if (!question || !question.trim()) continue;

      // Skip if this row contains a nested table (already handled above)
      const hasNestedTable = await answerCell.locator('table.complex-panel-table').count() > 0;
      if (hasNestedTable) continue;

      const answer = await answerCell.locator('span.text-16').first().textContent({ timeout: 500 }).catch(() => null);

      qaPairs.push({
        question: question.trim(),
        answer: (answer || '').trim()
      });
    }

    return qaPairs;
  }

  /**
   * Simple match for address fields - exact match or case-insensitive
   */
  private match(pageQuestion: string, collectedQuestion: string): boolean {
    const p = pageQuestion.trim();
    const c = collectedQuestion.trim();

    if (p === c) return true;

    // Case-insensitive match
    if (p.toLowerCase() === c.toLowerCase()) return true;

    // Handle minor punctuation/whitespace differences
    const pClean = p.replace(/[.,!?;:()'"]/g, '').replace(/\s+/g, ' ').trim();
    const cClean = c.replace(/[.,!?;:()'"]/g, '').replace(/\s+/g, ' ').trim();

    return pClean.toLowerCase() === cClean.toLowerCase();
  }
}
