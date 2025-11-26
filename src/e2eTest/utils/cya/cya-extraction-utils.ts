
import {Page} from '@playwright/test';

export interface QAPair { question: string; answer: string; }

type CaseData = Record<string, string>;

export function normalizeWhitespace(text: string): string {
  return text.trim().replace(/\s+/g, ' ');
}

export async function extractCCDTable( page: Page, tableLocator: string): Promise<Array<QAPair>> {
  const locator = page.locator(tableLocator).first();

  const caseData = await locator.evaluate((mainTable: HTMLTableElement) => {
    const cleanText = (text: string | null): string => {
      return text ? text.replace(/\s+/g, ' ').trim() : '';
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

    const extractSimpleRow = (row: HTMLTableRowElement): CaseData | null => {
      const question = row.querySelector('th');
      const answer = row.querySelector('td');
      if (!question || !answer) return null;

      const key = cleanText(question.innerText);

      const innerTable = answer.querySelector('table');
      const isComplexField = answer.querySelector('ccd-read-complex-field-table');

      let value = innerTable && !isComplexField
        ? Array.from(innerTable.querySelectorAll('td'))
          .map(cell => cleanText(cell.innerText))
          .filter(Boolean)
          .join(', ')
        : cleanText(answer.innerText);

      value = value.replace(/\s*Change\s*/gi, '').trim();

      if (!value) return null;

      const finalKey = key || ' ';

      return { [finalKey]: value };
    };

    return extractTableData(mainTable);
  }).catch(() => ({}));

  return Object.entries(caseData)
    .map(([question, answer]) => ({ question, answer }));
}
