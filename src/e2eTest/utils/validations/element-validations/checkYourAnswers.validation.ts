import { Page, test } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaData } from '@utils/cya/cya-field-collector';
import { extractCCDTable } from '@utils/cya/cya-validation-utils';

export class CheckYourAnswersValidation implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    const collectedQA = cyaData.collectedQAPairs || [];
    if (collectedQA.length === 0) {
      throw new Error('Final CYA: No CYA data collected. Make sure to collect Q&A pairs during the journey.');
    }

    const pageQA = await test.step('Extract Q&A pairs from CYA page', async () => {
      return await this.extractAllQAFromPage(page);
    });
    
    // Debug logging
    console.log('📝 [Final CYA] Collected Q&A pairs:', collectedQA.map(q => `${q.question}: ${q.answer}`));
    console.log('📝 [Final CYA] Page Q&A pairs:', pageQA.map(q => `${q.question}: ${q.answer}`));
    
    // Categorize errors for better reporting
    const missingInCollected: Array<{question: string; answer: string}> = [];
    const missingOnPage: Array<{question: string; answer: string}> = [];
    const answerMismatches: Array<{question: string; expected: string; found: string}> = [];

    // Debug: Show all extracted answers
    console.log(`📝 [Final CYA] Total Q&A pairs extracted: ${pageQA.length}`);
    console.log(`📝 [Final CYA] Total Q&A pairs collected: ${collectedQA.length}`);
    console.log(`📝 [Final CYA] Sample extracted Q&A (first 10):`, pageQA.slice(0, 10).map(q => `"${q.question}": "${q.answer}"`));
    
    // Normalize whitespace function (replace multiple spaces with single space)
    const normalizeWhitespace = (text: string): string => {
      return text.trim().replace(/\s+/g, ' ');
    };
    
    // Specific debug for "Mandatory grounds"
    const mandatoryGroundsCollected = collectedQA.find(q => q.question && q.question.includes('Mandatory grounds'));
    const mandatoryGroundsPage = pageQA.find(q => q.question && q.question.includes('Mandatory grounds'));
    if (mandatoryGroundsCollected) {
      console.log(`📝 [Final CYA] DEBUG - "Mandatory grounds" collected: "${mandatoryGroundsCollected.question}" = "${mandatoryGroundsCollected.answer || ''}"`);
      if (mandatoryGroundsPage) {
        console.log(`📝 [Final CYA] DEBUG - "Mandatory grounds" found on page: "${mandatoryGroundsPage.question}" = "${mandatoryGroundsPage.answer}"`);
        console.log(`📝 [Final CYA] DEBUG - Question match: ${normalizeWhitespace(mandatoryGroundsPage.question) === normalizeWhitespace(mandatoryGroundsCollected.question || '')}`);
        console.log(`📝 [Final CYA] DEBUG - Answer match: ${(mandatoryGroundsPage.answer || '').trim() === (mandatoryGroundsCollected.answer || '').trim()}`);
      } else {
        console.log(`📝 [Final CYA] DEBUG - "Mandatory grounds" NOT found on page`);
        // Check if any question contains "grounds"
        const groundsQuestions = pageQA.filter(q => q.question && q.question.toLowerCase().includes('ground'));
        console.log(`📝 [Final CYA] DEBUG - Questions containing "ground":`, groundsQuestions.map(q => `"${q.question}": "${q.answer}"`));
      }
    }

    // Debug: Check for specific missing questions (using whitespace normalization)
    const missingQuestions = collectedQA.filter(c => {
      if (!c.question) return false;
      const collectedQ = normalizeWhitespace(c.question);
      const found = pageQA.find(p => normalizeWhitespace(p.question) === collectedQ);
      return !found;
    });
    if (missingQuestions.length > 0) {
      console.log(`📝 [Final CYA] Questions in collected but not found in extracted:`, missingQuestions.map(q => `"${q.question}"`));
      
      // For each missing question, check if there's a similar question on the page
      for (const missing of missingQuestions) {
        if (!missing.question) continue;
        const missingQ = normalizeWhitespace(missing.question);
        const similarQuestions = pageQA.filter(p => {
          const pageQ = normalizeWhitespace(p.question);
          // Check if question contains key words from missing question
          const missingWords = missingQ.toLowerCase().split(/\s+/).filter(w => w.length > 3);
          const pageWords = pageQ.toLowerCase().split(/\s+/).filter(w => w.length > 3);
          const matchingWords = missingWords.filter(w => pageWords.includes(w));
          return matchingWords.length > 0;
        });
        if (similarQuestions.length > 0) {
          console.log(`📝 [Final CYA] Similar questions found for "${missing.question}":`, similarQuestions.map(q => `"${q.question}": "${q.answer}"`));
        }
        
        // Also check if the answer exists anywhere on the page
        if (missing.answer) {
          const missingAnswer = missing.answer.trim();
          const answerFound = pageQA.some(p => {
            const pageAnswer = typeof p.answer === 'string' ? p.answer.trim() : String(p.answer).trim();
            return pageAnswer === missingAnswer || pageAnswer.includes(missingAnswer);
          });
          if (answerFound) {
            console.log(`📝 [Final CYA] Answer "${missingAnswer}" found on page but with different question`);
          }
        }
      }
    }

    // Validate all collected answers appear on CYA page - EXACT MATCHING ONLY
    await test.step(`Validate ${collectedQA.length} collected Q&A pairs against CYA page`, async () => {
      for (const collected of collectedQA) {
        if (!collected.question || !collected.answer) continue;

        const collectedQuestion = normalizeWhitespace(collected.question || '');
        const collectedAnswer = (collected.answer || '').trim();
        
        // Exact match: question (case-sensitive, whitespace-normalized) and answer (exact)
        const found = pageQA.find(p => {
          const pageQuestion = normalizeWhitespace(p.question);
          const pageAnswer = typeof p.answer === 'string' ? p.answer.trim() : String(p.answer).trim();
          return pageQuestion === collectedQuestion && pageAnswer === collectedAnswer;
        });
        
        if (!found) {
          // Check if question exists but answer is different (with whitespace normalization)
          const questionFound = pageQA.find(p => {
            const pageQuestion = normalizeWhitespace(p.question);
            return pageQuestion === collectedQuestion;
          });
          
          if (questionFound) {
            // Question found but answer doesn't match exactly
            answerMismatches.push({
              question: questionFound.question,
              expected: collectedAnswer,
              found: typeof questionFound.answer === 'string' ? questionFound.answer.trim() : String(questionFound.answer).trim()
            });
          } else {
            // Question not found at all
            console.log(`📝 [Final CYA] Question not found: "${collectedQuestion}"`);
            missingOnPage.push({
              question: collectedQuestion,
              answer: collectedAnswer
            });
          }
        }
      }
    });

    // Validate all questions on CYA page were collected (case-sensitive, whitespace-normalized)
    await test.step('Check for questions on CYA page that were not collected', async () => {
      for (const pageItem of pageQA) {
        const pageQuestion = normalizeWhitespace(pageItem.question);
        const wasCollected = collectedQA.some(c => {
          if (!c.question) return false;
          const collectedQuestion = normalizeWhitespace(c.question);
          return collectedQuestion === pageQuestion;
        });
        if (!wasCollected) {
          // Question on page but not collected during journey
          missingInCollected.push({
            question: pageItem.question.trim(),
            answer: typeof pageItem.answer === 'string' ? pageItem.answer.trim() : String(pageItem.answer).trim()
          });
        }
      }
    });

    // Build comprehensive error message for Allure reports
    if (missingOnPage.length > 0 || missingInCollected.length > 0 || answerMismatches.length > 0) {
      const errorParts: string[] = [];
      
      if (missingOnPage.length > 0) {
        errorParts.push(`\n❌ QUESTIONS COLLECTED BUT MISSING ON CYA PAGE (${missingOnPage.length}):`);
        missingOnPage.forEach((item, index) => {
          errorParts.push(`  ${index + 1}. Question: "${item.question}"`);
          errorParts.push(`     Expected Answer: "${item.answer}"`);
        });
      }
      
      if (missingInCollected.length > 0) {
        errorParts.push(`\n⚠️  QUESTIONS ON CYA PAGE BUT NOT COLLECTED (${missingInCollected.length}):`);
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
      
      const errorMessage = `Final CYA validation failed:${errorParts.join('\n')}`;
      throw new Error(errorMessage);
    }
  }

  /**
   * Extract all Q&A pairs from table.form-table using simplified table extraction
   */
  private async extractAllQAFromPage(page: Page): Promise<Array<{question: string; answer: string}>> {
    return await test.step('Extract all Q&A pairs from CYA page', async () => {
      const qaPairs: Array<{question: string; answer: string}> = [];
      
      // Extract from main form table
      const results = await extractCCDTable(page, 'table.form-table');
    
    for (const item of results) {
      if (Array.isArray(item.answer)) {
        // Flatten nested answers
        for (const nested of item.answer) {
          if (nested.question && nested.answer) {
            qaPairs.push({ question: nested.question, answer: nested.answer });
          }
        }
      } else if (item.question && item.answer) {
        // Filter out "Change" links but keep the actual answer
        const cleanAnswer = String(item.answer).replace(/\s*Change\s*/gi, '').trim();
        if (cleanAnswer && !cleanAnswer.match(/^Change$/i)) {
          qaPairs.push({ question: item.question, answer: cleanAnswer });
        }
      }
    }

    // Also try extracting from tbody rows directly with multiple selectors
    const tbodyRows = page.locator('table.form-table tbody tr:not([hidden])');
    const tbodyRowCount = await tbodyRows.count().catch(() => 0);
    
    for (let i = 0; i < tbodyRowCount; i++) {
      const row = tbodyRows.nth(i);
      
      // Try multiple selectors for question
      let question = (await row.locator('th span, th').innerText({ timeout: 1000 }).catch(() => ''))?.trim() || '';
      if (!question) {
        question = (await row.locator('th').textContent({ timeout: 500 }).catch(() => ''))?.trim() || '';
      }
      if (!question) {
        question = (await row.locator('th.case-field-label').innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
      }
      
      const answerCell = row.locator('td');
      const hasNestedTable = await answerCell.locator('table').count().catch(() => 0);
      
      if (hasNestedTable === 0 && question) {
        // Simple question without nested table - try multiple selectors for answer
        let answer = (await answerCell.innerText({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        if (!answer) {
          answer = (await answerCell.textContent({ timeout: 500 }).catch(() => ''))?.trim() || '';
        }
        if (!answer) {
          answer = (await answerCell.locator('span.text-16').first().innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
        }
        if (!answer) {
          answer = (await answerCell.locator('span').first().innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
        }
        if (!answer) {
          // Try multi-select list field (like "Holiday let (ground 3)")
          // Can be directly in td or inside ccd-read-multi-select-list-field
          let multiSelectTable = answerCell.locator('table.multi-select-list-field-table');
          let hasMultiSelect = await multiSelectTable.count().catch(() => 0);
          
          // If not found, try inside ccd-read-multi-select-list-field
          if (hasMultiSelect === 0) {
            const multiSelectField = answerCell.locator('ccd-read-multi-select-list-field');
            const hasField = await multiSelectField.count().catch(() => 0);
            if (hasField > 0) {
              multiSelectTable = multiSelectField.locator('table.multi-select-list-field-table');
              hasMultiSelect = await multiSelectTable.count().catch(() => 0);
            }
          }
          
          if (hasMultiSelect > 0) {
            const multiSelectRows = multiSelectTable.locator('tbody tr, tr');
            const multiSelectCount = await multiSelectRows.count().catch(() => 0);
            const multiSelectValues: string[] = [];
            
            for (let k = 0; k < multiSelectCount; k++) {
              const msRow = multiSelectRows.nth(k);
              // Skip header rows (check if th exists and is hidden)
              const th = msRow.locator('th');
              const thCount = await th.count().catch(() => 0);
              if (thCount > 0) {
                const thDisplay = await th.first().evaluate(el => window.getComputedStyle(el).display).catch(() => '');
                if (thDisplay === 'none') {
                  continue; // Skip header row
                }
              }
              
              // Extract value from td
              let msValue = (await msRow.locator('td span.text-16').innerText({ timeout: 1000 }).catch(() => ''))?.trim() || '';
              if (!msValue) {
                msValue = (await msRow.locator('td').innerText({ timeout: 1000 }).catch(() => ''))?.trim() || '';
              }
              if (msValue) {
                multiSelectValues.push(msValue);
              }
            }
            
            if (multiSelectValues.length > 0) {
              answer = multiSelectValues.join(', ');
            }
          }
        }
        if (!answer) {
          // Try text area field (like "Reasons For possession Page Sample Text")
          const textAreaField = answerCell.locator('ccd-read-text-area-field span');
          const hasTextArea = await textAreaField.count().catch(() => 0);
          if (hasTextArea > 0) {
            const textAreaValue = (await textAreaField.first().innerText({ timeout: 1000 }).catch(() => ''))?.trim() || '';
            if (textAreaValue) {
              answer = textAreaValue;
            }
          }
        }
        if (!answer) {
          // Try getting all text from the cell, including nested elements
          const allTexts = await answerCell.allTextContents().catch(() => []);
          answer = allTexts.join(' ').trim() || '';
        }
        if (!answer) {
          // Try getting text from divs or paragraphs
          answer = (await answerCell.locator('div, p').first().innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
        }
        
        const cleanAnswer = answer.replace(/\s*Change\s*/gi, '').trim();
        if (cleanAnswer && !cleanAnswer.match(/^Change$/i) && question) {
          // Check if we already have this question (exact match, case-sensitive)
          const exists = qaPairs.some(q => q.question === question);
          if (!exists) {
            qaPairs.push({ question, answer: cleanAnswer });
          }
        }
      }
    }

    // Also try extracting from all table rows (including those without tbody)
    const allRows = page.locator('table.form-table tr:not([hidden])');
    const allRowCount = await allRows.count().catch(() => 0);
    
    for (let i = 0; i < allRowCount; i++) {
      const row = allRows.nth(i);
      const question = (await row.locator('th span, th').innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
      const answerCell = row.locator('td.form-cell.case-field-content, td');
      
      if (question && (await answerCell.count().catch(() => 0)) > 0) {
        const hasNested = await answerCell.locator('table').count().catch(() => 0);
        if (hasNested === 0) {
          let answer = (await answerCell.innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
          if (!answer) {
            answer = (await answerCell.textContent({ timeout: 300 }).catch(() => ''))?.trim() || '';
          }
          if (!answer) {
            answer = (await answerCell.locator('span').first().innerText({ timeout: 300 }).catch(() => ''))?.trim() || '';
          }
          if (!answer) {
            // Try multi-select list field (can be inside ccd-read-multi-select-list-field)
            let multiSelectTable = answerCell.locator('table.multi-select-list-field-table');
            let hasMultiSelect = await multiSelectTable.count().catch(() => 0);
            
            // If not found, try inside ccd-read-multi-select-list-field
            if (hasMultiSelect === 0) {
              const multiSelectField = answerCell.locator('ccd-read-multi-select-list-field');
              const hasField = await multiSelectField.count().catch(() => 0);
              if (hasField > 0) {
                multiSelectTable = multiSelectField.locator('table.multi-select-list-field-table');
                hasMultiSelect = await multiSelectTable.count().catch(() => 0);
              }
            }
            
            if (hasMultiSelect > 0) {
              const multiSelectRows = multiSelectTable.locator('tbody tr, tr');
              const multiSelectCount = await multiSelectRows.count().catch(() => 0);
              const multiSelectValues: string[] = [];
              
              for (let k = 0; k < multiSelectCount; k++) {
                const msRow = multiSelectRows.nth(k);
                // Skip header rows (check if th exists and is hidden)
                const th = msRow.locator('th');
                const thCount = await th.count().catch(() => 0);
                if (thCount > 0) {
                  const thDisplay = await th.first().evaluate(el => window.getComputedStyle(el).display).catch(() => '');
                  if (thDisplay === 'none') {
                    continue; // Skip header row
                  }
                }
                
                // Extract value from td
                let msValue = (await msRow.locator('td span.text-16').innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
                if (!msValue) {
                  msValue = (await msRow.locator('td').innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
                }
                if (msValue) {
                  multiSelectValues.push(msValue);
                }
              }
              
              if (multiSelectValues.length > 0) {
                answer = multiSelectValues.join(', ');
              }
            }
          }
          if (!answer) {
            // Try text area field
            const textAreaField = answerCell.locator('ccd-read-text-area-field span');
            const hasTextArea = await textAreaField.count().catch(() => 0);
            if (hasTextArea > 0) {
              const textAreaValue = (await textAreaField.first().innerText({ timeout: 500 }).catch(() => ''))?.trim() || '';
              if (textAreaValue) {
                answer = textAreaValue;
              }
            }
          }
          if (!answer) {
            // Try getting all text from the cell
            const allTexts = await answerCell.allTextContents().catch(() => []);
            answer = allTexts.join(' ').trim() || '';
          }
          
          const cleanAnswer = answer.replace(/\s*Change\s*/gi, '').trim();
          if (cleanAnswer && !cleanAnswer.match(/^Change$/i)) {
            // Check if we already have this question (exact match, case-sensitive)
            const exists = qaPairs.some(q => q.question === question);
            if (!exists) {
              qaPairs.push({ question, answer: cleanAnswer });
            }
          }
        }
      }
    }

      return qaPairs;
    });
  }

}
