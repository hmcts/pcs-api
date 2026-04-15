import * as fs from 'fs';
import * as path from 'path';

import { Page, expect } from '@playwright/test';

import { performAction } from '../../controller';
import { IValidation, validationRecord } from '../../interfaces';

type NavigationTestResult = {
  pageUrl: string;
  pageName: string;
  testName: string;
  passed: boolean;
  expected?: string;
  actual?: string;
  error?: string;
  hasPFTFile: boolean;
  sourcePage?: string;
  validationType?: 'element' | 'url';
};

export class PageNavigationValidation implements IValidation {
  private static navigationResults: NavigationTestResult[] = [];
  private static testCounter = 0;
  private static pagesWithNavigation = new Set<string>();
  private static pagesPassed = new Set<string>();
  private static missingNavigationMethods = new Set<string>();
  private static missingNavigationFiles = new Set<string>();
  private static shouldThrowError = true;
  private static readonly MAPPING_PATH = path.join(__dirname, '../../../data/page-data-figma/urlToFileMapping.ts');
  private static readonly PFT_DIR = path.join(__dirname, '../../../functional');
  private static currentPageUrl: string = '';
  private static currentSourcePage: string | null = null;
  private static navigationFailed = false;

  static setSourcePage(pageName: string): void {
    PageNavigationValidation.currentSourcePage = pageName;
  }

  static clearSourcePage(): void {
    PageNavigationValidation.currentSourcePage = null;
  }

  async validate(page: Page, validation: string, navigateButton: string, fieldName: validationRecord): Promise<void> {
    PageNavigationValidation.currentPageUrl = page.url();
    let newPage: Page | null = null;
    let isNewWindow = false;

    if (navigateButton) {
      const popupPromise = page
        .context()
        .waitForEvent('page')
        .catch(() => null);

      if (navigateButton.includes('Back') || navigateButton.includes('feedback')) {
        await performAction('clickLink', navigateButton);
        await page.waitForTimeout(200);
      } else {
        await performAction('clickButton', navigateButton);
      }

      const popup = await Promise.race([popupPromise, new Promise(resolve => setTimeout(() => resolve(null), 1000))]);

      let newPopupPage: Page | null = null;

      if (popup) {
        try {
          const testPage = popup as Page;

          if (!testPage.isClosed() && testPage.url() !== 'about:blank') {
            newPopupPage = testPage;
          }
        } catch {
          console.log('Popup closed while checking url or the page is closed');
          newPopupPage = null;
        }
      }

      if (newPopupPage) {
        try {
          await newPopupPage.waitForLoadState();
          newPage = newPopupPage;
          isNewWindow = true;
        } catch {
          console.log('new window closed');
        }
      }
    }

    const pageToValidate = isNewWindow && newPage ? newPage : page;

    try {
      await this.validatePageNavigation(pageToValidate, fieldName);
    } finally {
      if (newPage && !newPage.isClosed()) {
        await newPage.close();
      }
      if (
        PageNavigationValidation.currentPageUrl &&
        !isNewWindow &&
        page.url() !== PageNavigationValidation.currentPageUrl
      ) {
        await performAction('navigateToUrl', PageNavigationValidation.currentPageUrl);
      }
    }
  }

