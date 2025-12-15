import { Page } from '@playwright/test';

interface QAObject {
  question: string;
  answer: string | string[];
}

export interface FormattingRule {
  originalQuestion?: string;
  alternativeQuestion?: string;
  answerFormatter?: (answer: string | string[]) => string | string[];
  answerPatterns?: string[];
}

export class CYAStore {
  private static instance: CYAStore;
  private qaObjects: QAObject[] = [];
  private ignoreQuestions: string[] = [];
  private formattingRules: FormattingRule[] = [];
  private validationFailed: boolean = false;
  private failedValidations: Array<{question: string, expected: string, actual: string}> = [];

  static getInstance(): CYAStore {
    if (!CYAStore.instance) {
      CYAStore.instance = new CYAStore();
    }
    return CYAStore.instance;
  }

  private constructor() {
    this.loadIgnoreQuestions();
    this.loadFormattingRules();
  }

  private loadIgnoreQuestions(): void {
    try {
      const ignoreModule = require('./cya-ignore');
      if (ignoreModule && ignoreModule.cyaIgnoreQuestions) {
        this.ignoreQuestions = ignoreModule.cyaIgnoreQuestions;
      }
    } catch (error) {
      this.ignoreQuestions = [];
    }
  }

  private loadFormattingRules(): void {
    try {
      const formattingModule = require('./cya-value-formatting');
      if (formattingModule && formattingModule.cyaFormattingRules) {
        this.formattingRules = formattingModule.cyaFormattingRules;
      }
    } catch (error) {
      this.formattingRules = [];
    }
  }

  setIgnoreQuestions(questions: string[]): void {
    this.ignoreQuestions = questions;
  }

  captureAnswer(action: string, fieldName: any, value?: any): void {
    let qaObject: QAObject | null = null;

    switch (action) {
      case 'clickRadioButton':
        if (typeof fieldName === 'object' && fieldName.question && fieldName.option) {
          qaObject = { question: fieldName.question, answer: fieldName.option };
        }
        break;
      case 'inputText':
        if (typeof fieldName === 'object' && fieldName.textLabel && typeof value === 'string') {
          qaObject = { question: fieldName.textLabel, answer: value };
        } else if (typeof fieldName === 'string' && typeof value === 'string') {
          qaObject = { question: fieldName, answer: value };
        }
        break;
      case 'check':
        if (Array.isArray(fieldName)) {
          qaObject = { question: 'Selected options', answer: fieldName };
        } else if (typeof fieldName === 'string') {
          qaObject = { question: fieldName, answer: 'Checked' };
        }
        break;
      case 'select':
        if (typeof fieldName === 'string' && typeof value === 'string') {
          qaObject = { question: fieldName, answer: value };
        }
        break;
      case 'uploadFile':
        if (typeof fieldName === 'string') {
          qaObject = { question: 'Uploaded file', answer: fieldName };
        } else if (Array.isArray(fieldName)) {
          qaObject = { question: 'Uploaded files', answer: fieldName };
        }
        break;
    }

    if (qaObject) this.qaObjects.push(qaObject);
  }

  getQAObjects(): QAObject[] {
    return [...this.qaObjects];
  }

  shouldIgnore(question: string): boolean {
    if (this.ignoreQuestions.length === 0) return false;

    const normalizedQuestion = this.normalizeText(question);

    for (const ignoreQuestion of this.ignoreQuestions) {
      const normalizedIgnore = this.normalizeText(ignoreQuestion);

      if (normalizedQuestion === normalizedIgnore) {
        return true;
      }

      if (normalizedQuestion.startsWith(normalizedIgnore + ' ') ||
          normalizedIgnore.startsWith(normalizedQuestion + ' ')) {
        return true;
      }

      const questionWords = normalizedQuestion.split(/\s+/);
      const ignoreWords = normalizedIgnore.split(/\s+/);

      if (ignoreWords.length === 1) {
        if (questionWords.includes(ignoreWords[0])) {
          return true;
        }
        if (normalizedQuestion.startsWith(ignoreWords[0] + ' ')) {
          return true;
        }
      }

      if (ignoreWords.length > 1) {
        const pattern = ignoreWords.join(' ');
        if (normalizedQuestion.includes(pattern)) {
          return true;
        }
      }
    }

    return false;
  }

