import { Page, Locator } from '@playwright/test';
import { IAction } from '../../interfaces/action.interface';
import { waitForPageRedirectionTimeout } from '../../../playwright.config';

export class ClickButtonAction implements IAction {
  async execute(page: Page, action: string, buttonText: string , actionParams: string): Promise<void> {
    const button = page.locator(`button:text-is("${buttonText}"),
                                  [value="${buttonText}"],
                                  :has-text("${buttonText}") + button,
                                  :has-text("${buttonText}") ~ button,
                                  a >> text=${buttonText}`);
    const actionsMap = new Map<string, () => Promise<void>>([
      ['clickButton', () => this.clickButton(page, button)],
      ['clickButtonAndVerifyPageNavigation', () => this.clickButtonAndVerifyPageNavigation(page, button, actionParams)],
      ['verifyPageAndClickButton', () => this.verifyPageAndClickButton(page, actionParams, button)]
    ]);
    const actionToPerform = actionsMap.get(action);
    if (!actionToPerform) throw new Error(`No action found for '${action}'`);
    await actionToPerform();
  }

  private async clickButton(page: Page, button: Locator): Promise<void> {
    await button.first().click();
    await page.locator('.spinner-container').waitFor({state: 'detached'});
  }

  private async clickButtonAndVerifyPageNavigation(page: Page, button: Locator, nextPageElement: string): Promise<void> {
    const pageElement = page.locator(`h1:has-text("${nextPageElement}")`);
    const maxRetries = 3;
    let attempt = 0;
    let isVisible = false;
    do {
      attempt++;
      await this.clickButton(page, button);
      //Adding sleep to slow down execution when the application behaves abnormally
      await page.waitForTimeout(waitForPageRedirectionTimeout);
      isVisible = await pageElement.isVisible();
    } while (!isVisible && attempt < maxRetries);
    if (!isVisible) {
      throw new Error(`Navigation to "${nextPageElement}" page/element failed after ${attempt} attempts`);
    }
  }

  private async verifyPageAndClickButton(page: Page, currentPageHeader: string, button: Locator): Promise<void> {
    await page.locator('.spinner-container').waitFor({state: 'detached'});
    if(await page.locator('h1,h1.govuk-heading-xl, h1.govuk-heading-l').textContent() === currentPageHeader){
      await this.clickButton(page, button);
    }
  }
}
