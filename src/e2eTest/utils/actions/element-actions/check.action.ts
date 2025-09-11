import { Page } from '@playwright/test';
import { actionData, IAction } from '../../interfaces/action.interface';

export class CheckAction implements IAction {
  async execute(page: Page, action: string, fieldName: actionData): Promise<void> {
    if (fieldName === 'string') {
      await this.clickCheckBox(page, fieldName);
    } else {
      for (const option of fieldName as string[]) {
        await this.clickCheckBox(page, option);
      }
    }
  }

  private async clickCheckBox(page: Page, checkbox: string) {
    await page.locator(`input[type="checkbox"]+:has-text("${checkbox}")`).check();
  }
}
