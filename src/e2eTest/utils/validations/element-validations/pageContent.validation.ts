import { Page } from '@playwright/test';
import { IValidation } from '@utils/interfaces';
import * as fs from 'fs';
import * as path from 'path';
import { cyaValidation, CYAStore } from '../custom-validations/CYA/cyaPage.validation';

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
  private static pageToFileNameMap = new Map<string, string>();
  private static pageToHeaderTextMap = new Map<string, string>();

  private readonly locatorPatterns = {
    Button: (page: Page, value: string) => page.locator(`
                    button:text-is("${value}"),
                    [value="${value}"],
                    :has-text("${value}") + button,
                    [role="link"]:text("${value}"),
                    a:text("${value}"),
                    :has-text("${value}") ~ button,
                    button:has-text("${value}"),
                    button >> text=${value}`),
    Link: (page: Page, value: string) => page.locator(`
                    a:text("${value}"),
                    a.govuk-link:text("${value}"),
                    button.govuk-js-link:text("${value}"),
                    [role="link"]:text("${value}"),
                    [aria-label*="${value}"]:text("${value}"),
                    summary>span:text("${value}")`),
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
    Question: (page: Page, value: string) => page.locator(`span:has-text("${value}")`)
      .or(page.locator(`label:text("${value}") ~ input[type="radio"]`))
      .or(page.locator(`label:text("${value}") + input[type="radio"]`))
      .or(page.locator(`.radio:text("${value}") ~ input[type="radio"]`))
      .or(page.locator(`legend:text("${value}") ~ input[type="radio"]`))
      .or(page.locator(`.question:text("${value}") ~ input[type="radio"]`))
      .or(page.locator(`label >> text=${value} >> xpath=..//input[@type="radio"]`)),
    RadioOption: (page: Page, value: string) => page.locator(`
                    label:text("${value}") ~ input[type="radio"],
                    label:text("${value}") + input[type="radio"],
                    .radio-option:text("${value}") ~ input[type="radio"],
                    label >> text=${value} >> xpath=..//input[@type="radio"]`),
    SelectLabel: (page: Page, value: string) => page.locator(`
                    label:text("${value}") ~ select,
                    .select:text("${value}") ~ select`),
    SelectOption: (page: Page, value: string) => page.locator(`
                    option:text("${value}"),
                    select option:text("${value}")`),
    HintText: (page: Page, value: string) => page.locator(`
                    .hint:text("${value}"),
                    span:text-is("${value}")`),
    TextLabel: (page: Page, value: string) => page.locator(`
                    label:has-text("${value}"),
                    .label:has-text("${value}"),
                    span:text-is("${value}")`),
    Paragraph: (page: Page, value: string) => page.locator(`
                     p:text("${value}"),
                     .paragraph:text("${value}"),
                     markdown:text("${value}"),
                    .content:text("${value}"),
                    .body:text("${value}"),
                    .text-content:text("${value}"),
                    .govuk-body:text("${value}"),
                    .govuk-list:text("${value}"),
                    span:text-is("${value}"),
                    dl:has-text("${value}") > dt,
                    strong:text("${value}")`),
    Text: (page: Page, value: string) => page.locator(`:text("${value}")`),
    Tab: (page: Page, value: string) => page.getByRole('tab', { name: value }),
  };

  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await this.validateCurrentPage(page);
  }

  async validateCurrentPage(page: Page): Promise<void> {
    await page.waitForLoadState('load');
    const pageUrl = page.url();

    if (PageContentValidation.isCYAPage(pageUrl)) {
      await cyaValidation.validateCYAPage(page);
      return;
    }

    const pageData = await this.getPageData(page);

    if (!pageData) return;

    const pageResults: ValidationResult[] = [];
    for (const [key, value] of Object.entries(pageData)) {
      if (key.includes('Input') || key.includes('Hidden') || key.includes('Dynamic') || key.includes('NewTab')) continue;
      if (typeof value === 'string' && value.trim() !== '') {
        const elementType = this.getElementType(key);
        const isVisible = await this.isElementVisible(page, value as string, elementType);
        pageResults.push({ element: key, expected: value as string, status: isVisible ? 'pass' : 'fail' });
      }
    }

    PageContentValidation.validationResults.set(pageUrl, pageResults);
  }

  private static isCYAPage(url: string): boolean {
    try {
      const urlObj = new URL(url);
      const segments = urlObj.pathname.split('/').filter(Boolean);
      const lastSegment = segments[segments.length - 1];
      return lastSegment === 'submit';
    } catch {
      const segments = url.split('/').filter(Boolean);
      const lastSegment = segments[segments.length - 1];
      return lastSegment === 'submit';
    }
  }

  private async getPageData(page: Page): Promise<any> {
    const urlSegment = this.getUrlSegment(page.url());
    const fileName = await this.getFileName(urlSegment, page);

    if (!fileName) {
      PageContentValidation.missingDataFiles.add(urlSegment);
      return null;
    }

    PageContentValidation.pageToFileNameMap.set(page.url(), fileName);

    return this.loadPageDataFile(fileName);
  }

  private getUrlSegment(url: string): string {
    try {
      const urlObj = new URL(url);
      const segments = urlObj.pathname.split('/').filter(Boolean);
      return segments[segments.length - 1] || 'home';
    } catch {
      const segments = url.split('/').filter(Boolean);
      return segments[segments.length - 1] || 'home';
    }
  }

  private async getFileName(urlSegment: string, page: Page): Promise<string | null> {
    try {
      const mappingPath = path.join(__dirname, '../../../data/page-data-figma/urlToFileMapping.ts');
      if (!fs.existsSync(mappingPath)) return null;
      const mappingContent = fs.readFileSync(mappingPath, 'utf8');
      const match = mappingContent.match(/export default\s*({[\s\S]*?});/);
      if (!match) return null;
      const objectString = match[1].replace(/\s+/g, ' ').replace(/,\s*}/g, '}');
      const mapping = eval(`(${objectString})`);

      if (/^\d+$/.test(urlSegment)) {
        const headerText = await this.getHeaderText(page);
        if (headerText && mapping[headerText]) {
          PageContentValidation.pageToHeaderTextMap.set(page.url(), headerText);
          return mapping[headerText];
        }
        return null;
      }

      return mapping[urlSegment] || null;
    } catch {
      return null;
    }
  }

  private async getHeaderText(page: Page): Promise<string | null> {
    try {
      const h1Element = page.locator('h1').first();
      if (await h1Element.isVisible({ timeout: 2000 })) {
        const h1Text = await h1Element.textContent();
        if (h1Text && h1Text.trim() !== '') {
          return h1Text.trim();
        }
      }

      const h2Element = page.locator('h2').first();
      if (await h2Element.isVisible({ timeout: 2000 })) {
        const h2Text = await h2Element.textContent();
        if (h2Text && h2Text.trim() !== '') {
          return h2Text.trim();
        }
      }

      return null;
    } catch {
      return null;
    }
  }

  private async loadPageDataFile(fileName: string): Promise<any> {
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

  private async isElementVisible(page: Page, expectedValue: string, elementType: string): Promise<boolean> {
    const pattern = this.locatorPatterns[elementType as keyof typeof this.locatorPatterns];
    if (!pattern) return false;
    try {
      const locator = pattern(page, expectedValue);
      return await locator.first().isVisible({ timeout: 5000 });
    } catch {
      return false;
    }
  }

  private getElementType(key: string): string {
    for (const type of ELEMENT_TYPES) {
      if (key.includes(type)) return type;
    }
    return 'Text';
  }

  static finaliseTest(): void {
    PageContentValidation.testCounter++;

    if (this.validationExecuted && this.validationResults.size === 0 && this.missingDataFiles.size === 0) {
      return;
    }

    this.validationExecuted = true;

    const failedPages = new Map<string, Map<string, string[]>>();
    const passedPages = new Set<string>();
    const validatedPages = new Set<string>();

    for (const [pageUrl, results] of Array.from(this.validationResults.entries())) {
      if (this.isCYAPage(pageUrl)) continue;

      const pageName = this.getPageNameForLogging(pageUrl);
      validatedPages.add(pageName);

      const failedResults = results.filter(r => r.status === 'fail');

      if (failedResults.length === 0) {
        passedPages.add(pageName);
        continue;
      }

      const pageFailuresByType = new Map<string, string[]>();
      for (const result of failedResults) {
        const elementType = this.getElementType(result.element);
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
    console.log(`   Total regular pages validated: ${totalValidated}`);
    console.log(`   Number of pages passed: ${passedCount}`);
    console.log(`   Number of pages failed: ${failedCount}`);
    console.log(`   Missing data files: ${missingFilesCount}`);

    if (passedCount > 0) console.log(`   Passed pages: ${Array.from(passedPages).join(', ') || 'None'}`);
    if (failedCount > 0) console.log(`   Failed pages: ${Array.from(failedPages.keys()).join(', ') || 'None'}`);
    if (missingFilesCount > 0) console.log(`   Page files not found: ${Array.from(this.missingDataFiles).join(', ') || 'None'}`);

    let shouldThrowError = false;
    let errorMessages: string[] = [];

    if (failedPages.size > 0) {
      shouldThrowError = true;
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
      errorMessages.push(`Page content validation failed: ${failedPages.size} pages have missing elements`);
    }

    const cyaFailed = cyaValidation.hasValidationFailed();
    if (cyaFailed) {
      shouldThrowError = true;
      errorMessages.push(`CYA page validation failed`);
    }

    if (totalValidated > 0 && failedPages.size === 0 && !cyaFailed) {
      console.log(`\n✅ VALIDATION PASSED: All intended pages validated successfully!`);
    } else if (missingFilesCount > 0 && totalValidated === 0) {
      console.log(`\n⚠️  NO VALIDATION: Missing data files for all pages`);
    }

    this.clearValidationResults();
    CYAStore.getInstance().clearAll();

    if (shouldThrowError) {
      throw new Error(errorMessages.join(' | '));
    }
  }

  private static getPageNameForLogging(url: string): string {
    const headerText = this.pageToHeaderTextMap.get(url);
    if (headerText) {
      return headerText.replace(/\s+/g, '');
    }

    const segments = url.split('/').filter(Boolean);
    const segment = segments[segments.length - 1] || 'home';

    try {
      const mappingPath = path.join(__dirname, '../../../data/page-data-figma/urlToFileMapping.ts');
      if (!fs.existsSync(mappingPath)) return segment;
      const mappingContent = fs.readFileSync(mappingPath, 'utf8');
      const match = mappingContent.match(/export default\s*({[\s\S]*?});/);
      if (!match) return segment;
      const objectString = match[1].replace(/\s+/g, ' ').replace(/,\s*}/g, '}');
      const mapping = eval(`(${objectString})`);
      return mapping[segment] || segment;
    } catch {
      return segment;
    }
  }

  private static getElementType(key: string): string {
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
    this.pageToFileNameMap.clear();
    this.pageToHeaderTextMap.clear();
    this.validationExecuted = false;
  }
}
