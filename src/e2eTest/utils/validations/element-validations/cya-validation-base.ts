import { Page } from '@playwright/test';
import { CollectedQAPair } from '@utils/data/cya-types';

/**
 * Base validation logic shared between Address and Final CYA validations
 */
export abstract class CYAValidationBase {
  /**
   * Common validation logic for both Address and Final CYA
   */
  protected async validateQAPairs(
    page: Page,
    qaPairs: CollectedQAPair[],
    errorPrefix: string
  ): Promise<void> {
    // Wait for page to load with timeout
    await Promise.race([
      page.waitForLoadState('networkidle'),
      page.waitForSelector('table.form-table, table.complex-panel-table', { timeout: 10000 })
    ]).catch(() => {});
    await page.waitForTimeout(300);

    if (!qaPairs || qaPairs.length === 0) {
      throw new Error(`${errorPrefix}: No CYA data collected. Make sure to collect Q&A pairs during the journey.`);
    }

    console.log(`\n${'='.repeat(80)}`);
    console.log(`🔍 [${errorPrefix}] Starting Validation`);
    console.log(`📊 Total Q&A Pairs Collected: ${qaPairs.length}`);
    console.log(`${'='.repeat(80)}\n`);

    // Log all questions found on the CYA page (for debugging)
    const allPageQuestions = await this.getAllQuestionsOnPage(page);
    console.log(`📄 Questions Found on CYA Page (${allPageQuestions.length} total):`);
    allPageQuestions.forEach((q, index) => {
      console.log(`  ${index + 1}. "${q.question}" → "${q.answer}"`);
    });
    console.log('');

    // Log all collected data
    console.log(`📋 Collected Data During Journey:`);
    qaPairs.forEach((qaPair, index) => {
      console.log(`  ${index + 1}. [${qaPair.step || 'N/A'}]`);
      console.log(`     Question: "${qaPair.question}"`);
      console.log(`     Answer:   "${qaPair.answer}"`);
    });
    console.log('');

    const errors: string[] = [];
    for (let i = 0; i < qaPairs.length; i++) {
      const qaPair = qaPairs[i];
      if (!qaPair.question || !qaPair.answer) {
        console.log(`⚠️  [${i + 1}/${qaPairs.length}] Skipping - Missing question or answer`);
        continue;
      }

      console.log(`\n🔎 [${i + 1}/${qaPairs.length}] Validating:`);
      console.log(`   Collected Question: "${qaPair.question}"`);
      console.log(`   Collected Answer:   "${qaPair.answer}"`);

      const found = await Promise.race([
        this.findQuestionOnPage(page, qaPair.question),
        new Promise<{found: boolean; question: string; answer: string}>(resolve => 
          setTimeout(() => resolve({ found: false, question: qaPair.question || '', answer: '' }), 5000)
        )
      ]);

      if (!found.found) {
        console.log(`   ❌ Question NOT FOUND on CYA page`);
        
        // Find similar questions on the page
        const similarQuestions = allPageQuestions
          .map(q => ({
            question: q.question,
            similarity: this.calculateSimilarity(q.question.toLowerCase(), qaPair.question.toLowerCase())
          }))
          .filter(q => q.similarity > 0.3)
          .sort((a, b) => b.similarity - a.similarity)
          .slice(0, 3);
        
        if (similarQuestions.length > 0) {
          console.log(`   💡 Similar questions found on page:`);
          similarQuestions.forEach(sq => {
            console.log(`      - "${sq.question}" (similarity: ${(sq.similarity * 100).toFixed(0)}%)`);
          });
        } else {
          console.log(`   ⚠️  No similar questions found. This question might not appear on CYA page.`);
        }
        
        errors.push(`Question not found: "${qaPair.question}"`);
        continue;
      }

      console.log(`   ✅ Question FOUND on CYA page: "${found.question}"`);
      console.log(`   📄 Page Answer: "${found.answer}"`);

      const collected = qaPair.answer.trim().toLowerCase();
      const pageAnswer = found.answer.trim().toLowerCase();

      if (collected !== pageAnswer && !pageAnswer.includes(collected) && !collected.includes(pageAnswer)) {
        console.log(`   ❌ MISMATCH DETECTED`);
        console.log(`      Expected: "${qaPair.answer}"`);
        console.log(`      Found:    "${found.answer}"`);
        errors.push(`Mismatch for "${found.question}": Expected "${qaPair.answer}", Found "${found.answer}"`);
      } else {
        console.log(`   ✅ MATCH - Answers match!`);
      }
    }

    console.log(`\n${'='.repeat(80)}`);
    if (errors.length > 0) {
      console.log(`❌ [${errorPrefix}] Validation FAILED`);
      console.log(`   Errors: ${errors.length}`);
      console.log(`${'='.repeat(80)}\n`);
      throw new Error(`${errorPrefix} validation failed:\n${errors.join('\n')}`);
    } else {
      console.log(`✅ [${errorPrefix}] Validation PASSED`);
      console.log(`   All ${qaPairs.length} Q&A pairs matched successfully!`);
      console.log(`${'='.repeat(80)}\n`);
    }
  }

  /**
   * Find question on page - to be implemented by subclasses
   */
  protected abstract findQuestionOnPage(page: Page, questionText: string): Promise<{
    found: boolean;
    question: string;
    answer: string;
  }>;

