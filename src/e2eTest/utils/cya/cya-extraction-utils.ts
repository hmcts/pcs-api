
import {Page} from '@playwright/test';

export interface QAPair {
  question: string;
  answer: string;
}

type CaseData = Record<string, string>;

export function normalizeWhitespace(text: string): string {
  return text.trim().replace(/\s+/g, ' ');
}

export async function extractCCDTable(
  page: Page,
  tableLocator: string
): Promise<Array<QAPair>> {
  const locator = page.locator(tableLocator).first();

  const caseData = await locator.evaluate((mainTable: HTMLTableElement) => {
    const cleanText = (text: string | null): string => {
      return text ? text.replace(/\s+/g, ' ').trim() : '';
    };

    const extractSimpleRow = (row: HTMLTableRowElement): CaseData | null => {
      const keyEl = row.querySelector('th');
      const valueEl = row.querySelector('td');
      if (!keyEl || !valueEl) return null;

      const key = cleanText(keyEl.innerText);
      
      const innerTable = valueEl.querySelector('table');
      const isComplexField = valueEl.querySelector('ccd-read-complex-field-table');

      let value = innerTable && !isComplexField
        ? Array.from(innerTable.querySelectorAll('td'))
            .map(cell => cleanText(cell.innerText))
            .filter(Boolean)
            .join(', ')
        : cleanText(valueEl.innerText);

      value = value.replace(/\s*Change\s*/gi, '').trim();
      
      // If no value, skip this row
      if (!value) return null;
      
      // If key is empty/whitespace but value exists, use space as key (for checkboxes)
      const finalKey = key || ' ';
      
      return { [finalKey]: value };
    };

    const extractTableData = (table: HTMLTableElement): CaseData => {
      const results: CaseData = {};
      const rows = Array.from(table.querySelectorAll(':scope > tbody > tr')) as HTMLTableRowElement[];

      for (const row of rows) {
        if (row.hidden || row.style.display === 'none') continue;

        const complexFieldTable = row.querySelector('ccd-read-complex-field-table > div > table, ccd-read-complex-field-table div table') as HTMLTableElement;
        if (complexFieldTable) {
          Object.assign(results, extractTableData(complexFieldTable));
        } else {
          const simpleData = extractSimpleRow(row);
          if (simpleData) Object.assign(results, simpleData);
        }
      }
      return results;
    };

    return extractTableData(mainTable);
  }).catch(() => ({}));

  return Object.entries(caseData)
    .map(([question, answer]) => ({ question, answer }));
}

export const extractSimpleQAFromPage = (page: Page): Promise<Array<QAPair>> =>
  extractCCDTable(page, 'table.form-table');
