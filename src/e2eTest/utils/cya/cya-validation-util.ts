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

  const normalize = (t: string) =>
    useWhitespaceNormalization ? normalizeWhitespace(t) : t.trim();

  const missingInCollected = [];
  const missingOnCYAPage = [];
  const answerMismatches = [];

  // Create maps for fast lookup
  const collectedMap = new Map(
    collectedQA
      .filter(c => c.question && c.answer)
      .map(c => [normalize(c.question!), c.answer!.trim()])
  );

  const pageCYAMap = new Map(
    pageCYAQA.map(p => [normalize(p.question), p.answer.trim()])
  );

  // Check: Collected but missing / mismatched on CYA page
  for (const [q, expectedAnswer] of collectedMap.entries()) {
    if (!pageCYAMap.has(q)) {
      missingOnCYAPage.push({ question: q, answer: expectedAnswer });
      continue;
    }

    const foundAnswer = pageCYAMap.get(q)!;
    if (expectedAnswer !== foundAnswer) {
      answerMismatches.push({
        question: q,
        expected: expectedAnswer,
        found: foundAnswer
      });
    }
  }

  // Check: On CYA page but not collected
  for (const [q, answer] of pageCYAMap.entries()) {
    if (!collectedMap.has(q)) {
      missingInCollected.push({ question: q, answer });
    }
  }

  return { missingOnCYAPage, missingInCollected, answerMismatches };
}

export function hasValidationErrors(results: ValidationResults): boolean {
  return (
    results.missingOnCYAPage.length > 0 ||
    results.missingInCollected.length > 0 ||
    results.answerMismatches.length > 0
  );
}

export function buildCYAErrorMessage(
  results: ValidationResults,
  pageType: 'Final' | 'Address'
): string {
  const { missingOnCYAPage, missingInCollected, answerMismatches } = results;
  const msgs: string[] = [];

  if (missingOnCYAPage.length > 0) {
    msgs.push(`QUESTIONS COLLECTED BUT MISSING ON ${pageType.toUpperCase()} CYA PAGE (${missingOnCYAPage.length}):`);
    missingOnCYAPage.forEach((item, i) => {
      msgs.push(`  ${i + 1}. Question: "${item.question}"`);
      msgs.push(`     Expected Answer: "${item.answer}"`);
    });
  }

  if (missingInCollected.length > 0) {
    msgs.push(`QUESTIONS ON ${pageType.toUpperCase()} CYA PAGE BUT NOT COLLECTED (${missingInCollected.length}):`);
    missingInCollected.forEach((item, i) => {
      msgs.push(`  ${i + 1}. Question: "${item.question}"`);
      msgs.push(`     Answer on Page: "${item.answer}"`);
    });
  }

  if (answerMismatches.length > 0) {
    msgs.push(`ANSWER MISMATCHES (${answerMismatches.length}):`);
    answerMismatches.forEach((item, i) => {
      msgs.push(`  ${i + 1}. Question: "${item.question}"`);
      msgs.push(`     Expected: "${item.expected}"`);
      msgs.push(`     Found: "${item.found}"`);
    });
  }

  return `${pageType} CYA validation failed:\n${msgs.join('\n')}`;
}
