import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';

export class CheckAction implements IAction {
  async execute(page: Page, action: string, params: string | string[] | actionRecord): Promise<void> {
    if (typeof params === 'string') {
      await this.clickCheckBox(page, params);
    } else if (Array.isArray(params)) {
      for (const option of params) {
        await this.clickCheckBox(page, option);
      }
    } else {
      const fieldset = page.locator(`legend:has-text("${params.question}")`).locator('..');

      if (Array.isArray(params.option)) {
        for (const opt of params.option) {
          await fieldset.getByRole('checkbox', { name: opt }).check();
        }
      } else {
        await fieldset.getByRole('checkbox', { name: params.option as string }).check();
      }
    }
  }

  private async clickCheckBox(page: Page, label: string) {
    const checkbox = page.locator(`input[type="checkbox"] + label:text-is("${label}")`);
    //const checkbox = page.locator(`input[type="checkbox"] + label:has-text("${label}")`);
    //const checkbox = page.locator(`input[type="checkbox"] + :has-text("${label}")`);
    //const checkbox = page.getByText("Continue", { exact: true }) // strict
    await checkbox.click();
  }
}
