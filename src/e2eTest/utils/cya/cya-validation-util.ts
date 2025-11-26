import { QAPair, normalizeWhitespace } from './cya-extraction-utils';
import { CollectedQAPair } from '@utils/actions/custom-actions/collectCYAData.action';

export interface ValidationResults {
  missingInCollected: Array<{question: string; answer: string}>;
  missingOnCYAPage: Array<{question: string; answer: string}>;
  answerMismatches: Array<{question: string; expected: string; found: string}>;
}

export function validateCYAData(
  collectedQA: CollectedQAPair[],
  pageCYAQA: QAPair[],
  useWhitespaceNormalization: boolean = false
): ValidationResults {
  const missingInCollected: Array<{question: string; answer: string}> = [];
  const missingOnCYAPage: Array<{question: string; answer: string}> = [];
  const answerMismatches: Array<{question: string; expected: string; found: string}> = [];

  const normalizeQuestion = (q: string): string => {
    if (!q) return '';
    // Preserve space as space for matching (don't trim single space)
    if (q === ' ') return ' ';
    return useWhitespaceNormalization ? normalizeWhitespace(q) : q.trim();
  };

  for (const collected of collectedQA) {
    if (!collected.question || !collected.answer) continue;

    const collectedQuestion = normalizeQuestion(collected.question);
    const collectedAnswer = collected.answer.trim();

    const found = pageCYAQA.find(p => {
      const pageQuestion = normalizeQuestion(p.question);
      const pageAnswer = p.answer.trim();
      return pageQuestion === collectedQuestion && pageAnswer === collectedAnswer;
    });

    if (!found) {
      const questionFound = pageCYAQA.find(p =>
        normalizeQuestion(p.question) === collectedQuestion
      );

      if (questionFound) {
        answerMismatches.push({
          question: questionFound.question,
          expected: collectedAnswer,
          found: questionFound.answer.trim()
        });
      } else {
        missingOnCYAPage.push({ question: collectedQuestion, answer: collectedAnswer });
      }
    }
  }

  for (const pageItem of pageCYAQA) {
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

  return { missingOnCYAPage, missingInCollected, answerMismatches };
}

export function hasValidationErrors(results: ValidationResults): boolean {
  return results.missingOnCYAPage.length > 0 ||
         results.missingInCollected.length > 0 ||
         results.answerMismatches.length > 0;
}

export function buildCYAErrorMessage(
  results: ValidationResults,
  pageType: 'Final' | 'Address'
): string {
  const { missingOnCYAPage, missingInCollected, answerMismatches } = results;
  const errorParts: string[] = [];

  if (missingOnCYAPage.length > 0) {
    errorParts.push(`QUESTIONS COLLECTED BUT MISSING ON ${pageType.toUpperCase()} CYA PAGE (${missingOnCYAPage.length}):`);
    missingOnCYAPage.forEach((item, index) => {
      errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
      errorParts.push(`     Expected Answer: "${item.answer}"`);
    });
  }

  if (missingInCollected.length > 0) {
    errorParts.push(`QUESTIONS ON ${pageType.toUpperCase()} CYA PAGE BUT NOT COLLECTED (${missingInCollected.length}):`);
    missingInCollected.forEach((item, index) => {
      errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
      errorParts.push(`     Answer on Page: "${item.answer}"`);
    });
  }

  if (answerMismatches.length > 0) {
    errorParts.push(`ANSWER MISMATCHES (${answerMismatches.length}):`);
    answerMismatches.forEach((item, index) => {
      errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
      errorParts.push(`     Expected: "${item.expected}"`);
      errorParts.push(`     Found: "${item.found}"`);
    });
  }

  return `${pageType} CYA validation failed:${errorParts.join('\n')}`;
}

