import { Page } from '@playwright/test';
import { IValidation, validationRecord } from '../../interfaces/validation.interface';
import path from 'path';
import fs from 'fs';
import PNG from 'pngjs';
import pixelmatch from 'pixelmatch';
import sharp from 'sharp';

export class compareWithFigmaValidation implements IValidation {
  async validate(
    page: Page,
    validation: string,
    pageName: string,
    error: string | validationRecord
  ): Promise<void> {
    // Load Figma baseline
    const baselinePath = path.resolve(`figma/${pageName}.png`);
    if (!fs.existsSync(baselinePath)) throw new Error(`Figma baseline not found at ${baselinePath}`);
    const baselineBuffer = fs.readFileSync(baselinePath);
    const baselinePNG = PNG.PNG.sync.read(baselineBuffer);

    // Force viewport to match baseline and remove scrollbars
    await page.setViewportSize({ width: baselinePNG.width, height: baselinePNG.height });
    await page.evaluate(() => {
      document.documentElement.style.overflow = 'hidden';
    });

    // Mask dynamic content (timestamps, banners, etc.)
    await page.evaluate(() => {
      document.querySelectorAll('.dynamic, .timestamp, .banner').forEach(el => {
        // Cast Element to HTMLElement to access style
        (el as HTMLElement).style.visibility = 'hidden';
      });
    });

    // Take screenshot
    let currentBuffer = await page.screenshot({ fullPage: false });

    // Auto-align screenshot to baseline using sharp (optional shift correction)
    currentBuffer = await sharp(currentBuffer)
      .extract({ left: 0, top: 0, width: baselinePNG.width, height: baselinePNG.height })
      .toBuffer();

    const currentPNG = PNG.PNG.sync.read(currentBuffer);

    // Ensure dimensions match
    if (currentPNG.width !== baselinePNG.width || currentPNG.height !== baselinePNG.height) {
      throw new Error(
        `Screenshot size (${currentPNG.width}x${currentPNG.height}) does not match baseline (${baselinePNG.width}x${baselinePNG.height})`
      );
    }

    // Compare pixels with higher threshold and anti-alias tolerance
    const diff = new PNG.PNG({ width: currentPNG.width, height: currentPNG.height });
    const numDiffPixels = pixelmatch(
      currentPNG.data,
      baselinePNG.data,
      diff.data,
      currentPNG.width,
      currentPNG.height,
      {
        threshold: 0.25,   // tolerate minor line or horizontal shifts
        includeAA: true    // include anti-alias differences
      }
    );

    const maxDiffPixels = 5000; // adjust based on page complexity
    if (numDiffPixels > maxDiffPixels) {
      const diffPath = path.resolve(`figma/${pageName}-diff.png`);
      fs.writeFileSync(diffPath, PNG.PNG.sync.write(diff));
      throw new Error(`Visual mismatch found: ${numDiffPixels} pixels differ. Diff saved at ${diffPath}`);
    }
  }
}
