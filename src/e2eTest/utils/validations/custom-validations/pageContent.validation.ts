import * as fs from 'fs';
import * as path from 'path';

import { Page } from '@playwright/test';
import { escapeForRegex, exactTextWithOptionalWhitespaceRegex } from '../../common/string.utils';
import { IValidation } from '../../interfaces';

const ELEMENT_TYPES = [
  'Button',
  'Link',
  'Header',
  'Caption',
  'Checkbox',
  'Question',
  'RadioOption',
  'SelectLabel',
  'SelectOption',
  'HintText',
  'TextLabel',
  'Paragraph',
  'List',
  'Summary',
] as const;

type ValidationResult = { element: string; expected: string; status: 'pass' | 'fail' };

export class PageContentValidation implements IValidation {
  private static validationResults = new Map<string, ValidationResult[]>();
  private static validationExecuted = false;
  private static pagesWithData = new Set<string>();
  private static pagesValidated = new Set<string>();
  private static missingDataFiles = new Set<string>();
  private static testCounter = 0;
  private static pageToHeaderTextMap = new Map<string, string>();

  private readonly locatorPatterns = {
    Button: (page: Page, value: string) =>
      page.locator(`
                    button:text-is("${value}"),
                    [value="${value}"],
                    [role="link"]:text-is("${value}"),
                    a:text-is("${value}"),
                    button:has(:text-is("${value}"))`),
    Link: (page: Page, value: string) =>
      page.locator(`
                    a:text-is("${value}"),
                    a.govuk-link:text-is("${value}"),
                    button.govuk-js-link:text-is("${value}"),
                    [role="link"]:text-is("${value}"),
                    [aria-label="${value}"]`),
    Summary: (page: Page, value: string) =>
      page.locator(`
                    summary:text-is("${value}"),
                    summary .govuk-details__summary-text:text-is("${value}")`),
    Header: (page: Page, value: string) =>
      page.getByRole('heading', { name: new RegExp(`^${escapeForRegex(value)}$`) }).or(
        page.locator(`
                    legend:text-is("${value}"),
                    h1:text-is("${value}"),
                    h2:text-is("${value}"),
                    h3:text-is("${value}")`)
      ),
    Caption: (page: Page, value: string) =>
      page.locator(`
                    caption:text-is("${value}"),
                    .caption:text-is("${value}"),
                    figcaption:text-is("${value}"),
                    .figcaption:text-is("${value}"),
                    span.govuk-caption-l:text-is("${value}"),
                    [aria-label="${value}"]`),
    Checkbox: (page: Page, value: string) =>
      page.getByRole('checkbox', { name: new RegExp(`^${escapeForRegex(value)}$`) }).or(
        page.locator(`
                    label:text-is("${value}") ~ input[type="checkbox"],
                    label:text-is("${value}") + input[type="checkbox"],
                    .checkbox:text-is("${value}") ~ input[type="checkbox"]`)
      ),
    Question: (page: Page, value: string) =>
      page.getByText(value, { exact: true }).or(
        page.locator(`
                    h1:text-is("${value}"),
                    legend:text-is("${value}"),
                    label:text-is("${value}") ~ input[type="radio"]`)
      ),
    RadioOption: (page: Page, value: string) =>
      page.getByRole('radio', { name: new RegExp(`^${escapeForRegex(value)}$`) }).or(
        page.locator(`
                    label:text-is("${value}") ~ input[type="radio"],
                    label:text-is("${value}") + input[type="radio"],
                    .radio-option:text-is("${value}") ~ input[type="radio"]`)
      ),
    SelectLabel: (page: Page, value: string) =>
      page.locator(`
                    label:text-is("${value}") ~ select,
                    .select:text-is("${value}") ~ select`),
    SelectOption: (page: Page, value: string) =>
      page.locator(`
                    option:text-is("${value}"),
                    select option:text-is("${value}")`),
    HintText: (page: Page, value: string) =>
      page.locator(`
                    .govuk-hint:text-is("${value}"),
                    div:text-is("${value}")`),
    TextLabel: (page: Page, value: string) =>
      page.locator(`
                    label:text-is("${value}"),
                    .label:text-is("${value}")`),
    Paragraph: (page: Page, value: string) =>
      page
        .getByText(value, { exact: true })
        .or(page.getByText(exactTextWithOptionalWhitespaceRegex(value)))
        .or(
          page.locator(`span:text-is("${value}"),
                    .paragraph:text-is("${value}"),
                    p:text-is("${value}"),
                    markdown:text-is("${value}"),
                    .govuk-caption-l:text-is("${value}"),
                    .body:text-is("${value}"),
                    .text-content:text-is("${value}"),
                    .govuk-body:text-is("${value}"),
                    .govuk-list:text-is("${value}")`)
        ),
    List: (page: Page, value: string) =>
      page.locator(`
                    li:text-is("${value}"),
                    ul li:text-is("${value}"),
                    ol li:text-is("${value}")`),
    Text: (page: Page, value: string) => page.locator(`:text-is("${value}")`),
    Tab: (page: Page, value: string) => page.getByRole('tab', { name: new RegExp(`^${escapeForRegex(value)}$`) }),
  };