  getFormattingRule(question: string): FormattingRule | null {
    const normalizedQuestion = this.normalizeText(question);

    for (const rule of this.formattingRules) {
      if (rule.originalQuestion) {
        const normalizedOriginal = this.normalizeText(rule.originalQuestion);
        if (normalizedQuestion === normalizedOriginal) {
          return rule;
        }
      }
    }

    return null;
  }

  markValidationFailed(question: string, expected: string, actual: string): void {
    this.validationFailed = true;
    this.failedValidations.push({ question, expected, actual });
  }

  hasValidationFailed(): boolean {
    return this.validationFailed;
  }

  getFailedValidations(): Array<{question: string, expected: string, actual: string}> {
    return [...this.failedValidations];
  }

  private normalizeText(text: string): string {
    return text
        .replace(/\s+/g, ' ')
        .trim()
        .toLowerCase()
        .replace(/[^\w\s]/g, '');
  }

  clearAll(): void {
    this.qaObjects = [];
    this.validationFailed = false;
    this.failedValidations = [];
  }
}

export class CYAPageValidation {
  private store = CYAStore.getInstance();
  private maxQuestionWidth = 60;
  private maxAnswerWidth = 60;

  async validateCYAPage(page: Page): Promise<void> {
    const savedQA = this.store.getQAObjects();
    if (savedQA.length === 0) return;

    const extractedQA = await this.extractCYATable(page);

    console.log('\n🔍 CYA Validation Results');
    const { passed, failed, ignored, unvalidatedQAs, ignoredQAs } = this.validateAndPrintResults(savedQA, extractedQA);

    if (ignoredQAs.length > 0) {
      console.log('\n⏭️  Skipped Validations');
      this.printTable('Skipped Q&As', ignoredQAs, false);
    }

    if (unvalidatedQAs.length > 0) {
      console.log('\n⚠️  Unvalidated Questions');
      this.printTable('Unvalidated Q&As', unvalidatedQAs, false);
    }

    console.log('\n📊 CYA Validation Summary');
    console.log('═'.repeat(50));
    console.log(`   Total Questions on CYA Page: ${extractedQA.length}`);
    console.log(`   Questions Captured for Validation: ${savedQA.length}`);
    console.log(`   └─ Passed: ${passed}`);
    console.log(`   └─ Failed: ${failed}`);
    console.log(`   └─ Skipped: ${ignored}`);
    console.log(`   Questions Not Validated (on CYA page): ${unvalidatedQAs.length}`);
    console.log('═'.repeat(50));

    if (failed > 0) {
      throw new Error(`CYA validation failed: ${failed} question(s) did not match`);
    }
  }

  private async extractCYATable(page: Page): Promise<QAObject[]> {
    const qaObjects: QAObject[] = [];

    const mainRows = page.locator('table.form-table tr:visible:not([hidden])');
    const rowCount = await mainRows.count();

    for (let i = 0; i < rowCount; i++) {
      const row = mainRows.nth(i);
      const questionCell = row.locator('th.case-field-label').first();
      const answerCell = row.locator('td.case-field-content').first();

      if (await questionCell.isVisible() && await answerCell.isVisible()) {
        const question = (await questionCell.textContent())?.trim() || '';
        if (question && !this.isStructuralElement(question)) {
          const answer = await this.extractCleanAnswer(answerCell);
          if (answer) qaObjects.push({ question, answer });
        }
      }
    }

    const complexPanels = page.locator('ccd-read-complex-field-table:visible');
    const panelCount = await complexPanels.count();

    for (let i = 0; i < panelCount; i++) {
      const panel = complexPanels.nth(i);
      const panelRows = panel.locator('tr.complex-panel-simple-field:visible');
      const fieldCount = await panelRows.count();

      for (let j = 0; j < fieldCount; j++) {
        const field = panelRows.nth(j);
        const questionCell = field.locator('th[id="complex-panel-simple-field-label"]').first();
        const answerCell = field.locator('td').first();

        if (await questionCell.isVisible() && await answerCell.isVisible()) {
          const question = (await questionCell.textContent())?.trim() || '';
          if (question && !this.isStructuralElement(question)) {
            const answer = await this.extractCleanAnswer(answerCell);
            if (answer) qaObjects.push({ question, answer });
          }
        }
      }
    }

    return qaObjects;
  }

  private async extractCleanAnswer(answerCell: any): Promise<string> {
    try {
      const answerElement = answerCell.locator('span.text-16, .text-16, button, a').first();
      if (await answerElement.isVisible()) {
        const text = await answerElement.textContent();
        return text?.trim() || '';
      }

      const allText = await answerCell.textContent();
      return allText?.trim() || '';
    } catch {
      return '';
    }
  }

