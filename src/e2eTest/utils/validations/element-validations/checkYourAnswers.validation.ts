import { Page } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaData } from '@utils/data/cya-data';

export class CheckYourAnswersValidation implements IValidation {
  private static validationInProgress = false;

  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    if (CheckYourAnswersValidation.validationInProgress) {
      console.log(`⚠️  Validation already in progress, skipping duplicate call`);
      return;
    }

    CheckYourAnswersValidation.validationInProgress = true;
    try {
      await Promise.race([
        page.waitForLoadState('networkidle'),
        page.waitForSelector('table.form-table', { timeout: 10000 })
      ]).catch(() => {});
      await page.waitForTimeout(300);

      const collectedQA = cyaData.collectedQAPairs || [];
      if (collectedQA.length === 0) {
        throw new Error('Final CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
      }

      console.log(`\n${'='.repeat(80)}`);
      console.log(`🔍 [Final CYA] Starting Validation - ${collectedQA.length} collected Q&A pairs`);
      console.log(`${'='.repeat(80)}\n`);

      const pageQA = await this.extractAllQAFromPage(page);
      console.log(`📄 Questions on CYA Page: ${pageQA.length}`);
      console.log(`📋 Collected Q&A Pairs: ${collectedQA.length}\n`);

      const errors: string[] = [];

      // Check 1: All collected Q&A pairs appear on CYA page
      for (const collected of collectedQA) {
        if (!collected.question || !collected.answer) continue;

        // Ensure collected.question is string (not undefined) for .match parameter
        const found = pageQA.find(page => this.match(page.question, collected.question as string));
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
        console.log(`❌ [Final CYA] FAILED - ${errors.length} error(s)`);
        errors.forEach((err, i) => console.log(`  ${i + 1}. ${err}`));
        console.log(`${'='.repeat(80)}\n`);
        throw new Error(`Final CYA validation failed:\n${errors.join('\n')}`);
      } else {
        console.log(`✅ [Final CYA] PASSED - All ${collectedQA.length} pairs matched`);
        console.log(`${'='.repeat(80)}\n`);
      }
    } finally {
      CheckYourAnswersValidation.validationInProgress = false;
    }
  }

  /**
   * Extract all Q&A pairs from CYA table
   * Structure: tr -> th (question), td (answer), td (link)
   * Sometimes td has nested table with more Q&A pairs
   */
  private async extractAllQAFromPage(page: Page): Promise<Array<{question: string; answer: string}>> {
    const qaPairs: Array<{question: string; answer: string}> = [];
    const seenQuestions = new Set<string>();

    const tables = page.locator('table.form-table');
    const tableCount = await tables.count();

    for (let i = 0; i < tableCount; i++) {
      const table = tables.nth(i);
      const rows = table.locator('tr');
      const rowCount = await rows.count();

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        const questionCell = row.locator('th').first();
        const answerCell = row.locator('td.form-cell, td.case-field-content').first();

        const question = await questionCell.textContent({ timeout: 500 }).catch(() => null);
        const questionText = question?.trim() || '';

        // Check if answer cell has nested table
        const nestedTable = answerCell.locator('table.complex-panel-table');
        const hasNestedTable = await nestedTable.count() > 0;

        if (hasNestedTable) {
          // Extract Q&A pairs from nested table
          const nestedRows = nestedTable.locator('tr.complex-panel-simple-field');
          const nestedRowCount = await nestedRows.count();

          for (let k = 0; k < nestedRowCount; k++) {
            const nestedRow = nestedRows.nth(k);
            const nestedQuestionCell = nestedRow.locator('th#complex-panel-simple-field-label');
            const nestedAnswerCell = nestedRow.locator('td');

            const nestedQuestion = await nestedQuestionCell.textContent({ timeout: 500 }).catch(() => null);
            if (!nestedQuestion || !nestedQuestion.trim()) continue;

            const nestedQuestionText = nestedQuestion.trim();
            if (seenQuestions.has(nestedQuestionText)) continue;
            seenQuestions.add(nestedQuestionText);

            const nestedAnswer = await this.extractAnswer(nestedAnswerCell);
            qaPairs.push({
              question: nestedQuestionText,
              answer: nestedAnswer
            });
          }
        } else if (questionText) {
          // Simple Q&A pair: th (question) -> td (answer)
          if (seenQuestions.has(questionText)) continue;
          seenQuestions.add(questionText);

          const answer = await this.extractAnswer(answerCell);
          qaPairs.push({
            question: questionText,
            answer: answer
          });
        }
      }
    }

