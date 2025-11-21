import { Page } from '@playwright/test';
import { CollectedQAPair } from '@utils/data/cya-types';

/**
 * Base validation logic shared between Address and Final CYA validations
 */
export abstract class CYAValidationBase {
  /**
   * Common validation logic for both Address and Final CYA
   */
  protected async validateQAPairs(
    page: Page,
    qaPairs: CollectedQAPair[],
    errorPrefix: string
  ): Promise<void> {
    // Wait for page to load with timeout
    await Promise.race([
      page.waitForLoadState('networkidle'),
      page.waitForSelector('table.form-table, table.complex-panel-table', { timeout: 10000 })
    ]).catch(() => {});
    await page.waitForTimeout(300);

    if (!qaPairs || qaPairs.length === 0) {
      throw new Error(`${errorPrefix}: No CYA data collected. Make sure to collect Q&A pairs during the journey.`);
    }

    const errors: string[] = [];
    for (const qaPair of qaPairs) {
      if (!qaPair.question || !qaPair.answer) continue;

      const found = await Promise.race([
        this.findQuestionOnPage(page, qaPair.question),
        new Promise<{found: boolean; question: string; answer: string}>(resolve => 
          setTimeout(() => resolve({ found: false, question: qaPair.question || '', answer: '' }), 5000)
        )
      ]);

      if (!found.found) {
        errors.push(`Question not found: "${qaPair.question}"`);
        continue;
      }

      const collected = qaPair.answer.trim().toLowerCase();
      const pageAnswer = found.answer.trim().toLowerCase();

      if (collected !== pageAnswer && !pageAnswer.includes(collected) && !collected.includes(pageAnswer)) {
        errors.push(`Mismatch for "${found.question}": Expected "${qaPair.answer}", Found "${found.answer}"`);
      }
    }

    if (errors.length > 0) {
      throw new Error(`${errorPrefix} validation failed:\n${errors.join('\n')}`);
    }
  }

  /**
   * Find question on page - to be implemented by subclasses
   */
  protected abstract findQuestionOnPage(page: Page, questionText: string): Promise<{
    found: boolean;
    question: string;
    answer: string;
  }>;

  /**
   * Match question text (case-insensitive, partial match)
   */
  protected match(pageQuestion: string, collectedQuestion: string): boolean {
    const p = pageQuestion.toLowerCase().trim();
    const c = collectedQuestion.toLowerCase().trim();
    return p === c || p.includes(c) || c.includes(p);
  }
}

