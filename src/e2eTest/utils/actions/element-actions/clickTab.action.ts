import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { waitForSpinner } from '@utils/common/locator.utils';

export class ClickTabAction implements IAction {
  async execute(page: Page, action: string, tabName: string): Promise<void> {
    // Tab clicks are the most spinner-exposed of all: every tab switch on case-details
    // makes CCD re-fetch the case, so the previous switch's spinner is often still up.
    await waitForSpinner(page);

    const locator = page.getByRole('tab', { name: tabName })
      .or(page.getByRole('link', { name: tabName }));

    await locator.first().waitFor({ state: 'visible' });
    await locator.first().click();
  }
}
