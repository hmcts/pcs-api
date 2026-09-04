  import { expect, Page } from '@playwright/test';
  import path from 'path';
  import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
  import { performAction, performValidation } from '@utils/controller';
  // Matches the 180s ceiling XUI's own upload throttle doubles up to.
  export const MAX_UPLOAD_BACKOFF = 180000;

  export class UploadFileAction implements IAction {
    async execute(page: Page, action: string, files: actionData | actionRecord): Promise<void> {
      if (typeof files === 'string') {
        await this.uploadFile(page, files);
      } else if (Array.isArray(files)) {
        for (const [index, file] of files.entries()) {
          await this.uploadFile(page, file);
          if (index === files.length - 1) break;
        }
      }else if(typeof files === 'object' && 'files' in files){
        await this.uploadFile(page, files.files as string);
      }
    }

    private async uploadFile(page: Page, file: string): Promise<void> {
      await performAction('clickButton', 'Add new');
      const fileInput = page.locator('input[type="file"].form-control.bottom-30');
      const filePath = path.resolve(__dirname, '../../../data/inputFiles', file);
      await fileInput.last().setInputFiles(filePath);
      // 8s, not 6s. XUI returns 429 when a document POST arrives within 5s of the previous
      // upload completing (rpx-xui-webapp api/documents/index.ts handleRequest), and it
      // DOUBLES that window on every 429 it issues, up to 180s. This sleep is what keeps
      // consecutive uploads apart, so at 6s there was only ~1s of margin against the 5s
      // threshold — and the penalty for losing that margin is exponential, not linear:
      // one 429 pushes the window to 10s, which makes the next upload more likely to 429
      // as well, cascading toward the ceiling. Widening the gap avoids the 429 rather than
      // trying to recover from it, which is the cheaper direction: the retry loop below
      // re-uploads, and each re-upload re-arms the window it is waiting on.
      let timeout = 8000;
      await performValidation('waitUntilElementDisappears', 'Uploading...');
      // Deliberately kept. "Uploading..." disappearing is not the end of the upload —
      // CCD is still committing the row, and documentsLR uploads two files in a loop, so
      // returning here lets the next "Add new" build on a half-finished row. Removing this
      // failed 5 documentsLR tests in PR-2581 (36.2m, 7 failed) against a 22.8m / 1 failed
      // control. Waiting on the Cancel upload button's disabled state did not fix it
      // either. Needs the documentsLR upload lifecycle sorted out first.
      await page.waitForTimeout(timeout);
      // The while loop already retries until the rate-limit message clears, so toPass only
      // bounded it. Its 60s budget was too small for the backoff it wrapped: the sleeps
      // double 12s, 24s, 48s, so the third retry passes 84s cumulative and toPass kills it
      // mid-sleep, reported as "Timeout 60000ms exceeded while waiting on the predicate".
      // XUI's throttle doubles to a 180s ceiling (rpx-xui-webapp api/documents/index.ts),
      // so cap the attempts to match rather than fail part-way through waiting it out.
      const rateLimit = page.locator(`label:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload"),
                                        span:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload")`);
      const maxRateLimitRetries = 5;
      for (let attempt = 0; attempt < maxRateLimitRetries; attempt++) {
        // count() does not poll, and the banner renders a moment after the upload POST
        // returns, so give it a brief chance to appear before concluding we are clear.
        const rateLimited = await rateLimit
          .first()
          .waitFor({ state: 'visible', timeout: 1000 })
          .then(() => true)
          .catch(() => false);
        if (!rateLimited) {
          return;
        }
        timeout = Math.min(timeout * 2, MAX_UPLOAD_BACKOFF);
        await page.waitForTimeout(timeout);
        await fileInput.last().setInputFiles(filePath);
        await performValidation('waitUntilElementDisappears', 'Uploading...');
      }
      await expect(rateLimit, 'upload was still rate limited after retrying with backoff').toHaveCount(0);
    }
  }
