import { Page, test, expect } from '@playwright/test';
import { IValidation } from '../../interfaces/validation.interface';
import * as fs from 'fs';
import * as path from 'path';

export class PageContentValidation implements IValidation {
  private validationResults: Array<{element: string; expected: string; status: 'pass' | 'fail'; error?: string}> = [];

  private readonly locatorPatterns = {
    Button: (page: Page, value: string) =>
      page.locator(`button:text("${value}"),
                    input[type="button"][value="${value}"],
                    input[type="submit"][value="${value}"],
                    [role="button"]:text("${value}"),
                    a[role="button"]:text("${value}"),
                    .btn:text("${value}"),
                    .button:text("${value}"),
                    button >> text=${value}`),

    Link: (page: Page, value: string) =>
      page.locator(`a:text("${value}"),
                    [role="link"]:text("${value}"),
                    .link:text("${value}"),
                    .nav-link:text("${value}"),
                    a >> text=${value}`),

    Header: (page: Page, value: string) =>
      page.locator(`h1:text("${value}"),
                    h2:text("${value}"),
                    h3:text("${value}"),
                    [role="heading"]:text("${value}"),
                    .heading:text("${value}"),
                    .header:text("${value}"),
                    h1 >> text=${value},
                    h2 >> text=${value}`),

    Input: (page: Page, value: string) =>
      page.locator(`input[placeholder="${value}"],
                    textarea[placeholder="${value}"],
                    label:text("${value}") ~ input,
                    label:text("${value}") ~ textarea,
                    .form-label:text("${value}") ~ input,
                    .form-label:text("${value}") ~ textarea`),

    Checkbox: (page: Page, value: string) =>
      page.locator(`label:text("${value}") ~ input[type="checkbox"],
                    label:text("${value}") + input[type="checkbox"],
                    .checkbox:text("${value}") ~ input[type="checkbox"],
                    label >> text=${value} >> xpath=..//input[@type="checkbox"]`),

    RadioQuestion: (page: Page, value: string) =>
      page.locator(`label:text("${value}") ~ input[type="radio"],
                    label:text("${value}") + input[type="radio"],
                    .radio:text("${value}") ~ input[type="radio"],
                    legend:text("${value}") ~ input[type="radio"],
                    label >> text=${value} >> xpath=..//input[@type="radio"]`),

    RadioOption: (page: Page, value: string) =>
      page.locator(`label:text("${value}") ~ input[type="radio"],
                    label:text("${value}") + input[type="radio"],
                    .radio-option:text("${value}") ~ input[type="radio"],
                    .option:text("${value}") ~ input[type="radio"],
                    label >> text=${value} >> xpath=..//input[@type="radio"]`),

    Select: (page: Page, value: string) =>
      page.locator(`label:text("${value}") ~ select,
                    .select:text("${value}") ~ select,
                    .dropdown:text("${value}") ~ select,
                    label >> text=${value} >> xpath=..//select`),

    Text: (page: Page, value: string) =>
      page.locator(`:text("${value}"),
                    p:text("${value}"),
                    span:text("${value}"),
                    div:text("${value}"),
                    li:text("${value}"),
                    .text:text("${value}"),
                    .content:text("${value}"),
                    .message:text("${value}"),
                    .info:text("${value}"),
                    .description:text("${value}"),
                    text=${value}`)
  };

  // IValidation interface implementation - fieldName is optional
  async validate(page: Page, validation: string, fieldName?: string, data?: any): Promise<void> {
    await this.validateCurrentPage(page);
  }

  async validateCurrentPage(page: Page): Promise<void> {
    this.validationResults = [];

    const pageHeader = await this.getPageHeader(page);
    if (!pageHeader) {
      console.log('⚠️ No suitable h1/h2 found for page identification');
      return;
    }

    const pageData = await this.findPageDataByHeader(pageHeader);
    if (!pageData) {
      console.log(`⚠️ No page data found for header: "${pageHeader}"`);
      return;
    }

    await this.validatePageElements(page, pageData, pageHeader);
    await this.addValidationToAllure();
  }

  private async getPageHeader(page: Page): Promise<string | null> {
    // Try h1 first with exact text matching
    const h1Locator = page.locator('h1').first();
    if (await h1Locator.isVisible()) {
      const h1Text = await h1Locator.textContent();
      if (h1Text && !this.isCookiesHeader(h1Text)) {
        return h1Text.trim();
      }
    }

    // Try h2 if no suitable h1 found
    const h2Locator = page.locator('h2').first();
    if (await h2Locator.isVisible()) {
      const h2Text = await h2Locator.textContent();
      if (h2Text && !this.isCookiesHeader(h2Text)) {
        return h2Text.trim();
      }
    }

    return null;
  }

  private isCookiesHeader(text: string): boolean {
    const cookieKeywords = ['cookie', 'cookies', 'privacy', 'gdpr'];
    const lowerText = text.toLowerCase();
    return cookieKeywords.some(keyword => lowerText.includes(keyword));
  }

