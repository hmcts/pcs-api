import { Page } from '@playwright/test';
import { IValidation, validationRecord } from '../../interfaces/validation.interface';
import path from 'path';
import fs from 'fs';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';
import sharp from 'sharp';

export class compareWithFigmaValidation implements IValidation {
  async validate(
      page: Page,
      validation: string,
      pageName: string,
      error: string | validationRecord
  ): Promise<void> {
    console.log(`Starting visual validation for: ${pageName}`);

    // Load Figma baseline
    const baselinePath = path.resolve(`figma/${pageName}.png`);
    if (!fs.existsSync(baselinePath)) {
      throw new Error(`Figma baseline not found at ${baselinePath}`);
    }

    const baselineBuffer = fs.readFileSync(baselinePath);
    const baselinePNG = PNG.sync.read(baselineBuffer);

    console.log(`Baseline dimensions: ${baselinePNG.width}x${baselinePNG.height}`);

    // DEBUG: Check current page state before any changes
    console.log('Current URL:', page.url());
    console.log('Viewport size:', await page.evaluate(() => ({
      width: window.innerWidth,
      height: window.innerHeight,
      documentWidth: document.documentElement.clientWidth,
      documentHeight: document.documentElement.clientHeight
    })));

    // Set viewport to match baseline
    await page.setViewportSize({
      width: baselinePNG.width,
      height: baselinePNG.height
    });

    // Wait for page to be ready with better checks
    await this.waitForPageReady(page);

    // Check if page content is actually visible
    const isContentVisible = await page.evaluate(() => {
      const body = document.body;
      const html = document.documentElement;
      return {
        bodyVisible: body && getComputedStyle(body).visibility !== 'hidden' &&
            getComputedStyle(body).display !== 'none',
        htmlVisible: html && getComputedStyle(html).visibility !== 'hidden',
        hasContent: body.children.length > 0,
        bodyOpacity: getComputedStyle(body).opacity,
        bodyBackground: getComputedStyle(body).backgroundColor
      };
    });

    console.log('Content visibility check:', isContentVisible);

    // Apply minimal CSS adjustments (be more careful)
    await page.evaluate(() => {
      // Only hide overflow, don't change other styles that might break layout
      document.documentElement.style.overflow = 'hidden';
      document.body.style.overflow = 'hidden';
    });

    // Apply comprehensive masking before taking screenshot
    await this.maskDynamicContent(page);

    // Wait a bit after CSS changes and masking
    await page.waitForTimeout(500);

    // Take a debug screenshot after masking
    const debugAfterMaskingBuffer = await page.screenshot({
      fullPage: false,
      timeout: 10000
    });
    fs.writeFileSync(path.resolve(`figma/${pageName}-debug-after-masking.png`), debugAfterMaskingBuffer);

    // Take screenshot with multiple fallback strategies
    let currentBuffer: Buffer;

    try {
      // Strategy 1: Regular screenshot
      currentBuffer = await page.screenshot({
        fullPage: false,
        animations: 'disabled',
        caret: 'hide',
        timeout: 15000
      });
    } catch (error) {
      console.log('Regular screenshot failed, trying full page...');
      // Strategy 2: Full page screenshot
      currentBuffer = await page.screenshot({
        fullPage: true,
        timeout: 15000
      });
    }

    // Check if screenshot is mostly black/empty
    const screenshotCheck = await this.analyzeScreenshot(currentBuffer);
    console.log('Screenshot analysis:', screenshotCheck);

    if (screenshotCheck.isMostlyBlack || screenshotCheck.isMostlyEmpty) {
      throw new Error(
          `Screenshot appears to be blank or black. ` +
          `Average brightness: ${screenshotCheck.averageBrightness}, ` +
          `Non-white pixels: ${screenshotCheck.nonWhitePixels}`
      );
    }

    // Process screenshot
    currentBuffer = await this.processScreenshot(currentBuffer, baselinePNG);
    const currentPNG = PNG.sync.read(currentBuffer);

    // Compare images
    const { numDiffPixels, diffPNG } = this.compareImages(currentPNG, baselinePNG);
    const maxDiffPixels = this.calculateMaxDiffPixels(baselinePNG.width, baselinePNG.height);

    console.log(`Pixel difference: ${numDiffPixels}, Threshold: ${maxDiffPixels}`);

    if (numDiffPixels > maxDiffPixels) {
      const diffPath = path.resolve(`figma/${pageName}-diff.png`);
      fs.writeFileSync(diffPath, PNG.sync.write(diffPNG));

      const currentPath = path.resolve(`figma/${pageName}-current.png`);
      fs.writeFileSync(currentPath, currentBuffer);

      throw new Error(
          `Visual mismatch found: ${numDiffPixels} pixels differ (threshold: ${maxDiffPixels}). ` +
          `Diff saved at ${diffPath}, current screenshot at ${currentPath}`
      );
    }

    console.log(`Visual validation passed for: ${pageName}`);
  }

