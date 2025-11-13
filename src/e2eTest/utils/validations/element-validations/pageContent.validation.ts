import { Page } from '@playwright/test';
import { IValidation } from '../../interfaces/validation.interface';
import * as fs from 'fs';
import * as path from 'path';

const ELEMENT_TYPES = [
  'Button', 'Link', 'Header', 'Caption', 'Checkbox', 'Question',
  'RadioOption', 'SelectLabel', 'SelectOption', 'HintText',
  'TextLabel', 'Paragraph'
] as const;

type ValidationResult = { element: string; expected: string; status: 'pass' | 'fail' };

export class PageContentValidation implements IValidation {
  private static validationResults = new Map<string, ValidationResult[]>();
  private static validationExecuted = false;
  private static missingDataFiles = new Set<string>();
  private static testCounter = 0;

  private readonly locatorPatterns = {
    Button: (page: Page, value: string) => page.locator(`
                    button:text-is("${value}"),
                    [value="${value}"],
                    :has-text("${value}") + button,
                    :has-text("${value}") ~ button`),
    Link: (page: Page, value: string) => page.locator(`
                    a:text("${value}"),
                    a.govuk-link:text("${value}"),
                    button.govuk-js-link:text("${value}"),
                    [role="link"]:text("${value}"),
                    [aria-label*="${value}"]:text("${value}")`),
    Header: (page: Page, value: string) => page.locator(`
                    h1:text("${value}"),
                    h2:text("${value}"),
                    h3:text("${value}")`),
    Caption: (page: Page, value: string) => page.locator(`
                    caption:text("${value}"),
                    .caption:text("${value}"),
                    figcaption:text("${value}"),
                    .figcaption:text("${value}"),
                    span.govuk-caption-l:text("${value}"),
                    [aria-label*="${value}"]:text("${value}")`),
    Checkbox: (page: Page, value: string) => page.locator(`
                    label:text("${value}") ~ input[type="checkbox"],
                    label:text("${value}") + input[type="checkbox"],
                    .checkbox:text("${value}") ~ input[type="checkbox"],
                    label >> text=${value} >> xpath=..//input[@type="checkbox"]`),
    Question: (page: Page, value: string) => page.locator(`
                    label:text("${value}") ~ input[type="radio"],
                    label:text("${value}") + input[type="radio"],
                    .radio:text("${value}") ~ input[type="radio"],
                    legend:text("${value}") ~ input[type="radio"],
                    .question:text("${value}") ~ input[type="radio"],
                    label >> text=${value} >> xpath=..//input[@type="radio"]`),
    RadioOption: (page: Page, value: string) => page.locator(`
                    label:text("${value}") ~ input[type="radio"],
                    label:text("${value}") + input[type="radio"],
                    .radio-option:text("${value}") ~ input[type="radio"],
                    label >> text=${value} >> xpath=..//input[@type="radio"]`),
    SelectLabel: (page: Page, value: string) => page.locator(`
                    label:text("${value}") ~ select,
                    .select:text("${value}") ~ select,
                    .dropdown:text("${value}") ~ select,
                    .form-label:text("${value}") ~ select,
                    label >> text=${value} >> xpath=..//select`),
    SelectOption: (page: Page, value: string) => page.locator(`
                    option:text("${value}"),
                    select option:text("${value}"),
                    [role="option"]:text("${value}"),
                    option >> text=${value}`),
    HintText: (page: Page, value: string) => page.locator(`
                    .hint:text("${value}"),
                    .form-hint:text("${value}"),
                    .help-text:text("${value}"),
                    .helper-text:text("${value}"),
                    [role="tooltip"]:text("${value}"),
                    [data-hint="${value}"]:text("${value}"),
                    span.hint:text("${value}"),
                    div.hint:text("${value}")`),
    TextLabel: (page: Page, value: string) => page.locator(`
                    label:has-text("${value}"),
                    .label:has-text("${value}")`),
    Paragraph: (page: Page, value: string) => page.locator(`
                    p:text("${value}"),
                    li:text("${value}"),
                    .paragraph:text("${value}"),
                    .content:text("${value}"),
                    .body:text("${value}"),
                    .text-content:text("${value}"),
                    .govuk-body:text("${value}"),
                    .govuk-list:text("${value}"),
                    [data-paragraph]:text("${value}"),
                    [data-list-item]:text("${value}")`),
    Text: (page: Page, value: string) => page.locator(`:text("${value}")`)
  };

  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await this.validateCurrentPage(page);
  }

  async validateCurrentPage(page: Page): Promise<void> {
    const pageUrl = page.url();
    const pageResults: ValidationResult[] = [];
    const pageData = await this.findPageDataByUrl(page);

    if (!pageData) return;

    const failedElementsByType = new Map<string, string[]>();

    for (const [key, value] of Object.entries(pageData)) {
      if (key.includes('Input') || key.includes('Hidden')) continue;
      if (typeof value === 'string' && value.trim() !== '') {
        const elementType = this.detectElementType(key);
        const isVisible = await this.validateElement(page, value as string, elementType);

        pageResults.push({ element: key, expected: value as string, status: isVisible ? 'pass' : 'fail' });

        if (!isVisible) {
          const elements = failedElementsByType.get(elementType) || [];
          failedElementsByType.set(elementType, [...elements, value as string]);
        }
      }
    }

    PageContentValidation.validationResults.set(pageUrl, pageResults);
  }

  private async findPageDataByUrl(page: Page): Promise<any> {
    const cleanUrl = this.cleanUrlForLogging(page.url());
    const urlSegment = this.extractFinalUrlSegment(page.url());
    const fileName = await this.getFileNameFromMapping(urlSegment);

    if (!fileName) {
      PageContentValidation.missingDataFiles.add(cleanUrl);
      return null;
    }

    const pageData = await this.findPageDataFile(fileName);
    return pageData;
  }

  private cleanUrlForLogging(url: string): string {
    try {
      const urlObj = new URL(url);
      const pathname = urlObj.pathname;
      const segments = pathname.split('/').filter(Boolean);
      return segments[segments.length - 1] || 'home';
    } catch {
      const segments = url.split('/').filter(Boolean);
      return segments[segments.length - 1] || 'home';
    }
  }

  private async getFileNameFromMapping(urlSegment: string): Promise<string | null> {
    const mapping = await this.loadUrlMapping();
    return mapping[urlSegment] || null;
  }

  private async findPageDataFile(fileName: string): Promise<any> {
    const filePath = path.join(__dirname, '../../../data/page-data-figma', `${fileName}.page.data.ts`);
    if (!fs.existsSync(filePath)) return null;
    try {
      delete require.cache[require.resolve(filePath)];
      const module = require(filePath);
      return module.default || module[fileName] || module[Object.keys(module)[0]];
    } catch {
      return null;
    }
  }

  private extractFinalUrlSegment(url: string): string {
    const cleanUrl = this.cleanUrlForLogging(url);
    return cleanUrl;
  }

  private async loadUrlMapping(): Promise<Record<string, string>> {
    try {
      const mappingPath = path.join(__dirname, '../../../data/page-data-figma/urlToFileMapping.ts');
      if (!fs.existsSync(mappingPath)) return {};
      const mappingContent = fs.readFileSync(mappingPath, 'utf8');
      const match = mappingContent.match(/export default\s*({[\s\S]*?});/);
      if (!match) return {};
      const objectString = match[1].replace(/\s+/g, ' ').replace(/,\s*}/g, '}');
      return eval(`(${objectString})`);
    } catch {
      return {};
    }
  }

  private async validateElement(page: Page, expectedValue: string, elementType: string): Promise<boolean> {
    const pattern = this.locatorPatterns[elementType as keyof typeof this.locatorPatterns];
    if (!pattern) return false;
    try {
      const locator = pattern(page, expectedValue);
      return await locator.first().isVisible({ timeout: 5000 });
    } catch {
      return false;
    }
  }

  private detectElementType(key: string): string {
    for (const type of ELEMENT_TYPES) {
      if (key.includes(type)) return type;
    }
    return 'Text';
  }

  private async getPageNameFromUrl(url: string): Promise<string> {
    const urlSegment = this.extractFinalUrlSegment(url);
    const mapping = await this.loadUrlMapping();
    return mapping[urlSegment] || urlSegment;
  }

  static finalizeTest(): void {
    PageContentValidation.testCounter++;

    if (this.validationExecuted && this.validationResults.size === 0 && this.missingDataFiles.size === 0) {
      return;
    }

    this.validationExecuted = true;

    const allResults = Array.from(this.validationResults.entries());

    const hasValidationResults = this.validationResults.size > 0;
    const hasMissingFiles = this.missingDataFiles.size > 0;

    if (!hasValidationResults && !hasMissingFiles) {
      this.clearValidationResults();
      return;
    }

    const failedPages = new Map<string, Map<string, string[]>>();
    const passedPages = new Set<string>();
    const validatedPages = new Set<string>();

    for (const [pageUrl, results] of allResults) {
      const pageName = this.getPageNameFromUrlSync(pageUrl);
      validatedPages.add(pageName);

      const failedResults = results.filter(r => r.status === 'fail');

      if (failedResults.length === 0) {
        passedPages.add(pageName);
        continue;
      }

      const pageFailuresByType = new Map<string, string[]>();
      for (const result of failedResults) {
        const elementType = this.detectElementTypeFromKey(result.element);
        const elements = pageFailuresByType.get(elementType) || [];
        pageFailuresByType.set(elementType, [...elements, result.expected]);
      }
      failedPages.set(pageName, pageFailuresByType);
    }

    const totalValidated = validatedPages.size;
    const passedCount = passedPages.size;
    const failedCount = failedPages.size;
    const missingFilesCount = this.missingDataFiles.size;

    console.log(`\n📊 PAGE CONTENT VALIDATION SUMMARY (Test #${this.testCounter}):`);
    console.log(`   Total pages validated: ${totalValidated}`);
    console.log(`   Number of pages passed: ${passedCount}`);
    console.log(`   Number of pages failed: ${failedCount}`);
    console.log(`   Missing data files: ${missingFilesCount}`);

    if (passedCount > 0) {
      console.log(`   Passed pages: ${Array.from(passedPages).join(', ') || 'None'}`);
    }

    if (failedCount > 0) {
      console.log(`   Failed pages: ${Array.from(failedPages.keys()).join(', ') || 'None'}`);
    }

    if (missingFilesCount > 0) {
      console.log(`   Page files not found: ${Array.from(this.missingDataFiles).join(', ') || 'None'}`);
    }

    process.stdout.write('');

    if (failedPages.size > 0) {
      console.log(`\n❌ VALIDATION FAILED:\n`);

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

      process.stdout.write('');
      throw new Error(`Page content validation failed: ${failedPages.size} pages have missing elements`);
    } else if (totalValidated > 0) {
      console.log(`\n✅ VALIDATION PASSED: All intended pages validated successfully!`);
      process.stdout.write('');
    } else if (missingFilesCount > 0) {
      console.log(`\n⚠️  NO VALIDATION: Missing data files for all pages`);
      process.stdout.write('');
    }

    this.clearValidationResults();
  }

  private static getPageNameFromUrlSync(url: string): string {
    const segments = url.split('/').filter(Boolean);
    const urlSegment = segments[segments.length - 1] || 'home';

    try {
      const mappingPath = path.join(__dirname, '../../../data/page-data-figma/urlToFileMapping.ts');
      if (!fs.existsSync(mappingPath)) return urlSegment;
      const mappingContent = fs.readFileSync(mappingPath, 'utf8');
      const match = mappingContent.match(/export default\s*({[\s\S]*?});/);
      if (!match) return urlSegment;
      const objectString = match[1].replace(/\s+/g, ' ').replace(/,\s*}/g, '}');
      const mapping = eval(`(${objectString})`);
      return mapping[urlSegment] || urlSegment;
    } catch {
      return urlSegment;
    }
  }

  private static detectElementTypeFromKey(key: string): string {
    for (const type of ELEMENT_TYPES) {
      if (key.includes(type)) return type;
    }
    return 'Text';
  }

  static getValidationResults() {
    return this.validationResults;
  }

  static clearValidationResults(): void {
    this.validationResults.clear();
    this.missingDataFiles.clear();
    this.validationExecuted = false;
  }
}
