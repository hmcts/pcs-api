  import { expect, Page } from '@playwright/test';
  import path from 'path';
  import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
  import { performAction, performValidation } from '@utils/controller';
  // Matches the 180s ceiling XUI's own upload throttle doubles up to.
  export const MAX_UPLOAD_BACKOFF = 180000;
  // The gap consecutive uploads need between them. See uploadFile for the derivation.
  export const UPLOAD_GAP = 8000;
  // Paid after every upload so CCD can commit the row before anything else touches it. The
  // rest of the gap is deferred to whoever uploads next.
  export const POST_UPLOAD_SETTLE = 2000;
  // When the last upload finished, so the next one can top the gap up rather than restart it.
  // Module-level on purpose: the throttle is per XUI session, which spans the whole spec, and
  // Playwright constructs a fresh action instance per call. Shared with uploadADocument in
  // caseManagement.action.ts — that is a second upload path against the same session, so if it
  // kept its own timestamp each would compute a gap the other had already partly spent.
  let lastUploadCompletedAt = 0;

  export function markUploadCompleted(): void {
    lastUploadCompletedAt = Date.now();
  }

  // Sleep only the part of the gap that has not already elapsed since the last upload finished.
  // Logs what it skipped so the saving is measurable from the console rather than inferred from
  // the suite total — that is how the cost of the indexed-field wait stayed invisible.
  export async function waitForUploadWindow(page: Page): Promise<void> {
    if (lastUploadCompletedAt === 0) {
      return;
    }
    const elapsed = Date.now() - lastUploadCompletedAt;
    const remaining = UPLOAD_GAP - elapsed;
    if (remaining <= 0) {
      console.log(`[uploadFile] gap already elapsed (${elapsed}ms since last upload), not sleeping`);
      return;
    }
    console.log(`[uploadFile] topping up gap by ${remaining}ms (${elapsed}ms already elapsed)`);
    await page.waitForTimeout(remaining);
  }

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
        // Warn rather than throw. Two call sites are not guarded on the file itself —
        // createCaseWales requiredDocumentsUpload keys off `reqDocs.option === 'Yes'`, and
        // provideDetailsOfRentArrears passes `rentArrearsData.files` unguarded. Every current
        // caller does supply a file, so a throw is unreachable today, but it would convert a
        // silently-skipped optional upload into a hard failure for the first caller that
        // wanted one. The warning keeps the previous behaviour while making it visible,
        // which is the actual defect: this used to return with no trace at all.
        console.warn(`[uploadFile] no file to upload — received ${JSON.stringify(files)}; skipping`);
        return;
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
      // XUI returns 429 when a document POST arrives within 5s of the previous upload
      // COMPLETING (rpx-xui-webapp api/documents/index.ts handleRequest), and it DOUBLES that
      // window on every 429 it issues, up to 180s. At a 6s gap there was only ~1s of margin,
      // and the penalty for losing it is exponential rather than linear — one 429 pushes the
      // window to 10s, making the next upload more likely to 429 too, cascading to the
      // ceiling. So the 8s gap stays; avoiding the 429 is far cheaper than recovering from it.
      //
      // What changes is WHERE it is spent. It used to be a flat 8s sleep after every upload,
      // paid at all 65 call sites whether or not another upload followed. But the window XUI
      // measures runs from the previous upload's completion, so any test work in between —
      // navigation, form filling, assertions, the "Add new" click above — already counts
      // toward it. Sleeping the full 8s after each upload pays for a gap that has usually
      // elapsed on its own by the time the next upload arrives.
      //
      // So top the gap up here instead, immediately before the POST. Consecutive uploads in a
      // tight loop still get the full 8s; uploads separated by real work sleep only the
      // remainder, or nothing.
      await waitForUploadWindow(page);
      let timeout = UPLOAD_GAP;
      await fileInput.last().setInputFiles(filePath);
      await performValidation('waitUntilElementDisappears', 'Uploading...');
      // "Uploading..." disappearing is not the end of the upload — CCD is still committing the
      // row, and documentsLR uploads two files in a loop, so returning immediately lets the
      // next "Add new" build on a half-finished row. Removing the post-upload wait outright
      // failed 5 documentsLR tests in PR-2581 (36.2m, 7 failed) against a 22.8m / 1 failed
      // control, and waiting on the Cancel button's disabled state did not fix it either.
      // Hence a short settle is still paid here rather than deferred: the row-commit race
      // affects whatever runs next, not only the next upload.
      await page.waitForTimeout(POST_UPLOAD_SETTLE);
      markUploadCompleted();
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
        // A retried upload re-arms the window too, so record it — otherwise the next call site
        // would compute its gap from the first attempt and post too early.
        markUploadCompleted();
      }
      await expect(rateLimit, 'upload was still rate limited after retrying with backoff').toHaveCount(0);
    }
  }