  private async validatePageNavigation(page: Page, fieldName: validationRecord): Promise<void> {
    let elementPassed = true;
    let urlPassed = true;
    let elementError: string | undefined;
    let urlError: string | undefined;
    let actualElementText = '';
    let expectedElementText = '';
    let actualUrl = '';
    let expectedUrlPattern = '';

    try {
      if (fieldName && typeof fieldName === 'object') {
        const validationData = fieldName as any;

        if (validationData.element) {
          expectedElementText = validationData.element;
          const locator = page.locator(
            `h1, h1.govuk-heading-xl, h1.govuk-heading-l, span:text-is("${expectedElementText}")`
          );
          try {
            await expect(locator).toHaveText(expectedElementText, { timeout: 5000 });
            actualElementText = expectedElementText;
          } catch (error) {
            elementPassed = false;
            actualElementText =
              (await locator
                .first()
                .textContent()
                .catch(() => 'Not found')) || 'Not found';
            elementError = error instanceof Error ? error.message.split('\n')[0] : String(error);
          }
        }

        if (validationData.pageSlug) {
          try {
            expectedUrlPattern = `https://www.smartsurvey.co.uk/s/Poss_feedback/?pageurl=respond-to-claim/${validationData.pageSlug}`;
            actualUrl = page.url();
            if (actualUrl !== expectedUrlPattern) {
              urlPassed = false;
              urlError = `URL mismatch. Expected: ${expectedUrlPattern}, Actual: ${actualUrl}`;
            }
          } catch (error) {
            urlPassed = false;
            actualUrl = page.url();
            urlError = error instanceof Error ? error.message.split('\n')[0] : String(error);
          }
        }
      } else {
        expectedElementText = String(fieldName);
        const locator = page.locator('h1, h1.govuk-heading-xl, h1.govuk-heading-l');
        await expect(locator).toHaveText(expectedElementText, { timeout: 5000 });
        actualElementText = expectedElementText;
      }

      const pageName = await PageNavigationValidation.getPageNameFromUrl(page.url(), page);
      const hasPFTFile = await PageNavigationValidation.hasPFTFile(pageName);
      const overallPassed = elementPassed && urlPassed;

      if (!elementPassed) {
        PageNavigationValidation.navigationResults.push({
          pageUrl: page.url(),
          pageName,
          sourcePage: PageNavigationValidation.currentSourcePage || undefined,
          testName: 'Page Navigation Element Validation',
          passed: false,
          expected: expectedElementText,
          actual: actualElementText,
          error: elementError,
          hasPFTFile,
          validationType: 'element',
        });
        PageNavigationValidation.navigationFailed = true;
      }

      if (!urlPassed && fieldName && typeof fieldName === 'object' && (fieldName as any).pageSlug) {
        PageNavigationValidation.navigationResults.push({
          pageUrl: page.url(),
          pageName,
          sourcePage: PageNavigationValidation.currentSourcePage || undefined,
          testName: 'Page Navigation URL Validation',
          passed: false,
          expected: expectedUrlPattern,
          actual: actualUrl,
          error: urlError || 'Page slug validation failed',
          hasPFTFile,
          validationType: 'url',
        });
        PageNavigationValidation.navigationFailed = true;
      }

      if (overallPassed) {
        PageNavigationValidation.navigationResults.push({
          pageUrl: page.url(),
          pageName,
          sourcePage: PageNavigationValidation.currentSourcePage || undefined,
          testName: 'Page Navigation Validation',
          passed: true,
          expected: expectedElementText || 'URL validation passed',
          actual: actualElementText || page.url(),
          hasPFTFile,
        });

        if (hasPFTFile) {
          PageNavigationValidation.pagesPassed.add(pageName);
        }
      }
    } catch (error) {
      const pageName = await PageNavigationValidation.getPageNameFromUrl(page.url(), page);
      const actualText = await page
        .locator('h1')
        .first()
        .textContent()
        .catch(() => 'Not found');
      const hasPFTFile = await PageNavigationValidation.hasPFTFile(pageName);

      let expectedValue: string;
      if (typeof fieldName === 'object' && fieldName !== null) {
        const obj = fieldName as any;
        const parts: string[] = [];
        if (obj.element) {
          parts.push(`element: "${obj.element}"`);
        }
        if (obj.pageSlug) {
          parts.push(`pageSlug: "${obj.pageSlug}"`);
        }
        expectedValue = `{ ${parts.join(', ')} }`;
      } else {
        expectedValue = String(fieldName);
      }

      PageNavigationValidation.navigationResults.push({
        pageUrl: page.url(),
        pageName,
        sourcePage: PageNavigationValidation.currentSourcePage || undefined,
        testName: 'Page Navigation Validation',
        passed: false,
        expected: expectedValue,
        actual: actualText || 'Not found',
        error: error instanceof Error ? error.message.split('\n')[0] : String(error),
        hasPFTFile,
      });
      PageNavigationValidation.navigationFailed = true;
    }
  }