    return qaPairs;
  }

  /**
   * Extract answer from a cell
   */
  private async extractAnswer(cell: any): Promise<string> {
    // Try span.text-16 first (most common pattern)
    const text16 = cell.locator('span.text-16').first();
    const text16Count = await text16.count();
    if (text16Count > 0) {
      const text = await text16.textContent({ timeout: 1000 }).catch(() => null);
      if (text && text.trim() && !text.includes('Change')) {
        return text.trim();
      }
    }

    // Try field components
    const fieldComponents = [
      'ccd-read-fixed-radio-list-field span.text-16',
      'ccd-read-text-field span.text-16',
      'ccd-read-text-area-field span',
      'ccd-read-multi-select-list-field',
      'ccd-read-money-gbp-field',
      'ccd-read-date-field',
      'ccd-read-email-field'
    ];

    for (const selector of fieldComponents) {
      const field = cell.locator(selector).first();
      const count = await field.count();
      if (count > 0) {
        const text = await field.textContent({ timeout: 500 }).catch(() => null);
        if (text && text.trim() && !text.includes('Change')) {
          return text.trim();
        }
      }
    }

    // Fallback: get all text excluding links
    const allText = await cell.locator('*:not(a):not(button)').allTextContents().catch(() => []);
    if (allText.length > 0) {
      const combined = allText.join(' ').replace(/Change/gi, '').replace(/\s+/g, ' ').trim();
      if (combined) return combined;
    }

    return '';
  }

  /**
   * Match question text - handles minor whitespace/punctuation differences
   */
  private match(pageQuestion: string, collectedQuestion: string): boolean {
    const p = pageQuestion.trim();
    const c = collectedQuestion.trim();

    if (p === c) return true;

    const pClean = p.replace(/[.,!?;:()'"]/g, '').replace(/\s+/g, ' ').trim();
    const cClean = c.replace(/[.,!?;:()'"]/g, '').replace(/\s+/g, ' ').trim();

    if (pClean === cClean) return true;

    const pHasNumber = /\d+/.test(pClean);
    const cHasNumber = /\d+/.test(cClean);
    if (pHasNumber || cHasNumber) {
      const pNumber = pClean.match(/\d+/)?.[0];
      const cNumber = cClean.match(/\d+/)?.[0];
      if (pNumber && cNumber && pNumber !== cNumber) {
        return false;
      }
    }

    const stopWords = new Set(['a', 'an', 'the', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could', 'should', 'may', 'might', 'must', 'can', 'about', 'any', 'you', 'your', 'there', 'this', 'that', 'these', 'those']);
    const pWords = pClean.split(/\s+/).filter(w => w.length > 2 && !stopWords.has(w.toLowerCase()));
    const cWords = cClean.split(/\s+/).filter(w => w.length > 2 && !stopWords.has(w.toLowerCase()));

    const lengthDiff = Math.abs(pClean.length - cClean.length);
    const avgLength = (pClean.length + cClean.length) / 2;
    if (avgLength > 0 && lengthDiff / avgLength > 0.1) {
      return false;
    }

    if (Math.abs(pWords.length - cWords.length) > 1) {
      return false;
    }

    const commonWords = pWords.filter(w => cWords.includes(w));
    const uniqueWords = new Set([...pWords, ...cWords]);
    if (uniqueWords.size === 0) return false;

    const overlapRatio = commonWords.length / uniqueWords.size;
    const minWords = Math.min(pWords.length, cWords.length);
    const wordMatchRatio = minWords > 0 ? commonWords.length / minWords : 0;

    if (overlapRatio >= 0.95 && wordMatchRatio >= 0.95) {
      return true;
    }

    const pSignificant = pWords.join(' ');
    const cSignificant = cWords.join(' ');
    if (pSignificant === cSignificant) return true;
    if (pSignificant.includes(cSignificant) || cSignificant.includes(pSignificant)) {
      const sigLengthDiff = Math.abs(pSignificant.length - cSignificant.length);
      const sigAvgLength = (pSignificant.length + cSignificant.length) / 2;
      if (sigAvgLength > 0 && sigLengthDiff / sigAvgLength <= 0.05) return true;
    }

    return false;
  }
}
