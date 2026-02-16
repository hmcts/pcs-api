import { Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

class TextCaptureService {
  private static instance: TextCaptureService;
  private outputDir: string = path.join(process.cwd(), '/generators/output/text-capture-results');
  private locatorsFile: string = path.join(process.cwd(), '/src/e2eTest/generators/output/all-locators.json');
  private capturedPages: Set<string> = new Set();
  private isEnabled: boolean = false;
  private includeLocators: boolean = false;
  private elementPatterns: Map<string, Set<string>> = new Map();
  private locatorsInitialized: boolean = false;

  private constructor() {
    this.ensureOutputDirectory();
  }

  public static getInstance(): TextCaptureService {
    if (!TextCaptureService.instance) {
      TextCaptureService.instance = new TextCaptureService();
    }
    return TextCaptureService.instance;
  }

  private initializeDefaultPatterns(): void {
    if (this.locatorsInitialized) return;

    const defaultPatterns = {
      'button': [
        "page.getByRole('button', { name: '${text}' })",
        "page.locator('button').filter({ hasText: '${text}' })"
      ],
      'link': [
        "page.getByRole('link', { name: '${text}' })",
        "page.locator('a').filter({ hasText: '${text}' })"
      ],
      'heading': [
        "page.getByRole('heading', { name: '${text}' })",
        "page.locator('h1, h2, h3').filter({ hasText: '${text}' })"
      ],
      'input': [
        "page.getByLabel('${label}')",
        "page.getByPlaceholder('${placeholder}')",
        "page.getByRole('textbox', { name: '${name}' })"
      ],
      'checkbox': [
        "page.getByRole('checkbox', { name: '${label}' })",
        "page.getByLabel('${label}')"
      ],
      'radio': [
        "page.getByRole('radio', { name: '${label}' })"
      ],
      'select': [
        "page.getByLabel('${label}')",
        "page.locator('select')"
      ],
      'file_upload': [
        "page.locator('input[type=\"file\"]')",
        "page.getByLabel('${label}').filter({ hasAttribute: 'type', hasValue: 'file' })"
      ],
      'paragraph': [
        "page.locator('p').filter({ hasText: '${text}' })",
        "page.getByText('${text}')"
      ]
    };

    Object.entries(defaultPatterns).forEach(([elementType, patterns]) => {
      if (!this.elementPatterns.has(elementType)) {
        this.elementPatterns.set(elementType, new Set());
      }
      patterns.forEach(pattern => {
        this.elementPatterns.get(elementType)!.add(pattern);
      });
    });

    this.locatorsInitialized = true;
  }

  public enable(): void {
    this.isEnabled = true;
  }

  public disable(): void {
    this.isEnabled = false;
  }

  public setIncludeLocators(include: boolean): void {
    this.includeLocators = include;
    if (include && !this.locatorsInitialized) {
      this.initializeDefaultPatterns();
    }
  }

  private ensureOutputDirectory() {
    if (!fs.existsSync(this.outputDir)) {
      fs.mkdirSync(this.outputDir, { recursive: true });
    }
  }

  public async capturePageText(page: Page): Promise<void> {
    if (!this.isEnabled) return;

    const currentUrl = this.normalizeUrl(page.url());
    if (!currentUrl || currentUrl === 'about:blank' || this.capturedPages.has(currentUrl)) return;

    try {
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(800);

      const pageData = await this.extractPageData(page);

      const filename = await this.generateFilename(page);
      await this.savePageData(pageData, filename);

      if (this.includeLocators) {
        await this.extractElementPatternsFromPage(page);
        this.saveElementPatterns();
      }

      this.capturedPages.add(currentUrl);
      console.log(`✅ Captured: ${filename}`);

    } catch (error) {
      console.error(`❌ Error capturing ${currentUrl}:`, error);
    }
  }

  private normalizeUrl(url: string): string {
    return url.split('?')[0].split('#')[0].replace(/\/+$/, '').toLowerCase();
  }

  private normalizeText(text: string): string {
    return text.replace(/\s+/g, ' ').trim();
  }

  private async extractPageData(page: Page): Promise<any> {
    const [headings, paragraphs, links, buttons] = await Promise.all([
      this.extractElementsSafe(page, 'h1, h2, h3, h4, h5, h6'),
      this.extractParagraphsSafe(page),
      this.extractLinksSafe(page),
      this.extractButtonsSafe(page)
    ]);

    // Return only the text content, no metadata
    return {
      headings,
      paragraphs,
      links,
      buttons
    };
  }

  private async extractElementsSafe(page: Page, selector: string): Promise<string[]> {
    try {
      const locator = page.locator(selector);
      const count = await locator.count().catch(() => 0);
      const elements: string[] = [];

      for (let i = 0; i < count; i++) {
        const text = await locator.nth(i).textContent().catch(() => null);
        if (text?.trim()) {
          elements.push(this.normalizeText(text));
        }
      }
      return elements;
    } catch (error) {
      return [];
    }
  }

  private async extractParagraphsSafe(page: Page): Promise<string[]> {
    try {
      const locator = page.locator('p');
      const count = await locator.count().catch(() => 0);
      const paragraphs: string[] = [];

      for (let i = 0; i < count; i++) {
        const text = await locator.nth(i).textContent().catch(() => null);
        if (text?.trim()) {
          const normalizedText = this.normalizeText(text);
          if (normalizedText.length > 10 && !this.isNavigationText(normalizedText)) {
            paragraphs.push(normalizedText);
          }
        }
      }
      return paragraphs.slice(0, 20);
    } catch (error) {
      return [];
    }
  }

  private isNavigationText(text: string): boolean {
    const navigationPatterns = [
      'skip to main content',
      'cookie',
      'privacy',
      'terms',
      'accessibility',
      'menu',
      'navigation',
      'home',
      'back',
      'next',
      'previous'
    ];

    const lowerText = text.toLowerCase();
    return navigationPatterns.some(pattern => lowerText.includes(pattern));
  }

  private async extractLinksSafe(page: Page): Promise<string[]> {
    try {
      const locator = page.locator('a');
      const count = await locator.count().catch(() => 0);
      const links: string[] = [];

      for (let i = 0; i < count; i++) {
        const text = await locator.nth(i).textContent().catch(() => null);
        if (text?.trim()) {
          links.push(this.normalizeText(text));
        }
      }
      return links;
    } catch (error) {
      return [];
    }
  }

  private async extractButtonsSafe(page: Page): Promise<string[]> {
    try {
      const buttonSelectors = [
        'button', '[role="button"]', 'input[type="button"]',
        'input[type="submit"]', 'input[type="reset"]'
      ];
      const buttons: string[] = [];

      for (const selector of buttonSelectors) {
        const locator = page.locator(selector);
        const count = await locator.count().catch(() => 0);

        for (let i = 0; i < count; i++) {
          let text = await locator.nth(i).textContent().catch(() => null);

          if (!text?.trim()) {
            text = await locator.nth(i).getAttribute('value').catch(() => null) ||
              await locator.nth(i).getAttribute('aria-label').catch(() => null);
          }

          if (text?.trim()) {
            buttons.push(this.normalizeText(text));
          }
        }
      }
      return buttons;
    } catch (error) {
      return [];
    }
  }

  private async extractElementPatternsFromPage(page: Page): Promise<void> {
    if (!this.includeLocators) return;

    try {
      await Promise.all([
        this.extractButtonPatterns(page),
        this.extractLinkPatterns(page),
        this.extractHeadingPatterns(page),
        this.extractParagraphPatterns(page)
      ]);
    } catch (error) {
      console.error('Error extracting element patterns:', error);
    }
  }

  private async extractButtonPatterns(page: Page): Promise<void> {
    const locator = page.locator('button, [role="button"]');
    const count = await locator.count().catch(() => 0);
    if (count > 0) {
      this.addElementPattern('button', `page.getByRole('button')`);
      this.addElementPattern('button', `page.locator('button, [role="button"]')`);
    }
  }

  private async extractLinkPatterns(page: Page): Promise<void> {
    const locator = page.locator('a');
    const count = await locator.count().catch(() => 0);
    if (count > 0) {
      this.addElementPattern('link', `page.getByRole('link')`);
      this.addElementPattern('link', `page.locator('a')`);
    }
  }

  private async extractHeadingPatterns(page: Page): Promise<void> {
    const locator = page.locator('h1, h2, h3');
    const count = await locator.count().catch(() => 0);
    if (count > 0) {
      this.addElementPattern('heading', `page.getByRole('heading')`);
      this.addElementPattern('heading', `page.locator('h1, h2, h3')`);
    }
  }

  private async extractParagraphPatterns(page: Page): Promise<void> {
    const locator = page.locator('p');
    const count = await locator.count().catch(() => 0);
    if (count > 0) {
      this.addElementPattern('paragraph', `page.locator('p')`);
      this.addElementPattern('paragraph', `page.getByText('\${text}')`);
    }
  }

  private addElementPattern(elementType: string, pattern: string): void {
    if (!this.elementPatterns.has(elementType)) {
      this.elementPatterns.set(elementType, new Set());
    }
    this.elementPatterns.get(elementType)!.add(pattern);
  }

  private async generateFilename(page: Page): Promise<string> {
    // Use same logic as flowchart to avoid cookies pages
    let filenameText = '';

    // Strategy 1: H1 heading (avoid cookies)
    const h1Locator = page.locator('h1').first();
    const h1Count = await h1Locator.count().catch(() => 0);
    if (h1Count > 0) {
      const h1Text = await h1Locator.textContent().catch(() => null);
      if (h1Text?.trim() && !this.isCookiesHeader(h1Text)) {
        filenameText = h1Text;
      }
    }

    // Strategy 2: Meaningful H2 heading (avoid cookies, navigation)
    if (!filenameText) {
      const h2Locator = page.locator('h2');
      const h2Count = await h2Locator.count().catch(() => 0);

      if (h2Count > 0) {
        for (let i = 0; i < h2Count; i++) {
          const h2Text = await h2Locator.nth(i).textContent().catch(() => null);
          if (h2Text?.trim() && !this.isCookiesHeader(h2Text) && !this.isNavigationHeader(h2Text)) {
            filenameText = h2Text;
            break;
          }
        }
      }
    }

    // Strategy 3: Page title (avoid cookies)
    if (!filenameText) {
      const pageTitle = await page.title().catch(() => '');
      if (pageTitle && !this.isCookiesHeader(pageTitle)) {
        filenameText = pageTitle;
      }
    }

    // Strategy 4: Any other meaningful heading
    if (!filenameText) {
      const headingSelectors = ['h3', 'h4', 'h5', 'h6'];
      for (const selector of headingSelectors) {
        const locator = page.locator(selector).first();
        const count = await locator.count().catch(() => 0);
        if (count > 0) {
          const text = await locator.textContent().catch(() => null);
          if (text?.trim() && !this.isCookiesHeader(text)) {
            filenameText = text;
            break;
          }
        }
      }
    }

    // Final fallback: URL path
    if (!filenameText) {
      const urlPath = this.normalizeUrl(page.url());
      const pathParts = urlPath.split('/').filter(part => part.length > 0);
      filenameText = pathParts[pathParts.length - 1] || 'page';
    }

    return this.cleanFilename(filenameText);
  }

  private isCookiesHeader(header: string): boolean {
    const cookiesPatterns = [
      /cookies? on this service/i,
      /cookies? (policy|notice|information)/i,
      /your cookies? settings/i,
      /manage your cookies?/i,
      /cookie banner/i,
      /cookie settings/i,
      /accept.*cookies?/i,
      /reject.*cookies?/i
    ];

    const lowerHeader = header.toLowerCase().trim();
    return cookiesPatterns.some(pattern => pattern.test(lowerHeader));
  }

  private isNavigationHeader(header: string): boolean {
    const navigationHeaders = [
      'navigation', 'menu', 'main menu', 'site menu', 'breadcrumb',
      'you are here', 'accessibility', 'help', 'support', 'feedback'
    ];

    const lowerHeader = header.toLowerCase().trim();
    return navigationHeaders.some(nav => lowerHeader.includes(nav));
  }

  private cleanFilename(text: string): string {
    return this.normalizeText(text)
      .replace(/[^a-zA-Z0-9\s-]/g, '')
      .replace(/\s+/g, '-')
      .toLowerCase()
      .substring(0, 40);
  }

  private async savePageData(pageData: any, filename: string): Promise<void> {
    try {
      const filepath = path.join(this.outputDir, `${filename}.json`);
      fs.writeFileSync(filepath, JSON.stringify(pageData, null, 2), 'utf8');
    } catch (error) {
      console.error('Error saving page data:', error);
    }
  }

  private saveElementPatterns(): void {
    try {
      const locatorsData: any = {
        timestamp: new Date().toISOString(),
        elements: {}
      };

      this.elementPatterns.forEach((patterns, elementType) => {
        const patternArray = Array.from(patterns);
        const patternObj: any = {};

        patternArray.forEach((pattern, index) => {
          patternObj[`pattern${index + 1}`] = pattern;
        });

        locatorsData.elements[elementType] = patternObj;
      });

      fs.writeFileSync(this.locatorsFile, JSON.stringify(locatorsData, null, 2), 'utf8');
    } catch (error) {
      console.error('Error saving element patterns:', error);
    }
  }
}

export const textCaptureService = TextCaptureService.getInstance();