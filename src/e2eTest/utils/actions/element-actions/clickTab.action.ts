import { Page } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { waitForSpinner } from '@utils/common/locator.utils';

export class ClickTabAction implements IAction {
  async execute(page: Page, action: string, tabName: string): Promise<void> {
    // Every tab switch re-fetches the case, so the previous switch's spinner is often still up.
    // Timing only — see controller.ts for why the CaseFile View tab is under suspicion.
    const spinnerStart = Date.now();
    await waitForSpinner(page);
    const spinnerMs = Date.now() - spinnerStart;

    const locator = page.getByRole('tab', { name: tabName })
      .or(page.getByRole('link', { name: tabName }));

    const findStart = Date.now();
    await locator.first().waitFor({ state: 'visible' });
    const findMs = Date.now() - findStart;
    const clickStart = Date.now();
    await locator.first().click();
    console.log(`[clickTab] "${tabName}": spinner ${spinnerMs}ms, find ${findMs}ms, `
      + `click ${Date.now() - clickStart}ms`);
  }
}
