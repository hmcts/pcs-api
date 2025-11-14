/**
 * Helper script to extract actual questions from the CYA page
 * 
 * This can be used to:
 * 1. See what questions actually appear on the CYA page
 * 2. Compare with hardcoded questions in custom actions
 * 3. Update hardcoded questions to match actual CYA page
 * 
 * Usage in a test:
 *   import { extractCYAQuestions } from '@utils/cya/extract-cya-questions';
 *   const questions = await extractCYAQuestions(page);
 *   console.log('Questions on CYA page:', questions);
 */

import { Page } from '@playwright/test';
import { CYAPageParser } from './cya-page-parser';

/**
 * Extract all questions from the CYA page
 * Returns a map of normalized question -> original question for easy lookup
 */
export async function extractCYAQuestions(page: Page): Promise<Map<string, string>> {
  const parser = new CYAPageParser(page);
  const cyaData = await parser.extractCYAAnswers();
  
  const questions = new Map<string, string>();
  for (const [normalizedQuestion, data] of cyaData.entries()) {
    questions.set(normalizedQuestion, data.question);
  }
  
  return questions;
}

/**
 * Print all questions from CYA page in a readable format
 */
export async function printCYAQuestions(page: Page): Promise<void> {
  const questions = await extractCYAQuestions(page);
  
  console.log('\n=== Questions Found on CYA Page ===');
  console.log(`Total questions: ${questions.size}\n`);
  
  let index = 1;
  for (const [normalized, original] of questions.entries()) {
    console.log(`${index}. Original: "${original}"`);
    console.log(`   Normalized: "${normalized}"\n`);
    index++;
  }
  console.log('===================================\n');
}

/**
 * Get questions as an array for easy copying
 */
export async function getCYAQuestionsArray(page: Page): Promise<string[]> {
  const questions = await extractCYAQuestions(page);
  return Array.from(questions.values());
}

