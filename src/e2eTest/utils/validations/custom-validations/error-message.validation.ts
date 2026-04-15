import * as fs from 'fs';
import * as path from 'path';

import { Page } from '@playwright/test';

import { IValidation, validationData, validationRecord } from '../../interfaces';

type ValidationResult = {
  pageUrl: string;
  pageName: string;
  scenario: string;
  passed: boolean;
  expected: string;
  actual?: string;
  error?: string;
};

export class ErrorMessageValidation implements IValidation {
  private static results: ValidationResult[] = [];
  private static testCounter = 0;
  private static pagesWithEMV = new Set<string>();
  private static pagesPassed = new Set<string>();
  private static missingEMVFiles = new Set<string>();
  private static shouldThrowError = true;
  private static emvFailed = false;
  private static readonly MAPPING_PATH = path.join(__dirname, '../../../data/page-data-figma/urlToFileMapping.ts');

  async validate(
    page: Page,
    validation: string,
    fieldName: string,
    error?: validationData | validationRecord
  ): Promise<void> {
    if (validation !== 'errorMessage' || !error) {
      return;
    }

    const scenario = 'Error message validation';
    let expected = '';
    let actualText: string | null = null;
    let passed = false;

    try {
      // Handle string error message
      if (typeof error === 'string' || typeof error === 'number' || typeof error === 'boolean') {
        const errorStr = String(error);
        expected = errorStr;

        // Try different error message selectors
        const errorMessage = page.locator(`
          .govuk-error-message:has-text("${errorStr}"),
          .govuk-list--error:has-text("${errorStr}"),
          a.validation-error:has-text("${errorStr}")
        `);

        const count = await errorMessage.count();
        if (count > 0) {
          actualText = await errorMessage.first().textContent();
          passed = actualText?.includes(errorStr) || false;
        } else {
          actualText = 'No error message found';
          passed = false;
        }
      }
      // Handle error record with header and message
      else if (typeof error === 'object') {
        const errorRecord = error as validationRecord;
        const header = String(errorRecord.header || '');
        const message = String(errorRecord.message || '');

        expected = `${header}: ${message}`;

        // Check for error summary header
        const headerLocator = page.locator(`
          h2.govuk-error-summary__title:has-text("${header}"),
          h2#error-summary-title:has-text("${header}"),
          h3.error-summary-heading:has-text("${header}")
        `);
        await headerLocator.waitFor({ state: 'visible', timeout: 5000 });
        const headerCount = await headerLocator.count();

        if (headerCount === 0) {
          actualText = `Header "${header}" not found`;
          passed = false;
        } else {
          // Check for error message in the list
          const messageLocator = page.locator(`
            .govuk-error-summary__list a:has-text("${message}"),
            .govuk-error-summary__list li:has-text("${message}"),
            ul.govuk-list--error li:has-text("${message}"),
            #errors li:has-text("${message}")
          `);

          const messageCount = await messageLocator.count();
          if (messageCount > 0) {
            actualText = await messageLocator.first().textContent();
            passed = actualText?.includes(message) || false;
          } else {
            actualText = `Message "${message}" not found in error summary`;
            passed = false;
          }
        }
      } else {
        return;
      }
    } catch (e) {
      actualText = `Error during validation: ${e instanceof Error ? e.message : String(e)}`;
      passed = false;
    }

    const pageUrl = page.url();
    const pageName = await ErrorMessageValidation.getPageNameFromUrl(pageUrl, page);

    ErrorMessageValidation.results.push({
      pageUrl,
      pageName,
      scenario,
      passed,
      expected,
      actual: actualText || 'Not found',
      error: passed ? undefined : `Expected "${expected}" but found "${actualText || 'nothing'}"`,
    });

    if (!passed) {
      ErrorMessageValidation.emvFailed = true;
    }

    if (passed) {
      ErrorMessageValidation.pagesPassed.add(pageName);
    }
  }

  static trackPageWithEMV(pageName: string): void {
    ErrorMessageValidation.pagesWithEMV.add(pageName);
  }

  static trackPagePassed(pageName: string): void {
    ErrorMessageValidation.pagesPassed.add(pageName);
  }

  static trackMissingEMVFile(pageName: string): void {
    ErrorMessageValidation.missingEMVFiles.add(pageName);
  }

  static trackValidationError(pageName: string, error?: unknown): void {
    const errorMessage = error instanceof Error ? error.message : String(error);
    ErrorMessageValidation.results.push({
      pageUrl: '',
      pageName,
      scenario: 'Error message validation',
      passed: false,
      expected: 'Error message validation should execute successfully',
      actual: errorMessage,
      error: errorMessage,
    });
    ErrorMessageValidation.emvFailed = true;
  }

  private static async getPageNameFromUrl(url: string, page?: Page): Promise<string> {
    try {
      const { pathname } = new URL(url);
      const segments = pathname.split('/').filter(Boolean);
      const urlSegment = segments.at(-1) || 'home';

      const mapping = ErrorMessageValidation.loadMapping();
      if (!mapping) {
        return urlSegment;
      }

      if (/^\d+$/.test(urlSegment) && page) {
        const headerText = await ErrorMessageValidation.getHeaderText(page);
        if (headerText) {
          for (const value of Object.values(mapping)) {
            if (value === headerText.replace(/\s+/g, '')) {
              return value;
            }
          }
          return headerText.replace(/\s+/g, '');
        }
      }

      return mapping[urlSegment] || urlSegment;
    } catch {
      const segments = url.split('/').filter(Boolean);
      return segments.at(-1) || 'home';
    }
  }

