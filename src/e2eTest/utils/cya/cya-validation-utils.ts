/**
 * Shared utilities for CYA (Check Your Answers) validation
 * Contains common extraction and matching logic used by both Address and Final CYA validations
 */

import {Page} from '@playwright/test';

export interface QAPair {
  question: string;
  answer: string;
}

// Internal type for extraction (not exported)
type CaseData = Record<string, string>;

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
      const valueEl = row.querySelector('td');
      if (!keyEl || !valueEl) return null;

      const key = cleanText(keyEl.innerText);
      if (!key) return null;

      const innerTable = valueEl.querySelector('table');
      const isComplexField = valueEl.querySelector('ccd-read-complex-field-table');
      
      let value = innerTable && !isComplexField
        ? Array.from(innerTable.querySelectorAll('td'))
            .map(cell => cleanText(cell.innerText))
            .filter(Boolean)
            .join(', ')
        : cleanText(valueEl.innerText);

      value = value.replace(/\s*Change\s*/gi, '').trim();
      return value ? { [key]: value } : null;
    };

    /**
     * Strategy: Recursive Table Scraper
     */
    const scrapeTable = (table: HTMLTableElement): CaseData => {
      const results: CaseData = {};
      const rows = Array.from(table.querySelectorAll(':scope > tbody > tr')) as HTMLTableRowElement[];

      for (const row of rows) {
        if (row.hidden || row.style.display === 'none') continue;

        const complexFieldTable = row.querySelector('ccd-read-complex-field-table > div > table, ccd-read-complex-field-table div table') as HTMLTableElement;
        if (complexFieldTable) {
          Object.assign(results, scrapeTable(complexFieldTable));
        } else {
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

  // Convert CaseData (Record) to Array<QAPair> format
  return Object.entries(caseData)
    .map(([question, answer]) => ({ question, answer }));
}

export const extractSimpleQAFromPage = (page: Page): Promise<Array<QAPair>> =>
  extractCCDTable(page, 'table.form-table');
