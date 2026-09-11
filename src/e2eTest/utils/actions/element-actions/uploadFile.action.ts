  import { expect, Page } from '@playwright/test';
  import path from 'path';
  import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
  import { performAction, performValidation } from '@utils/controller';
  import { VERY_SHORT_TIMEOUT } from 'playwright.config';
  export const MAX_UPLOAD_BACKOFF = 180000;
  export const MAX_CUMULATIVE_BACKOFF = 60000;
  // XUI no longer rate limits a document POST that follows the previous one closely, so this only
  // seeds the retry backoff. POST_UPLOAD_SETTLE stays: it covers CCD committing the row, and
  // removing it broke three specs on #2741.
  export const UPLOAD_GAP = Number(process.env.E2E_UPLOAD_GAP_MS ?? 2000);
  export const POST_UPLOAD_SETTLE = 2000;

// Module-level: shared with uploadADocument, which uses the same XUI session.
  let lastUploadCompletedAt = 0;

  export function markUploadCompleted(): void {
    lastUploadCompletedAt = Date.now();
  }

  const RATE_LIMIT_TEXT = 'Your request was rate limited. Please wait a few seconds before retrying your document upload';

  export function rateLimitBanner(page: Page) {
    return page.locator(`label:text-is("${RATE_LIMIT_TEXT}"), span:text-is("${RATE_LIMIT_TEXT}")`);
  }

  export async function waitForUploadWindow(page: Page): Promise<void> {
    if (lastUploadCompletedAt === 0) {
      return;
    }
    const remaining = UPLOAD_GAP - (Date.now() - lastUploadCompletedAt);
    if (remaining > 0) {
      await page.waitForTimeout(remaining);
    }
  }

  export class UploadFileAction implements IAction {
    async execute(page: Page, action: string, files: actionData | actionRecord): Promise<void> {
      // Normalised: an array reaching path.resolve throws, and a `files`-less object uploads nothing.
      const list = this.toFileList(files);
      if (list.length === 0) {
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
      await waitForUploadWindow(page);
      // Seeded independently of UPLOAD_GAP: a zero gap would leave timeout * 2 at zero.
      let timeout = 2000;
// Nothing dismisses these banners, so only a NEW one belongs to this upload.
      const bannersBefore = await rateLimitBanner(page).count();
      await fileInput.last().setInputFiles(filePath);
      await performValidation('waitUntilElementDisappears', 'Uploading...');
      // "Uploading..." going does not mean CCD has committed the row.
      await page.waitForTimeout(POST_UPLOAD_SETTLE);
      markUploadCompleted();
      const rateLimit = rateLimitBanner(page);
      // Budget the total backoff, not the attempt count: a doomed upload then fails in ~1m, not 7.
      let backoffSpent = 0;
      while (backoffSpent < MAX_CUMULATIVE_BACKOFF) {
        // A NEW banner means this upload was throttled; the count does not poll, so allow the render.
        await page.waitForTimeout(VERY_SHORT_TIMEOUT);
        if ((await rateLimit.count()) <= bannersBefore) {
          return;
        }
        // Clamped to the remaining budget, or the last retry overshoots it.
        timeout = Math.min(timeout * 2, MAX_UPLOAD_BACKOFF, MAX_CUMULATIVE_BACKOFF - backoffSpent);
        backoffSpent += timeout;
        await page.waitForTimeout(timeout);
        await fileInput.last().setInputFiles(filePath);
        await performValidation('waitUntilElementDisappears', 'Uploading...');
        markUploadCompleted();
      }
      await expect(rateLimit, 'upload was still rate limited after retrying with backoff')
        .toHaveCount(bannersBefore);
    }
  }