  static trackPageWithNavigation(pageName: string): void {
    PageNavigationValidation.pagesWithNavigation.add(pageName);
  }

  static trackPagePassed(pageName: string): void {
    PageNavigationValidation.pagesPassed.add(pageName);
  }

  static trackMissingNavigationMethod(pageName: string): void {
    PageNavigationValidation.missingNavigationMethods.add(pageName);
  }

  static trackMissingNavigationFile(pageName: string): void {
    PageNavigationValidation.missingNavigationFiles.add(pageName);
  }

  static trackNavigationFailure(pageName: string, error?: unknown): void {
    const errorMessage = error instanceof Error ? error.message : String(error);
    PageNavigationValidation.navigationResults.push({
      pageUrl: '',
      pageName,
      sourcePage: PageNavigationValidation.currentSourcePage || undefined,
      testName: 'Navigation Tests Execution',
      passed: false,
      expected: 'Navigation tests should execute successfully',
      actual: errorMessage,
      error: errorMessage,
      hasPFTFile: true,
    });
    PageNavigationValidation.navigationFailed = true;
  }

  private static async hasPFTFile(pageName: string): Promise<boolean> {
    if (!pageName || pageName === 'home' || pageName === 'Dashboard') {
      return false;
    }
    const pftPath = path.join(PageNavigationValidation.PFT_DIR, `${pageName}.pft.ts`);
    return fs.existsSync(pftPath);
  }

