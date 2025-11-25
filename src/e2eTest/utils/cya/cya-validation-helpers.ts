/**
 * Shared validation helpers for CYA (Check Your Answers) validation
 * Contains common validation logic used by both Address and Final CYA validations
 */

import { QAPair, normalizeWhitespace } from './cya-validation-utils';
import { CollectedQAPair } from '@utils/actions/custom-actions/collectCYAData.action';

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

  const normalizeQuestion = (q: string): string => 
    useWhitespaceNormalization ? normalizeWhitespace(q) : q.trim();

  for (const collected of collectedQA) {
    if (!collected.question || !collected.answer) continue;

    const collectedQuestion = normalizeQuestion(collected.question);
    const collectedAnswer = collected.answer.trim();

    const found = pageQA.find(p => {
      const pageQuestion = normalizeQuestion(p.question);
      const pageAnswer = p.answer.trim();
      return pageQuestion === collectedQuestion && pageAnswer === collectedAnswer;
    });

    if (!found) {
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
        missingOnPage.push({ question: collectedQuestion, answer: collectedAnswer });
      }
    }
  }

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

export function hasValidationErrors(results: ValidationResults): boolean {
  return results.missingOnPage.length > 0 || 
         results.missingInCollected.length > 0 || 
         results.answerMismatches.length > 0;
}

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

