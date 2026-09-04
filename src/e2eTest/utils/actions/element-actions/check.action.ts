import { Page } from '@playwright/test';
import { actionRecord, IAction } from '../../interfaces/action.interface';
import { waitForSpinner } from '@utils/common/locator.utils';

export class CheckAction implements IAction {
  async execute(page: Page, action: string, params: string | actionRecord): Promise<void> {
    await waitForSpinner(page);
    if (typeof params === 'string') {
      await this.clickCheckBox(page, params, action);
    } else if (Array.isArray(params)) {
      for (const option of params) {
        await this.clickCheckBox(page, option, action);
      }
    } else if (typeof params.label === 'string') {
      await this.clickCheckBox(page, params.label);
    } else {
      const fieldset = page.locator('fieldset', {
        has: page.getByText(params.question as string, { exact: true })
      });

      if (Array.isArray(params.option)) {
        for (const opt of params.option) {
          const checkbox = fieldset.getByRole('checkbox', { name: opt, exact: true });
          action === 'uncheck'
            ? await checkbox.uncheck()
            : await checkbox.check();
        }
      } else {
        const checkbox = fieldset.getByRole('checkbox', {
          name: params.option as string,
          exact: true
        });

        action === 'uncheck'
          ? await checkbox.uncheck()
          : await checkbox.check();
      }
    }
  }

  private async clickCheckBox(page: Page, label: string, action: string) {
    const checkbox = page.getByLabel(label, { exact: true });

    action === 'uncheck'
      ? await checkbox.uncheck()
      : await checkbox.check();
  }
}
