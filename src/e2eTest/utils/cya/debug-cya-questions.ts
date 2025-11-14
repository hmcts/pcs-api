/**
 * Debug helper to extract and display all questions from CYA page
 * 
 * Add this to your test temporarily to see all questions:
 * 
 * import { debugCYAQuestions } from '@utils/cya/debug-cya-questions';
 * await debugCYAQuestions(page);
 */

import { Page } from '@playwright/test';
import { CYAPageParser } from './cya-page-parser';

/**
 * Debug function to print all questions found on CYA page
 * Useful for updating hardcoded questions in custom actions
 */
export async function debugCYAQuestions(page: Page): Promise<void> {
  const parser = new CYAPageParser(page);
  const cyaData = await parser.extractCYAAnswers();
  
  console.log('\n');
  console.log('='.repeat(80));
  console.log('CYA PAGE QUESTIONS - Copy these to update hardcoded questions');
  console.log('='.repeat(80));
  console.log(`\nTotal questions found: ${cyaData.size}\n`);
  
  const questions = Array.from(cyaData.values());
  questions.forEach((qa, index) => {
    console.log(`${index + 1}. "${qa.question}"`);
    console.log(`   Answer: "${qa.answer}"\n`);
  });
  
  console.log('\n--- Questions as JavaScript strings (for easy copy) ---\n');
  questions.forEach((qa, index) => {
    console.log(`'${qa.question}',`);
  });
  
  console.log('\n' + '='.repeat(80) + '\n');
}

/**
 * Get questions as a formatted string for documentation
 */
export async function getCYAQuestionsFormatted(page: Page): Promise<string> {
  const parser = new CYAPageParser(page);
  const cyaData = await parser.extractCYAAnswers();
  
  const questions = Array.from(cyaData.values());
  let output = `\n=== CYA Page Questions (${questions.length} total) ===\n\n`;
  
  questions.forEach((qa, index) => {
    output += `${index + 1}. "${qa.question}"`\n`;
    output += `   Answer: "${qa.answer}"\n\n`;
  });
  
  return output;
}