  /**
   * Match question text (case-insensitive, partial match)
   * Improved to handle similar questions like "Address Line 2" vs "Address Line 3"
   */
  protected match(pageQuestion: string, collectedQuestion: string): boolean {
    const p = pageQuestion.toLowerCase().trim();
    const c = collectedQuestion.toLowerCase().trim();
    
    // Exact match
    if (p === c) return true;
    
    // For questions with numbers (like "Address Line 2" vs "Address Line 3"), require exact number match
    const pHasNumber = /\d+/.test(p);
    const cHasNumber = /\d+/.test(c);
    if (pHasNumber || cHasNumber) {
      // Extract numbers and compare
      const pNumber = p.match(/\d+/)?.[0];
      const cNumber = c.match(/\d+/)?.[0];
      if (pNumber && cNumber && pNumber !== cNumber) {
        return false; // Different numbers, don't match
      }
      // If numbers match, continue with other checks
    }
    
    // One contains the other (but only if no number mismatch)
    if (p.includes(c) || c.includes(p)) return true;
    
    // Remove common punctuation and compare
    const pClean = p.replace(/[.,!?;:()]/g, '').replace(/\s+/g, ' ');
    const cClean = c.replace(/[.,!?;:()]/g, '').replace(/\s+/g, ' ');
    if (pClean === cClean || pClean.includes(cClean) || cClean.includes(pClean)) return true;
    
    // Check if key words match (at least 3 words in common, but all important words must match)
    const pWords = pClean.split(/\s+/).filter(w => w.length > 2);
    const cWords = cClean.split(/\s+/).filter(w => w.length > 2);
    const commonWords = pWords.filter(w => cWords.includes(w));
    const allImportantWordsMatch = pWords.length === cWords.length && commonWords.length === pWords.length;
    if (allImportantWordsMatch) return true;
    
    // Fallback: at least 3 words in common
    if (commonWords.length >= Math.min(3, Math.min(pWords.length, cWords.length))) return true;
    
    return false;
  }

  /**
   * Get all questions on the CYA page (for debugging)
   * Also extracts questions from complex fields (like Property address)
   * Only processes main table.form-table to avoid duplicates from nested tables
   */
  protected async getAllQuestionsOnPage(page: Page): Promise<Array<{question: string; answer: string}>> {
    const questions: Array<{question: string; answer: string}> = [];
    const seenQuestions = new Set<string>(); // Track seen questions to avoid duplicates
    
    // Only process main form tables, not nested complex-panel-table
    const mainTables = page.locator('table.form-table');
    const tableCount = await mainTables.count();

    for (let i = 0; i < tableCount; i++) {
      const table = mainTables.nth(i);
      const rows = table.locator('tr');
      const rowCount = await rows.count();

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        const questionCell = row.locator('th').first();
        const answerCell = row.locator('td.form-cell, td.case-field-content').first();

        const question = await questionCell.textContent({ timeout: 500 }).catch(() => null);
        if (!question || !question.trim()) continue;

        // Check if this is a complex field (Property address)
        const complexField = answerCell.locator('ccd-read-complex-field-table table.complex-panel-table');
        const hasComplexField = await complexField.count() > 0;

        if (hasComplexField && this.match(question, 'Property address')) {
          // Extract sub-questions from complex field
          const complexRows = complexField.locator('tr.complex-panel-simple-field');
          const complexRowCount = await complexRows.count();

          for (let k = 0; k < complexRowCount; k++) {
            const complexRow = complexRows.nth(k);
            const complexQuestionCell = complexRow.locator('th');
            const complexAnswerCell = complexRow.locator('td');

            const complexQuestion = await complexQuestionCell.textContent({ timeout: 500 }).catch(() => null);
            if (!complexQuestion || !complexQuestion.trim()) continue;

            const complexQuestionTrimmed = complexQuestion.trim();
            // Skip if we've already seen this question
            if (seenQuestions.has(complexQuestionTrimmed)) continue;
            seenQuestions.add(complexQuestionTrimmed);

            // Extract answer using formLabelValue pattern
            const valueLocator = complexRow.locator(`th#complex-panel-simple-field-label > span.text-16:has-text("${complexQuestionTrimmed}")`)
              .locator('xpath=../..')
              .locator('td span.text-16:not(:has(ccd-field-read-label))');
            
            let answer = await valueLocator.textContent({ timeout: 500 }).catch(() => null);
            if (!answer || !answer.trim()) {
              answer = await complexAnswerCell.textContent({ timeout: 500 }).catch(() => null);
            }

            questions.push({
              question: complexQuestionTrimmed,
              answer: (answer || '').trim().substring(0, 50)
            });
          }
        } else if (!hasComplexField) {
          // Simple question (not a complex field)
          const questionTrimmed = question.trim();
          // Skip if we've already seen this question
          if (seenQuestions.has(questionTrimmed)) continue;
          seenQuestions.add(questionTrimmed);

          let answer = await answerCell.textContent({ timeout: 500 }).catch(() => null);
          if (answer) {
            answer = answer.replace(/Change/gi, '').trim();
          }
          questions.push({
            question: questionTrimmed,
            answer: (answer || '').trim().substring(0, 50)
          });
        }
      }
    }

    return questions;
  }

  /**
   * Calculate similarity between two strings (0-1 scale)
   */
  protected calculateSimilarity(str1: string, str2: string): number {
    const words1 = str1.split(/\s+/).filter(w => w.length > 2);
    const words2 = str2.split(/\s+/).filter(w => w.length > 2);
    
    if (words1.length === 0 || words2.length === 0) return 0;
    
    const commonWords = words1.filter(w => words2.includes(w));
    const totalWords = new Set([...words1, ...words2]).size;
    
    return commonWords.length / totalWords;
  }
}

