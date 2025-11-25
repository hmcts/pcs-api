/**
 * Shared utilities for CYA (Check Your Answers) validation
 * Contains common extraction and matching logic used by both Address and Final CYA validations
 */

import {Page} from '@playwright/test';

export interface QAPair {
  question: string;
  answer: string;
}

export type CaseData = Record<string, string>;

/**
 * Normalize whitespace (replace multiple spaces with single space)
 */
export function normalizeWhitespace(text: string): string {
  return text.trim().replace(/\s+/g, ' ');
}

/**
 * Extract Q&A pairs from a table using modular, recursive approach
 * Uses Playwright Locator.evaluate() for better type safety
 */
export async function extractCCDTable(
  page: Page,
  tableLocator: string
): Promise<Array<QAPair>> {
  const locator = page.locator(tableLocator).first();
  
  const caseData = await locator.evaluate((mainTable: HTMLTableElement) => {
    // Helper: Clean text logic
    const cleanText = (text: string | null): string => {
      return text ? text.replace(/\s+/g, ' ').trim() : '';
    };

    /**
     * Strategy: Extract a Simple Row
     * - Key: The <th> text
     * - Value: The <td> text OR, if the <td> contains a list table, the combined text of that table.
     */
    const extractSimpleRow = (row: HTMLTableRowElement): CaseData | null => {
      const keyEl = row.querySelector('th');
      const valueEl = row.querySelector('td'); // This gets the first td (the answer cell)

      if (!keyEl || !valueEl) return null;

      const key = cleanText(keyEl.innerText);
      if (!key) return null; // Skip empty rows

      let value = '';

      // Check if the Answer Cell contains an inner table (like Multi-select or Fixed-list)
      // BUT exclude Complex Fields (which are handled by the main loop)
      const innerTable = valueEl.querySelector('table');
      const isComplexField = valueEl.querySelector('ccd-read-complex-field-table');

      if (innerTable && !isComplexField) {
        // It's a list of values (e.g., Mandatory Grounds). Extract all cell texts.
        const cells = Array.from(innerTable.querySelectorAll('td'));
        value = cells.map(cell => cleanText(cell.innerText)).filter(Boolean).join(', ');
      } else {
        // Standard text extraction
        value = cleanText(valueEl.innerText);
      }

      // Clean "Change" links
      value = value.replace(/\s*Change\s*/gi, '').trim();
      if (value.match(/^Change$/i)) {
        value = '';
      }

      return value ? { [key]: value } : null;
    };

    /**
     * Strategy: Recursive Table Scraper
     */
    const scrapeTable = (table: HTMLTableElement): CaseData => {
      let results: CaseData = {};
      
      // Use :scope to ensure we only get the direct children rows of THIS table
      // This prevents the loop from accidentally grabbing rows from nested tables.
      const rows = Array.from(table.querySelectorAll(':scope > tbody > tr')) as HTMLTableRowElement[];

      for (const row of rows) {
        // Skip hidden rows
        if (row.hidden || row.style.display === 'none') continue;

        // 1. Check for Nested Complex Field (Questions inside Questions)
        // Handle both direct child and descendant selectors for flexibility
        const complexFieldTable = row.querySelector('ccd-read-complex-field-table > div > table, ccd-read-complex-field-table div table') as HTMLTableElement;

        if (complexFieldTable) {
          // Recurse: Go deeper
          const nestedData = scrapeTable(complexFieldTable);
          Object.assign(results, nestedData);
        } 
        // 2. Otherwise, treat as Simple Row (Key -> Value)
        else {
          const simpleData = extractSimpleRow(row);
          if (simpleData) {
            Object.assign(results, simpleData);
          }
        }
      }
      return results;
    };

    return scrapeTable(mainTable);
  }).catch(async () => {
    // Simplified fallback: return empty object if evaluate fails
    return {};
  });

  // Convert CaseData (Record) to Array<QAPair> format for compatibility
  return Object.entries(caseData)
    .map(([question, answer]) => ({ question, answer }))
    .filter(item => item.answer && !item.answer.match(/^Change$/i));
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
 * Reuses extractCCDTable with the main form table selector
 */
export async function extractSimpleQAFromPage(page: Page): Promise<Array<QAPair>> {
  return await extractCCDTable(page, 'table.form-table');
}
