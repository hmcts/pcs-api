  import { expect, Page } from '@playwright/test';
  import path from 'path';
  import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
  import { performAction, performValidation } from '@utils/controller';
  // Matches the 180s ceiling XUI's own upload throttle doubles up to.
  export const MAX_UPLOAD_BACKOFF = 180000;

  export class UploadFileAction implements IAction {
    async execute(page: Page, action: string, files: actionData | actionRecord): Promise<void> {
      // Normalised to a list: `{files: [...]}` used to reach path.resolve as an array (throws),
      // and an object with no `files` key matched no branch and uploaded nothing silently.
      const list = this.toFileList(files);
      if (list.length === 0) {
        // Warn rather than throw: two call sites pass the file unguarded, and every current caller
        // does supply one, so throwing would break an optional upload that used to be skipped.
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
      // Diagnostic: caseWorkerGenApps:54 fails here intermittently (on master too) and the console
      // truncates the values, so log the full error plus page state.
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
      await fileInput.last().setInputFiles(filePath);
      // 8s, not 6s: XUI 429s a POST arriving within 5s of the previous upload completing and
      // DOUBLES that window per 429 up to 180s, so ~1s of margin was too little.
      let timeout = 8000;
      await performValidation('waitUntilElementDisappears', 'Uploading...');
      // Kept: "Uploading..." going does not mean CCD has committed the row. Removing this failed
      // 5 documentsLR tests in PR-2581.
      await page.waitForTimeout(timeout);
      // Bounded loop rather than a toPass: the doubling sleeps reach 84s cumulative, which outgrew
      // the old 60s wrapper. Capped to match XUI throttle ceiling of 180s.
      const rateLimit = page.locator(`label:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload"),
                                        span:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload")`);
      const maxRateLimitRetries = 5;
      for (let attempt = 0; attempt < maxRateLimitRetries; attempt++) {
        // count() does not poll and the banner renders after the POST returns, so wait briefly.
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
