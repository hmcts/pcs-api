  import { expect, Page } from '@playwright/test';
  import path from 'path';
  import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
  import { performAction, performValidation } from '@utils/controller';
  // Matches the 180s ceiling XUI's own upload throttle doubles up to.
  export const MAX_UPLOAD_BACKOFF = 180000;
  export const MAX_CUMULATIVE_BACKOFF = 60000;
  // XUI 429s a POST within 5s of the previous upload completing, so consecutive uploads need a gap
  // with margin; the rest of it is deferred to whoever uploads next.
  export const UPLOAD_GAP = 8000;
  export const POST_UPLOAD_SETTLE = 2000;

  // Module-level: the throttle is per XUI session, which spans the whole spec, and Playwright builds
  // a fresh action instance per call. Shared with uploadADocument, the other path into the same
  // session, so separate timestamps would each compute a gap the other had already partly spent.
  let lastUploadCompletedAt = 0;

  export function markUploadCompleted(): void {
    lastUploadCompletedAt = Date.now();
  }

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
      // Normalised to a list: an array reaching path.resolve throws, and an object with no `files`
      // key matched no branch and uploaded nothing.
      const list = this.toFileList(files);
      if (list.length === 0) {
        // Warn rather than throw: two call sites pass the file unguarded.
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
      // The console truncates assertion values, so log the full error and page state.
      try {
        await performAction('clickButton', 'Add new');
      } catch (error) {
        const addNewCount = await page.getByRole('button', { name: 'Add new' }).count().catch(() => -1);
        const heading = await page.locator('h1').first().innerText().catch(() => '<no heading>');
        const message = error instanceof Error ? `${error.message}\n${error.stack ?? ''}` : String(error);
        console.warn(`[uploadFile] "Add new" failed for "${file}" on page "${heading}" with `
          + `${addNewCount} "Add new" button(s) present:\n${message.slice(0, 1500)}`);
        throw error;
      }
      const fileInput = page.locator('input[type="file"].form-control.bottom-30');
      const filePath = path.resolve(__dirname, '../../../data/inputFiles', file);
      // Before the POST, not after: work done between two uploads then counts towards the gap and a
      // trailing upload pays nothing.
      await waitForUploadWindow(page);
      let timeout = UPLOAD_GAP;
      await fileInput.last().setInputFiles(filePath);
      await performValidation('waitUntilElementDisappears', 'Uploading...');
      // "Uploading..." going does not mean CCD has committed the row.
      await page.waitForTimeout(POST_UPLOAD_SETTLE);
      markUploadCompleted();
      const rateLimit = page.locator(`label:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload"),
                                        span:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload")`);
      // Budget the total backoff rather than the attempt count: early short sleeps still absorb a
      // transient 429, but a doomed upload fails in ~1m instead of 7 and Playwright retries sooner.
      let backoffSpent = 0;
      while (backoffSpent < MAX_CUMULATIVE_BACKOFF) {
        // count() does not poll and the banner renders after the POST returns, so wait briefly.
        const rateLimited = await rateLimit
          .first()
          .waitFor({ state: 'visible', timeout: 1000 })
          .then(() => true)
          .catch(() => false);
        if (!rateLimited) {
          return;
        }
        // Clamped to the remaining budget too, or the last retry overshoots it: 16+32 then 64
        // against a 60s budget spent 112s.
        timeout = Math.min(timeout * 2, MAX_UPLOAD_BACKOFF, MAX_CUMULATIVE_BACKOFF - backoffSpent);
        backoffSpent += timeout;
        await page.waitForTimeout(timeout);
        await fileInput.last().setInputFiles(filePath);
        await performValidation('waitUntilElementDisappears', 'Uploading...');
        markUploadCompleted();
      }
      await expect(rateLimit, 'upload was still rate limited after retrying with backoff').toHaveCount(0);
    }
  }
