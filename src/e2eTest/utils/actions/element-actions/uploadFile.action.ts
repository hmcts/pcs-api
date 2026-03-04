import { expect, Page } from '@playwright/test';
import path from 'path';
import { actionData, IAction } from '@utils/interfaces/action.interface';
import { performAction, performValidation } from '@utils/controller';
import { LONG_TIMEOUT, SHORT_TIMEOUT } from 'playwright.config';

export class UploadFileAction implements IAction {
  async execute(page: Page, action: string, files: actionData): Promise<void> {
    if (typeof files === 'string') {
      await this.uploadFile(page, files);
    } else if (Array.isArray(files)) {
      for (const [index, file] of files.entries()) {
        await this.uploadFile(page, file);
        if (index === files.length - 1) break;
      }
    }
  }

  private async uploadFile(page: Page, file: string): Promise<void> {
    await performAction('clickButton', 'Add new');
    const fileInput = page.locator('input[type="file"].form-control.bottom-30');
    const filePath = path.resolve(__dirname, '../../../data/inputFiles', file);
    await fileInput.last().setInputFiles(filePath);

    await performValidation('waitUntilElementDisappears', 'Uploading...');
    await expect(async () => {
      const rateLimit = page.locator(`label:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload"),
                                         span:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload")`);
      let limit = await rateLimit.count();

      while (limit > 0) {
        await page.waitForTimeout(8000);
        await fileInput.last().setInputFiles(filePath);
        await performValidation('waitUntilElementDisappears', 'Uploading...');
        limit = await rateLimit.count();
      };
    }).toPass({
      timeout: LONG_TIMEOUT,
    });
  }
}
