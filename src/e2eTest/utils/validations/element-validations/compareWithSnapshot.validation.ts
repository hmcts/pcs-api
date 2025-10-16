import { Page, expect, test } from '@playwright/test';
import { IValidation, validationRecord } from '../../interfaces/validation.interface';
import path from 'path';
import fs from 'fs';
import { PNG } from 'pngjs';
import pixelmatch from 'pixelmatch';

export class compareWithSnapshotValidation implements IValidation {
  async validate(
    page: Page,
    validation: string,
    pageName: string,
    error: string | validationRecord
  ): Promise<void> {
    // Paths
    const baselinePath = path.resolve(`appSnapshots/${pageName}.png`);
    const actualPath = path.resolve(`test-results/${pageName}-actual.png`);
    const diffPath = path.resolve(`test-results/${pageName}-diff.png`);

    // Take the actual screenshot
    await page.screenshot({
      path: actualPath,
      fullPage: true,
      mask: [page.locator('p:has-text("Case number:")')],
    });

    // Ensure baseline exists
    if (!fs.existsSync(baselinePath)) {
      throw new Error(`Baseline image not found at: ${baselinePath}`);
    }

    // Load both baseline and actual images
    const imgBaseline = PNG.sync.read(fs.readFileSync(baselinePath));
    const imgActual = PNG.sync.read(fs.readFileSync(actualPath));

    // Ensure same dimensions
    if (imgBaseline.width !== imgActual.width || imgBaseline.height !== imgActual.height) {
      throw new Error(
        `Image size mismatch between baseline and actual:
        baseline: ${imgBaseline.width}x${imgBaseline.height},
        actual: ${imgActual.width}x${imgActual.height}`
      );
    }

    const { width, height } = imgBaseline;
    const diff = new PNG({ width, height });

    // Compare and create a diff-highlighted image
    const diffPixels = pixelmatch(
      imgBaseline.data,
      imgActual.data,
      diff.data,
      width,
      height,
      {
        threshold: 0.1,   // sensitivity — lower = stricter comparison
        includeAA: true,  // include anti-aliasing detection
        alpha: 0.7,       // opacity of diff pixels
        diffColor: [255, 0, 0], // red highlight for diffs
        diffColorAlt: [0, 255, 0], // green for antialiasing
      }
    );

    // Save the diff-highlighted image
    fs.writeFileSync(diffPath, PNG.sync.write(diff));

    // Attach all images to Allure report
    await test.info().attach('Baseline Screenshot', {
      path: baselinePath,
      contentType: 'image/png',
    });
    await test.info().attach('Current Screenshot', {
      path: actualPath,
      contentType: 'image/png',
    });
    await test.info().attach('Diff Highlighted Screenshot', {
      path: diffPath,
      contentType: 'image/png',
    });

    // Compare and fail if too many pixels differ
    const maxDiffPixels = 100;
    expect(
      diffPixels,
      `Visual diff exceeds threshold (${maxDiffPixels} pixels differ)`
    ).toBeLessThanOrEqual(maxDiffPixels);
  }
}
