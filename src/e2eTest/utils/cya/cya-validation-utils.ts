/**
 * Shared utilities for CYA (Check Your Answers) validation
 * Contains common extraction and matching logic used by both Address and Final CYA validations
 */

import { Page } from '@playwright/test';

export interface QAPair {
  question: string;
  answer: string;
}

export interface QAPairWithNested {
  question: string;
  answer: string | Array<QAPair>;
}

/**
 * Extract Q&A pairs from a table using page.evaluate for better performance
 * This runs in a single browser context call to reduce Allure steps
 */
export async function extractCCDTable(
  page: Page,
  tableLocator: string
): Promise<Array<QAPairWithNested>> {
  // Use evaluate to extract data in a single browser context call to reduce Allure steps
  return await page.evaluate((locator) => {
    const results: Array<QAPairWithNested> = [];
    const table = document.querySelector(locator) as HTMLTableElement;
    
    if (!table) return results;

    const rows = table.querySelectorAll('tr');
    
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const th = row.querySelector('th');
      const td = row.querySelector('td');
      
      if (!th || !td) continue;
      
      // Extract question - try multiple selectors
      let question = '';
      
      // Try 1: span.text-16 (most common)
      question = (th.querySelector('span.text-16')?.textContent || '').trim();
      
      // Try 2: any span
      if (!question) {
        question = (th.querySelector('span')?.textContent || '').trim();
      }
      
      // Try 3: direct textContent (last resort)
      if (!question) {
        question = (th.textContent || '').trim();
      }
      
      if (!question) continue;
      
      // Check for nested table - but first check if it's a multi-select table (which should be treated as a simple answer, not nested Q&A)
      let multiSelectTable = td.querySelector('table.multi-select-list-field-table');
      // If not found, try inside ccd-read-multi-select-list-field
      if (!multiSelectTable) {
        const multiSelectField = td.querySelector('ccd-read-multi-select-list-field');
        if (multiSelectField) {
          multiSelectTable = multiSelectField.querySelector('table.multi-select-list-field-table');
        }
      }
      
      // If it's a multi-select table, extract it as a simple answer (not nested Q&A)
      if (multiSelectTable) {
        const msRows = multiSelectTable.querySelectorAll('tbody tr, tr');
        const msValues: string[] = [];
        msRows.forEach(msRow => {
          // Skip header rows (th with display:none)
          const th = msRow.querySelector('th');
          if (th) {
            const styleAttr = th.getAttribute('style');
            if (styleAttr && styleAttr.includes('display: none')) {
              return; // Skip header row
            }
            const thStyle = window.getComputedStyle(th);
            if (thStyle.display === 'none') {
              return; // Skip header row
            }
          }
          
          const msTd = msRow.querySelector('td');
          if (msTd) {
            // Try span.text-16 first
            let msValue = (msTd.querySelector('span.text-16')?.textContent || '').trim();
            if (!msValue) {
              msValue = (msTd.textContent || '').trim();
            }
            if (msValue) msValues.push(msValue);
          }
        });
        if (msValues.length > 0) {
          results.push({ question, answer: msValues.join(', ') });
          continue; // Skip to next row
        }
      }
      
      // Check for nested complex table (with Q&A pairs, not multi-select)
      const nestedTable = td.querySelector('table:not(.multi-select-list-field-table)');
      if (nestedTable) {
        const nestedRows = nestedTable.querySelectorAll('tr');
        const nestedData: Array<QAPair> = [];
        
        for (let j = 0; j < nestedRows.length; j++) {
          const nr = nestedRows[j];
          const nestedTh = nr.querySelector('th');
          const nestedTd = nr.querySelector('td');
          
          if (nestedTh && nestedTd) {
            // Extract nested question - try multiple selectors
            let nestedQ = (nestedTh.querySelector('span.text-16')?.textContent || '').trim();
            if (!nestedQ) {
              nestedQ = (nestedTh.querySelector('span')?.textContent || '').trim();
            }
            if (!nestedQ) {
              nestedQ = (nestedTh.textContent || '').trim();
            }
            
            // Extract nested answer - try multiple selectors
            let nestedA = (nestedTd.querySelector('span.text-16')?.textContent || '').trim();
            if (!nestedA) {
              nestedA = (nestedTd.querySelector('span')?.textContent || '').trim();
            }
            if (!nestedA) {
              nestedA = (nestedTd.textContent || '').trim();
            }
            
            // Handle multi-select list field in nested table
            if (!nestedA) {
              const nestedMultiSelectTable = nestedTd.querySelector('table.multi-select-list-field-table');
              if (nestedMultiSelectTable) {
                const msRows = nestedMultiSelectTable.querySelectorAll('tr');
                const msValues: string[] = [];
                msRows.forEach(msRow => {
                  const msTd = msRow.querySelector('td');
                  const msValue = (msTd?.querySelector('span.text-16')?.textContent || msTd?.textContent || '').trim();
                  if (msValue) msValues.push(msValue);
                });
                if (msValues.length > 0) nestedA = msValues.join(', ');
              }
            }
            
            // Handle text area field
            if (!nestedA) {
              const textAreaSpan = nestedTd.querySelector('ccd-read-text-area-field span');
              nestedA = (textAreaSpan?.textContent || '').trim();
            }
            
            if (nestedQ || nestedA) {
              nestedData.push({ question: nestedQ, answer: nestedA });
            }
          }
        }
        
        if (nestedData.length > 0) {
          results.push({ question, answer: nestedData });
          continue;
        }
      }
      
      // Simple answer - try multiple selectors in order of specificity
      let answer = '';
      
      // Note: Multi-select tables are already handled above (lines 61-102), so we skip them here
      // Try 1: span.text-16 (most common for simple fields)
      if (!answer) {
        answer = (td.querySelector('span.text-16')?.textContent || '').trim();
      }
      
      // Try 3: any span
      if (!answer) {
        answer = (td.querySelector('span')?.textContent || '').trim();
      }
      
      // Try 4: ccd-read-text-field
      if (!answer) {
        const textField = td.querySelector('ccd-read-text-field span, ccd-read-text-field');
        answer = (textField?.textContent || '').trim();
      }
      
      // Try 5: text area field
      if (!answer) {
        const textAreaSpan = td.querySelector('ccd-read-text-area-field span');
        answer = (textAreaSpan?.textContent || '').trim();
      }
      
      // Try 6: direct textContent (last resort, but skip if it's just "Change")
      if (!answer) {
        const rawText = (td.textContent || '').trim();
        // Remove "Change" link text if present
        answer = rawText.replace(/\s*Change\s*/gi, '').trim();
        if (answer.match(/^Change$/i)) {
          answer = ''; // Don't use "Change" as answer
        }
      }
      
      if (answer && !answer.match(/^Change$/i)) {
        results.push({ question, answer });
      }
    }
    
    return results;
  }, tableLocator).catch(async () => {
    // Fallback to Playwright locators if evaluate fails
    const results: Array<QAPairWithNested> = [];
    const table = page.locator(tableLocator).first();
    
    const isVisible = await table.isVisible({ timeout: 5000 }).catch(() => false);
    if (!isVisible) {
      return results;
    }

    const rows = table.locator('tr');
    const rowCount = await rows.count().catch(() => 0);

    for (let i = 0; i < rowCount; i++) {
      const row = rows.nth(i);
      
      // Try multiple selectors for question text
      let question = (await row.locator('th span.text-16').innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
      if (!question) {
        question = (await row.locator('th span').innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
      }
      if (!question) {
        question = (await row.locator('th').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
      }
      
      // Extract answer
      let answer: string | Array<QAPair> = '';
      const td = row.locator('td');
      
      if (await td.count().catch(() => 0) > 0) {
        // Case 1 - simple text answer (try multiple selectors)
        let simpleText = (await td.locator('span.text-16').first().innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
        if (!simpleText) {
          simpleText = (await td.locator('span').first().innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
        }
        if (!simpleText) {
          simpleText = (await td.locator('ccd-read-text-field span').first().innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
        }
        if (!simpleText) {
          simpleText = (await td.innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
        }
        if (!simpleText) {
          simpleText = (await td.textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        answer = simpleText;
        
        // Case 1a - multi-select list field (can be inside ccd-read-multi-select-list-field)
        if (!answer || answer.includes('Change')) {
          // Try direct table.multi-select-list-field-table first
          let multiSelectTable = td.locator('table.multi-select-list-field-table');
          let hasMultiSelect = await multiSelectTable.count().catch(() => 0);
          
          // If not found, try inside ccd-read-multi-select-list-field
          if (hasMultiSelect === 0) {
            const multiSelectField = td.locator('ccd-read-multi-select-list-field');
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
              // Skip header rows (check if th exists with display:none)
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
        
        // Case 1b - text area field
        if (!answer || answer.includes('Change')) {
          const textAreaField = td.locator('ccd-read-text-area-field span');
          const hasTextArea = await textAreaField.count().catch(() => 0);
          if (hasTextArea > 0) {
            const textAreaValue = (await textAreaField.first().innerText({ timeout: 1000 }).catch(() => ''))?.trim() || '';
            if (textAreaValue) {
              answer = textAreaValue;
            }
          }
        }
        
        // Case 2 - nested complex table
        const hasNestedTable = await td.locator('table.complex-panel-table, table:not(.multi-select-list-field-table)').count().catch(() => 0);
        if (hasNestedTable > 0 && (typeof answer === 'string' && (!answer || answer.includes('Change')))) {
          const nestedRows = td.locator('table tr');
          const nestedCount = await nestedRows.count().catch(() => 0);
          const nestedData: Array<QAPair> = [];
          
          for (let j = 0; j < nestedCount; j++) {
            const nr = nestedRows.nth(j);
            let nestedQ = (await nr.locator('th span, th').innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
            if (!nestedQ) {
              nestedQ = (await nr.locator('th').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
            }
            let nestedA = (await nr.locator('td').innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
            if (!nestedA) {
              nestedA = (await nr.locator('td').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
            }
            
            if (nestedQ || nestedA) {
              nestedData.push({ question: nestedQ, answer: nestedA });
            }
          }
          
          if (nestedData.length > 0) {
            answer = nestedData;
          }
        }
      }
      
      // Only push if we found meaningful content
      if (question || (typeof answer === 'string' ? answer : answer.length > 0)) {
        results.push({ question, answer });
      }
    }

    return results;
  });
}

/**
 * Match question text - handles variations in formatting, case, and punctuation
 */
export function matchQuestion(pageQuestion: string, collectedQuestion: string): boolean {
  const p = pageQuestion.trim();
  const c = collectedQuestion.trim();

  if (p === c) return true;

  // Case-insensitive match
  if (p.toLowerCase() === c.toLowerCase()) return true;

  // Normalize: remove punctuation and normalize whitespace
  const normalize = (s: string) => s.replace(/[.,!?;:()'"]/g, '').replace(/\s+/g, ' ').trim().toLowerCase();
  const pNorm = normalize(p);
  const cNorm = normalize(c);

  if (pNorm === cNorm) return true;

  // Check if one contains the other (for partial matches)
  if (pNorm.includes(cNorm) || cNorm.includes(pNorm)) return true;

  // For longer questions, check if key words match
  const pWords = pNorm.split(/\s+/).filter(w => w.length > 2); // Filter out short words
  const cWords = cNorm.split(/\s+/).filter(w => w.length > 2);
  if (pWords.length > 0 && cWords.length > 0) {
    const matchingWords = pWords.filter(w => cWords.includes(w));
    // If more than 50% of key words match, consider it a match
    const matchRatio = matchingWords.length / Math.max(pWords.length, cWords.length);
    if (matchRatio >= 0.5) return true;
  }

  return false;
}

/**
 * Check if a question is an address field
 */
export function isAddressField(question: string): boolean {
  const addressFields = ['building', 'street', 'address line', 'town', 'city', 'postcode', 'country'];
  const lowerQ = question.toLowerCase();
  return addressFields.some(field => lowerQ.includes(field));
}

/**
 * Simple extraction for Address CYA page
 * Address CYA structure:
 * - Main table with "Property address" row containing nested complex-panel-table
 * - Nested table has address fields: Building and Street, Address Line 2, etc.
 * - Optional: One additional question like "Is the property located in England or Wales?"
 */
export async function extractSimpleQAFromPage(page: Page): Promise<Array<QAPair>> {
  // Wait for the complex-panel-table to be visible first
  const tableVisible = await page.locator('table.complex-panel-table').first().waitFor({ timeout: 5000 }).catch(() => false);
  console.log(`🏠 [Address CYA] Complex-panel-table visible: ${!!tableVisible}`);
  
  const result = await page.evaluate(() => {
    const qaPairs: Array<QAPair> = [];
    
    // Extract address fields from nested complex-panel-table
    // Structure: table.complex-panel-table > tbody > tr.complex-panel-simple-field
    const complexTables = document.querySelectorAll('table.complex-panel-table');
    console.log(`[Address CYA] Found ${complexTables.length} complex-panel-table(s)`);
    
    for (const table of Array.from(complexTables)) {
      const rows = table.querySelectorAll('tr.complex-panel-simple-field');
      console.log(`[Address CYA] Found ${rows.length} complex-panel-simple-field rows`);
      
      for (const row of Array.from(rows)) {
        // Try multiple selectors for question
        const th = row.querySelector('th');
        const thSpan = th?.querySelector('span.text-16, span');
        const question = (thSpan?.textContent || th?.textContent || '').trim();
        
        // Try multiple selectors for answer
        const td = row.querySelector('td');
        let answer = '';
        
        // Try ccd-read-text-field first (most specific)
        const textField = td?.querySelector('ccd-read-text-field span.text-16, ccd-read-text-field span');
        if (textField) {
          answer = (textField.textContent || '').trim();
        }
        
        // Fallback to any span.text-16 in td
        if (!answer) {
          const span = td?.querySelector('span.text-16');
          if (span) {
            answer = (span.textContent || '').trim();
          }
        }
        
        // Last resort: get all text from td
        if (!answer && td) {
          answer = (td.textContent || '').trim();
        }
        
        if (question && answer) {
          console.log(`[Address CYA] Extracted: "${question}" = "${answer}"`);
          qaPairs.push({ question, answer });
        } else {
          console.log(`[Address CYA] Skipped row - question: "${question}", answer: "${answer}"`);
        }
      }
    }
    
    // Extract other simple questions from main form table (like "Is the property located...")
    // These are direct th/td pairs in the main table, not nested
    const mainTables = document.querySelectorAll('table.form-table');
    for (const table of Array.from(mainTables)) {
      const rows = table.querySelectorAll('tbody tr, tr');
      for (const row of Array.from(rows)) {
        // Skip rows that contain nested complex tables (we already extracted those)
        const hasComplexTable = row.querySelector('table.complex-panel-table');
        if (hasComplexTable) continue;
        
        const th = row.querySelector('th span, th');
        const td = row.querySelector('td.form-cell, td');
        
        if (th && td) {
          const question = (th.textContent || '').trim();
          let answer = (td.textContent || '').trim();
          
          // Remove "Change" links
          answer = answer.replace(/\s*Change\s*/gi, '').trim();
          
          // Skip if it's just "Property address" (we already extracted the nested fields)
          if (question && answer && !answer.match(/^Change$/i) && question !== 'Property address') {
            // Check for duplicates (case-sensitive)
            const exists = qaPairs.some(q => q.question === question);
            if (!exists) {
              qaPairs.push({ question, answer });
            }
          }
        }
      }
    }
    
    return qaPairs;
  }).catch(async () => {
    // Fallback to Playwright locators if evaluate fails
    console.log('[Address CYA] Using Playwright fallback extraction');
    const qaPairs: Array<QAPair> = [];
    
    // Extract from complex-panel-table (address fields)
    const complexTables = page.locator('table.complex-panel-table');
    const complexTableCount = await complexTables.count().catch(() => 0);
    console.log(`[Address CYA] Found ${complexTableCount} complex-panel-table(s) (fallback)`);
    
    for (let i = 0; i < complexTableCount; i++) {
      const table = complexTables.nth(i);
      const rows = table.locator('tr.complex-panel-simple-field');
      const rowCount = await rows.count().catch(() => 0);
      console.log(`[Address CYA] Found ${rowCount} complex-panel-simple-field rows (fallback)`);
      
      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        
        // Try multiple selectors for question
        let question = (await row.locator('th span.text-16').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        if (!question) {
          question = (await row.locator('th span').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        if (!question) {
          question = (await row.locator('th').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        
        // Try multiple selectors for answer
        let answer = '';
        
        // Try ccd-read-text-field first
        const textField = row.locator('ccd-read-text-field span.text-16').first();
        answer = (await textField.textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        
        // Fallback to any span.text-16 in td
        if (!answer) {
          const span = row.locator('td span.text-16').first();
          answer = (await span.textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        
        // Last resort: get all text from td
        if (!answer) {
          answer = (await row.locator('td').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        
        if (question && answer) {
          console.log(`[Address CYA] Extracted (fallback): "${question}" = "${answer}"`);
          qaPairs.push({ question, answer });
        } else {
          console.log(`[Address CYA] Skipped row (fallback) - question: "${question}", answer: "${answer}"`);
        }
      }
    }
    
    // Extract other questions from main form table
    const mainTables = page.locator('table.form-table');
    const mainTableCount = await mainTables.count().catch(() => 0);
    
    for (let i = 0; i < mainTableCount; i++) {
      const table = mainTables.nth(i);
      const rows = table.locator('tbody tr, tr');
      const rowCount = await rows.count().catch(() => 0);
      
      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        
        // Skip rows with nested complex tables
        const hasComplexTable = await row.locator('table.complex-panel-table').count().catch(() => 0);
        if (hasComplexTable > 0) continue;
        
        const question = (await row.locator('th span, th').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        const answer = (await row.locator('td.form-cell, td').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        
        if (question && answer && question !== 'Property address') {
          const cleanAnswer = answer.replace(/\s*Change\s*/gi, '').trim();
          if (cleanAnswer && !cleanAnswer.match(/^Change$/i)) {
            // Check for duplicates (case-sensitive)
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
  
  console.log(`🏠 [Address CYA] Extracted ${result.length} Q&A pairs from page`);
  if (result.length > 0) {
    console.log(`🏠 [Address CYA] Sample extracted pairs:`, result.slice(0, 3).map(q => `"${q.question}": "${q.answer}"`));
  }
  
  return result;
}

