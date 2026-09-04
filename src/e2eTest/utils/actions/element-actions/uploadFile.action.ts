  import { expect, Page } from '@playwright/test';
  import path from 'path';
  import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
  import { performAction, performValidation } from '@utils/controller';
  import { VERY_LONG_TIMEOUT } from 'playwright.config';

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
      let timeout = 6000;
      await performValidation('waitUntilElementDisappears', 'Uploading...');
      // Deliberately kept. "Uploading..." disappearing is not the end of the upload —
      // CCD is still committing the row, and documentsLR uploads two files in a loop, so
      // returning here lets the next "Add new" build on a half-finished row. Removing this
      // failed 5 documentsLR tests in PR-2581 (36.2m, 7 failed) against a 22.8m / 1 failed
      // control. Waiting on the Cancel upload button's disabled state did not fix it
      // either. Needs the documentsLR upload lifecycle sorted out first.
      await page.waitForTimeout(timeout);
      await expect(async () => {
        const rateLimit = page.locator(`label:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload"),
                                          span:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload")`);
        let limit = await rateLimit.count();

        while (limit > 0) {
          timeout *= 2;
          await page.waitForTimeout(timeout);
          await fileInput.last().setInputFiles(filePath);
          await performValidation('waitUntilElementDisappears', 'Uploading...');
          limit = await rateLimit.count();
        };
      }).toPass({
        timeout: VERY_LONG_TIMEOUT,
      });
    }
  }
