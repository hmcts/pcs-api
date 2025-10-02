import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, buttonText: string , actionParams: string): Promise<void> {
    const button = page.locator(`button:text-is("${buttonText}"),
                                  [value="${buttonText}"],
                                  :has-text("${buttonText}") + button,
                                  :has-text("${buttonText}") ~ button,
                                  a >> text=${buttonText}`);
    const actionsMap = new Map<string, () => Promise<void>>([
      ['clickButton', () => this.clickButton(page, button)],
      ['clickButtonAndVerifyPageNavigation', () => this.clickButtonAndVerifyPageNavigation(page, button, actionParams)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async clickButton(page: Page, button: Locator): Promise<void> {
    await button.first().click();
    page.locator('.spinner-container').waitFor({state: 'hidden'});
  }

  private async clickButtonAndVerifyPageNavigation(page: Page, button: Locator, nextPageTitle: string): Promise<void> {
    for (let i = 0; i < 2; i++) {
      await button.click();
      page.locator('.spinner-container').waitFor({state: 'hidden'});
      if (await page.locator('h1, h1.govuk-heading-xl, h1.govuk-heading-l').textContent() === nextPageTitle) break;
      if (i === 1) throw new Error(`Navigation to ${nextPageTitle} page has failed`);
    }
  }
}