  async validate(_page: Page, _validation: string, _fieldName?: string, _data?: never): Promise<void> {}

  async validateCurrentPage(page: Page, pageName: string): Promise<void> {
    await page.waitForLoadState('load');
    const pageUrl = page.url();
    const pageResults: ValidationResult[] = [];
    try {
      const pageData = await this.getPageData(pageName);

      if (!pageData) {
        return;
      }

      PageContentValidation.pagesValidated.add(pageName);

      for (const [key, value] of Object.entries(pageData)) {
        if (
          key.includes('Input') ||
          key.includes('Hidden') ||
          key.includes('Validation') ||
          key.includes('pageSlug') ||
          key.includes('ErrorMessage')
        ) {
          continue;
        }
        if (typeof value === 'string' && value.trim() !== '') {
          const elementType = this.getElementType(key);
          const isVisible = await this.isElementVisible(page, value as string, elementType);
          pageResults.push({ element: key, expected: value as string, status: isVisible ? 'pass' : 'fail' });
        }
      }

      PageContentValidation.validationResults.set(pageUrl, pageResults);
    } catch (error) {
      // Add the error as a validation failure
      pageResults.push({
        element: 'SectionData',
        expected: error instanceof Error ? error.message : String(error),
        status: 'fail',
      });
      PageContentValidation.pagesValidated.add(pageName);
      PageContentValidation.validationResults.set(pageUrl, pageResults);
    }
  }

  private async getPageData(pageName: string): Promise<object | null> {
    return this.loadPageDataFile(pageName);
  }

  private loadPageDataFile(fileName: string): object | null {
    const filePath = path.join(__dirname, '../../../data/page-data', `${fileName}.page.data.ts`);
    if (!fs.existsSync(filePath)) {
      return null;
    }
    try {
      delete require.cache[require.resolve(filePath)];
      const module = require(filePath);
      return module.default || module[fileName] || module[Object.keys(module)[0]];
    } catch {
      return null;
    }
  }

  private async isElementVisible(page: Page, expectedValue: string, elementType: string): Promise<boolean> {
    const pattern = this.locatorPatterns[elementType as keyof typeof this.locatorPatterns];
    if (!pattern) {
      return false;
    }
    try {
      const locator = pattern(page, expectedValue);
      return await locator.filter({ visible: true }).first().isVisible({ timeout: 5000 });
    } catch {
      return false;
    }
  }

  private getElementType(key: string): string {
    for (const type of ELEMENT_TYPES) {
      if (key.includes(type)) {
        return type;
      }
    }
    return 'Text';
  }

  static trackPageWithData(pageName: string): void {
    PageContentValidation.pagesWithData.add(pageName);
  }

  static trackMissingDataFile(pageName: string): void {
    PageContentValidation.missingDataFiles.add(pageName);
  }

  static trackValidationError(pageName: string, _error?: unknown): void {
    PageContentValidation.missingDataFiles.add(pageName);
  }

