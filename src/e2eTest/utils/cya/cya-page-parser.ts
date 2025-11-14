/**
 * CYA Page Parser
 * 
 * Extracts questions and answers from the Check Your Answers (CYA) page.
 * The CYA page typically displays questions and answers in a structured format
 * with "Change" links next to each answer.
 * 
 * Usage:
 *   const parser = new CYAPageParser(page);
 *   const cyaData = await parser.extractCYAAnswers();
 */

import { Page, Locator } from '@playwright/test';
import { LONG_TIMEOUT } from '../playwright.config';

export interface CYAQuestionAnswer {
  question: string;
  answer: string;
  changeLink?: Locator;
}

export class CYAPageParser {
  constructor(private page: Page) {}

  /**
   * Extract all questions and answers from the CYA page
   * Supports multiple formats:
   * - Definition lists (dl > dt + dd)
   * - Summary lists (govuk-summary-list)
   * - Custom formats with question/answer pairs
   */
  async extractCYAAnswers(): Promise<Map<string, CYAQuestionAnswer>> {
    const cyaData = new Map<string, CYAQuestionAnswer>();

    // Wait for CYA page to load
    await this.page.waitForLoadState('domcontentloaded', { timeout: LONG_TIMEOUT });

    // Try multiple selector patterns to find question-answer pairs
    const patterns = [
      // Pattern 1: Definition lists (dl > dt + dd)
      async () => {
        const definitions = this.page.locator('dl.case-field, dl.govuk-summary-list, dl');
        const count = await definitions.count();
        
        for (let i = 0; i < count; i++) {
          const dl = definitions.nth(i);
          const dts = dl.locator('dt, dt.case-field__label, dt.govuk-summary-list__key');
          const dtCount = await dts.count();
          
          for (let j = 0; j < dtCount; j++) {
            const question = await this.normalizeText(dts.nth(j));
            const dd = dl.locator('dd, dd.case-field__value, dd.govuk-summary-list__value').nth(j);
            const answer = await this.normalizeText(dd);
            const changeLink = dd.locator('a:has-text("Change"), a:has-text("change")').first();
            
            if (question && answer) {
              cyaData.set(question, { question, answer, changeLink });
            }
          }
        }
      },

      // Pattern 2: Summary list rows (govuk-summary-list__row)
      async () => {
        const rows = this.page.locator('.govuk-summary-list__row, [class*="summary-list__row"]');
        const count = await rows.count();
        
        for (let i = 0; i < count; i++) {
          const row = rows.nth(i);
          const question = await this.normalizeText(
            row.locator('.govuk-summary-list__key, [class*="summary-list__key"], dt').first()
          );
          const answer = await this.normalizeText(
            row.locator('.govuk-summary-list__value, [class*="summary-list__value"], dd').first()
          );
          const changeLink = row.locator('a:has-text("Change"), a:has-text("change")').first();
          
          if (question && answer) {
            cyaData.set(question, { question, answer, changeLink });
          }
        }
      },

      // Pattern 3: Generic question-answer pairs with labels
      async () => {
        // Look for elements with question-like text followed by answer
        const questionElements = this.page.locator(
          'strong:has-text("?"), ' +
          'label:has-text("?"), ' +
          '[class*="question"]:has-text("?"), ' +
          'dt:has-text("?")'
        );
        const count = await questionElements.count();
        
        for (let i = 0; i < count; i++) {
          const questionEl = questionElements.nth(i);
          const question = await this.normalizeText(questionEl);
          
          // Find answer in next sibling or parent's next sibling
          const answerEl = questionEl.locator('xpath=following-sibling::*[1]')
            .or(questionEl.locator('xpath=../following-sibling::*[1]'))
            .or(questionEl.locator('xpath=../../following-sibling::*[1]'));
          
          const answer = await this.normalizeText(answerEl.first());
          const changeLink = answerEl.locator('a:has-text("Change"), a:has-text("change")').first();
          
          if (question && answer) {
            cyaData.set(question, { question, answer, changeLink });
          }
        }
      },

      // Pattern 4: List items with question-answer structure
      async () => {
        const listItems = this.page.locator('li, .case-field, [class*="field"]');
        const count = await listItems.count();
        
        for (let i = 0; i < count; i++) {
          const item = listItems.nth(i);
          const text = await this.normalizeText(item);
          
          // Try to split by common patterns (e.g., "Question: Answer")
          const match = text.match(/^(.+?)[:：]\s*(.+)$/);
          if (match) {
            const question = match[1].trim();
            const answer = match[2].trim();
            const changeLink = item.locator('a:has-text("Change"), a:has-text("change")').first();
            
            if (question && answer) {
              cyaData.set(question, { question, answer, changeLink });
            }
          }
        }
      }
    ];

    // Try each pattern and merge results
    for (const pattern of patterns) {
      try {
        await pattern();
      } catch (error) {
        // Continue with next pattern if one fails
        console.warn(`CYA parsing pattern failed: ${error}`);
      }
    }

    // Fallback: Extract from page text if no structured data found
    if (cyaData.size === 0) {
      await this.extractFromPageText(cyaData);
    }

    return cyaData;
  }

  /**
   * Fallback method to extract Q&A from page text
   */
  private async extractFromPageText(cyaData: Map<string, CYAQuestionAnswer>): Promise<void> {
    const pageText = await this.page.textContent('body');
    if (!pageText) return;

    // Look for question-answer patterns in text
    const lines = pageText.split('\n').map(line => line.trim()).filter(line => line);
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      
      // Check if line looks like a question (ends with ?)
      if (line.endsWith('?')) {
        const question = this.normalizeTextSync(line);
        // Next non-empty line might be the answer
        let answer = '';
        for (let j = i + 1; j < lines.length; j++) {
          if (lines[j] && !lines[j].includes('Change') && !lines[j].includes('change')) {
            answer = this.normalizeTextSync(lines[j]);
            break;
          }
        }
        
        if (question && answer) {
          cyaData.set(question, { question, answer });
        }
      }
    }
  }

  /**
   * Normalize text from an element (async)
   */
  private async normalizeText(locator: Locator): Promise<string> {
    try {
      const isVisible = await locator.isVisible({ timeout: 1000 }).catch(() => false);
      if (!isVisible) return '';
      
      const text = await locator.textContent();
      return this.normalizeTextSync(text || '');
    } catch {
      return '';
    }
  }

  /**
   * Normalize text synchronously
   */
  private normalizeTextSync(text: string): string {
    if (!text) return '';
    
    return text
      .trim()
      .replace(/\s+/g, ' ') // Replace multiple spaces/newlines with single space
      .replace(/\n/g, ' ')
      .toLowerCase();
  }

  /**
   * Get all questions from CYA page
   */
  async getQuestions(): Promise<string[]> {
    const answers = await this.extractCYAAnswers();
    return Array.from(answers.keys());
  }

  /**
   * Get answer for a specific question
   */
  async getAnswer(question: string): Promise<string | undefined> {
    const answers = await this.extractCYAAnswers();
    const normalizedQuestion = this.normalizeTextSync(question);
    
    // Try exact match first
    if (answers.has(normalizedQuestion)) {
      return answers.get(normalizedQuestion)?.answer;
    }
    
    // Try partial match
    for (const [key, value] of answers.entries()) {
      if (key.includes(normalizedQuestion) || normalizedQuestion.includes(key)) {
        return value.answer;
      }
    }
    
    return undefined;
  }
}

