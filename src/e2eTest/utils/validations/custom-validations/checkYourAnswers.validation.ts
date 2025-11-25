import { Page, test } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaData } from '@utils/actions/custom-actions/collectCYAData.action';
import { extractCCDTable, normalizeWhitespace } from '@utils/cya/cya-validation-utils';
import { buildCYAErrorMessage, ValidationResults } from '@utils/cya/cya-validation-helpers';

export class CheckYourAnswersValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    const collectedQA = cyaData.collectedQAPairs || [];
    if (collectedQA.length === 0) {
      throw new Error('Final CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
    }

    const pageQA = await test.step('Extract Q&A pairs from CYA page', async () => {
      return await this.extractAllQAFromPage(page);
    });

    // Categorize errors for better reporting
    const missingInCollected: Array<{question: string; answer: string}> = [];
    const missingOnPage: Array<{question: string; answer: string}> = [];
    const answerMismatches: Array<{question: string; expected: string; found: string}> = [];

    // Validate all collected answers appear on CYA page - EXACT MATCHING ONLY
    await test.step(`Validate ${collectedQA.length} collected Q&A pairs against CYA page`, async () => {
      for (const collected of collectedQA) {
        if (!collected.question || !collected.answer) continue;

        const collectedQuestion = normalizeWhitespace(collected.question || '');
        const collectedAnswer = (collected.answer || '').trim();

        // Exact match: question (case-sensitive, whitespace-normalized) and answer (exact)
        const found = pageQA.find(p => {
          const pageQuestion = normalizeWhitespace(p.question);
          const pageAnswer = p.answer.trim();
          return pageQuestion === collectedQuestion && pageAnswer === collectedAnswer;
        });

        if (!found) {
          // Check if question exists but answer is different (with whitespace normalization)
          const questionFound = pageQA.find(p => {
            const pageQuestion = normalizeWhitespace(p.question);
            return pageQuestion === collectedQuestion;
          });

          if (questionFound) {
            // Question found but answer doesn't match exactly
            answerMismatches.push({
              question: questionFound.question,
              expected: collectedAnswer,
              found: questionFound.answer.trim()
            });
          } else {
            // Question not found at all
            missingOnPage.push({
              question: collectedQuestion,
              answer: collectedAnswer
            });
          }
        }
      }
    });

    // Validate all questions on CYA page were collected (case-sensitive, whitespace-normalized)
    await test.step('Check for questions on CYA page that were not collected', async () => {
      for (const pageItem of pageQA) {
        const pageQuestion = normalizeWhitespace(pageItem.question);
        const wasCollected = collectedQA.some(c => {
          if (!c.question) return false;
          const collectedQuestion = normalizeWhitespace(c.question);
          return collectedQuestion === pageQuestion;
        });
        if (!wasCollected) {
          // Question on page but not collected during journey
          missingInCollected.push({
            question: pageItem.question.trim(),
            answer: pageItem.answer.trim()
          });
        }
      }
    });

    // Build comprehensive error message for Allure reports
    if (missingOnPage.length > 0 || missingInCollected.length > 0 || answerMismatches.length > 0) {
      const results: ValidationResults = { missingOnPage, missingInCollected, answerMismatches };
      throw new Error(buildCYAErrorMessage(results, 'Final'));
    }
  }

  private async extractAllQAFromPage(page: Page): Promise<Array<{question: string; answer: string}>> {
    return await extractCCDTable(page, 'table.form-table');
  }

}