  private static loadMapping(): Record<string, string> | null {
    try {
      if (!fs.existsSync(ErrorMessageValidation.MAPPING_PATH)) {
        return null;
      }

      const mappingContent = fs.readFileSync(ErrorMessageValidation.MAPPING_PATH, 'utf8');
      const match = mappingContent.match(/export default\s*({[\s\S]*?});/);

      if (!match) {
        return null;
      }

      const objectString = match[1].replace(/\s+/g, ' ').replace(/,\s*}/g, '}');
      return eval(`(${objectString})`);
    } catch {
      return null;
    }
  }

  private static async getHeaderText(page: Page): Promise<string | null> {
    try {
      const h1Element = page.locator('h1').first();
      if (await h1Element.isVisible({ timeout: 2000 }).catch(() => false)) {
        const h1Text = await h1Element.textContent();
        if (h1Text && h1Text.trim() !== '') {
          return h1Text.trim();
        }
      }

      const h2Element = page.locator('h2').first();
      if (await h2Element.isVisible({ timeout: 2000 }).catch(() => false)) {
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

  static finaliseTest(): void {
    ErrorMessageValidation.testCounter++;

    const totalPages = ErrorMessageValidation.pagesWithEMV.size + ErrorMessageValidation.missingEMVFiles.size;

    if (totalPages === 0) {
      console.log(`\n📊 ERROR MESSAGE VALIDATION SUMMARY (Test #${ErrorMessageValidation.testCounter}):`);
      console.log('   No pages checked for error message validation');
      return;
    }

    // Track failures
    const failureDetails = new Map<string, { expected: string; actual: string }>();
    const failedPages = new Set<string>();
    const passedPages = new Set<string>();

    for (const result of ErrorMessageValidation.results) {
      if (!result.passed) {
        failedPages.add(result.pageName);
        if (result.expected && result.actual) {
          failureDetails.set(result.pageName, {
            expected: result.expected,
            actual: result.actual,
          });
        }
      } else {
        passedPages.add(result.pageName);
      }
    }

    // Remove any passed pages that also failed
    for (const pageName of failedPages) {
      passedPages.delete(pageName);
    }

    console.log(`\n📊 ERROR MESSAGE VALIDATION SUMMARY (Test #${ErrorMessageValidation.testCounter}):`);
    console.log(`   Total pages validated for error messages: ${totalPages}`);
    console.log(`   Number of pages passed: ${passedPages.size}`);
    console.log(`   Number of pages failed: ${failedPages.size}`);
    console.log(`   Number of missing EMV methods: ${ErrorMessageValidation.missingEMVFiles.size}`);

    if (passedPages.size > 0) {
      console.log(`   Passed pages: ${Array.from(passedPages).join(', ')}`);
    }

    if (failedPages.size > 0) {
      console.log(`   Failed pages: ${Array.from(failedPages).join(', ')}`);
    }

    if (ErrorMessageValidation.missingEMVFiles.size > 0) {
      console.log(`   EMV methods not found: ${Array.from(ErrorMessageValidation.missingEMVFiles).join(', ')}`);
    }

    // Show failure details
    if (failedPages.size > 0) {
      console.log('\n❌ FAILED ERROR MESSAGE VALIDATIONS:');

      for (const pageName of Array.from(failedPages).sort()) {
        const details = failureDetails.get(pageName);
        console.log(`   Page: ${pageName}`);
        if (details) {
          let errorMessage = details.actual;
          if (errorMessage.includes('Header') && errorMessage.includes('not found')) {
            errorMessage = `"${details.expected.split(':')[0]}" header not found`;
          } else if (errorMessage.includes('Message') && errorMessage.includes('not found')) {
            errorMessage = `"${details.expected.split(':')[1]?.trim() || details.expected}" message not found`;
          } else if (errorMessage === 'No error message found') {
            errorMessage = `"${details.expected}" error message not found`;
          }
          console.log(`       Error: ${errorMessage}`);
        }
        console.log('');
      }
    }

    const hasFailures = failedPages.size > 0 || ErrorMessageValidation.emvFailed;

    if (hasFailures) {
      console.log('❌ ERROR MESSAGE VALIDATIONS FAILED\n');
    } else if (passedPages.size > 0) {
      console.log('\n✅ ALL ERROR MESSAGE VALIDATIONS PASSED\n');
    } else if (ErrorMessageValidation.pagesWithEMV.size > 0) {
      console.log('\n⚠️  EMV methods found but no validations performed\n');
    }

    // Collect errors to throw with full error message
    const errors: string[] = [];

    for (const [pageName, details] of failureDetails) {
      // Use the actual error message (which contains the full stack trace)
      const errorMessage = details.actual;
      // Don't format the error message - keep it as is to show the full error
      errors.push(`${pageName}: ${errorMessage}`);
    }

    const shouldThrow =
      (failedPages.size > 0 || ErrorMessageValidation.emvFailed) && ErrorMessageValidation.shouldThrowError;

    ErrorMessageValidation.clearResults();

    if (shouldThrow && errors.length > 0) {
      throw new Error(`Error message validations failed:\n\n${errors.join('\n\n')}`);
    }
  }

  static clearResults(): void {
    ErrorMessageValidation.results = [];
    ErrorMessageValidation.pagesWithEMV.clear();
    ErrorMessageValidation.pagesPassed.clear();
    ErrorMessageValidation.missingEMVFiles.clear();
    ErrorMessageValidation.emvFailed = false;
  }
}