  private isStructuralElement(text: string): boolean {
    const lowerText = text.toLowerCase();
    return lowerText.length < 2 ||
        /defendant.*name$|defendant.*address$|add additional|enter address|^change$/i.test(lowerText);
  }

  private validateAndPrintResults(savedQA: QAObject[], extractedQA: QAObject[]): {
    passed: number;
    failed: number;
    ignored: number;
    unvalidatedQAs: QAObject[];
    ignoredQAs: QAObject[];
  } {
    console.log('┌─────┬──────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────┬──────────────┐');
    console.log('│ No. │ Question                                                     │ Expected / Actual                                            │ Result       │');
    console.log('├─────┼──────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────┼──────────────┤');

    let passed = 0;
    let failed = 0;
    let ignored = 0;
    const validatedExtractedQuestions = new Set<string>();
    const ignoredQAs: QAObject[] = [];

    savedQA.forEach((saved, index) => {
      if (this.store.shouldIgnore(saved.question)) {
        ignored++;
        ignoredQAs.push(saved);

        const { pageAnswer } = this.findAnswerInExtractedQA(saved.question, extractedQA);
        const expected = this.answerToString(saved.answer);
        const actual = pageAnswer || 'NOT FOUND';

        const rowNumber = (index + 1).toString().padStart(3);
        const questionLines = this.wrapText(saved.question, this.maxQuestionWidth);
        const combinedLines = this.wrapText(`${expected} / ${actual}`, this.maxAnswerWidth);

        const maxLines = Math.max(questionLines.length, combinedLines.length);

        for (let i = 0; i < maxLines; i++) {
          const qLine = questionLines[i] || '';
          const cLine = combinedLines[i] || '';
          const statusLine = i === 0 ? '⏭️ SKIPPED  ' : '';

          if (i === 0) {
            console.log(`│ ${rowNumber} │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${cLine.padEnd(this.maxAnswerWidth)} │ ${statusLine.padEnd(12)} │`);
          } else {
            console.log(`│     │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${cLine.padEnd(this.maxAnswerWidth)} │ ${statusLine.padEnd(12)} │`);
          }
        }
        return;
      }

      const { pageAnswer, extractedQuestion } = this.findAnswerInExtractedQA(saved.question, extractedQA);

      if (extractedQuestion) {
        validatedExtractedQuestions.add(this.normalizeText(extractedQuestion));
      }

      const formattingRule = this.store.getFormattingRule(saved.question);
      let found = false;

      if (pageAnswer) {
        // Try standard comparison first
        found = this.compareAnswers(pageAnswer, saved.answer);

        // If not found and we have formatting rule, try formatted answer
        if (!found && formattingRule) {
          // Try with formatted answer
          if (formattingRule.answerFormatter) {
            const formattedAnswer = formattingRule.answerFormatter(saved.answer);
            found = this.compareAnswers(pageAnswer, formattedAnswer);
          }

          // Try with answer patterns
          if (!found && formattingRule.answerPatterns) {
            for (const pattern of formattingRule.answerPatterns) {
              if (this.compareAnswers(pageAnswer, pattern)) {
                found = true;
                break;
              }
            }
          }
        }
      }

      if (!found && pageAnswer) {
        const expected = this.answerToString(saved.answer);
        this.store.markValidationFailed(saved.question, expected, pageAnswer);
      }

      const expected = formattingRule && formattingRule.answerFormatter
          ? this.answerToString(formattingRule.answerFormatter(saved.answer))
          : this.answerToString(saved.answer);

      const actual = pageAnswer || 'NOT FOUND';
      const rowNumber = (index + 1).toString().padStart(3);
      const questionLines = this.wrapText(saved.question, this.maxQuestionWidth);
      const combinedLines = this.wrapText(`${expected} / ${actual}`, this.maxAnswerWidth);
      const status = found ? '✅ PASS' : '❌ FAIL';

      const maxLines = Math.max(questionLines.length, combinedLines.length);

      for (let i = 0; i < maxLines; i++) {
        const qLine = questionLines[i] || '';
        const cLine = combinedLines[i] || '';
        const statusLine = i === 0 ? status : '';

        if (i === 0) {
          console.log(`│ ${rowNumber} │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${cLine.padEnd(this.maxAnswerWidth)} │ ${statusLine.padEnd(12)} │`);
        } else {
          console.log(`│     │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${cLine.padEnd(this.maxAnswerWidth)} │ ${statusLine.padEnd(12)} │`);
        }
      }

      if (found) passed++;
      else failed++;
    });

    console.log('└─────┴──────────────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────┴───────────────┘');

    const unvalidatedQAs = extractedQA.filter(qa =>
        !validatedExtractedQuestions.has(this.normalizeText(qa.question))
    );

    return { passed, failed, ignored, unvalidatedQAs, ignoredQAs };
  }

