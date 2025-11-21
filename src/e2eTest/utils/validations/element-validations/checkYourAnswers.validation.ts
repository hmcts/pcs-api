import { Page } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import { cyaData } from '@utils/data/cya-data';
import { CYAValidationBase } from './cya-validation-base';

export class CheckYourAnswersValidation extends CYAValidationBase implements IValidation {
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await this.validateQAPairs(page, cyaData.collectedQAPairs || [], 'Final CYA');
  }

  protected async findQuestionOnPage(page: Page, questionText: string): Promise<{
    found: boolean;
    question: string;
    answer: string;
  }> {
    // First, search in main table rows
    const mainTables = page.locator('table.form-table');
    const mainTableCount = await mainTables.count();

    for (let i = 0; i < mainTableCount; i++) {
      const table = mainTables.nth(i);
      const rows = table.locator('tr');
      const rowCount = await rows.count();

      for (let j = 0; j < rowCount; j++) {
        const row = rows.nth(j);
        const question = await row.locator('th').first().textContent({ timeout: 1000 }).catch(() => null);
        if (question && question.trim()) {
          if (this.match(question, questionText)) {
            const answerCell = row.locator('td.form-cell, td.case-field-content').first();
            
            // Try direct extraction from td using formLabelValue-like pattern
            const directValue = answerCell.locator('span.text-16:not(:has(ccd-field-read-label)):not(:has(a))').first();
            const directValueCount = await directValue.count();
            if (directValueCount > 0) {
              const value = await directValue.textContent({ timeout: 1000 }).catch(() => null);
              if (value && value.trim()) {
                return { found: true, question: question.trim(), answer: value.trim() };
              }
            }
            
            // Fallback to comprehensive cell extraction
            const answer = await this.extractAnswerFromCell(answerCell);
            return { found: true, question: question.trim(), answer: answer || '' };
          }
        }

        // Also search within nested complex field tables in this row (even if th is empty)
        const complexFieldTable = row.locator('ccd-read-complex-field-table table.complex-panel-table');
        const complexTableCount = await complexFieldTable.count();
        
        for (let k = 0; k < complexTableCount; k++) {
          const complexTable = complexFieldTable.nth(k);
          const complexRows = complexTable.locator('tr.complex-panel-simple-field');
          const complexRowCount = await complexRows.count();

          for (let l = 0; l < complexRowCount; l++) {
            const complexRow = complexRows.nth(l);
            const complexQuestionCell = complexRow.locator('th#complex-panel-simple-field-label');
            const complexQuestion = await complexQuestionCell.textContent({ timeout: 1000 }).catch(() => null);
            
            if (complexQuestion && complexQuestion.trim() && this.match(complexQuestion, questionText)) {
              // Extract answer from complex field row
              const complexAnswerCell = complexRow.locator('td');
              const complexAnswer = await this.extractAnswerFromComplexField(complexAnswerCell);
              return { found: true, question: complexQuestion.trim(), answer: complexAnswer || '' };
            }
          }
        }
      }
    }

    // Also search ALL complex field tables on the page (in case they're not in main table rows)
    const allComplexTables = page.locator('ccd-read-complex-field-table table.complex-panel-table');
    const allComplexTableCount = await allComplexTables.count();
    
    for (let i = 0; i < allComplexTableCount; i++) {
      const complexTable = allComplexTables.nth(i);
      const complexRows = complexTable.locator('tr.complex-panel-simple-field');
      const complexRowCount = await complexRows.count();

      for (let j = 0; j < complexRowCount; j++) {
        const complexRow = complexRows.nth(j);
        const complexQuestionCell = complexRow.locator('th#complex-panel-simple-field-label');
        const complexQuestion = await complexQuestionCell.textContent({ timeout: 1000 }).catch(() => null);
        
        if (complexQuestion && complexQuestion.trim() && this.match(complexQuestion, questionText)) {
          // Extract answer from complex field row
          const complexAnswerCell = complexRow.locator('td');
          const complexAnswer = await this.extractAnswerFromComplexField(complexAnswerCell);
          return { found: true, question: complexQuestion.trim(), answer: complexAnswer || '' };
        }
      }
    }

    return { found: false, question: questionText, answer: '' };
  }

  private async extractAnswerFromComplexField(cell: any): Promise<string | null> {
    // For complex field cells, extract from nested structure:
    // td > span.text-16 > ccd-field-read > ccd-read-fixed-radio-list-field > span.text-16
    
    // Strategy 1: Direct path to radio field answer
    const radioAnswer = cell.locator('span.text-16 ccd-read-fixed-radio-list-field span.text-16').first();
    const radioCount = await radioAnswer.count();
    if (radioCount > 0) {
      const value = await radioAnswer.textContent({ timeout: 1000 }).catch(() => null);
      if (value && value.trim()) return value.trim();
    }

    // Strategy 2: Text field answer
    const textAnswer = cell.locator('span.text-16 ccd-read-text-field span.text-16').first();
    const textCount = await textAnswer.count();
    if (textCount > 0) {
      const value = await textAnswer.textContent({ timeout: 1000 }).catch(() => null);
      if (value && value.trim()) return value.trim();
    }

    // Strategy 3: Multi-select field answer
    const multiSelectAnswer = cell.locator('span.text-16 ccd-read-multi-select-list-field').first();
    const multiCount = await multiSelectAnswer.count();
    if (multiCount > 0) {
      const value = await multiSelectAnswer.textContent({ timeout: 1000 }).catch(() => null);
      if (value && value.trim()) return value.trim();
    }

    // Strategy 4: Get text from nested span.text-16 within ccd-field-read
    const nestedText16 = cell.locator('span.text-16 ccd-field-read span.text-16').first();
    const nestedCount = await nestedText16.count();
    if (nestedCount > 0) {
      const text = await nestedText16.textContent({ timeout: 1000 }).catch(() => null);
      if (text && text.trim()) return text.trim();
    }

    // Strategy 5: Get text from any span.text-16 in the cell (deepest nested one)
    const allText16 = cell.locator('span.text-16');
    const allText16Count = await allText16.count();
    // Get the last one (usually the answer, not the wrapper)
    if (allText16Count > 1) {
      const lastSpan = allText16.nth(allText16Count - 1);
      const text = await lastSpan.textContent({ timeout: 1000 }).catch(() => null);
      if (text && text.trim() && !text.includes('Change')) return text.trim();
    } else if (allText16Count === 1) {
      const text = await allText16.first().textContent({ timeout: 1000 }).catch(() => null);
      if (text && text.trim() && !text.includes('Change')) return text.trim();
    }

    return null;
  }

  private async extractAnswerFromCell(cell: any): Promise<string | null> {
    // Strategy 1: Use formLabelValue pattern - td span.text-16 (most reliable for CYA pages)
    const text16Span = cell.locator('td span.text-16:not(:has(ccd-field-read-label)):not(:has(a)), span.text-16:not(:has(ccd-field-read-label)):not(:has(a))').first();
    const text16Count = await text16Span.count();
    if (text16Count > 0) {
      const text16Value = await text16Span.textContent({ timeout: 1000 }).catch(() => null);
      if (text16Value && text16Value.trim()) return text16Value.trim();
    }
    
    // Strategy 1b: Try all span.text-16 elements and get the first non-empty one
    const allText16 = cell.locator('span.text-16');
    const allText16Count = await allText16.count();
    for (let i = 0; i < allText16Count; i++) {
      const span = allText16.nth(i);
      const hasLink = await span.locator('a').count() > 0;
      if (!hasLink) {
        const text = await span.textContent({ timeout: 500 }).catch(() => null);
        if (text && text.trim() && !text.includes('Change')) {
          return text.trim();
        }
      }
    }

    // Strategy 2: For radio/checkbox fields - try multiple extraction strategies
    const radioField = cell.locator('ccd-read-fixed-radio-list-field, ccd-read-multi-select-list-field');
    const radioCount = await radioField.count();
    if (radioCount > 0) {
      // Get text content from the field component
      let radioText = await radioField.textContent({ timeout: 1000 }).catch(() => null);
      if (radioText && radioText.trim()) return radioText.trim();
      
      // Find selected radio button label
      const selectedRadio = radioField.locator('input[type="radio"]:checked + label, input[type="checkbox"]:checked + label');
      const selectedCount = await selectedRadio.count();
      if (selectedCount > 0) {
        radioText = await selectedRadio.first().textContent({ timeout: 1000 }).catch(() => null);
        if (radioText && radioText.trim()) return radioText.trim();
      }
      
      // Find any label within the radio field
      const label = radioField.locator('label').first();
      const labelCount = await label.count();
      if (labelCount > 0) {
        radioText = await label.textContent({ timeout: 1000 }).catch(() => null);
        if (radioText && radioText.trim()) return radioText.trim();
      }
      
      // Get inner text from span elements within radio field
      const span = radioField.locator('span.text-16, span').first();
      const spanCount = await span.count();
      if (spanCount > 0) {
        radioText = await span.textContent({ timeout: 1000 }).catch(() => null);
        if (radioText && radioText.trim()) return radioText.trim();
      }
    }

    // For text fields
    const textField = cell.locator('ccd-read-text-field, ccd-read-text-area-field');
    const textCount = await textField.count();
    if (textCount > 0) {
      const textValue = await textField.textContent({ timeout: 500 }).catch(() => null);
      if (textValue && textValue.trim()) return textValue.trim();
    }

    // For complex fields (like address), extract from nested structure
    const complexField = cell.locator('ccd-read-complex-field-table table.complex-panel-table');
    const complexCount = await complexField.count();
    if (complexCount > 0) {
      const parts: string[] = [];
      const complexRows = complexField.locator('tr');
      const complexRowCount = await complexRows.count();

      for (let i = 0; i < complexRowCount; i++) {
        const row = complexRows.nth(i);
        const label = await row.locator('th').textContent({ timeout: 500 }).catch(() => null);
        const value = await row.locator('td').textContent({ timeout: 500 }).catch(() => null);
        if (label && value && value.trim()) {
          parts.push(`${label.trim()}: ${value.trim()}`);
        }
      }

      if (parts.length > 0) {
        return parts.join(', ');
      }
    }

    // For money fields
    const moneyField = cell.locator('ccd-read-money-gbp-field');
    const moneyCount = await moneyField.count();
    if (moneyCount > 0) {
      const moneyValue = await moneyField.textContent({ timeout: 500 }).catch(() => null);
      if (moneyValue && moneyValue.trim()) return moneyValue.trim();
    }

    // For date fields
    const dateField = cell.locator('ccd-read-date-field');
    const dateCount = await dateField.count();
    if (dateCount > 0) {
      const dateValue = await dateField.textContent({ timeout: 500 }).catch(() => null);
      if (dateValue && dateValue.trim()) return dateValue.trim();
    }

    // For document fields
    const docField = cell.locator('ccd-read-document-field');
    const docCount = await docField.count();
    if (docCount > 0) {
      const docLinks = docField.locator('a');
      const docLinkCount = await docLinks.count();
      const docNames: string[] = [];
      for (let i = 0; i < docLinkCount; i++) {
        const docName = await docLinks.nth(i).textContent({ timeout: 500 }).catch(() => null);
        if (docName && docName.trim()) docNames.push(docName.trim());
      }
      if (docNames.length > 0) return docNames.join(', ');
    }

    // For email fields
    const emailField = cell.locator('ccd-read-email-field');
    const emailCount = await emailField.count();
    if (emailCount > 0) {
      const emailValue = await emailField.textContent({ timeout: 500 }).catch(() => null);
      if (emailValue && emailValue.trim()) return emailValue.trim();
    }

    // Strategy 3: Try all span.text-16 elements (excluding those with links)
    const allText16SpansNoLinks = cell.locator('span.text-16:not(:has(a))');
    const allText16SpansCount = await allText16SpansNoLinks.count();
    if (allText16SpansCount > 0) {
      const allTexts: string[] = [];
      for (let i = 0; i < allText16SpansCount; i++) {
        const spanText = await allText16SpansNoLinks.nth(i).textContent({ timeout: 500 }).catch(() => null);
        if (spanText && spanText.trim() && !spanText.includes('Change')) {
          allTexts.push(spanText.trim());
        }
      }
      if (allTexts.length > 0) {
        return allTexts.join(' ').trim();
      }
    }
    
    // Strategy 4: Get all text but exclude "Change" and links
    const allText = await cell.locator('*:not(a):not(button)').allTextContents().catch(() => []);
    if (allText.length > 0) {
      const combined = allText.join(' ').replace(/Change/gi, '').replace(/\s+/g, ' ').trim();
      if (combined) return combined;
    }
    
    // Strategy 5: Get all visible text nodes (most aggressive approach)
    // This gets ALL text content including from nested elements
    const allVisibleText = await cell.evaluate((el: HTMLElement) => {
      // Get all text nodes, excluding script and style tags
      const walker = document.createTreeWalker(
        el,
        NodeFilter.SHOW_TEXT,
        {
          acceptNode: (node: Node) => {
            const parent = node.parentElement;
            if (!parent) return NodeFilter.FILTER_REJECT;
            // Skip if parent is a link or button
            if (parent.tagName === 'A' || parent.tagName === 'BUTTON') {
              return NodeFilter.FILTER_REJECT;
            }
            // Skip if parent contains a link
            if (parent.querySelector('a')) {
              return NodeFilter.FILTER_REJECT;
            }
            return NodeFilter.FILTER_ACCEPT;
          }
        }
      );
      
      const texts: string[] = [];
      let node: Node | null;
      while ((node = walker.nextNode()) !== null) {
        const text = node.textContent?.trim();
        if (text && !text.toLowerCase().includes('change')) {
          texts.push(text);
        }
      }
      return texts.join(' ').trim();
    }).catch(() => null);
    
    if (allVisibleText && allVisibleText.trim()) {
      return allVisibleText.trim();
    }
    
    // Strategy 6: Final fallback - get direct text content from cell
    const cellText = await cell.textContent({ timeout: 1000 }).catch(() => null);
    if (cellText) {
      const cleaned = cellText.replace(/Change/gi, '').replace(/\s+/g, ' ').trim();
      if (cleaned) return cleaned;
    }

    return null;
  }
}