  private async waitForPageReady(page: Page): Promise<void> {
    // Wait for DOM to be ready
    await page.waitForLoadState('domcontentloaded');

    // Wait for body to be available and visible
    await page.waitForSelector('body', { state: 'attached' });

    // Wait for any main content element
    await page.waitForSelector('body :not(script):not(style):not(link)', {
      state: 'attached',
      timeout: 5000
    });

    // Short wait for any final rendering
    await page.waitForTimeout(1000);
  }

  private async analyzeScreenshot(buffer: Buffer): Promise<{
    isMostlyBlack: boolean;
    isMostlyEmpty: boolean;
    averageBrightness: number;
    nonWhitePixels: number;
  }> {
    const png = PNG.sync.read(buffer);
    let totalBrightness = 0;
    let blackPixels = 0;
    let whitePixels = 0;

    for (let i = 0; i < png.data.length; i += 4) {
      const r = png.data[i];
      const g = png.data[i + 1];
      const b = png.data[i + 2];

      const brightness = (r + g + b) / 3;
      totalBrightness += brightness;

      if (brightness < 10) blackPixels++;
      if (brightness > 240) whitePixels++;
    }

    const totalPixels = png.width * png.height;
    const averageBrightness = totalBrightness / totalPixels;
    const blackPixelRatio = blackPixels / totalPixels;
    const whitePixelRatio = whitePixels / totalPixels;

    return {
      isMostlyBlack: blackPixelRatio > 0.8,
      isMostlyEmpty: whitePixelRatio > 0.9,
      averageBrightness,
      nonWhitePixels: totalPixels - whitePixels
    };
  }

  private async processScreenshot(
      screenshotBuffer: Buffer,
      baselinePNG: PNG
  ): Promise<Buffer> {
    // First, check if we need to resize
    const screenshotMeta = await sharp(screenshotBuffer).metadata();

    let processedBuffer = screenshotBuffer;

    if (screenshotMeta.width !== baselinePNG.width || screenshotMeta.height !== baselinePNG.height) {
      console.log(`Resizing screenshot from ${screenshotMeta.width}x${screenshotMeta.height} to ${baselinePNG.width}x${baselinePNG.height}`);
      processedBuffer = await sharp(processedBuffer)
          .resize(baselinePNG.width, baselinePNG.height, {
            fit: 'fill'
          })
          .toBuffer();
    }

    return processedBuffer;
  }

  private compareImages(currentPNG: PNG, baselinePNG: PNG): {
    numDiffPixels: number;
    diffPNG: PNG;
  } {
    const diffPNG = new PNG({
      width: currentPNG.width,
      height: currentPNG.height
    });

    const numDiffPixels = pixelmatch(
        currentPNG.data,
        baselinePNG.data,
        diffPNG.data,
        currentPNG.width,
        currentPNG.height,
        {
          threshold: 0.2,
          includeAA: true,
          alpha: 0.5,
          diffColor: [255, 0, 0],
          diffColorAlt: [0, 0, 255]
        }
    );

    return { numDiffPixels, diffPNG };
  }

  private calculateMaxDiffPixels(width: number, height: number): number {
    const totalPixels = width * height;

    if (totalPixels > 2000000) return 10000;
    if (totalPixels > 1000000) return 7000;
    if (totalPixels > 500000) return 5000;

    return 3000;
  }