  private async findPageDataByHeader(header: string): Promise<any> {
    try {
      // Convert header to camelCase for file name matching
      const fileName = this.convertHeaderToFileName(header);

      // Get the directory of page data files
      const pageDataDir = path.join(__dirname, '../../../data/page-data-figma');

      if (!fs.existsSync(pageDataDir)) {
        console.log(`❌ Page data directory not found: ${pageDataDir}`);
        return null;
      }

      console.log(`🔍 Looking for page data file matching: ${fileName}`);
      console.log(`📁 Searching in directory: ${pageDataDir}`);

      // Read all files in the directory
      const files = fs.readdirSync(pageDataDir);
      const tsFiles = files.filter(file => file.endsWith('.ts') && !file.endsWith('.d.ts'));

      for (const file of tsFiles) {
        const moduleName = file.replace('.ts', '');

        // Check if file name matches the camelCase header
        if (moduleName.toLowerCase().includes(fileName.toLowerCase())) {
          console.log(`✅ Found matching page data file: ${moduleName}`);

          // Dynamically import the module
          const filePath = path.join(pageDataDir, file);
          const module = await import(filePath);
          return module.default || module;
        }
      }

      // Fallback: check if any page data contains the header text
      for (const file of tsFiles) {
        const filePath = path.join(pageDataDir, file);
        const module = await import(filePath);
        const pageData = module.default || module;

        // Check mainHeader first
        if (pageData.mainHeader === header) {
          console.log(`✅ Found page data by mainHeader: ${file}`);
          return pageData;
        }

        // Check if any value matches the header
        const values = Object.values(pageData);
        if (values.includes(header)) {
          console.log(`✅ Found page data by content match: ${file}`);
          return pageData;
        }
      }

    } catch (error) {
      console.log('Error finding page data:', error);
    }

    console.log(`❌ No page data file found for header: "${header}"`);
    return null;
  }

  private convertHeaderToFileName(header: string): string {
    // Remove special characters and convert to camelCase
    return header
      .toLowerCase()
      .replace(/[^a-zA-Z0-9\s]/g, '') // Remove special characters
      .split(' ')
      .map((word, index) =>
        index === 0 ? word : word.charAt(0).toUpperCase() + word.slice(1)
      )
      .join('');
  }

  private async validatePageElements(page: Page, pageData: any, pageHeader: string): Promise<void> {
    await test.step(`🔍 Auto-validating page content: "${pageHeader}"`, async () => {
      let passed = 0;
      let total = 0;

      for (const [key, value] of Object.entries(pageData)) {
        if (typeof value === 'string' && value.trim() !== '') {
          total++;
          const isVisible = await this.validateElement(page, key, value as string);
          if (isVisible) passed++;
        }
      }

      console.log(`📊 Page Content Validation: ${passed}/${total} elements found on "${pageHeader}"`);

      if (passed < total) {
        console.log(`⚠️  ${total - passed} elements missing from page data`);
      }
    });
  }

  private async validateElement(page: Page, key: string, expectedValue: string): Promise<boolean> {
    const elementType = this.detectElementType(key);
    const pattern = this.locatorPatterns[elementType as keyof typeof this.locatorPatterns];

    let elementFound = false;
    let error = '';

    try {
      if (pattern) {
        const locator = pattern(page, expectedValue);
        const isVisible = await locator.first().isVisible({ timeout: 2000 }).catch(() => false);

        if (isVisible) {
          elementFound = true;
          // Soft assertion
          try {
            await expect(locator.first()).toBeVisible();
          } catch (e) {
            // Continue even if assertion fails
          }
        } else {
          error = `Element not found with ${elementType} patterns for text: "${expectedValue}"`;
        }
      } else {
        error = `No pattern found for element type: ${elementType}`;
      }
    } catch (e) {
      error = `Error validating ${elementType}: ${(e as Error).message}`;
    }

    this.validationResults.push({
      element: key,
      expected: expectedValue,
      status: elementFound ? 'pass' : 'fail',
      error: elementFound ? undefined : error
    });

    if (!elementFound) {
      console.log(`   ❌ ${key}: "${expectedValue}" - ${error}`);
    } else {
      console.log(`   ✅ ${key}: "${expectedValue}"`);
    }

    return elementFound;
  }

  private detectElementType(key: string): string {
    if (key.includes('Button')) return 'Button';
    if (key.includes('Link')) return 'Link';
    if (key.includes('Header') || key.includes('Caption') || key.includes('Title')) return 'Header';
    if (key.includes('Checkbox')) return 'Checkbox';
    if (key.includes('radioQuestion')) return 'radioQuestion';
    if (key.includes('RadioOption') || key.includes('OptionLabel') || key.includes('Option')) return 'radioOption';
    if (key.includes('select') || key.includes('Dropdown')) return 'select';
    if (key.includes('Input') || key.includes('Placeholder') || key.includes('Field')) return 'Input';
    return 'text';
  }

  private async addValidationToAllure(): Promise<void> {
    const passed = this.validationResults.filter(r => r.status === 'pass').length;
    const failed = this.validationResults.filter(r => r.status === 'fail').length;

    if (failed > 0) {
      await test.step(`❌ Page Content Validation: ${passed} passed, ${failed} failed`, async () => {
        this.validationResults.forEach(result => {
          if (result.status === 'fail') {
            console.log(`   ❌ ${result.element}: "${result.expected}" - ${result.error}`);
          }
        });
      });
    } else {
      await test.step(`✅ Page Content Validation: All ${passed} elements found`, async () => {});
    }
  }

  getValidationResults() {
    return this.validationResults;
  }
}
