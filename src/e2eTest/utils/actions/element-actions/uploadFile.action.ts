import { Page } from '@playwright/test';
import path from 'path';
import { IAction } from '@utils/interfaces/action.interface';

export class UploadFileAction implements IAction {
  async execute(page: Page, action: string, params: string): Promise<void> {
    const fileInput = page.locator('input[type="file"].form-control.bottom-30');
    const filePath = path.resolve(__dirname, '../../../data/inputFiles', params);
    await fileInput.last().setInputFiles(filePath);
  }
}