  private static async getPageNameFromUrl(url: string, page?: Page): Promise<string> {
    try {
      const { pathname } = new URL(url);
      const segments = pathname.split('/').filter(Boolean);
      const urlSegment = segments.at(-1) || 'home';

      if (urlSegment.toLowerCase() === 'dashboard') {
        return 'Dashboard';
      }

      const mapping = PageNavigationValidation.loadMapping();
      if (!mapping) {
        return urlSegment;
      }

      if (/^\d+$/.test(urlSegment) && page) {
        const headerText = await PageNavigationValidation.getHeaderText(page);
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
      if (!fs.existsSync(PageNavigationValidation.MAPPING_PATH)) {
        return null;
      }

      const mappingContent = fs.readFileSync(PageNavigationValidation.MAPPING_PATH, 'utf8');
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
    PageNavigationValidation.testCounter++;

    const pagesWithNavigationTests = new Set<string>();

    for (const pageName of PageNavigationValidation.pagesWithNavigation) {
      pagesWithNavigationTests.add(pageName);
    }

    for (const pageName of PageNavigationValidation.missingNavigationMethods) {
      pagesWithNavigationTests.add(pageName);
    }

    for (const pageName of PageNavigationValidation.missingNavigationFiles) {
      pagesWithNavigationTests.add(pageName);
    }

    const totalPages = pagesWithNavigationTests.size;

    if (totalPages === 0) {
      console.log(`\n📊 NAVIGATION TESTS SUMMARY (Test #${PageNavigationValidation.testCounter}):`);
      console.log('   No pages checked for navigation tests');
      return;
    }

    const failureDetails = new Map<string, { expected: string; actual: string; validationType?: string }>();
    const failedPages = new Set<string>();
    const actuallyPassedPages = new Set<string>();

    for (const result of PageNavigationValidation.navigationResults) {
      if (!result.passed) {
        if (result.sourcePage) {
          failedPages.add(result.sourcePage);
          if (result.expected && result.actual) {
            failureDetails.set(result.sourcePage, {
              expected: result.expected,
              actual: result.actual,
              validationType: result.validationType,
            });
          }
        } else if (result.hasPFTFile) {
          failedPages.add(result.pageName);
          if (result.expected && result.actual) {
            failureDetails.set(result.pageName, {
              expected: result.expected,
              actual: result.actual,
              validationType: result.validationType,
            });
          }
        }
      }
    }

    for (const pageName of PageNavigationValidation.pagesPassed) {
      if (!failedPages.has(pageName)) {
        actuallyPassedPages.add(pageName);
      }
    }

    for (const result of PageNavigationValidation.navigationResults) {
      if (
        result.passed &&
        result.hasPFTFile &&
        !failedPages.has(result.pageName) &&
        !failedPages.has(result.sourcePage || '')
      ) {
        actuallyPassedPages.add(result.pageName);
      }
    }

    const passedPages = new Set<string>();
    for (const pageName of pagesWithNavigationTests) {
      if (!failedPages.has(pageName) && actuallyPassedPages.has(pageName)) {
        passedPages.add(pageName);
      }
    }

    console.log(`\n📊 NAVIGATION TESTS SUMMARY (Test #${PageNavigationValidation.testCounter}):`);
    console.log(`   Total pages with navigation tests: ${totalPages}`);
    console.log(`   Number of pages passed: ${passedPages.size}`);
    console.log(`   Number of pages failed: ${failedPages.size}`);
    console.log(
      `   Missing navigation methods: ${PageNavigationValidation.missingNavigationMethods.size + PageNavigationValidation.missingNavigationFiles.size}`
    );

    if (passedPages.size > 0) {
      console.log(`   Passed pages: ${Array.from(passedPages).join(', ')}`);
    }

    if (failedPages.size > 0) {
      console.log(`   Failed pages: ${Array.from(failedPages).join(', ')}`);
    }

    if (PageNavigationValidation.missingNavigationMethods.size > 0) {
      console.log(
        `   Navigation methods not found: ${Array.from(PageNavigationValidation.missingNavigationMethods).join(', ')}`
      );
    }

    if (PageNavigationValidation.missingNavigationFiles.size > 0) {
      console.log(
        `   Navigation files not found: ${Array.from(PageNavigationValidation.missingNavigationFiles).join(', ')}`
      );
    }

    if (failedPages.size > 0) {
      console.log('\n❌ FAILED NAVIGATION TESTS:');

      for (const pageName of Array.from(failedPages).sort()) {
        const details = failureDetails.get(pageName);
        console.log(`   Page: ${pageName}`);
        if (details) {
          let errorMessage = details.actual;
          if (errorMessage.includes('expect(locator).toHaveText(expected) failed')) {
            errorMessage = `"${details.expected}" element not found`;
          }
          console.log(`       Error: ${errorMessage}`);
        }
        console.log('');
      }
    }

    const hasFailures = failedPages.size > 0 || PageNavigationValidation.navigationFailed;

    if (hasFailures) {
      console.log('❌ NAVIGATION TESTS FAILED\n');
    } else if (passedPages.size > 0) {
      console.log('\n✅ ALL NAVIGATION TESTS PASSED\n');
    } else if (pagesWithNavigationTests.size > 0) {
      console.log('\n⚠️  Navigation files found but no tests performed\n');
    }

    const shouldThrow = hasFailures && PageNavigationValidation.shouldThrowError;
    const errors = Array.from(failureDetails.entries()).map(([page, details]) => {
      let errorMessage = details.actual;
      if (errorMessage.includes('expect(locator).toHaveText(expected) failed')) {
        errorMessage = `"${details.expected}" element not found`;
      }
      return `${page}: ${errorMessage}`;
    });

    PageNavigationValidation.clearResults();

    if (shouldThrow && errors.length > 0) {
      throw new Error(`Navigation tests failed:\n\n${errors.join('\n\n')}`);
    }
  }

  static clearResults(): void {
    PageNavigationValidation.navigationResults = [];
    PageNavigationValidation.pagesWithNavigation.clear();
    PageNavigationValidation.pagesPassed.clear();
    PageNavigationValidation.missingNavigationMethods.clear();
    PageNavigationValidation.missingNavigationFiles.clear();
    PageNavigationValidation.currentPageUrl = '';
    PageNavigationValidation.currentSourcePage = null;
    PageNavigationValidation.navigationFailed = false;
  }
}