  private async maskDynamicContent(page: Page): Promise<void> {
    console.log('Applying dynamic content masking...');

    // Define comprehensive masking rules
    const maskingRules = [
      // Case number and similar identifiers
      { selector: 'p:has-text("Case number:")', description: 'Case number paragraph' },
      { selector: 'span:has-text("Case number:")', description: 'Case number span' },
      { selector: 'div:has-text("Case number:")', description: 'Case number div' },
      { selector: '[data-testid*="case-number"]', description: 'Case number test ID' },
      { selector: '[id*="case-number"]', description: 'Case number ID' },
      { selector: '[class*="case-number"]', description: 'Case number class' },

      // Timestamps and dates
      { selector: '.timestamp', description: 'Timestamp class' },
      { selector: '[data-testid*="timestamp"]', description: 'Timestamp test ID' },
      { selector: 'time', description: 'Time element' },
      { selector: '[datetime]', description: 'Datetime attribute' },

      // Dynamic content markers
      { selector: '.dynamic', description: 'Dynamic content class' },
      { selector: '.banner', description: 'Banner class' },
      { selector: '.notification', description: 'Notification class' },
      { selector: '[aria-live]', description: 'ARIA live regions' },

      // User-specific content
      { selector: '[data-testid*="user"]', description: 'User data' },
      { selector: '[class*="user"]', description: 'User class' },
      { selector: '[id*="user"]', description: 'User ID' },

      // Session and temporary data
      { selector: '[data-testid*="session"]', description: 'Session data' },
      { selector: '[class*="session"]', description: 'Session class' },

      // Additional text-based masking for dynamic content
      { selector: 'text=Today', description: '"Today" text' },
      { selector: 'text=Yesterday', description: '"Yesterday" text' },
      { selector: 'text=Now', description: '"Now" text' },
    ];

    // Apply CSS-based masking
    for (const rule of maskingRules) {
      try {
        const elements = await page.$$(rule.selector);
        if (elements.length > 0) {
          console.log(`Masking ${elements.length} elements for: ${rule.description}`);

          for (const element of elements) {
            await element.evaluate((el: Element) => {
              const htmlEl = el as HTMLElement;
              htmlEl.style.visibility = 'hidden';
              htmlEl.style.opacity = '0';
              htmlEl.style.background = 'transparent';
            });
          }
        }
      } catch (error) {
        // @ts-ignore
        console.log(`Could not mask ${rule.description}: ${error.message}`);
        // Continue with other masking rules
      }
    }

    // Additional text content masking using evaluate for broader coverage
    await page.evaluate(() => {
      // Mask elements containing specific patterns
      const elements = document.querySelectorAll('*');

      elements.forEach((element) => {
        const text = element.textContent?.trim() || '';
        const htmlEl = element as HTMLElement;

        // Mask case numbers (format: Case number: ABC123)
        if (text.match(/Case number:\s*\w+/i)) {
          htmlEl.style.visibility = 'hidden';
          htmlEl.style.opacity = '0';
        }

        // Mask dates in various formats
        if (text.match(/\d{1,2}\/\d{1,2}\/\d{4}/) || // MM/DD/YYYY
            text.match(/\d{1,2}-\d{1,2}-\d{4}/) || // MM-DD-YYYY
            text.match(/\d{1,2}:\d{2}\s*(AM|PM)/i)) { // Time
          htmlEl.style.visibility = 'hidden';
          htmlEl.style.opacity = '0';
        }

        // Mask IDs and codes
        if (text.match(/\b[A-Z0-9]{6,12}\b/) && text.length < 20) {
          // Likely an ID/code if it's all caps/numbers and reasonably short
          htmlEl.style.visibility = 'hidden';
          htmlEl.style.opacity = '0';
        }
      });
    });

    // Specific locator-based masking for more precise control
    const specificLocators = [
      page.locator('p:has-text("Case number:")'),
      page.locator('span:has-text("Case number:")'),
      page.locator('div:has-text("Case number:")'),
    ];

    for (const locator of specificLocators) {
      try {
        const count = await locator.count();
        if (count > 0) {
          console.log(`Masking ${count} specific case number locators`);
          await locator.evaluateAll((elements: Element[]) => {
            elements.forEach((el) => {
              const htmlEl = el as HTMLElement;
              htmlEl.style.visibility = 'hidden';
              htmlEl.style.opacity = '0';
              htmlEl.style.background = 'transparent';
            });
          });
        }
      } catch (error) {
        // @ts-ignore
        console.log(`Could not mask specific locator: ${error.message}`);
      }
    }

    console.log('Dynamic content masking completed');
  }

  // Optional: Method to generate masking report
  async generateMaskingReport(page: Page, pageName: string): Promise<void> {
    const maskedElements = await page.evaluate(() => {
      const elements = document.querySelectorAll('*');
      const masked: Array<{ selector: string, text: string }> = [];

      elements.forEach((element) => {
        const htmlEl = element as HTMLElement;
        if (htmlEl.style.visibility === 'hidden' || htmlEl.style.opacity === '0') {
          masked.push({
            selector: element.tagName + (element.className ? `.${element.className}` : ''),
            text: element.textContent?.substring(0, 50) || ''
          });
        }
      });

      return masked;
    });

    console.log(`Masking report for ${pageName}:`, {
      totalMaskedElements: maskedElements.length,
      maskedElements: maskedElements.slice(0, 10) // Show first 10
    });
  }
}
