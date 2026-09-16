import { expect, Page } from '@playwright/test';
import path from 'path';
import { actionData, actionRecord, IAction } from '@utils/interfaces/action.interface';
import { performAction, performValidation } from '@utils/controller';
import { SHORT_TIMEOUT, VERY_LONG_TIMEOUT } from 'playwright.config';

export class UploadFileAction implements IAction {
  async execute(page: Page, action: string, files: actionData | actionRecord): Promise<void> {
    if (typeof files === 'string') {
      await this.uploadFile(page, files);
    } else if (Array.isArray(files)) {
      for (const [index, file] of files.entries()) {
        await this.uploadFile(page, file);
        if (index === files.length - 1) break;
      }
    } else if (typeof files === 'object' && 'files' in files) {
      await this.uploadFile(page, files.files as string);
    }
  }

  private async uploadFile(page: Page, file: string): Promise<void> {
    const fileInputs = page.locator('input[type="file"].form-control.bottom-30');

    const countBeforeClick = await fileInputs.count();

    await performAction('clickButton', 'Add new');

    await expect(async () => {
      const currentCount = await fileInputs.count();

      if (currentCount === 0) {
        await performAction('clickButton', 'Add new');
        await expect(fileInputs.first()).toBeVisible({timeout: SHORT_TIMEOUT,});
      } else {
        await expect(fileInputs).toHaveCount(countBeforeClick + 1);
        await expect(fileInputs.nth(countBeforeClick)).toBeVisible({timeout: SHORT_TIMEOUT,});
      }
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });

    const filePath = path.resolve(__dirname, '../../../data/inputFiles', file);
    await fileInputs.last().setInputFiles(filePath);
    let timeout = 6000;
    await performValidation('waitUntilElementDisappears', 'Uploading...');
    await page.waitForTimeout(timeout);
    await expect(async () => {
      const rateLimit = page.locator(`label:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload"),
                                          span:text-is("Your request was rate limited. Please wait a few seconds before retrying your document upload")`);
      let limit = await rateLimit.count();

      while (limit > 0) {
        timeout *= 2;
        await page.waitForTimeout(timeout);
        await fileInputs.last().setInputFiles(filePath);
        await performValidation('waitUntilElementDisappears', 'Uploading...');
        limit = await rateLimit.count();
      };
    }).toPass({
      timeout: VERY_LONG_TIMEOUT,
    });
  }
}
