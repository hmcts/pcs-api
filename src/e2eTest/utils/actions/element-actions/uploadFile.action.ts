  import { expect, Page } from '@playwright/test';
  import path from 'path';
  import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
  import { performAction, performValidation } from '@utils/controller';
  // Matches the 180s ceiling XUI's own upload throttle doubles up to.
  export const MAX_UPLOAD_BACKOFF = 180000;

  export class UploadFileAction implements IAction {
    async execute(page: Page, action: string, files: actionData | actionRecord): Promise<void> {
      // Normalised to a list first. Two defects in the previous shape, both verified:
      //
      // - `{files: [...]}` passed the array straight to uploadFile(file: string), and
      //   path.resolve throws on an array: 'The "paths[1]" argument must be of type string.
      //   Received an instance of Array'. Only the string form of `files` is used today, so
      //   this was a trap rather than a live failure — enterGenAppUploadRelatedEvidence's
      //   value is a single string while other page data of the same name is an array.
      // - an object without a `files` key matched no branch at all, so the action returned
      //   silently having uploaded nothing, and the failure surfaced later as a missing
      //   document.
      const list = this.toFileList(files);
      if (list.length === 0) {
        throw new Error(`uploadFile: no file given (received ${JSON.stringify(files)})`);
      }
      for (const file of list) {
        await this.uploadFile(page, file);
      }
    }

    private toFileList(files: actionData | actionRecord): string[] {
      if (typeof files === 'string') {
        return [files];
      }
      if (Array.isArray(files)) {
        return files.map(String);
      }
      if (typeof files === 'object' && files !== null && 'files' in files) {
        const inner = (files as actionRecord).files;
        if (typeof inner === 'string') {
          return [inner];
        }
        if (Array.isArray(inner)) {
          return inner.map(String);
        }
      }
      return [];
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
