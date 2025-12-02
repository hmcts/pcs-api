// validation/cyaPage.validation.ts
import { Page } from '@playwright/test';
import { IValidation } from '../../interfaces/validation.interface';

interface CYAPair {
  question: string;
  answer: string | string[];
}

class CYAStore {
  private static instance: CYAStore;
  private answers: CYAPair[] = [];

  private constructor() {}

  static getInstance(): CYAStore {
    if (!CYAStore.instance) {
      CYAStore.instance = new CYAStore();
    }
    return CYAStore.instance;
  }

  captureAnswer(action: string, fieldName: any, value?: any): void {
    let cyaPair: CYAPair | null = null;

    switch (action) {
      case 'clickRadioButton':
        if (typeof fieldName === 'object' && fieldName.question && fieldName.option) {
          cyaPair = { question: fieldName.question, answer: fieldName.option };
        }
        break;

      case 'inputText':
        if (typeof fieldName === 'object' && fieldName.textLabel && typeof value === 'string') {
          cyaPair = { question: fieldName.textLabel, answer: value };
        } else if (typeof fieldName === 'string' && typeof value === 'string') {
          cyaPair = { question: fieldName, answer: value };
        }
        break;

      case 'check':
        if (Array.isArray(fieldName)) {
          cyaPair = { question: 'Selected options', answer: fieldName };
        } else if (typeof fieldName === 'string') {
          cyaPair = { question: fieldName, answer: 'Checked' };
        } else if (typeof fieldName === 'object' && fieldName.question) {
          cyaPair = {
            question: fieldName.question,
            answer: fieldName.options || fieldName.option || 'Checked'
          };
        }
        break;

      case 'select':
        if (typeof fieldName === 'string' && typeof value === 'string') {
          cyaPair = { question: fieldName, answer: value };
        }
        break;

      case 'uploadFile':
        if (typeof fieldName === 'string') {
          cyaPair = { question: 'Uploaded file', answer: fieldName };
        } else if (Array.isArray(fieldName)) {
          cyaPair = { question: 'Uploaded files', answer: fieldName };
        }
        break;
    }

    if (cyaPair) {
      this.answers.push(cyaPair);
    }
  }

  getAnswers(): CYAPair[] {
    return [...this.answers];
  }

  clearAnswers(): void {
    this.answers = [];
  }

  getAnswerCount(): number {
    return this.answers.length;
  }
}

export class CYAPageValidation implements IValidation {
  private store = CYAStore.getInstance();

  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await this.validateCYAPage(page);
  }

  async validateCYAPage(page: Page): Promise<void> {
    const answers = this.store.getAnswers();

    if (answers.length === 0) {
      console.log('⚠️ No answers captured for CYA validation');
      return;
    }

    let allPassed = true;
    const failedAnswers: Array<{question: string; expected: string | string[]; actual?: string}> = [];

    for (const answer of answers) {
      const found = await this.verifyAnswerOnPage(page, answer.question, answer.answer);

      if (!found.found) {
        allPassed = false;
        failedAnswers.push({
          question: answer.question,
          expected: answer.answer,
          actual: found.actualValue
        });
      }
    }

    if (!allPassed) {
      console.log(`\n❌ CYA VALIDATION FAILED: ${failedAnswers.length}/${answers.length} answers not found`);
      failedAnswers.forEach((failed, index) => {
        const expectedText = Array.isArray(failed.expected)
          ? `[${failed.expected.join(', ')}]`
          : failed.expected;
        console.log(`   ${index + 1}. "${failed.question}"`);
        console.log(`      Expected: ${expectedText}`);
        if (failed.actual) {
          console.log(`      Found on page: ${failed.actual}`);
        }
      });
      throw new Error(`CYA validation failed. ${failedAnswers.length} answers not found on page.`);
    }
  }

  private async verifyAnswerOnPage(page: Page, question: string, answer: string | string[]): Promise<{
    found: boolean;
    actualValue?: string;
  }> {
    try {
      // Find the question row
      const questionSelector = `th.case-field-label span.text-16:has-text("${question}")`;
      const questionLocator = page.locator(questionSelector).first();

      let questionFound = false;
      let actualQuestionLocator = questionLocator;

      if (!(await questionLocator.isVisible({ timeout: 3000 }))) {
        const altSelector = `th.case-field-label:has-text("${question}")`;
        const altQuestionLocator = page.locator(altSelector).first();

        if (await altQuestionLocator.isVisible({ timeout: 2000 })) {
          questionFound = true;
          actualQuestionLocator = altQuestionLocator;
        }
      } else {
        questionFound = true;
      }

      if (!questionFound) {
        return { found: false };
      }

      const row = actualQuestionLocator.locator('xpath=..');

      if (!(await row.isVisible({ timeout: 2000 }))) {
        return { found: false };
      }

      const answerCell = row.locator('td.case-field-content').first();

      if (!(await answerCell.isVisible({ timeout: 2000 }))) {
        return { found: false };
      }

      const answerText = await answerCell.textContent();
      const normalizedAnswerText = answerText?.trim() || '';

      let found = false;

      if (Array.isArray(answer)) {
        found = answer.every(ans => normalizedAnswerText.includes(ans));
      } else {
        found = normalizedAnswerText.includes(answer);

        if (!found) {
          const answerSpans = answerCell.locator('span.text-16');
          const spanCount = await answerSpans.count();

          for (let i = 0; i < spanCount; i++) {
            const spanText = await answerSpans.nth(i).textContent();
            if (spanText?.includes(answer)) {
              found = true;
              break;
            }
          }
        }
      }

      return {
        found,
        actualValue: normalizedAnswerText.substring(0, 100) + (normalizedAnswerText.length > 100 ? '...' : '')
      };

    } catch (error) {
      return { found: false };
    }
  }
}

// Export for easy access
export const cyaStore = CYAStore.getInstance();
export const cyaValidation = new CYAPageValidation();
