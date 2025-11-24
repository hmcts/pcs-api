import { Page, test } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaAddressData } from '@utils/cya/cya-field-collector';
import { extractSimpleQAFromPage, isAddressField } from '@utils/cya/cya-validation-utils';

export class CheckYourAnswersAddressValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
      const collectedQA = cyaAddressData.collectedQAPairs || [];
      if (collectedQA.length === 0) {
        throw new Error('Address CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
      }

    const pageQA = await test.step('Extract Q&A pairs from Address CYA page', async () => {
      return await this.extractAllQAFromPage(page);
    });
    
    // Debug logging
    console.log('🏠 [Address CYA] Collected Q&A pairs:', collectedQA.map(q => `${q.question}: ${q.answer}`));
    console.log('🏠 [Address CYA] Page Q&A pairs:', pageQA.map(q => `${q.question}: ${q.answer}`));

    // Categorize errors for better reporting
    const missingInCollected: Array<{question: string; answer: string}> = [];
    const missingOnPage: Array<{question: string; answer: string}> = [];
    const answerMismatches: Array<{question: string; expected: string; found: string}> = [];

    // Separate address fields from other questions
    const addressFields = collectedQA.filter(q => q.question && isAddressField(q.question!));
    const otherQuestions = collectedQA.filter(q => q.question && !isAddressField(q.question!));

    // For address fields, match by exact question and answer
    for (const addressField of addressFields) {
      if (!addressField.question || !addressField.answer) continue;
      
      const collectedQuestion = addressField.question.trim();
      const collectedAnswer = addressField.answer.trim();
      
      // Find exact question match (case-sensitive for question, exact for answer)
      const found = pageQA.find(p => 
        p.question.trim() === collectedQuestion &&
        p.answer.trim() === collectedAnswer
      );
      
      if (!found) {
        // Check if question exists but answer is different
        const questionFound = pageQA.find(p => p.question.trim() === collectedQuestion);
        
        if (questionFound) {
          // Question found but answer doesn't match
          answerMismatches.push({
            question: questionFound.question,
            expected: collectedAnswer,
            found: typeof questionFound.answer === 'string' ? questionFound.answer.trim() : String(questionFound.answer).trim()
          });
        } else {
          // Question not found at all
          console.log(`🏠 [Address CYA] Address field not found: "${collectedQuestion}" = "${collectedAnswer}"`);
          missingOnPage.push({
            question: collectedQuestion,
            answer: collectedAnswer
          });
        }
      }
    }

    // For non-address questions, exact match only
    await test.step(`Validate ${otherQuestions.length} non-address questions against CYA page`, async () => {
      for (const collected of otherQuestions) {
        if (!collected.question || !collected.answer) continue;

        const collectedQuestion = collected.question.trim();
        const collectedAnswer = collected.answer.trim();
        
        // Exact match: question (case-sensitive) and answer (exact)
        const found = pageQA.find(p => 
          p.question.trim() === collectedQuestion &&
          p.answer.trim() === collectedAnswer
        );
        
        if (!found) {
          // Check if question exists but answer is different
          const questionFound = pageQA.find(p => p.question.trim() === collectedQuestion);
          
          if (questionFound) {
            // Question found but answer doesn't match
            answerMismatches.push({
              question: questionFound.question,
              expected: collectedAnswer,
              found: typeof questionFound.answer === 'string' ? questionFound.answer.trim() : String(questionFound.answer).trim()
            });
          } else {
            // Question not found at all
            console.log(`🏠 [Address CYA] Question not found: "${collectedQuestion}"`);
            missingOnPage.push({
              question: collectedQuestion,
              answer: collectedAnswer
            });
          }
        }
      }
    });

    // Validate all questions on CYA page were collected (case-sensitive exact match)
    await test.step('Check for questions on Address CYA page that were not collected', async () => {
      for (const pageItem of pageQA) {
        const pageQuestion = pageItem.question.trim();
        const wasCollected = collectedQA.some(c => 
          c.question && c.question.trim() === pageQuestion
        );
        if (!wasCollected) {
          missingInCollected.push({
            question: pageQuestion,
            answer: typeof pageItem.answer === 'string' ? pageItem.answer.trim() : String(pageItem.answer).trim()
          });
        }
      }
    });

    // Build comprehensive error message for Allure reports
    if (missingOnPage.length > 0 || missingInCollected.length > 0 || answerMismatches.length > 0) {
      const errorParts: string[] = [];
      
      if (missingOnPage.length > 0) {
        errorParts.push(`\n❌ QUESTIONS COLLECTED BUT MISSING ON ADDRESS CYA PAGE (${missingOnPage.length}):`);
        missingOnPage.forEach((item, index) => {
          errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
          errorParts.push(`     Expected Answer: "${item.answer}"`);
        });
      }
      
      if (missingInCollected.length > 0) {
        errorParts.push(`\n⚠️  QUESTIONS ON ADDRESS CYA PAGE BUT NOT COLLECTED (${missingInCollected.length}):`);
        missingInCollected.forEach((item, index) => {
          errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
          errorParts.push(`     Answer on Page: "${item.answer}"`);
        });
      }
      
      if (answerMismatches.length > 0) {
        errorParts.push(`\n🔴 ANSWER MISMATCHES (${answerMismatches.length}):`);
        answerMismatches.forEach((item, index) => {
          errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
          errorParts.push(`     Expected: "${item.expected}"`);
          errorParts.push(`     Found: "${item.found}"`);
        });
      }
      
      const errorMessage = `Address CYA validation failed:${errorParts.join('\n')}`;
      throw new Error(errorMessage);
    }
  }

  /**
   * Extract all Q&A pairs from Address CYA page
   * Simple structure: address fields in nested complex-panel-table + optional country question
   */
  private async extractAllQAFromPage(page: Page): Promise<Array<{question: string; answer: string}>> {
    return await test.step('Extract all Q&A pairs from Address CYA page', async () => {
      return await extractSimpleQAFromPage(page);
    });
  }

}
