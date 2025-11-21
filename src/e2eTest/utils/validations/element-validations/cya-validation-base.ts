import { Page } from '@playwright/test';
import { CollectedQAPair } from '@utils/data/cya-types';

/**
 * Base validation logic shared between Address and Final CYA validations
 */
export abstract class CYAValidationBase {
  private static validationInProgress = false;
  
  /**
   * Common validation logic for both Address and Final CYA
   */
  protected async validateQAPairs(
    page: Page,
    qaPairs: CollectedQAPair[],
    errorPrefix: string
  ): Promise<void> {
    // Prevent duplicate execution
    if (CYAValidationBase.validationInProgress) {
      console.log(`⚠️  Validation already in progress, skipping duplicate call`);
      return;
    }
    
    CYAValidationBase.validationInProgress = true;
    try {
    await Promise.race([
      page.waitForLoadState('networkidle'),
      page.waitForSelector('table.form-table, table.complex-panel-table', { timeout: 10000 })
    ]).catch(() => {});
    await page.waitForTimeout(300);

    if (!qaPairs || qaPairs.length === 0) {
      throw new Error(`${errorPrefix}: No CYA data collected. Make sure to collect Q&A pairs during the journey.`);
    }

    console.log(`\n${'='.repeat(80)}`);
    console.log(`🔍 [${errorPrefix}] Starting Validation - ${qaPairs.length} Q&A pairs`);
    console.log(`${'='.repeat(80)}\n`);

    const allPageQuestions = await this.getAllQuestionsOnPage(page);
    console.log(`📄 Questions on CYA Page (${allPageQuestions.length}):`);
    allPageQuestions.forEach((q, i) => console.log(`  ${i + 1}. "${q.question}" → "${q.answer.substring(0, 50)}"`));
    console.log(`📋 Collected Data (${qaPairs.length}):`);
    qaPairs.forEach((qa, i) => console.log(`  ${i + 1}. [${qa.step || 'N/A'}] "${qa.question}" → "${qa.answer}"`));
    console.log('');

    const errors: string[] = [];
    for (let i = 0; i < qaPairs.length; i++) {
      const qaPair = qaPairs[i];
      if (!qaPair.question || !qaPair.answer) {
        console.log(`⚠️  [${i + 1}/${qaPairs.length}] Skipping - Missing question or answer`);
        continue;
      }

      console.log(`\n🔎 [${i + 1}/${qaPairs.length}] "${qaPair.question}" → "${qaPair.answer}"`);

      const found = await Promise.race([
        this.findQuestionOnPage(page, qaPair.question),
        new Promise<{found: boolean; question: string; answer: string}>(resolve => 
          setTimeout(() => resolve({ found: false, question: qaPair.question || '', answer: '' }), 5000)
        )
      ]) as {found: boolean; question: string; answer: string};

      if (!found.found) {
        console.log(`   ❌ Question NOT FOUND on CYA page`);
        
        // Find similar questions on the page (case-sensitive)
        const similarQuestions = allPageQuestions
          .map(q => ({
            question: q.question,
            answer: q.answer,
            similarity: qaPair.question
              ? this.calculateSimilarity(q.question, qaPair.question)
              : 0
          }))
          .filter(q => q.similarity > 0.3)
          .sort((a, b) => b.similarity - a.similarity)
          .slice(0, 3);
        
        if (similarQuestions.length > 0) {
          console.log(`   💡 Similar questions found on page:`);
          similarQuestions.forEach(sq => {
            console.log(`      - "${sq.question}" → "${sq.answer}" (similarity: ${(sq.similarity * 100).toFixed(0)}%)`);
          });
        } else {
          console.log(`   ⚠️  No similar questions found. This question might not appear on CYA page.`);
        }
        
        errors.push(`Question not found: "${qaPair.question}"`);
        continue;
      }

      console.log(`   ✅ Found: "${found.question}" → "${found.answer}"`);

      // Case-sensitive exact match
      const collected = qaPair.answer.trim();
      const pageAnswer = found.answer.trim();

      if (collected !== pageAnswer) {
        console.log(`   ❌ MISMATCH: Expected "${qaPair.answer}", Found "${found.answer}"`);
        errors.push(`Mismatch for "${found.question}": Expected "${qaPair.answer}", Found "${found.answer}"`);
      } else {
        console.log(`   ✅ MATCH`);
      }
    }

    // Check for questions on CYA page that weren't collected during journey
    const collectedQuestionTexts = new Set(qaPairs.map(qa => qa.question?.trim().toLowerCase()).filter(Boolean));
    const uncollectedQuestions = allPageQuestions.filter(
      pageQ => !collectedQuestionTexts.has(pageQ.question.trim().toLowerCase())
    );

    if (uncollectedQuestions.length > 0) {
      console.log(`\n⚠️  Questions on CYA page NOT collected during journey (${uncollectedQuestions.length}):`);
      uncollectedQuestions.forEach((q, i) => {
        console.log(`  ${i + 1}. "${q.question}" → "${q.answer.substring(0, 50)}"`);
        errors.push(`Question on CYA page not collected: "${q.question}"`);
      });
    }

    console.log(`\n${'='.repeat(80)}`);
    if (errors.length > 0) {
      console.log(`❌ [${errorPrefix}] FAILED - ${errors.length} error(s)`);
      console.log(`${'='.repeat(80)}\n`);
      throw new Error(`${errorPrefix} validation failed:\n${errors.join('\n')}`);
    } else {
      console.log(`✅ [${errorPrefix}] PASSED - All ${qaPairs.length} pairs matched`);
      console.log(`${'='.repeat(80)}\n`);
    }
    } finally {
      CYAValidationBase.validationInProgress = false;
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
   * Match question text - Case-sensitive exact match with minor whitespace/punctuation tolerance
   */
  protected match(pageQuestion: string, collectedQuestion: string): boolean {
    const p = pageQuestion.trim();
    const c = collectedQuestion.trim();
    
    // Case-sensitive exact match
    if (p === c) return true;
    
    // Remove common punctuation and normalize whitespace (case-sensitive)
    const pClean = p.replace(/[.,!?;:()'"]/g, '').replace(/\s+/g, ' ').trim();
    const cClean = c.replace(/[.,!?;:()'"]/g, '').replace(/\s+/g, ' ').trim();
    
    // Case-sensitive exact match after cleaning
    if (pClean === cClean) return true;
    
    // For questions with numbers (like "Address Line 2" vs "Address Line 3"), require exact number match
    const pHasNumber = /\d+/.test(pClean);
    const cHasNumber = /\d+/.test(cClean);
    if (pHasNumber || cHasNumber) {
      const pNumber = pClean.match(/\d+/)?.[0];
      const cNumber = cClean.match(/\d+/)?.[0];
      if (pNumber && cNumber && pNumber !== cNumber) {
        return false;
      }
    }
    
    // Very strict word-based matching (only for minor punctuation/whitespace differences)
    // Require 95%+ similarity to prevent false matches between different questions
    const stopWords = new Set(['a', 'an', 'the', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could', 'should', 'may', 'might', 'must', 'can', 'about', 'any', 'you', 'your', 'there', 'this', 'that', 'these', 'those']);
    const pWords = pClean.split(/\s+/).filter(w => w.length > 2 && !stopWords.has(w.toLowerCase()));
    const cWords = cClean.split(/\s+/).filter(w => w.length > 2 && !stopWords.has(w.toLowerCase()));
    
    // Require very similar length (within 10%)
    const lengthDiff = Math.abs(pClean.length - cClean.length);
    const avgLength = (pClean.length + cClean.length) / 2;
    if (avgLength > 0 && lengthDiff / avgLength > 0.1) {
      return false; // Too different in length
    }
    
    // Require word count difference <= 1
    if (Math.abs(pWords.length - cWords.length) > 1) {
      return false;
    }
    
    // Require 95%+ word overlap (very strict)
    const commonWords = pWords.filter(w => cWords.includes(w));
    const uniqueWords = new Set([...pWords, ...cWords]);
    if (uniqueWords.size === 0) return false;
    
    const overlapRatio = commonWords.length / uniqueWords.size;
    const minWords = Math.min(pWords.length, cWords.length);
    const wordMatchRatio = minWords > 0 ? commonWords.length / minWords : 0;
    
    // Require 95% overlap AND 95% of shorter question words match
    if (overlapRatio >= 0.95 && wordMatchRatio >= 0.95) {
      return true;
    }
    
    // Only allow substring match if length difference is <= 5% (very strict)
    const pSignificant = pWords.join(' ');
    const cSignificant = cWords.join(' ');
    if (pSignificant === cSignificant) return true;
    if (pSignificant.includes(cSignificant) || cSignificant.includes(pSignificant)) {
      const sigLengthDiff = Math.abs(pSignificant.length - cSignificant.length);
      const sigAvgLength = (pSignificant.length + cSignificant.length) / 2;
      if (sigAvgLength > 0 && sigLengthDiff / sigAvgLength <= 0.05) return true;
    }
    
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
   * Calculate similarity between two strings (0-1 scale) for debugging
   */
  protected calculateSimilarity(str1: string, str2: string): number {
    const words1 = str1.split(/\s+/).filter(w => w.length > 2);
    const words2 = str2.split(/\s+/).filter(w => w.length > 2);
    if (words1.length === 0 || words2.length === 0) return 0;
    const commonWords = words1.filter(w => words2.includes(w));
    return commonWords.length / new Set([...words1, ...words2]).size;
  }
}

