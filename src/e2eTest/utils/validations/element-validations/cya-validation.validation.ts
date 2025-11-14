/**
 * CYA Validation
 * 
 * Validates the Check Your Answers page against collected journey data.
 * Provides detailed Allure reporting for test results.
 */

import { Page } from '@playwright/test';
import { IValidation, validationData, validationRecord } from '@utils/interfaces';
import { JourneyDataCollector } from '@utils/cya/journey-data-collector';
import { CYAValidator } from '@utils/cya/cya-validator';
import { CYAPageParser } from '@utils/cya/cya-page-parser';
import { allure } from 'allure-playwright';

export class CYAValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: validationData | validationRecord, data?: validationData | validationRecord): Promise<void> {
    const collector = JourneyDataCollector.getInstance();
    
    // Filter out address-related questions (they were validated in Address CYA)
    const nonAddressQuestions = this.getNonAddressQuestions(collector);
    const filteredCollector = this.createFilteredCollector(collector, nonAddressQuestions);
    const validator = new CYAValidator(page, filteredCollector);
    
    // Start Allure step for CYA validation
    await allure.step('Validate Check Your Answers Page', async () => {
      const result = await validator.validate();
      const report = await validator.generateReport();
      
      // Add validation summary to Allure
      await allure.step(`Validation Summary: ${result.matchedQuestions}/${result.totalQuestions} questions matched`, async () => {
        // Attach the full report as text
        await allure.attachment('CYA Validation Report', report, 'text/plain');
        
        // Create detailed JSON attachment for structured data
        const jsonReport = {
          journey: collector.getJourneyName(),
          overallResult: result.allMatch ? 'PASS' : 'FAIL',
          totalQuestions: result.totalQuestions,
          matchedQuestions: result.matchedQuestions,
          mismatches: result.mismatches,
          missingInCYA: result.missingInCYA,
          extraInCYA: result.extraInCYA,
          collectedData: Array.from(collector.getAllAnswers().entries()).map(([q, a]) => ({ question: q, answer: a }))
        };
        await allure.attachment('CYA Validation Details', JSON.stringify(jsonReport, null, 2), 'application/json');
      });
      
      // Add detailed mismatch information if any
      if (result.mismatches.length > 0) {
        await allure.step(`Found ${result.mismatches.length} mismatched answers`, async () => {
          for (let index = 0; index < result.mismatches.length; index++) {
            const mismatch = result.mismatches[index];
            await allure.step(`Mismatch ${index + 1}: "${mismatch.question}"`, async () => {
              await allure.attachment('Expected Answer', mismatch.expectedAnswer, 'text/plain');
              await allure.attachment('Actual Answer', mismatch.actualAnswer, 'text/plain');
            });
          }
        });
      }
      
      // Add missing questions information if any
      if (result.missingInCYA.length > 0) {
        await allure.step(`Found ${result.missingInCYA.length} questions missing on CYA page`, async () => {
          await allure.attachment('Missing Questions', result.missingInCYA.join('\n'), 'text/plain');
        });
      }
      
      // Add extra questions information if any
      if (result.extraInCYA.length > 0) {
        await allure.step(`Found ${result.extraInCYA.length} extra questions on CYA page`, async () => {
          await allure.attachment('Extra Questions', result.extraInCYA.join('\n'), 'text/plain');
        });
      }

      // Log all questions found on CYA page for easy reference (useful for updating hardcoded questions)
      const parser = new CYAPageParser(page);
      const allCYAQuestions = await parser.extractCYAAnswers();
      const questionsList = Array.from(allCYAQuestions.values())
        .map((qa, idx) => `${idx + 1}. "${qa.question}"`)
        .join('\n');
      await allure.attachment('All Questions Found on CYA Page', questionsList, 'text/plain');
      console.log('\n=== All Questions Found on CYA Page ===');
      Array.from(allCYAQuestions.values()).forEach((qa, idx) => {
        console.log(`${idx + 1}. "${qa.question}"`);
      });
      console.log('========================================\n');
      
      if (!result.allMatch) {
        console.error(report);
        
        // Build detailed error message
        let errorMessage = `CYA validation failed:\n`;
        errorMessage += `Matched: ${result.matchedQuestions}/${result.totalQuestions}\n`;
        
        if (result.mismatches.length > 0) {
          errorMessage += `\nMismatches:\n`;
          result.mismatches.forEach((mismatch, index) => {
            errorMessage += `  ${index + 1}. "${mismatch.question}"\n`;
            errorMessage += `     Expected: "${mismatch.expectedAnswer}"\n`;
            errorMessage += `     Actual:   "${mismatch.actualAnswer}"\n`;
          });
        }
        
        if (result.missingInCYA.length > 0) {
          errorMessage += `\nMissing on CYA page: ${result.missingInCYA.join(', ')}\n`;
        }
        
        if (result.extraInCYA.length > 0) {
          errorMessage += `\nExtra on CYA page: ${result.extraInCYA.join(', ')}\n`;
        }
        
        // Mark test as failed in Allure
        await allure.step('CYA Validation Failed', async () => {
          throw new Error(errorMessage);
        });
      } else {
        console.log(`✓ CYA validation passed: ${result.matchedQuestions}/${result.totalQuestions} questions matched`);
      }
    });
  }

  /**
   * Get non-address questions from collector (exclude address-related questions)
   */
  private getNonAddressQuestions(collector: JourneyDataCollector): Map<string, string> {
    const allAnswers = collector.getAllAnswers();
    const nonAddressQuestions = new Map<string, string>();
    
    // Address-related question patterns to exclude
    const addressPatterns = [
      'what is the address',
      'property address',
      'address of the property',
      'is the property located',
      'england or wales',
      'country'
    ];
    
    for (const [question, answer] of allAnswers.entries()) {
      const normalizedQuestion = question.toLowerCase();
      const isAddressQuestion = addressPatterns.some(pattern => 
        normalizedQuestion.includes(pattern)
      );
      
      if (!isAddressQuestion) {
        nonAddressQuestions.set(question, answer.answer);
      }
    }
    
    return nonAddressQuestions;
  }

  /**
   * Create a filtered collector that excludes address questions
   * Creates a wrapper object that implements the necessary interface
   */
  private createFilteredCollector(originalCollector: JourneyDataCollector, nonAddressQuestions: Map<string, string>): JourneyDataCollector {
    // Create a wrapper object that implements the JourneyDataCollector interface
    // This avoids modifying the singleton instance
    return {
      getAllAnswers: () => {
        const tempMap = new Map();
        for (const [question, answer] of nonAddressQuestions.entries()) {
          tempMap.set(question, { question, answer, timestamp: new Date() });
        }
        return tempMap;
      },
      getJourneyName: () => originalCollector.getJourneyName()
    } as JourneyDataCollector;
  }
}

