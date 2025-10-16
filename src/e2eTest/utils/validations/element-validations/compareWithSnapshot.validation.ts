import { Page, expect, test, Locator } from '@playwright/test';
import { IValidation, validationData, validationRecord } from '../../interfaces/validation.interface';
import path from 'path';
import fs from 'fs';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';

export class compareWithSnapshotValidation implements IValidation {

  async validate(
    page: Page,
    validation: string,
    fieldName?: validationData | validationRecord,
    data?: validationData | validationRecord
  ): Promise<void> {

    if (typeof fieldName !== 'string') {
      throw new Error(`[${validation}] Validation Error: The 3rd argument (fieldName) must be a string for 'pageName'.`);
    }
    const pageName: string = fieldName;

    let locatorsToMask: Locator[] = [];

    // --- THIS IS THE KEY ---
    // 1. We check if 'data' is an object and if 'selectorsToMask' is a key 'in' it.
    if (typeof data === 'object' && data !== null && !Array.isArray(data) && 'selectorsToMask' in data) {

      // 2. We then use 'as string[]' to tell TypeScript what's inside.
      // This works because the interface is generic (object).
      const selectorStrings = data.selectorsToMask as string[];

      // 3. Convert the strings into real Locators
      locatorsToMask = selectorStrings.map(selector => page.locator(selector));
    }
    // --- END OF KEY SECTION ---

    // --- Paths ---
    const baselinePath = path.resolve(`baselineSnapshots/${pageName}.png`);
    const actualPath = path.resolve(`test-results/${pageName}-actual.png`);
    const diffPath = path.resolve(`test-results/${pageName}-diff.png`);

    fs.mkdirSync(path.dirname(baselinePath), { recursive: true });
    fs.mkdirSync(path.dirname(actualPath), { recursive: true });

    // --- Take the actual screenshot ---
    await page.screenshot({
      path: actualPath,
      fullPage: true,
      mask: locatorsToMask,
    });

    // --- Handle Baseline Creation/Updates ---
    const isUpdateMode = (test.info().project as any).updateSnapshots === 'all';
    if (isUpdateMode || !fs.existsSync(baselinePath)) {
      fs.copyFileSync(actualPath, baselinePath);
      console.log(`✅ Baseline snapshot created/updated at: ${baselinePath}`);
      await test.info().attach('New/Updated Baseline', {
        path: baselinePath,
        contentType: 'image/png',
      });
      return;
    }

    // --- Perform Comparison ---
    const imgBaseline = PNG.sync.read(fs.readFileSync(baselinePath));
    const imgActual = PNG.sync.read(fs.readFileSync(actualPath));

    if (imgBaseline.width !== imgActual.width || imgBaseline.height !== imgActual.height) {
      throw new Error(
        `Image size mismatch: baseline ${imgBaseline.width}x${imgBaseline.height}, actual ${imgActual.width}x${imgActual.height}`
      );
    }

    const { width, height } = imgBaseline;
    const diff = new PNG({ width, height });

    // --- Restored pixelmatch arguments ---
    const diffPixels = pixelmatch(
      imgBaseline.data,
      imgActual.data,
      diff.data,
      width,
      height,
      {
        threshold: 0.1,
        includeAA: true,
        alpha: 0.7,
        diffColor: [255, 0, 0],
        diffColorAlt: [0, 255, 0],
      }
    );

    // --- Attach reports ---
    await test.info().attach('Baseline Screenshot', {
      path: baselinePath,
      contentType: 'image/png'
    });
    await test.info().attach('Current Screenshot', {
      path: actualPath,
      contentType: 'image/png'
    });

    // --- Handle diffs ---
    const maxDiffPixels = 100;
    if (diffPixels > maxDiffPixels) {
      fs.writeFileSync(diffPath, PNG.sync.write(diff));
      await test.info().attach('Diff Highlighted Screenshot', {
        path: diffPath,
        contentType: 'image/png'
      });
    }

    expect(
      diffPixels,
      `Visual diff exceeds threshold (${maxDiffPixels} pixels differ)`
    ).toBeLessThanOrEqual(maxDiffPixels);
  }
}
