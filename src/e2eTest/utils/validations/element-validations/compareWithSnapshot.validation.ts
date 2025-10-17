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

    if (typeof data === 'object' && data !== null && !Array.isArray(data) && 'selectorsToMask' in data) {
      const selectorStrings = data.selectorsToMask as string[];
      locatorsToMask = selectorStrings.map(selector => page.locator(selector));
    }

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
    // --- THIS IS THE UPDATED LOGIC ---
    const updateMode = (test.info() as any).config.updateSnapshots;
    const isUpdateMode = updateMode === 'all' || updateMode === 'changed';

    if (isUpdateMode) {
      // 1. UPDATE MODE: We are running with -u.
      // Create or update the baseline and pass.
      fs.copyFileSync(actualPath, baselinePath);
      console.log(`✅ Baseline snapshot created/updated at: ${baselinePath}`);
      await test.info().attach('New/Updated Baseline', {
        path: baselinePath,
        contentType: 'image/png',
      });
      return; // <-- Exit successfully
    }

    if (!fs.existsSync(baselinePath)) {
      // 2. BASELINE MISSING: We are NOT in update mode.
      // Attach the new image for review and throw an error.
      await test.info().attach('Actual (New) Screenshot', {
        path: actualPath,
        contentType: 'image/png',
      });

      throw new Error(
        `Baseline image not found for page "${pageName}" at: ${baselinePath}\n` +
        `Run with the -u or --update-snapshots flag to create a new baseline.`
      );
    }
    // --- END OF UPDATED LOGIC ---


    // 3. COMPARE MODE: We are not in update mode and the baseline exists.
    // --- Perform Comparison ---
    const imgBaseline = PNG.sync.read(fs.readFileSync(baselinePath));
    const imgActual = PNG.sync.read(fs.readFileSync(actualPath));

    if (imgBaseline.width !== imgActual.width || imgBaseline.height !== imgActual.height) {
      throw new Error(
        `Image size mismatch for page "${pageName}": baseline ${imgBaseline.width}x${imgBaseline.height}, actual ${imgActual.width}x${imgActual.height}`
      );
    }

    const { width, height } = imgBaseline;
    const diff = new PNG({ width, height });

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
      `Visual diff for page "${pageName}" exceeds threshold. ${diffPixels} pixels differ (max allowed: ${maxDiffPixels}).`
    ).toBeLessThanOrEqual(maxDiffPixels);
  }
}
