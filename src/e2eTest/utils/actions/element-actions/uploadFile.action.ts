import {Page} from '@playwright/test';
import path from 'path';
import {actionData, IAction} from '@utils/interfaces/action.interface';
import {performAction} from '@utils/controller';

export class UploadFileAction implements IAction {
  async execute(page: Page, action: string, files: actionData): Promise<void> {
    if (typeof files == 'string') {
      await this.uploadFile(page, files);
    } else if (Array.isArray(files)) {
      for (const file of files) {
        await this.uploadFile(page, file);
      }
    }
  }

  private async uploadFile(page: Page, file: string): Promise<void> {
    await performAction('clickButton', 'Add new');
    const fileInput = page.locator('input[type="file"].form-control.bottom-30');
    const filePath = path.resolve(__dirname, '../../../data/inputFiles', file);
    await fileInput.last().setInputFiles(filePath);
  }
}
