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
 * Normalize whitespace (replace multiple spaces with single space)
 */
export function normalizeWhitespace(text: string): string {
  return text.trim().replace(/\s+/g, ' ');
}

/**
 * Extract Q&A pairs from a table using page.evaluate for better performance
 * This runs in a single browser context call to reduce Allure steps
 */
export async function extractCCDTable(
  page: Page,
  tableLocator: string
): Promise<Array<QAPairWithNested>> {
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
      let question = (th.querySelector('span.text-16')?.textContent || '').trim();
      if (!question) {
        question = (th.querySelector('span')?.textContent || '').trim();
      }
      if (!question) {
        question = (th.textContent || '').trim();
      }
      
      if (!question) continue;
      
      // Check for multi-select table first (treat as simple answer, not nested Q&A)
      let multiSelectTable = td.querySelector('table.multi-select-list-field-table');
      if (!multiSelectTable) {
        const multiSelectField = td.querySelector('ccd-read-multi-select-list-field');
        if (multiSelectField) {
          multiSelectTable = multiSelectField.querySelector('table.multi-select-list-field-table');
        }
      }
      
      if (multiSelectTable) {
        const msRows = multiSelectTable.querySelectorAll('tbody tr, tr');
        const msValues: string[] = [];
        msRows.forEach(msRow => {
          // Skip header rows (th with display:none)
          const th = msRow.querySelector('th');
          if (th) {
            const styleAttr = th.getAttribute('style');
            if (styleAttr && styleAttr.includes('display: none')) {
              return;
            }
            const thStyle = window.getComputedStyle(th);
            if (thStyle.display === 'none') {
              return;
            }
          }
          
          const msTd = msRow.querySelector('td');
          if (msTd) {
            let msValue = (msTd.querySelector('span.text-16')?.textContent || '').trim();
            if (!msValue) {
              msValue = (msTd.textContent || '').trim();
            }
            if (msValue) msValues.push(msValue);
          }
        });
        if (msValues.length > 0) {
          results.push({ question, answer: msValues.join(', ') });
          continue;
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
            // Extract nested question
            let nestedQ = (nestedTh.querySelector('span.text-16')?.textContent || '').trim();
            if (!nestedQ) {
              nestedQ = (nestedTh.querySelector('span')?.textContent || '').trim();
            }
            if (!nestedQ) {
              nestedQ = (nestedTh.textContent || '').trim();
            }
            
            // Extract nested answer
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
      
      // Try 1: span.text-16 (most common for simple fields)
      answer = (td.querySelector('span.text-16')?.textContent || '').trim();
      
      // Try 2: any span
      if (!answer) {
        answer = (td.querySelector('span')?.textContent || '').trim();
      }
      
      // Try 3: ccd-read-text-field
      if (!answer) {
        const textField = td.querySelector('ccd-read-text-field span, ccd-read-text-field');
        answer = (textField?.textContent || '').trim();
      }
      
      // Try 4: text area field
      if (!answer) {
        const textAreaSpan = td.querySelector('ccd-read-text-area-field span');
        answer = (textAreaSpan?.textContent || '').trim();
      }
      
      // Try 5: direct textContent (last resort, but skip if it's just "Change")
      if (!answer) {
        const rawText = (td.textContent || '').trim();
        answer = rawText.replace(/\s*Change\s*/gi, '').trim();
        if (answer.match(/^Change$/i)) {
          answer = '';
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
      
      // Extract question
      let question = (await row.locator('th span.text-16').innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
      if (!question) {
        question = (await row.locator('th span').innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
      }
      if (!question) {
        question = (await row.locator('th').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
      }
      
      if (!question) continue;
      
      // Extract answer
      let answer: string | Array<QAPair> = '';
      const td = row.locator('td');
      
      if (await td.count().catch(() => 0) > 0) {
        // Try simple text answer first
        let simpleText = (await td.locator('span.text-16').first().innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
        if (!simpleText) {
          simpleText = (await td.locator('span').first().innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
        }
        if (!simpleText) {
          simpleText = (await td.innerText({ timeout: 2000 }).catch(() => ''))?.trim() || '';
        }
        answer = simpleText;
        
        // Try multi-select list field
        if (!answer || answer.includes('Change')) {
          let multiSelectTable = td.locator('table.multi-select-list-field-table');
          let hasMultiSelect = await multiSelectTable.count().catch(() => 0);
          
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
              const th = msRow.locator('th');
              const thCount = await th.count().catch(() => 0);
              if (thCount > 0) {
                const thDisplay = await th.first().evaluate(el => window.getComputedStyle(el).display).catch(() => '');
                if (thDisplay === 'none') {
                  continue;
                }
              }
              
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
        
        // Try text area field
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
        
        // Try nested complex table
        const hasNestedTable = await td.locator('table:not(.multi-select-list-field-table)').count().catch(() => 0);
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
      
      if (question && (typeof answer === 'string' ? answer : answer.length > 0)) {
        results.push({ question, answer });
      }
    }

    return results;
  });
}

/**
 * Check if a question is an address field
 */
export function isAddressField(question: string): boolean {
  const addressFields = ['building', 'street', 'address line', 'town', 'city', 'postcode', 'country'];
  return addressFields.some(field => question.toLowerCase().includes(field));
}

/**
 * Simple extraction for Address CYA page
 * Address CYA structure:
 * - Main table with "Property address" row containing nested complex-panel-table
 * - Nested table has address fields: Building and Street, Address Line 2, etc.
 * - Optional: One additional question like "Is the property located in England or Wales?"
 */
export async function extractSimpleQAFromPage(page: Page): Promise<Array<QAPair>> {
  await page.locator('table.complex-panel-table').first().waitFor({ timeout: 5000 }).catch(() => {});
  
  const result = await page.evaluate(() => {
    const qaPairs: Array<QAPair> = [];
    
    // Extract address fields from nested complex-panel-table
    const complexTables = document.querySelectorAll('table.complex-panel-table');
    for (const table of Array.from(complexTables)) {
      const rows = table.querySelectorAll('tr.complex-panel-simple-field');
      for (const row of Array.from(rows)) {
        const th = row.querySelector('th');
        const thSpan = th?.querySelector('span.text-16, span');
        const question = (thSpan?.textContent || th?.textContent || '').trim();
        
        const td = row.querySelector('td');
        let answer = '';
        
        // Try ccd-read-text-field first
        const textField = td?.querySelector('ccd-read-text-field span.text-16, ccd-read-text-field span');
        if (textField) {
          answer = (textField.textContent || '').trim();
        }
        
        // Fallback to any span.text-16
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
          qaPairs.push({ question, answer });
        }
      }
    }
    
    // Extract other simple questions from main form table
    const mainTables = document.querySelectorAll('table.form-table');
    for (const table of Array.from(mainTables)) {
      const rows = table.querySelectorAll('tbody tr, tr');
      for (const row of Array.from(rows)) {
        // Skip rows that contain nested complex tables
        const hasComplexTable = row.querySelector('table.complex-panel-table');
        if (hasComplexTable) continue;
        
        const th = row.querySelector('th span, th');
        const td = row.querySelector('td.form-cell, td');
        
        if (th && td) {
          const question = (th.textContent || '').trim();
          let answer = (td.textContent || '').trim();
          answer = answer.replace(/\s*Change\s*/gi, '').trim();
          
          // Skip if it's just "Property address" (we already extracted the nested fields)
          if (question && answer && !answer.match(/^Change$/i) && question !== 'Property address') {
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
    const qaPairs: Array<QAPair> = [];
    
    const complexTables = page.locator('table.complex-panel-table');
    const complexTableCount = await complexTables.count().catch(() => 0);
    
    for (let i = 0; i < complexTableCount; i++) {
      const table = complexTables.nth(i);
      const rows = table.locator('tr.complex-panel-simple-field');
      const rowCount = await rows.count().catch(() => 0);
      
      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        
        let question = (await row.locator('th span.text-16').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        if (!question) {
          question = (await row.locator('th span').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        if (!question) {
          question = (await row.locator('th').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        
        let answer = '';
        const textField = row.locator('ccd-read-text-field span.text-16').first();
        answer = (await textField.textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        
        if (!answer) {
          const span = row.locator('td span.text-16').first();
          answer = (await span.textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        
        if (!answer) {
          answer = (await row.locator('td').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        }
        
        if (question && answer) {
          qaPairs.push({ question, answer });
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
        
        const hasComplexTable = await row.locator('table.complex-panel-table').count().catch(() => 0);
        if (hasComplexTable > 0) continue;
        
        const question = (await row.locator('th span, th').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        const answer = (await row.locator('td.form-cell, td').textContent({ timeout: 1000 }).catch(() => ''))?.trim() || '';
        
        if (question && answer && question !== 'Property address') {
          const cleanAnswer = answer.replace(/\s*Change\s*/gi, '').trim();
          if (cleanAnswer && !cleanAnswer.match(/^Change$/i)) {
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
  
  return result;
}
