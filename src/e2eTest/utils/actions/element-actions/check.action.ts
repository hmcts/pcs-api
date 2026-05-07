import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';

export class CheckAction implements IAction {
  async execute(page: Page, action: string, params: string | actionRecord): Promise<void> {
    if (typeof params === 'string') {
      await this.clickCheckBox(page, params);
    } else if (Array.isArray(params)) {
      for (const option of params) {
        await this.clickCheckBox(page, option);
      }
    } else {
      const fieldset = page.locator('fieldset', {has: page.getByText(params.question as string, { exact: true })});
      if (Array.isArray(params.option)) {
        for (const opt of params.option) {
          await fieldset.getByRole('checkbox', { name: opt, exact: true }).check();
        }
      } else {
        await fieldset.getByRole('checkbox', { name: params.option as string, exact: true }).check();
      }
    }
  }

  private async clickCheckBox(page: Page, label: string) {
    await page.getByLabel(label, { exact: true }).check();
  }
}