  static finaliseTest(): void {
    PageContentValidation.testCounter++;

    if (
      PageContentValidation.validationExecuted &&
      PageContentValidation.validationResults.size === 0 &&
      PageContentValidation.missingDataFiles.size === 0
    ) {
      return;
    }

    PageContentValidation.validationExecuted = true;

    const failedPages = new Map<string, Map<string, string[]>>();
    const passedPages = new Set<string>();

    const pageToResults = new Map<string, ValidationResult[]>();

    for (const [pageUrl, results] of Array.from(PageContentValidation.validationResults.entries())) {
      let pageName = 'unknown';
      const urlParts = pageUrl.split('/');
      if (urlParts.length > 0) {
        pageName = urlParts[urlParts.length - 1] || 'home';
      }
      pageToResults.set(pageName, results);
    }

    for (const [pageName, results] of Array.from(pageToResults.entries())) {
      const failedResults = results.filter(r => r.status === 'fail');

      if (failedResults.length === 0) {
        passedPages.add(pageName);
        continue;
      }

      const pageFailuresByType = new Map<string, string[]>();
      for (const result of failedResults) {
        const elementType = PageContentValidation.getElementTypeStatic(result.element);
        const elements = pageFailuresByType.get(elementType) || [];
        pageFailuresByType.set(elementType, [...elements, result.expected]);
      }
      failedPages.set(pageName, pageFailuresByType);
    }

    const totalValidated = PageContentValidation.pagesValidated.size;
    const passedCount = passedPages.size;
    const failedCount = failedPages.size;
    const missingFilesCount = PageContentValidation.missingDataFiles.size;

    console.log(`\n📊 PAGE CONTENT VALIDATION SUMMARY (Test #${PageContentValidation.testCounter}):`);
    console.log(`   Total pages validated: ${totalValidated}`);
    console.log(`   Number of pages passed: ${passedCount}`);
    console.log(`   Number of pages failed: ${failedCount}`);
    console.log(`   Missing data files: ${missingFilesCount}`);

    if (passedCount > 0) {
      console.log(`   Passed pages: ${Array.from(passedPages).join(', ')}`);
    }

    if (failedCount > 0) {
      console.log(`   Failed pages: ${Array.from(failedPages.keys()).join(', ')}`);
    }

    if (missingFilesCount > 0) {
      console.log(`   Page files not found: ${Array.from(PageContentValidation.missingDataFiles).join(', ')}`);
    }

    if (failedPages.size > 0) {
      console.log('\n❌ VALIDATION FAILED:');
      for (const [pageName, pageFailures] of failedPages) {
        console.log(`   Page: ${pageName}`);
        let pageFailureCount = 0;
        for (const [elementType, elements] of pageFailures) {
          pageFailureCount += elements.length;
          console.log(`     ${elementType} elements (${elements.length}):`);
          elements.forEach(element => console.log(`       - ${element}`));
        }
        console.log(`     Total missing on this page: ${pageFailureCount}\n`);
      }
    } else if (totalValidated > 0) {
      console.log('\n✅ VALIDATION PASSED: All intended pages validated successfully!\n');
    } else if (missingFilesCount > 0) {
      console.log('\n⚠️  NO VALIDATION: Missing data files for all pages\n');
    }

    PageContentValidation.clearValidationResults();

    // Throw after clearing so failures don't cascade into subsequent tests
    if (failedPages.size > 0) {
      throw new Error(`Page content validation failed: ${failedPages.size} pages have missing elements`);
    }
  }

  private static getElementTypeStatic(key: string): string {
    for (const type of ELEMENT_TYPES) {
      if (key.includes(type)) {
        return type;
      }
    }
    return 'Text';
  }

  static clearValidationResults(): void {
    PageContentValidation.validationResults.clear();
    PageContentValidation.missingDataFiles.clear();
    PageContentValidation.pagesWithData.clear();
    PageContentValidation.pagesValidated.clear();
    PageContentValidation.pageToHeaderTextMap.clear();
    PageContentValidation.validationExecuted = false;
  }
}
