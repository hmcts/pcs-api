/**
 * Shared validation helpers for CYA (Check Your Answers) validation
 * Contains common validation logic used by both Address and Final CYA validations
 */

import { CollectedQAPair, QAPair } from './cya-validation-utils';
import { normalizeWhitespace } from './cya-validation-utils';

export interface ValidationResults {
  missingInCollected: Array<{question: string; answer: string}>;
  missingOnPage: Array<{question: string; answer: string}>;
  answerMismatches: Array<{question: string; expected: string; found: string}>;
}

/**
 * Shared validation logic for comparing collected Q&A pairs with page Q&A pairs
 * Returns validation results that can be used to build error messages
 */
export function validateCYAData(
  collectedQA: CollectedQAPair[],
  pageQA: QAPair[],
  useWhitespaceNormalization: boolean = false
): ValidationResults {
  const missingInCollected: Array<{question: string; answer: string}> = [];
  const missingOnPage: Array<{question: string; answer: string}> = [];
  const answerMismatches: Array<{question: string; expected: string; found: string}> = [];

  // Helper to normalize question if needed
  const normalizeQuestion = (q: string): string => 
    useWhitespaceNormalization ? normalizeWhitespace(q) : q.trim();

  // Validate all collected answers appear on CYA page
  for (const collected of collectedQA) {
    if (!collected.question || !collected.answer) continue;

    const collectedQuestion = normalizeQuestion(collected.question);
    const collectedAnswer = collected.answer.trim();

    // Find exact match
    const found = pageQA.find(p => {
      const pageQuestion = normalizeQuestion(p.question);
      const pageAnswer = p.answer.trim();
      return pageQuestion === collectedQuestion && pageAnswer === collectedAnswer;
    });

    if (!found) {
      // Check if question exists but answer is different
      const questionFound = pageQA.find(p => 
        normalizeQuestion(p.question) === collectedQuestion
      );

      if (questionFound) {
        answerMismatches.push({
          question: questionFound.question,
          expected: collectedAnswer,
          found: questionFound.answer.trim()
        });
      } else {
        missingOnPage.push({
          question: collectedQuestion,
          answer: collectedAnswer
        });
      }
    }
  }

  // Validate all questions on CYA page were collected
  for (const pageItem of pageQA) {
    const pageQuestion = normalizeQuestion(pageItem.question);
    const wasCollected = collectedQA.some(c => {
      if (!c.question) return false;
      return normalizeQuestion(c.question) === pageQuestion;
    });
    
    if (!wasCollected) {
      missingInCollected.push({
        question: pageItem.question.trim(),
        answer: pageItem.answer.trim()
      });
    }
  }

  return { missingOnPage, missingInCollected, answerMismatches };
}

/**
 * Build comprehensive error message for Allure reports
 */
export function buildCYAErrorMessage(
  results: ValidationResults,
  pageType: 'Final' | 'Address'
): string {
  const { missingOnPage, missingInCollected, answerMismatches } = results;
  const errorParts: string[] = [];
  
  if (missingOnPage.length > 0) {
    errorParts.push(`\n❌ QUESTIONS COLLECTED BUT MISSING ON ${pageType.toUpperCase()} CYA PAGE (${missingOnPage.length}):`);
    missingOnPage.forEach((item, index) => {
      errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
      errorParts.push(`     Expected Answer: "${item.answer}"`);
    });
  }
  
  if (missingInCollected.length > 0) {
    errorParts.push(`\n⚠️  QUESTIONS ON ${pageType.toUpperCase()} CYA PAGE BUT NOT COLLECTED (${missingInCollected.length}):`);
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
  
  return `${pageType} CYA validation failed:${errorParts.join('\n')}`;
}

