/**
 * CYA Validator
 * 
 * Validates collected journey data against the Check Your Answers page.
 * Compares expected answers (collected during test) with actual answers (displayed on CYA page).
 * 
 * Usage:
 *   const collector = JourneyDataCollector.getInstance();
 *   collector.setAnswer('What is the address?', '15 Garden Drive, Luton, Bedfordshire, LU1 1AB');
 *   
 *   const validator = new CYAValidator(page, collector);
 *   const results = await validator.validate();
 *   if (!results.allMatch) {
 *     console.error('Validation failed:', results.mismatches);
 *   }
 */

import { Page } from '@playwright/test';
import { JourneyDataCollector } from './journey-data-collector';
import { CYAPageParser, CYAQuestionAnswer } from './cya-page-parser';

export interface ValidationResult {
  allMatch: boolean;
  totalQuestions: number;
  matchedQuestions: number;
  mismatches: Mismatch[];
  missingInCYA: string[]; // Questions collected but not found on CYA page
  extraInCYA: string[]; // Questions on CYA page but not collected
}

export interface Mismatch {
  question: string;
  expectedAnswer: string;
  actualAnswer: string;
}

export class CYAValidator {
  constructor(
    private page: Page,
    private collector: JourneyDataCollector
  ) {}

  /**
   * Validate collected answers against CYA page
   */
  async validate(): Promise<ValidationResult> {
    const parser = new CYAPageParser(this.page);
    const cyaAnswers = await parser.extractCYAAnswers();
    const collectedAnswers = this.collector.getAllAnswers();

    const result: ValidationResult = {
      allMatch: true,
      totalQuestions: 0,
      matchedQuestions: 0,
      mismatches: [],
      missingInCYA: [],
      extraInCYA: []
    };

    // Normalize all questions for comparison
    const normalizedCYA = this.normalizeMap(cyaAnswers);
    const normalizedCollected = this.normalizeMapFromJourney(collectedAnswers);

    // Check collected answers against CYA page
    for (const [question, collectedAnswer] of normalizedCollected.entries()) {
      result.totalQuestions++;
      
      const cyaAnswer = normalizedCYA.get(question);
      
      if (!cyaAnswer) {
        // Question not found on CYA page
        result.missingInCYA.push(question);
        result.allMatch = false;
      } else if (this.answersMatch(collectedAnswer, cyaAnswer.answer)) {
        result.matchedQuestions++;
      } else {
        // Answer mismatch
        result.mismatches.push({
          question,
          expectedAnswer: collectedAnswer,
          actualAnswer: cyaAnswer.answer
        });
        result.allMatch = false;
      }
    }

    // Check for extra questions on CYA page (not collected)
    for (const [question] of normalizedCYA.entries()) {
      if (!normalizedCollected.has(question)) {
        result.extraInCYA.push(question);
        // Extra questions don't necessarily mean failure, but log them
      }
    }

    return result;
  }

  /**
   * Normalize CYA map for comparison
   */
  private normalizeMap(cyaMap: Map<string, CYAQuestionAnswer>): Map<string, CYAQuestionAnswer> {
    const normalized = new Map<string, CYAQuestionAnswer>();
    
    for (const [question, value] of cyaMap.entries()) {
      const normalizedQuestion = this.normalizeText(question);
      normalized.set(normalizedQuestion, {
        ...value,
        question: normalizedQuestion,
        answer: this.normalizeText(value.answer)
      });
    }
    
    return normalized;
  }

  /**
   * Normalize journey answers map for comparison
   */
  private normalizeMapFromJourney(journeyMap: Map<string, { question: string; answer: string }>): Map<string, string> {
    const normalized = new Map<string, string>();
    
    for (const [question, value] of journeyMap.entries()) {
      const normalizedQuestion = this.normalizeText(question);
      normalized.set(normalizedQuestion, this.normalizeText(value.answer));
    }
    
    return normalized;
  }

  /**
   * Check if two answers match (with fuzzy matching)
   */
  private answersMatch(expected: string, actual: string): boolean {
    // Exact match
    if (expected === actual) {
      return true;
    }

    // Normalize both for comparison
    const normalizedExpected = this.normalizeText(expected);
    const normalizedActual = this.normalizeText(actual);

    // Exact match after normalization
    if (normalizedExpected === normalizedActual) {
      return true;
    }

    // Partial match (one contains the other)
    if (normalizedExpected.includes(normalizedActual) || normalizedActual.includes(normalizedExpected)) {
      return true;
    }

    // Check if key parts match (for complex answers)
    const expectedParts = normalizedExpected.split(/[,\s]+/).filter(p => p.length > 2);
    const actualParts = normalizedActual.split(/[,\s]+/).filter(p => p.length > 2);
    
    if (expectedParts.length > 0 && actualParts.length > 0) {
      const matchingParts = expectedParts.filter(part => 
        actualParts.some(actualPart => actualPart.includes(part) || part.includes(actualPart))
      );
      
      // If most parts match, consider it a match
      return matchingParts.length >= Math.min(expectedParts.length, actualParts.length) * 0.7;
    }

    return false;
  }

  /**
   * Normalize text for comparison
   */
  private normalizeText(text: string): string {
    return text
      .trim()
      .toLowerCase()
      .replace(/\s+/g, ' ') // Replace multiple spaces with single space
      .replace(/[^\w\s,.-]/g, '') // Remove special characters except common punctuation
      .trim();
  }

  /**
   * Generate a detailed validation report
   */
  async generateReport(): Promise<string> {
    const result = await this.validate();
    
    let report = `\n=== CYA Validation Report ===\n`;
    report += `Journey: ${this.collector.getJourneyName()}\n`;
    report += `Total Questions: ${result.totalQuestions}\n`;
    report += `Matched: ${result.matchedQuestions}\n`;
    report += `Status: ${result.allMatch ? '✓ PASS' : '✗ FAIL'}\n\n`;

    if (result.mismatches.length > 0) {
      report += `Mismatches (${result.mismatches.length}):\n`;
      result.mismatches.forEach((mismatch, index) => {
        report += `  ${index + 1}. Question: "${mismatch.question}"\n`;
        report += `     Expected: "${mismatch.expectedAnswer}"\n`;
        report += `     Actual:   "${mismatch.actualAnswer}"\n\n`;
      });
    }

    if (result.missingInCYA.length > 0) {
      report += `Missing on CYA Page (${result.missingInCYA.length}):\n`;
      result.missingInCYA.forEach((question, index) => {
        report += `  ${index + 1}. "${question}"\n`;
      });
      report += '\n';
    }

    if (result.extraInCYA.length > 0) {
      report += `Extra on CYA Page (${result.extraInCYA.length}):\n`;
      result.extraInCYA.forEach((question, index) => {
        report += `  ${index + 1}. "${question}"\n`;
      });
      report += '\n';
    }

    return report;
  }
}