  private findAnswerInExtractedQA(question: string, extractedQA: QAObject[]): {
    pageAnswer: string;
    extractedQuestion: string;
  } {
    const cleanQuestion = this.normalizeText(question);

    for (const qa of extractedQA) {
      const pageQuestion = this.normalizeText(qa.question);
      if (pageQuestion === cleanQuestion || pageQuestion.includes(cleanQuestion) || cleanQuestion.includes(pageQuestion)) {
        return { pageAnswer: qa.answer as string, extractedQuestion: qa.question };
      }
    }

    return { pageAnswer: '', extractedQuestion: '' };
  }

  private compareAnswers(pageAnswer: string, savedAnswer: string | string[]): boolean {
    const cleanPage = this.normalizeText(pageAnswer);

    if (Array.isArray(savedAnswer)) {
      return savedAnswer.every(ans => cleanPage.includes(this.normalizeText(ans)));
    } else {
      return cleanPage.includes(this.normalizeText(savedAnswer));
    }
  }

  private printTable(title: string, qaObjects: QAObject[], showStatus: boolean = false): void {
    if (qaObjects.length === 0) return;

    if (showStatus) {
      console.log('┌─────┬──────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────┬──────────────┐');
      console.log('│ No. │ Question                                                     │ Answer                                                   │ Result       │');
      console.log('├─────┼──────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────┼──────────────┤');
    } else {
      console.log('┌─────┬──────────────────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────┐');
      console.log('│ No. │ Question                                                     │ Answer                                                       │');
      console.log('├─────┼──────────────────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────┤');
    }

    qaObjects.forEach((qa, index) => {
      const rowNumber = (index + 1).toString().padStart(3);
      const questionLines = this.wrapText(qa.question, this.maxQuestionWidth);
      const answerLines = this.wrapText(this.answerToString(qa.answer), this.maxAnswerWidth);

      const maxLines = Math.max(questionLines.length, answerLines.length);

      for (let i = 0; i < maxLines; i++) {
        const qLine = questionLines[i] || '';
        const aLine = answerLines[i] || '';

        if (i === 0) {
          if (showStatus) {
            console.log(`│ ${rowNumber} │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${aLine.padEnd(this.maxAnswerWidth)} │ ${''.padEnd(12)} │`);
          } else {
            console.log(`│ ${rowNumber} │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${aLine.padEnd(this.maxAnswerWidth)} │`);
          }
        } else {
          if (showStatus) {
            console.log(`│     │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${aLine.padEnd(this.maxAnswerWidth)} │ ${''.padEnd(12)} │`);
          } else {
            console.log(`│     │ ${qLine.padEnd(this.maxQuestionWidth)} │ ${aLine.padEnd(this.maxAnswerWidth)} │`);
          }
        }
      }
    });

    if (showStatus) {
      console.log('└─────┴──────────────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────┴──────────────┘');
    } else {
      console.log('└─────┴──────────────────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────┘');
    }
  }

  private wrapText(text: string, maxWidth: number): string[] {
    if (text.length <= maxWidth) return [text];

    const words = text.split(' ');
    const lines: string[] = [];
    let currentLine = '';

    for (const word of words) {
      if ((currentLine + ' ' + word).length <= maxWidth) {
        currentLine = currentLine ? currentLine + ' ' + word : word;
      } else {
        if (currentLine) lines.push(currentLine);
        currentLine = word;
      }
    }

    if (currentLine) lines.push(currentLine);
    return lines;
  }

  private answerToString(answer: string | string[]): string {
    return Array.isArray(answer) ? answer.join(', ') : answer;
  }

  private normalizeText(text: string): string {
    return text.replace(/\s+/g, ' ').trim().toLowerCase();
  }

  getStore(): CYAStore {
    return this.store;
  }

  hasValidationFailed(): boolean {
    return this.store.hasValidationFailed();
  }

  getFailedValidations(): Array<{question: string, expected: string, actual: string}> {
    return this.store.getFailedValidations();
  }
}

export const cyaStore = CYAStore.getInstance();
export const cyaValidation = new CYAPageValidation();