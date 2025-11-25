/**
 * Shared utilities for CYA (Check Your Answers) validation
 * Contains common extraction and matching logic used by both Address and Final CYA validations
 */

import {Page} from '@playwright/test';

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

      // Try 4: direct textContent (last resort, but skip if it's just "Change")
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
    // Simplified fallback: return empty array if evaluate fails
    // The page.evaluate should work for simple journey CYA pages
    return [];
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

  return await page.evaluate(() => {
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
          qaPairs.push({question, answer});
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
              qaPairs.push({question, answer});
            }
          }
        }
      }
    }

    return qaPairs;
  }).catch(async () => {
    // Simplified fallback: return empty array if evaluate fails
    // The page.evaluate should work for simple journey Address CYA page
    return [];
  });
}
