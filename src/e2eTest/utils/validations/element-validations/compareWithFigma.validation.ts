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
    const baselinePath = path.resolve(`figmaExport/${pageName}.png`);
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
// Wait for DOM to be interactive and visible content to settle,
// but don't rely on networkidle (since apps keep polling)
    try {
      await page.waitForLoadState('domcontentloaded', {timeout: 15000});
      await page.waitForSelector('main, body', {state: 'visible', timeout: 10000});
      await page.waitForTimeout(1000); // small settle delay
      console.log('✅ Page rendered and stable');
    } catch (err) {
      console.warn('⚠️ Page did not fully settle before screenshot:', err);
    }
    await page.waitForSelector('body', {state: 'visible', timeout: 10000});
    await page.waitForTimeout(500);
    const debugAfterMaskingBuffer = await page.screenshot({
      fullPage: false,
      timeout: 10000
    });
    fs.writeFileSync(path.resolve(`figmaExport/${pageName}-debug-after-masking.png`), debugAfterMaskingBuffer);

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
    const {numDiffPixels, diffPNG} = this.compareImages(currentPNG, baselinePNG);
    const maxDiffPixels = this.calculateMaxDiffPixels(baselinePNG.width, baselinePNG.height);

    console.log(`Pixel difference: ${numDiffPixels}, Threshold: ${maxDiffPixels}`);

    if (numDiffPixels > maxDiffPixels) {
      const diffPath = path.resolve(`figmaExport/${pageName}-diff.png`);
      fs.writeFileSync(diffPath, PNG.sync.write(diffPNG));

      const currentPath = path.resolve(`figmaExport/${pageName}-current.png`);
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
    await page.waitForSelector('body', {state: 'attached'});

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

    return {numDiffPixels, diffPNG};
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

    await page.evaluate(() => {
      // Helper function to apply the mask styles to an element
      const maskElement = (el: HTMLElement) => {
        if (!el || el.style.color === 'transparent') return;
        const rect = el.getBoundingClientRect();
        if (rect.width > 600 || rect.height > 300 || rect.width < 5) return;

        el.style.backgroundColor = '#e0e0e0';
        el.style.color = 'transparent';
        el.style.textShadow = 'none';
        el.style.filter = 'blur(4px)';
      };

      // --- NEW STRATEGY: MASK PARTIAL TEXT (E.G., JUST THE CASE NUMBER) ---
      // This is the most precise way to handle your specific request.
      console.log('Running partial text masking for case numbers...');
      const elements = document.querySelectorAll('body *');
      // Regex to find "Case number: " followed by the number to be masked
      const caseNumberRegex = /(Case number: )(\s*[\d-]+)/i;

      elements.forEach(el => {
        // Check only elements that are not scripts and contain child nodes
        if (el.tagName === 'SCRIPT' || el.children.length > 0) {
          return;
        }

        // If the element's text content matches, replace its HTML
        if (caseNumberRegex.test(el.textContent || '')) {
          // The magic happens here: we wrap the number (group 2) in a styled span
          el.innerHTML = (el.textContent || '').replace(
              caseNumberRegex,
              `$1<span style="background-color: #e0e0e0; color: transparent; filter: blur(4px);">$2</span>`
          );
        }
      });

      // --- STRATEGY 2: PRE-DEFINED SENSITIVE SELECTORS ---
      const sensitiveSelectors = [
        '[data-testid*="case-number-value"]', // A specific test-id for the value is ideal
        '[data-testid*="user"]',
        '.timestamp',
        'time',
        '[datetime]',
      ];
      document.querySelectorAll(sensitiveSelectors.join(',')).forEach(el => maskElement(el as HTMLElement));

      // --- STRATEGY 3: TEXT PATTERN MATCHING (FALLBACK) ---
      // This is a fallback for other dynamic data like dates or random IDs
      document.querySelectorAll('body *').forEach(el => {
        const htmlEl = el as HTMLElement;
        // Only act on "leaf" nodes that have no other elements inside them
        if (htmlEl.children.length === 0 && htmlEl.textContent) {
          const text = htmlEl.textContent.trim();
          if (
              /\d{2}\/\d{2}\/\d{4}/.test(text) ||   // Date format dd/mm/yyyy
              /\b[A-Z0-9]{8,}\b/.test(text) && !/case number/i.test(text) // Generic ID, but ignore if already handled
          ) {
            maskElement(htmlEl);
          }
        }
      });
    });

    console.log('✅ Dynamic content masking applied.');
  }
}
