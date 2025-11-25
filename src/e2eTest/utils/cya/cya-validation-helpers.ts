/**
 * Shared validation helpers for CYA (Check Your Answers) validation
 * Contains common validation logic used by both Address and Final CYA validations
 */

export interface ValidationResults {
  missingInCollected: Array<{question: string; answer: string}>;
  missingOnPage: Array<{question: string; answer: string}>;
  answerMismatches: Array<{question: string; expected: string; found: string}>;
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

